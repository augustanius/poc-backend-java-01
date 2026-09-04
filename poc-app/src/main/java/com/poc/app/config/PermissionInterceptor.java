package com.poc.app.config;

import com.poc.common.context.RequirePermission;
import com.poc.common.context.RequireRole;
import com.poc.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

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
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return false;
            }
            if (UserContext.hasAnyPermission(annotation.value())) {
                return true;
            }
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }

        // Then check @RequireRole
        RequireRole roleAnnotation = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (roleAnnotation == null) {
            roleAnnotation = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }

        if (roleAnnotation != null) {
            if (!UserContext.isAuthenticated()) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return false;
            }
            if (UserContext.hasAnyRole(roleAnnotation.value())) {
                return true;
            }
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }

        // No annotation = public endpoint
        return true;
    }
}
