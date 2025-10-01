package com.possable.user;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Compatibility shim delegating to `UserFacade`.
 * Keeps existing callers working while modules use the facade.
 */
@Deprecated
@Service
public class UserService {

	private final UserFacade delegate;

	@Autowired
	public UserService(UserFacade delegate) {
		this.delegate = delegate;
	}

	public boolean addUser(String username, String rawPincode, String rawDrawing, Set<String> roles) {
		return delegate.addUser(username, rawPincode, rawDrawing, roles);
	}

	public boolean removeUser(String username) {
		return delegate.removeUser(username);
	}

	public boolean updatePincode(String username, String newRawPincode) {
		return delegate.updatePincode(username, newRawPincode);
	}

	public boolean updateDrawing(String username, String newRawDrawing) {
		return delegate.updateDrawing(username, newRawDrawing);
	}

	public Set<String> listUsernames() {
		return delegate.listUsernames();
	}

	public boolean authenticate(String username, String drawing, String pincode) {
		return delegate.authenticate(username, drawing, pincode);
	}

	public Set<String> getRoles(String username) {
		return delegate.getRoles(username);
	}

	/* Profile methods */
	public Map<String, Object> getProfile(String username) {
		return delegate.getProfile(username);
	}

	public boolean updateProfile(String username, String displayName, String email, String phone) {
		return delegate.updateProfile(username, displayName, email, phone);
	}

	public boolean uploadAvatar(String username, byte[] bytes, String contentType) {
		return delegate.uploadAvatar(username, bytes, contentType);
	}

	public Map<String, Object> getAvatar(String username) {
		return delegate.getAvatar(username);
	}

	/* API key methods */
	public Map<String, String> listApiKeys(String username) {
		return delegate.listApiKeys(username);
	}

	public String createApiKey(String username) {
		return delegate.createApiKey(username);
	}

	public boolean revokeApiKey(String username, String keyId) {
		return delegate.revokeApiKey(username, keyId);
	}

} 