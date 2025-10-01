package com.possable.model;

import java.io.Serializable;

public class UserRoleId implements Serializable {
    private String username;
    private String role;

    public UserRoleId() {}
    public UserRoleId(String username, String role) { this.username = username; this.role = role; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRoleId that = (UserRoleId) o;
        return java.util.Objects.equals(username, that.username) && java.util.Objects.equals(role, that.role);
    }

    public int hashCode() { return java.util.Objects.hash(username, role); }
} 