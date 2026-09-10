package com.bienestar.sistema_bienestar_universitario.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int CLEANUP_INTERVAL_MS = 60_000;
    private long lastCleanup = System.currentTimeMillis();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        cleanupIfNeeded();

        String ip = getClientIp(request);
        String key = ip;

        RequestCounter counter = requestCounts.computeIfAbsent(key,
                k -> new RequestCounter());

        synchronized (counter) {
            long now = System.currentTimeMillis();
            if (now - counter.windowStart > 60_000) {
                counter.windowStart = now;
                counter.count.set(0);
            }

            if (counter.count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(429);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write(
                        "Demasiadas solicitudes. Intenta nuevamente en un minuto.");
                return false;
            }
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            lastCleanup = now;
            requestCounts.entrySet().removeIf(entry -> {
                RequestCounter c = entry.getValue();
                return now - c.windowStart > 120_000;
            });
        }
    }

    private static class RequestCounter {
        AtomicInteger count = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();
    }
}
