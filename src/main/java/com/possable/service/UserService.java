package com.possable.user;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.possable.user.internal.UserProfileRepository;
import com.possable.user.internal.UserRoleEntity;
import com.possable.user.internal.UserCredentialRepository;
import com.possable.user.internal.UserProfileEntity;
import com.possable.user.internal.ApiKeyEntity;
import com.possable.user.internal.ApiKeyRepository;
import com.possable.user.internal.UserCredentialEntity;
import com.possable.user.internal.UserRoleRepository;

@Service
public class UserService {

    private static record UserRecord(String pincodeHash, String drawingHash, Set<String> roles) {}
    // Credentials are persisted in the DB (UserCredentialEntity); the in-memory users map was removed.
    private final PasswordEncoder passwordEncoder;
    private final UserProfileRepository profileRepo;
    private final UserCredentialRepository credentialRepo;
    private final ApiKeyRepository apiKeyRepo;
    private final UserRoleRepository roleRepo;

    @Value("${app.auth.pincode:1234}")
    private String defaultPincode;

    public UserService(PasswordEncoder passwordEncoder, UserProfileRepository profileRepo, UserCredentialRepository credentialRepo, ApiKeyRepository apiKeyRepo, UserRoleRepository roleRepo) {
        this.passwordEncoder = passwordEncoder;
        this.profileRepo = profileRepo;
        this.credentialRepo = credentialRepo;
        this.apiKeyRepo = apiKeyRepo;
        this.roleRepo = roleRepo;
    }

    @PostConstruct
    public void initDefaults() {
        String encoded = passwordEncoder.encode(defaultPincode);
        // seed credentials table and role entries
        createCredentialIfMissing("service", encoded, "");
        createCredentialIfMissing("kitchen", encoded, "");
        createCredentialIfMissing("management", encoded, "");
        createCredentialIfMissing("cashier", encoded, "");
        createProfileIfMissing("service", "Service");
        createProfileIfMissing("kitchen", "Kitchen");
        createProfileIfMissing("management", "Management");
        createProfileIfMissing("cashier", "Cashier");
    }

    private void createCredentialIfMissing(String username, String pHash, String dHash) {
        if (!credentialRepo.existsById(username)) {
            credentialRepo.save(new UserCredentialEntity(username, pHash, dHash));
        }
        if (roleRepo.countByUsername(username) == 0) {
            // add default role mapping for compatibility
            var r = new UserRoleEntity(username, username.equals("service") ? "SERVICE" : username.toUpperCase());
            roleRepo.save(r);
        }
    }

    private void createProfileIfMissing(String username, String displayName) {
        if (!profileRepo.existsById(username)) {
            var e = new UserProfileEntity(username, displayName, "", "", null, null, Instant.now());
            profileRepo.save(e);
        }
    }

    public boolean addUser(String username, String rawPincode, String rawDrawing, Set<String> roles) {
        if (username == null || rawPincode == null || rawDrawing == null) return false;
        if (!rawPincode.matches("\\d{4,6}")) return false;
        String pHash = passwordEncoder.encode(rawPincode);
        String dHash = passwordEncoder.encode(rawDrawing);
        // persist credential
        credentialRepo.save(new UserCredentialEntity(username, pHash, dHash));
        // persist profile
        createProfileIfMissing(username, username);
        // persist roles
        if (roles != null) {
            for (String r : roles) {
                roleRepo.save(new UserRoleEntity(username, r));
            }
        }
        return true;
    }

    public boolean removeUser(String username) {
        if (username == null) return false;
        try { profileRepo.deleteById(username); } catch (Exception e) { }
        try { credentialRepo.deleteById(username); } catch (Exception e) { }
        roleRepo.deleteByUsername(username);
        apiKeyRepo.findByUsernameAndRevokedFalse(username).forEach(k -> { k.setRevoked(true); apiKeyRepo.save(k); });
        return true;
    }

    public boolean updatePincode(String username, String newRawPincode) {
        if (username == null || newRawPincode == null) return false;
        if (!newRawPincode.matches("\\d{4,6}")) return false;
        var ent = credentialRepo.findById(username);
        if (ent.isEmpty()) return false;
        var c = ent.get();
        c.setPincodeHash(passwordEncoder.encode(newRawPincode));
        credentialRepo.save(c);
        return true;
    }

    public boolean updateDrawing(String username, String newRawDrawing) {
        if (username == null || newRawDrawing == null) return false;
        var ent = credentialRepo.findById(username);
        if (ent.isEmpty()) return false;
        var c = ent.get();
        c.setDrawingHash(passwordEncoder.encode(newRawDrawing));
        credentialRepo.save(c);
        return true;
    }

    public Set<String> listUsernames() {
        return credentialRepo.findAll().stream().map(UserCredentialEntity::getUsername).collect(Collectors.toSet());
    }

    public boolean authenticate(String username, String drawing, String pincode) {
        if (username == null) return false;
        var ent = credentialRepo.findById(username);
        if (ent.isEmpty()) return false;
        var r = ent.get();
        if (drawing != null && !drawing.isBlank() && r.getDrawingHash() != null && !r.getDrawingHash().isBlank()) {
            if (passwordEncoder.matches(drawing, r.getDrawingHash())) return true;
        }
        if (pincode != null && pincode.matches("\\d{4,6}") && r.getPincodeHash() != null) {
            return passwordEncoder.matches(pincode, r.getPincodeHash());
        }
        return false;
    }

    public Set<String> getRoles(String username) {
        return roleRepo.findByUsername(username).stream().map(UserRoleEntity::getRole).collect(Collectors.toSet());
    }

    /* Profile methods */
    public Map<String, Object> getProfile(String username) {
        var ent = profileRepo.findById(username);
        if (ent.isEmpty()) return Map.of();
        var p = ent.get();
        return Map.of(
            "displayName", p.getDisplayName(),
            "email", p.getEmail(),
            "phone", p.getPhone(),
            "createdAt", p.getCreatedAt() == null ? null : p.getCreatedAt().toString()
        );
    }

    public boolean updateProfile(String username, String displayName, String email, String phone) {
        if (username == null) return false;
        var ent = profileRepo.findById(username).orElseGet(() -> new UserProfileEntity(username, username, "", "", null, null, Instant.now()));
        ent.setDisplayName(displayName == null ? ent.getDisplayName() : displayName);
        ent.setEmail(email == null ? ent.getEmail() : email);
        ent.setPhone(phone == null ? ent.getPhone() : phone);
        if (ent.getCreatedAt() == null) ent.setCreatedAt(Instant.now());
        profileRepo.save(ent);
        return true;
    }

    public boolean uploadAvatar(String username, byte[] bytes, String contentType) {
        if (username == null || bytes == null || contentType == null) return false;
        var ent = profileRepo.findById(username).orElseGet(() -> new UserProfileEntity(username, username, "", "", null, null, Instant.now()));
        ent.setAvatarBytes(bytes);
        ent.setAvatarContentType(contentType);
        if (ent.getCreatedAt() == null) ent.setCreatedAt(Instant.now());
        profileRepo.save(ent);
        return true;
    }

    public Map<String, Object> getAvatar(String username) {
        var ent = profileRepo.findById(username);
        if (ent.isEmpty()) return Map.of();
        var p = ent.get();
        if (p.getAvatarBytes() == null) return Map.of();
        return Map.of("contentType", p.getAvatarContentType(), "bytes", p.getAvatarBytes());
    }

    /* API key persistence via ApiKeyRepository */
    public Map<String, String> listApiKeys(String username) {
        var list = apiKeyRepo.findByUsernameAndRevokedFalse(username);
        if (list == null || list.isEmpty()) return Map.of();
        Map<String, String> out = new ConcurrentHashMap<>();
        for (var e : list) out.put(e.getKeyId(), "****");
        return out;
    }

    public String createApiKey(String username) {
        if (username == null) return null;
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        String keyId = UUID.randomUUID().toString();
        String hashed = passwordEncoder.encode(token);
        var ent = new ApiKeyEntity(keyId, username, hashed, null, Instant.now(), false);
        apiKeyRepo.save(ent);
        return keyId + ":" + token;
    }

    public boolean revokeApiKey(String username, String keyId) {
        var ent = apiKeyRepo.findById(keyId);
        if (ent.isEmpty()) return false;
        var e = ent.get();
        if (!username.equals(e.getUsername())) return false;
        e.setRevoked(true);
        apiKeyRepo.save(e);
        return true;
    }

} 
