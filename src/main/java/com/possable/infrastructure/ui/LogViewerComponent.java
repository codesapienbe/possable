package com.possable.infrastructure.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.io.ByteArrayInputStream;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.StreamResource;
import java.util.Collection;
import java.time.LocalDateTime;

/**
 * Simple log viewer that expects each log line to be a JSON object.
 * Shows parsed entries in a Grid and full JSON in a details area when selected.
 */
public class LogViewerComponent extends VerticalLayout {

	private static final Path DEFAULT_LOG = Paths.get("application.log");
	private static final int DEFAULT_TAIL_LINES = 500;
	private final ObjectMapper mapper = new ObjectMapper();
	private final Grid<LogEntry> grid = new Grid<>();
	private final TextField filter = new TextField();
	private final Button refresh = new Button("Refresh");
	private final Button export = new Button("Export Selected");
	private final Pre details = new Pre();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "logviewer-poller");
		t.setDaemon(true);
		return t;
	});
	private ScheduledFuture<?> scheduledFuture;
	private Path logPath = DEFAULT_LOG;
	// keep the last loaded entries so export can use them
	private volatile List<LogEntry> lastEntries = new ArrayList<>();
	
	public LogViewerComponent() {
		setPadding(true);
		setSpacing(true);
		setWidthFull();
		addClassName("pos-root");

		add(new H3("Application Logs (JSON lines expected)"));

		HorizontalLayout tools = new HorizontalLayout();
		filter.setPlaceholder("Search (matches message/logger/level)");
		filter.setClearButtonVisible(true);
		filter.addValueChangeListener(e -> refreshGrid());
		refresh.addClickListener(e -> refreshGrid());
		export.addClickListener(e -> doExport());
		tools.add(filter, refresh, export);
		add(tools);

		grid.addColumn(LogEntry::timestamp).setHeader("Timestamp").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(LogEntry::level).setHeader("Level").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(LogEntry::logger).setHeader("Logger").setAutoWidth(true);
		grid.addColumn(LogEntry::message).setHeader("Message");
		grid.setHeight("360px");
		grid.setWidthFull();
		grid.addSelectionListener(ev -> ev.getFirstSelectedItem().ifPresent(this::showDetails));

		Div gridWrap = new Div(grid);
		gridWrap.setWidthFull();
		add(gridWrap);

		Div detailsWrap = new Div();
		details.getElement().getStyle().set("white-space", "pre-wrap");
		details.getElement().getStyle().set("max-height", "280px");
		details.getElement().getStyle().set("overflow", "auto");
		detailsWrap.add(details);
		add(detailsWrap);

		// initial load
		refreshGrid();
	}

	private void showDetails(LogEntry entry) {
		if (entry == null) {
			details.setText("");
			return;
		}
		try {
			JsonNode node = mapper.readTree(entry.rawJson);
			String pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
			details.setText(pretty);
		} catch (Exception ex) {
			details.setText(entry.rawJson);
		}
	}

	private void refreshGrid() {
		List<LogEntry> entries = readRecentJsonLines(logPath, DEFAULT_TAIL_LINES);
		// cache entries for export
		this.lastEntries = entries == null ? new ArrayList<>() : new ArrayList<>(entries);
		String q = filter.getValue() == null ? "" : filter.getValue().trim().toLowerCase();
		if (!q.isEmpty()) {
			entries = entries.stream().filter(le -> (le.message != null && le.message.toLowerCase().contains(q))
				|| (le.logger != null && le.logger.toLowerCase().contains(q))
				|| (le.level != null && le.level.toLowerCase().contains(q))).collect(Collectors.toList());
		}
		grid.setItems(entries);
		if (!entries.isEmpty()) {
			grid.select(entries.get(0));
			showDetails(entries.get(0));
		} else {
			showDetails(null);
		}
	}

	private List<LogEntry> readRecentJsonLines(Path path, int maxLines) {
		if (path == null || !Files.exists(path)) return Collections.emptyList();
		try {
			List<String> all = Files.readAllLines(path, StandardCharsets.UTF_8);
			int from = Math.max(0, all.size() - maxLines);
			List<String> slice = all.subList(from, all.size());
			List<LogEntry> out = new ArrayList<>(slice.size());
			for (String line : slice) {
				if (line == null || line.isBlank()) continue;
				try {
					JsonNode node = mapper.readTree(line);
					String ts = getFirstText(node, "@timestamp", "timestamp", "time", "@time");
					String level = getFirstText(node, "level", "level_name", "lvl");
					String logger = getFirstText(node, "logger_name", "logger", "logger_name");
					String message = getFirstText(node, "message", "msg", "messageText");
					out.add(new LogEntry(ts, level, logger, message, line));
				} catch (Exception ex) {
					// skip non-json lines
				}
			}
			return out;
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	private String getFirstText(JsonNode node, String... keys) {
		for (String k : keys) {
			if (node.has(k)) {
				JsonNode v = node.get(k);
				if (v != null && v.isTextual()) return v.asText();
				if (v != null && v.isNumber()) return v.asText();
			}
		}
		return "";
	}

	private void doExport() {
		Collection<LogEntry> sel = grid.getSelectedItems();
		List<String> lines;
		if (sel != null && !sel.isEmpty()) {
			lines = sel.stream().map(le -> le.rawJson).collect(Collectors.toList());
		} else {
			lines = lastEntries.stream().map(le -> le.rawJson).collect(Collectors.toList());
		}
		if (lines == null || lines.isEmpty()) return;
		String content = String.join("\n", lines);
		String fname = "logs-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".json";
		StreamResource resource = new StreamResource(fname, () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
		// create temporary hidden anchor
		String aid = "dl-" + System.nanoTime();
		Anchor download = new Anchor(resource, "");
		download.getElement().setAttribute("id", aid);
		download.getElement().setAttribute("download", true);
		download.getStyle().set("display", "none");
		add(download);
		// click and remove element client-side after click
		getUI().ifPresent(ui -> ui.getPage().executeJs("(id)=>{const el=document.getElementById(id); if(el){el.click(); setTimeout(()=>el.remove(),250);} }", aid));
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		// schedule poller to refresh every 3 seconds
		scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
			try {
				attachEvent.getUI().access(() -> refreshGrid());
			} catch (Exception ignored) {}
		}, 3, 3, TimeUnit.SECONDS);
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		super.onDetach(detachEvent);
		if (scheduledFuture != null) {
			scheduledFuture.cancel(false);
		}
		scheduler.shutdownNow();
	}

	public static record LogEntry(String timestamp, String level, String logger, String message, String rawJson) {}
} 
