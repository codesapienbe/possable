package com.possable.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.possable.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger("application.log");

    private final UserService userService;

    // simple rate limiter: key -> (count, windowStartEpochSec)
    private static record RateInfo(AtomicInteger count, long windowStart) {}
    private final ConcurrentHashMap<String, RateInfo> rateMap = new ConcurrentHashMap<>();
    private final int MAX_ATTEMPTS = 6;
    private final int WINDOW_SECONDS = 60; // 1 minute

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String username = (String) body.get("username");
        String pincode = (String) body.getOrDefault("pincode", null);
        String drawing = (String) body.getOrDefault("drawing", null);
        String remote = request == null ? "-" : request.getRemoteAddr();
        String key = username + "@" + remote;
        long now = Instant.now().getEpochSecond();
        RateInfo ri = rateMap.compute(key, (k, v) -> {
            if (v == null || now - v.windowStart() > WINDOW_SECONDS) return new RateInfo(new AtomicInteger(0), now);
            return v;
        });
        int current = ri.count().incrementAndGet();
        if (current > MAX_ATTEMPTS) {
            log.warn("{\"component\":\"Auth\",\"action\":\"rate_limited\",\"key\":\"{}\",\"ip\":\"{}\"}", username, remote);
            return ResponseEntity.status(429).body(Map.of("ok", false, "error", "too_many_requests"));
        }
        boolean ok = userService.authenticate(username, drawing, pincode);
        log.info("{\"component\":\"Auth\", \"action\":\"login_attempt\", \"user\":\"{}\", \"ip\":\"{}\", \"result\":\"{}\"}", username, remote, ok);
        if (!ok) return ResponseEntity.status(401).body(Map.of("ok", false));
        // reset rate on success
        rateMap.remove(key);
        Set<String> roles = userService.getRoles(username);
        var authorities = roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList());
        var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        // store in session for Vaadin UIs
        if (request != null) request.getSession(true).setAttribute("user", username);
        return ResponseEntity.ok(Map.of("ok", true, "roles", roles));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('MANAGEMENT')")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String pincode = (String) body.get("pincode");
        String drawing = (String) body.getOrDefault("drawing", "");
        @SuppressWarnings("unchecked")
        Set<String> roles = (Set<String>) body.getOrDefault("roles", Set.of());
        boolean ok = userService.addUser(username, pincode, drawing, roles);
        log.info("{\"component\":\"Auth\", \"action\":\"register\", \"user\":\"{}\", \"result\":\"{}\"}", username, ok);
        return ok ? ResponseEntity.ok(Map.of("ok", true)) : ResponseEntity.badRequest().body(Map.of("ok", false));
    }
} 