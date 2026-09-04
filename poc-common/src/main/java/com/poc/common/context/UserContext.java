package com.poc.common.context;

import java.util.Collections;
import java.util.List;

public final class UserContext {

    private UserContext() {}

    private static final ThreadLocal<String> userId = new ThreadLocal<>();
    private static final ThreadLocal<String> userRoles = new ThreadLocal<>();
    private static final ThreadLocal<String> userPermissions = new ThreadLocal<>();
    private static final ThreadLocal<String> username = new ThreadLocal<>();

    public static void set(String id, String roles, String permissions, String name) {
        userId.set(id);
        userRoles.set(roles);
        userPermissions.set(permissions);
        username.set(name);
    }

    public static String getUserId() {
        return userId.get();
    }

    public static String getUserRoles() {
        return userRoles.get();
    }

    public static String getUserPermissions() {
        return userPermissions.get();
    }

    public static String getUsername() {
        return username.get();
    }

    public static void clear() {
        userId.remove();
        userRoles.remove();
        userPermissions.remove();
        username.remove();
    }

    public static boolean isAuthenticated() {
        return userId.get() != null;
    }

    public static List<String> getRolesList() {
        String roles = userRoles.get();
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(roles.split(","));
    }

    public static List<String> getPermissionsList() {
        String permissions = userPermissions.get();
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(permissions.split(","));
    }

    public static boolean hasPermission(String permission) {
        return getPermissionsList().contains(permission);
    }

    public static boolean hasAnyPermission(String... permissions) {
        List<String> granted = getPermissionsList();
        for (String permission : permissions) {
            if (granted.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasRole(String role) {
        return getRolesList().contains(role);
    }

    public static boolean hasAnyRole(String... roles) {
        List<String> granted = getRolesList();
        for (String role : roles) {
            if (granted.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
