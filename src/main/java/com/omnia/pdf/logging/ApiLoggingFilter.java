package com.omnia.pdf.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingFilter.class);
    private static final int MAX_BODY_LOG_LENGTH = 2048;
    // 대용량 multipart 업로드의 메모리 낭비를 막기 위해 캐시 한도를 작게 둠 — JSON body 로깅에는 충분
    private static final int REQUEST_CACHE_LIMIT_BYTES = 8 * 1024;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Vite로 빌드된 정적 리소스/파비콘은 로깅 대상에서 제외
        return path.startsWith("/dist/") || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT_BYTES);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            logRequest(wrappedRequest);
            logResponse(wrappedResponse, duration);
            // 캐시된 응답을 실제 응답 스트림으로 흘려보내야 클라이언트에 전달됨
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = query != null ? uri + "?" + query : uri;
        String contentType = request.getContentType();

        if (shouldLogBody(method, contentType)) {
            log.info("--> {} {} content-type={} body={}",
                    method, fullPath, contentType, readBody(request));
        } else {
            log.info("--> {} {} content-type={}", method, fullPath, contentType);
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long durationMs) {
        log.info("<-- status={} content-type={} ({}ms)",
                response.getStatus(), response.getContentType(), durationMs);
    }

    private boolean shouldLogBody(String method, String contentType) {
        // JSON이 아닌 body(multipart, form-urlencoded, binary 등)는 의미 없거나 너무 크므로 로깅 제외
        return contentType != null && contentType.toLowerCase().startsWith("application/json");
    }

    private String readBody(ContentCachingRequestWrapper request) {
        byte[] bytes = request.getContentAsByteArray();
        if (bytes.length == 0) {
            return "";
        }
        Charset charset = resolveCharset(request.getCharacterEncoding());
        String body = new String(bytes, charset);
        if (body.length() > MAX_BODY_LOG_LENGTH) {
            return body.substring(0, MAX_BODY_LOG_LENGTH) + "...(truncated)";
        }
        return body;
    }

    private Charset resolveCharset(String name) {
        if (name == null) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(name);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
}
