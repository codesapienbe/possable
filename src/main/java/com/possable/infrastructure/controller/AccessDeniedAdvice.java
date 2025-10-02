package com.possable.infrastructure.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.possable.infrastructure.Broadcaster;

import java.util.Map;

@ControllerAdvice
public class AccessDeniedAdvice {

    private static final Logger log = LoggerFactory.getLogger(AccessDeniedAdvice.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String,String>> handleAccessDenied(AccessDeniedException ex) {
        try {
            String msg = "{\"type\":\"access_denied\", \"message\":\"Access denied: " + sanitize(ex.getMessage()) + "\"}";
            Broadcaster.broadcast(msg);
        } catch (Exception e) {
            log.warn("Failed to broadcast access_denied", e);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "access_denied", "message", "Access is denied"));
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").replaceAll("[\n\r]+", " ");
    }
} 