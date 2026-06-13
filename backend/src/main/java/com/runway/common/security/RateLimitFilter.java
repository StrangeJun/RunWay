package com.runway.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 인증 엔드포인트 IP 기반 Rate Limiting.
 * 10초 슬라이딩 윈도우에서 최대 10회 허용.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/auth/login", "/api/auth/signup",
            "/api/auth/reissue", "/api/auth/google", "/api/auth/kakao", "/api/auth/naver",
            "/api/auth/email-verification/signup/request",
            "/api/auth/email-verification/signup/verify",
            "/api/auth/password-reset/request",
            "/api/auth/password-reset/verify",
            "/api/auth/password-reset/confirm",
            "/api/auth/nickname/check"
    );
    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS   = 10_000L;

    private final Map<String, Deque<Long>> requests = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        if (!RATE_LIMITED_PATHS.contains(req.getRequestURI())) {
            chain.doFilter(req, res);
            return;
        }

        String ip  = resolveIp(req);
        long   now = System.currentTimeMillis();
        Deque<Long> timestamps = requests.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        // 만료된 항목 제거
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_REQUESTS) {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "success",   false,
                    "message",   "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.",
                    "errorCode", "TOO_MANY_REQUESTS"
            )));
            return;
        }

        timestamps.addLast(now);
        chain.doFilter(req, res);
    }

    private static String resolveIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
