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

	private static record UserRecord(String passwordHash, Set<String> roles) {}

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
		// roles are stored as uppercase names
		users.putIfAbsent("service", new UserRecord(encoded, Set.of("SERVICE")));
		users.putIfAbsent("kitchen", new UserRecord(encoded, Set.of("KITCHEN")));
		users.putIfAbsent("management", new UserRecord(encoded, Set.of("MANAGEMENT")));
	}

	public boolean addUser(String username, String rawPassword, Set<String> roles) {
		if (username == null || rawPassword == null) return false;
		String hash = passwordEncoder.encode(rawPassword);
		users.put(username, new UserRecord(hash, roles == null ? Collections.emptySet() : Set.copyOf(roles)));
		return true;
	}

	public boolean removeUser(String username) {
		if (username == null) return false;
		return users.remove(username) != null;
	}

	public boolean updatePassword(String username, String newRawPassword) {
		if (username == null || newRawPassword == null) return false;
		UserRecord existing = users.get(username);
		if (existing == null) return false;
		String hash = passwordEncoder.encode(newRawPassword);
		users.put(username, new UserRecord(hash, existing.roles));
		return true;
	}

	public Set<String> listUsernames() {
		return Set.copyOf(users.keySet());
	}

	public boolean authenticate(String username, String rawPassword) {
		if (username == null || rawPassword == null) return false;
		UserRecord r = users.get(username);
		if (r == null) return false;
		return passwordEncoder.matches(rawPassword, r.passwordHash);
	}

	public Set<String> getRoles(String username) {
		UserRecord r = users.get(username);
		return r == null ? Collections.emptySet() : r.roles;
	}

} 