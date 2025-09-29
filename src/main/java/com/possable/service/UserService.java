package com.possable.service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static record UserRecord(String pincodeHash, String drawingHash, Set<String> roles) {}

    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.pincode:1234}")
    private String defaultPincode;

    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initDefaults() {
        String encoded = passwordEncoder.encode(defaultPincode);
        // default demo users have no drawing set (empty)
        users.putIfAbsent("service", new UserRecord(encoded, "", Set.of("SERVICE")));
        users.putIfAbsent("kitchen", new UserRecord(encoded, "", Set.of("KITCHEN")));
        users.putIfAbsent("management", new UserRecord(encoded, "", Set.of("MANAGEMENT")));
        users.putIfAbsent("cashier", new UserRecord(encoded, "", Set.of("CASHIER")));
    }

    public boolean addUser(String username, String rawPincode, String rawDrawing, Set<String> roles) {
        if (username == null || rawPincode == null || rawDrawing == null) return false;
        // pincode must be 4-6 digits
        if (!rawPincode.matches("\\d{4,6}")) return false;
        String pHash = passwordEncoder.encode(rawPincode);
        String dHash = passwordEncoder.encode(rawDrawing);
        users.put(username, new UserRecord(pHash, dHash, roles == null ? Collections.emptySet() : Set.copyOf(roles)));
        return true;
    }

    public boolean removeUser(String username) {
        if (username == null) return false;
        return users.remove(username) != null;
    }

    public boolean updatePincode(String username, String newRawPincode) {
        if (username == null || newRawPincode == null) return false;
        if (!newRawPincode.matches("\\d{4,6}")) return false;
        UserRecord existing = users.get(username);
        if (existing == null) return false;
        String hash = passwordEncoder.encode(newRawPincode);
        users.put(username, new UserRecord(hash, existing.drawingHash(), existing.roles()));
        return true;
    }

    public boolean updateDrawing(String username, String newRawDrawing) {
        if (username == null || newRawDrawing == null) return false;
        UserRecord existing = users.get(username);
        if (existing == null) return false;
        String hash = passwordEncoder.encode(newRawDrawing);
        users.put(username, new UserRecord(existing.pincodeHash(), hash, existing.roles()));
        return true;
    }

    public Set<String> listUsernames() {
        return Set.copyOf(users.keySet());
    }

    /**
     * Authenticate using drawing first (preferred). If drawing provided and matches stored drawing hash, success.
     * Otherwise fallback to pincode match. Returns true when either matches.
     */
    public boolean authenticate(String username, String drawing, String pincode) {
        if (username == null) return false;
        UserRecord r = users.get(username);
        if (r == null) return false;
        // prefer drawing
        if (drawing != null && !drawing.isBlank() && r.drawingHash() != null && !r.drawingHash().isBlank()) {
            if (passwordEncoder.matches(drawing, r.drawingHash())) return true;
        }
        if (pincode != null && pincode.matches("\\d{4,6}") && r.pincodeHash() != null) {
            return passwordEncoder.matches(pincode, r.pincodeHash());
        }
        return false;
    }

    public Set<String> getRoles(String username) {
        UserRecord r = users.get(username);
        return r == null ? Collections.emptySet() : r.roles();
    }

} 