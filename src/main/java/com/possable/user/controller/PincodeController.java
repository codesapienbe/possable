package com.possable.user.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
public class PincodeController {

    private static final Logger log = LoggerFactory.getLogger(PincodeController.class);

    @Value("${app.auth.pincode:1234}")
    private String configuredPincode;

    @GetMapping(path = "/pincode-login")
    public void pincodeLogin(@RequestParam(name = "pincode") String pincode,
                             @RequestParam(name = "table", required = false) String table,
                             @RequestParam(name = "redirect", required = false) String redirect,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
        try {
            boolean ok = configuredPincode != null && configuredPincode.equals(pincode);
            if (!ok) {
                log.warn("{\"message\":\"pincode_login_failed\", \"component\":\"auth\", \"table\":\"{}\"}", table == null ? "-" : table);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"invalid_pincode\"}");
                return;
            }

            String principal = "customer" + (table == null ? "" : (":table-" + table));
            var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            // persist security context into session so subsequent Vaadin server requests are authenticated
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            log.info("{\"message\":\"pincode_login_success\", \"component\":\"auth\", \"table\":\"{}\"}", table == null ? "-" : table);

            if (redirect != null && !redirect.isBlank()) {
                response.sendRedirect(redirect);
            } else {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"status\":\"ok\"}");
            }
        } catch (Exception ex) {
            log.error("{\"message\":\"pincode_login_error\", \"component\":\"auth\"}", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"internal_error\"}");
        }
    }
} 