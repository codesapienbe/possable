package com.possable.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.possable.model.PrintJobEntity;
import com.possable.repository.PrintJobRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Service
public class PrintJobService {

    private static final Logger log = LoggerFactory.getLogger(PrintJobService.class);

    private final List<PrintJob> inMemoryJobs = Collections.synchronizedList(new ArrayList<>());
    private final PrinterService printerService;
    private final PrintTemplateService templateService;
    private final OrderService orderService;
    private final ItemService itemService;
    private final TaskExecutor taskExecutor;
    private final List<EmitterState> emitterStates = Collections.synchronizedList(new ArrayList<>());
    private static final long MIN_INTERVAL_MS = 200; // max ~5 events/sec per connection
    private final Counter collapsedEventsCounter;
    private final Counter droppedEmittersCounter;
    private final Counter totalSentCounter;
    private final MeterRegistry meterRegistry;
    private final PrintJobRepository printJobRepository;

    private class EmitterState {
        final SseEmitter emitter;
        final Set<String> topics;
        volatile long lastSentMs = 0L;
        volatile String pendingEvent = null;
        volatile List<String> pendingEventTopics = null;

        EmitterState(SseEmitter emitter, Set<String> topics) {
            this.emitter = emitter;
            this.topics = topics;
        }

        boolean isSubscribedToAny(List<String> eventTopics) {
            if (topics.contains("all")) return true;
            for (String t : eventTopics) {
                if (topics.contains(t)) return true;
            }
            return false;
        }

        synchronized void sendOrQueueIfSubscribed(String eventJson, List<String> eventTopics) {
            if (!isSubscribedToAny(eventTopics)) return;
            long now = System.currentTimeMillis();
            if (now - lastSentMs >= MIN_INTERVAL_MS) {
                trySend(eventJson, eventTopics);
            } else {
                // keep only latest pending event
                pendingEvent = eventJson;
                collapsedEventsCounter.increment();
                // per-topic collapsed metrics
                if (eventTopics != null) {
                    for (String t : eventTopics) {
                        meterRegistry.counter("possable.sse.collapsed", "component", "print-job-service", "topic", t).increment();
                    }
                }
                pendingEventTopics = eventTopics;
                long delay = MIN_INTERVAL_MS - (now - lastSentMs);
                scheduleFlush(delay);
            }
        }

        private void scheduleFlush(long delayMs) {
            taskExecutor.execute(() -> {
                try { Thread.sleep(Math.max(1, delayMs)); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                String toSend;
                List<String> topicsToSend;
                synchronized (this) {
                    toSend = pendingEvent;
                    topicsToSend = pendingEventTopics;
                    pendingEvent = null;
                    pendingEventTopics = null;
                }
                if (toSend != null) {
                    trySend(toSend, topicsToSend);
                }
            });
        }

        private void trySend(String eventJson, List<String> eventTopics) {
            try {
                emitter.send(SseEmitter.event().name("print-job").data(eventJson));
                lastSentMs = System.currentTimeMillis();
                totalSentCounter.increment();
                // per-topic total sent
                if (eventTopics != null) {
                    for (String t : eventTopics) {
                        meterRegistry.counter("possable.sse.sent", "component", "print-job-service", "topic", t).increment();
                    }
                }
            } catch (Exception ex) {
                // on send failure remove emitter from list
                emitterStates.remove(this);
                droppedEmittersCounter.increment();
                // per-topic dropped emitters metric for emitter subscriptions
                try {
                    for (String sub : topics) {
                        meterRegistry.counter("possable.sse.dropped_emitters", "component", "print-job-service", "topic", sub).increment();
                    }
                } catch (Exception ignore) {}
            }
        }
    }

    public record PrintJob(String id, String orderId, String printerId, String templateId, String status, Instant createdAt) {}

    @Autowired
    public PrintJobService(PrinterService printerService, PrintTemplateService templateService, OrderService orderService, ItemService itemService, TaskExecutor taskExecutor, MeterRegistry meterRegistry, PrintJobRepository printJobRepository) {
        this.printerService = printerService;
        this.templateService = templateService;
        this.orderService = orderService;
        this.itemService = itemService;
        this.taskExecutor = taskExecutor;
        this.meterRegistry = meterRegistry;
        this.printJobRepository = printJobRepository;
        // register global counters with a consistent metric namespace and component tag
        this.collapsedEventsCounter = meterRegistry.counter("possable.sse.collapsed", "component", "print-job-service");
        this.droppedEmittersCounter = meterRegistry.counter("possable.sse.dropped_emitters", "component", "print-job-service");
        this.totalSentCounter = meterRegistry.counter("possable.sse.sent", "component", "print-job-service");
    }

    // Backwards-compatible constructor used by existing tests and callers
    public PrintJobService(PrinterService printerService, PrintTemplateService templateService, TaskExecutor taskExecutor) {
        // provide a simple in-memory MeterRegistry fallback for tests and older callers
        this(printerService, templateService, null, null, taskExecutor, new SimpleMeterRegistry(), null);
    }

    private PrintJob toRecord(PrintJobEntity e) {
        if (e == null) return null;
        return new PrintJob(e.getId(), e.getOrderId(), e.getPrinterId(), e.getTemplateId(), e.getStatus(), e.getCreatedAt());
    }

    private PrintJobEntity toEntity(String orderId, String printerId, String templateId, String status) {
        PrintJobEntity e = new PrintJobEntity();
        e.setOrderId(orderId);
        e.setPrinterId(printerId);
        e.setTemplateId(templateId);
        e.setStatus(status);
        e.setCreatedAt(Instant.now());
        return e;
    }

    public List<PrintJob> listJobs(Map<String, String> filters) {
        if (printJobRepository != null) {
            int page = 0;
            int size = 100;
            if (filters != null) {
                try { if (filters.containsKey("page")) page = Math.max(0, Integer.parseInt(filters.get("page"))); } catch (Exception ignored) {}
                try { if (filters.containsKey("limit")) size = Math.max(1, Math.min(1000, Integer.parseInt(filters.get("limit")))); } catch (Exception ignored) {}
            }
            String orderId = filters != null ? filters.get("orderId") : null;
            String status = filters != null ? filters.get("status") : null;
            org.springframework.data.domain.Page<PrintJobEntity> pageRes;
            var pageable = org.springframework.data.domain.PageRequest.of(page, size);
            if (orderId != null && status != null) {
                pageRes = printJobRepository.findByOrderIdAndStatus(orderId, status, pageable);
            } else if (orderId != null) {
                pageRes = printJobRepository.findByOrderId(orderId, pageable);
            } else if (status != null) {
                pageRes = printJobRepository.findByStatus(status, pageable);
            } else {
                pageRes = printJobRepository.findAll(pageable);
            }
            return pageRes.stream().map(this::toRecord).collect(Collectors.toList());
        }
        synchronized (inMemoryJobs) {
            return inMemoryJobs.stream().filter(j -> filters == null || filters.isEmpty() || ((filters.get("orderId") == null || filters.get("orderId").equals(j.orderId())) && (filters.get("status") == null || filters.get("status").equals(j.status())))).collect(Collectors.toList());
        }
    }

    public PrintJob createJob(String orderId, String printerId, String templateId) {
        if (printJobRepository != null) {
            PrintJobEntity e = toEntity(orderId, printerId, templateId, "pending");
            PrintJobEntity saved = printJobRepository.save(e);
            PrintJob job = toRecord(saved);
            log.info("{\"message\":\"print_job_created\", \"print_job_id\":\"{}\", \"component\":\"print-job-service\"}", job.id());

            // Notify SSE clients and Vaadin broadcaster about created job
            String createdEvent = "{\"id\":\"" + job.id() + "\", \"status\":\"pending\"}";
            List<String> topics = List.of("all", "printer:" + printerId, "order:" + orderId, "job:" + job.id());
            sendSseEvent(createdEvent, topics);
            Broadcaster.broadcast(createdEvent);

            // Schedule processing
            CompletableFuture.runAsync(() -> processJob(job.id()), taskExecutor::execute).exceptionally(ex -> { log.error("print job processing failed for {}", job.id(), ex); return null; });
            return job;
        }

        String id = UUID.randomUUID().toString();
        PrintJob job = new PrintJob(id, orderId, printerId, templateId, "pending", Instant.now());
        inMemoryJobs.add(job);
        log.info("{\"message\":\"print_job_created\", \"print_job_id\":\"{}\", \"component\":\"print-job-service\"}", id);

        // Notify SSE clients and Vaadin broadcaster about created job
        String createdEvent = "{\"id\":\"" + id + "\", \"status\":\"pending\"}";
        List<String> topics = List.of("all", "printer:" + printerId, "order:" + orderId, "job:" + id);
        sendSseEvent(createdEvent, topics);
        Broadcaster.broadcast(createdEvent);

        // Schedule processing
        CompletableFuture.runAsync(() -> processJob(id), taskExecutor::execute).exceptionally(ex -> { log.error("print job processing failed for {}", id, ex); return null; });

        return job;
    }

    public void updateStatus(String jobId, String status) {
        if (printJobRepository != null) {
            var opt = printJobRepository.findById(jobId);
            if (opt.isPresent()) {
                PrintJobEntity e = opt.get();
                e.setStatus(status);
                PrintJobEntity saved = printJobRepository.save(e);
                log.info("{\"message\":\"print_job_status_updated\", \"print_job_id\":\"{}\", \"status\":\"{}\", \"component\":\"print-job-service\"}", jobId, status);
                String ev = "{\"id\":\"" + jobId + "\", \"status\":\"" + status + "\"}";
                List<String> evTopics = List.of("all", "printer:" + saved.getPrinterId(), "order:" + saved.getOrderId(), "job:" + saved.getId());
                sendSseEvent(ev, evTopics);
                Broadcaster.broadcast(ev);
                return;
            }
            return;
        }
        synchronized (inMemoryJobs) {
            for (int i = 0; i < inMemoryJobs.size(); i++) {
                var j = inMemoryJobs.get(i);
                if (j.id().equals(jobId)) {
                    inMemoryJobs.set(i, new PrintJob(j.id(), j.orderId(), j.printerId(), j.templateId(), status, j.createdAt()));
                    log.info("{\"message\":\"print_job_status_updated\", \"print_job_id\":\"{}\", \"status\":\"{}\", \"component\":\"print-job-service\"}", jobId, status);
                    String ev = "{\"id\":\"" + jobId + "\", \"status\":\"" + status + "\"}";
                    List<String> evTopics = List.of("all", "printer:" + j.printerId(), "order:" + j.orderId(), "job:" + j.id());
                    sendSseEvent(ev, evTopics);
                    Broadcaster.broadcast(ev);
                    return;
                }
            }
        }
    }

    private void processJob(String jobId) {
        // Render template and perform non-blocking "print" (log the output)
        PrintJob job = null;
        if (printJobRepository != null) {
            var opt = printJobRepository.findById(jobId);
            if (opt.isPresent()) job = toRecord(opt.get());
        } else {
            synchronized (inMemoryJobs) {
                job = inMemoryJobs.stream().filter(j -> j.id().equals(jobId)).findFirst().orElse(null);
            }
        }

        if (job == null) {
            log.error("{\"message\":\"print_job_not_found\", \"print_job_id\":\"{}\", \"component\":\"print-job-service\"}", jobId);
            return;
        }

        try {
            updateStatus(jobId, "printing");
            String printingEvent = "{\"id\":\"" + jobId + "\", \"status\":\"printing\"}";
            List<String> printingTopics = List.of("all", "printer:" + job.printerId(), "order:" + job.orderId(), "job:" + job.id());
            sendSseEvent(printingEvent, printingTopics);
            Broadcaster.broadcast(printingEvent);

            var template = templateService.findById(job.templateId());
            var order = orderService.findById(job.orderId());
            var printer = printerService.findById(job.printerId());

            if (template == null) {
                log.warn("{\"message\":\"template_missing\", \"template_id\":\"{}\", \"component\":\"print-job-service\"}", job.templateId());
                updateStatus(jobId, "failed");
                return;
            }
            if (order == null) {
                log.warn("{\"message\":\"order_missing\", \"order_id\":\"{}\", \"component\":\"print-job-service\"}", job.orderId());
                updateStatus(jobId, "failed");
                return;
            }

            // Build items list and total
            StringBuilder itemsBuilder = new StringBuilder();
            double total = 0.0;
            if (order.getItems() != null) {
                for (String itemId : order.getItems()) {
                    com.possable.service.ItemService.Item it = null;
                    if (itemService != null) {
                        it = itemService.findById(itemId);
                    }
                    if (it != null) {
                        itemsBuilder.append(it.name()).append(" x1, ");
                        total += it.price();
                    } else {
                        itemsBuilder.append(itemId).append(" x1, ");
                    }
                }
                if (itemsBuilder.length() > 2) itemsBuilder.setLength(itemsBuilder.length() - 2);
            }

            String rendered = template.content()
                    .replace("{{orderId}}", order.getId())
                    .replace("{{items}}", itemsBuilder.toString())
                    .replace("{{total}}", String.format("$%.2f", total))
                    .replace("{{notes}}", "");

            // Log the rendered content as the print output (structured)
            log.info("{\"message\":\"print_job_executed\", \"print_job_id\":\"{}\", \"printer_id\":\"{}\", \"printer_name\":\"{}\", \"component\":\"print-job-service\", \"output\":\"{}\"}", jobId, printer != null ? printer.id() : job.printerId(), printer != null ? printer.name() : "unknown", rendered.replace("\n", "\\n"));

            updateStatus(jobId, "completed");
            String completedEvent = "{\"id\":\"" + jobId + "\", \"status\":\"completed\"}";
            List<String> completedTopics = List.of("all", "printer:" + job.printerId(), "order:" + job.orderId(), "job:" + job.id());
            sendSseEvent(completedEvent, completedTopics);
            Broadcaster.broadcast(completedEvent);
        } catch (Exception e) {
            log.error("print job processing error {}", jobId, e);
            updateStatus(jobId, "failed");
        }
    }

    // Subscribe to 'all' by default (backwards-compatible)
    public SseEmitter createEmitter() {
        return createEmitterForTopics("all");
    }

    // Create emitter subscribing to comma-separated topic list (e.g. "printer:123,order:abc")
    public SseEmitter createEmitterForTopics(String topicCsv) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        Set<String> topics = new HashSet<>();
        if (topicCsv != null && !topicCsv.isBlank()) {
            Arrays.stream(topicCsv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(topics::add);
        }
        if (topics.isEmpty()) topics.add("all");
        EmitterState state = new EmitterState(emitter, topics);
        emitterStates.add(state);
        emitter.onCompletion(() -> emitterStates.remove(state));
        emitter.onTimeout(() -> emitterStates.remove(state));
        return emitter;
    }

    private void sendSseEvent(String eventJson, List<String> eventTopics) {
        synchronized (emitterStates) {
            for (var it = emitterStates.iterator(); it.hasNext();) {
                EmitterState s = it.next();
                try {
                    s.sendOrQueueIfSubscribed(eventJson, eventTopics);
                } catch (Exception ex) {
                    it.remove();
                    droppedEmittersCounter.increment();
                }
            }
        }
    }

    public double getCollapsedEvents() { return collapsedEventsCounter.count(); }
    public double getDroppedEmitters() { return droppedEmittersCounter.count(); }
    public double getTotalSent() { return totalSentCounter.count(); }
} 