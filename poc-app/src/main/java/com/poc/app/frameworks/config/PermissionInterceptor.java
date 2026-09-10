package com.poc.app.frameworks.config;

import com.poc.common.context.RequirePermission;
import com.poc.common.context.RequireRole;
import com.poc.common.context.UserContext;
import com.poc.common.exception.ForbiddenException;
import com.poc.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Check @RequirePermission first
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }

        if (annotation != null) {
            if (!UserContext.isAuthenticated()) {
                throw new UnauthorizedException("Authentication required",
                        java.util.Map.of("requiredPermission", Arrays.toString(annotation.value())));
            }
            if (UserContext.hasAnyPermission(annotation.value())) {
                return true;
            }
            throw new ForbiddenException("Insufficient permissions: " + Arrays.toString(annotation.value()),
                    java.util.Map.of(
                            "requiredPermissions", Arrays.toString(annotation.value()),
                            "userPermissions", UserContext.getUserPermissions()
                    ));
        }

        // Then check @RequireRole
        RequireRole roleAnnotation = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (roleAnnotation == null) {
            roleAnnotation = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }

        if (roleAnnotation != null) {
            if (!UserContext.isAuthenticated()) {
                throw new UnauthorizedException("Authentication required",
                        java.util.Map.of("requiredRole", Arrays.toString(roleAnnotation.value())));
            }
            if (UserContext.hasAnyRole(roleAnnotation.value())) {
                return true;
            }
            throw new ForbiddenException("Insufficient roles: " + Arrays.toString(roleAnnotation.value()),
                    java.util.Map.of(
                            "requiredRoles", Arrays.toString(roleAnnotation.value()),
                            "userRoles", UserContext.getUserRoles()
                    ));
        }

        // No annotation = public endpoint
        return true;
    }
}
