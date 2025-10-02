package com.possable.user;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Module-local seeder for the User module. Enabled with `app.seed.user.enable=true`.
 * Uses the public `UserFacade` so the module boundary remains intact.
 */
@Component
@ConditionalOnProperty(name = "app.seed.user.enable", havingValue = "true", matchIfMissing = false)
public class UserSeeder {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);
    private final UserFacade userFacade;
    private final Random rnd = new Random();

    public UserSeeder(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            var existing = userFacade.listUsernames();
            if (existing != null && existing.size() >= 5) {
                log.info("{\"message\":\"user_seeder_skipped\", \"count\":{} }", existing.size());
                return;
            }

            List<String> names = Arrays.asList("alice", "bob", "carol", "dave", "eve");
            for (String n : names) {
                if (!userFacade.listUsernames().contains(n)) {
                    String pin = randomNumericPin(4);
                    String drawing = randomHex(16);
                    Set<String> roles = new HashSet<>();
                    roles.add("USER");
                    if (n.equals("alice") || n.equals("dave")) roles.add("MANAGER");
                    boolean created = userFacade.addUser(n, pin, drawing, roles);
                    if (created) {
                        String apiKey = userFacade.createApiKey(n);
                        log.info("{\"message\":\"user_seeded\", \"username\":\"{}\", \"apiKey_id\":\"{}\"}", n, apiKey == null ? "-" : apiKey.split(":")[0]);
                    }
                }
            }

            log.info("{\"message\":\"user_seeder_completed\", \"timestamp\":\"{}\"}", Instant.now());
        } catch (Exception ex) {
            log.error("{\"message\":\"user_seeder_failed\", \"error\":\"{}\"}", sanitize(ex.getMessage()), ex);
        }
    }

    private String randomNumericPin(int digits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits; i++) sb.append(rnd.nextInt(10));
        return sb.toString();
    }

    private String randomHex(int len) {
        byte[] b = new byte[(len + 1) / 2];
        rnd.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte by : b) sb.append(String.format(Locale.ROOT, "%02x", by));
        return sb.toString().substring(0, Math.min(len, sb.length()));
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
} 