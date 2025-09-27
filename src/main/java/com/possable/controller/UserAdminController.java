package com.possable.controller;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.possable.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Tag(name = "User Admin", description = "Management operations for users")
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('MANAGEMENT')")
public class UserAdminController {

    private static final Logger log = LoggerFactory.getLogger("application.log");

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List usernames")
    @GetMapping("/list")
    public ResponseEntity<Set<String>> listUsers() {
        var list = userService.listUsernames();
        log.info("{" + "\"component\":\"UserAdmin\", \"action\":\"list_users\", \"count\": {} }", list.size());
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Add a user")
    @PostMapping("/add")
    public ResponseEntity<?> addUser(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String drawing = (String) body.getOrDefault("drawing", "");
        var roles = (Set<String>) body.getOrDefault("roles", Set.of());
        boolean ok = userService.addUser(username, password, drawing, roles);
        log.info("{" + "\"component\":\"UserAdmin\", \"action\":\"add_user\", \"user\":\"{}\", \"result\":\"{}\" }", username, ok);
        return ok ? ResponseEntity.ok(Map.of("ok", true)) : ResponseEntity.badRequest().body(Map.of("ok", false));
    }

    @Operation(summary = "Remove a user")
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeUser(@RequestParam String username) {
        boolean ok = userService.removeUser(username);
        log.info("{" + "\"component\":\"UserAdmin\", \"action\":\"remove_user\", \"user\":\"{}\", \"result\":\"{}\" }", username, ok);
        return ok ? ResponseEntity.ok(Map.of("ok", true)) : ResponseEntity.badRequest().body(Map.of("ok", false));
    }

    @Operation(summary = "Update a user's PIN")
    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        boolean ok = userService.updatePincode(username, password);
        log.info("{" + "\"component\":\"UserAdmin\", \"action\":\"update_password\", \"user\":\"{}\", \"result\":\"{}\" }", username, ok);
        return ok ? ResponseEntity.ok(Map.of("ok", true)) : ResponseEntity.badRequest().body(Map.of("ok", false));
    }

    @Operation(summary = "Logout (invalidate session)")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) session.invalidate();
        } catch (Exception e) {
            // ignore
        }
        SecurityContextHolder.clearContext();
        log.info("{" + "\"component\":\"UserAdmin\", \"action\":\"logout\" }");
        return ResponseEntity.ok(Map.of("ok", true));
    }
} 