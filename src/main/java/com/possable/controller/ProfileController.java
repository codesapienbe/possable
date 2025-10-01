package com.possable.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.possable.service.UserService;

@RestController
@RequestMapping("/api/me")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    private String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) return null;
        return a.getName();
    }

    @GetMapping
    public ResponseEntity<?> getProfile() {
        String u = currentUsername();
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(userService.getProfile(u));
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody Map<String,String> body) {
        String u = currentUsername();
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String name = body.getOrDefault("displayName", null);
        String email = body.getOrDefault("email", null);
        String phone = body.getOrDefault("phone", null);
        boolean ok = userService.updateProfile(u, name, email, phone);
        return ok ? ResponseEntity.ok(Map.of("ok", true)) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("ok", false));
    }

    @PostMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@RequestPart("file") MultipartFile file) {
        String u = currentUsername();
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            byte[] bytes = file.getBytes();
            String ct = file.getContentType() == null ? "image/png" : file.getContentType();
            boolean ok = userService.uploadAvatar(u, bytes, ct);
            return ok ? ResponseEntity.ok(Map.of("ok", true)) : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("ok", false));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("ok", false));
        }
    }

    @GetMapping(path = "/avatar")
    public ResponseEntity<byte[]> getAvatar() {
        String u = currentUsername();
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var map = userService.getAvatar(u);
        if (map == null || !map.containsKey("bytes")) return ResponseEntity.notFound().build();
        byte[] bytes = (byte[]) map.get("bytes");
        String ct = (String) map.getOrDefault("contentType", "image/png");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(ct));
        headers.setCacheControl("max-age=3600, public");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping(path = "/api-keys")
    public ResponseEntity<?> listKeys() {
        String u = currentUsername();
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(userService.listApiKeys(u));
    }

    @PostMapping(path = "/api-keys")
    public ResponseEntity<?> createKey() {
        String u = currentUsername();
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String token = userService.createApiKey(u);
        if (token == null) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("ok", false));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("token", token));
    }

    @DeleteMapping(path = "/api-keys/{keyId}")
    public ResponseEntity<?> revokeKey(@PathVariable String keyId) {
        String u = currentUsername();
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean ok = userService.revokeApiKey(u, keyId);
        return ok ? ResponseEntity.ok(Map.of("ok", true)) : ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ok", false));
    }
} 