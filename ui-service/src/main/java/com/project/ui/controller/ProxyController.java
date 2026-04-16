package com.project.ui.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * Reverse proxy — forwards requests from browser to microservices server-side.
 * Eliminates CORS completely — browser only ever talks to localhost:18090.
 *
 * Routes:
 *   /proxy/iac/**      → IAC service  (auth, users, roles)
 *   /proxy/property/** → Property service
 *   /proxy/pricing/**  → Pricing service
 *   /proxy/order/**    → Order service
 */
@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
@Slf4j
public class ProxyController {

    @Value("${services.iac-url}")      private String iacUrl;
    @Value("${services.property-url}") private String propertyUrl;
    @Value("${services.pricing-url}")  private String pricingUrl;
    @Value("${services.order-url}")    private String orderUrl;

    private static final List<String> FORWARD_HEADERS = List.of("Authorization");
    private static final int    MAX_RETRIES    = 3;
    private static final long   RETRY_DELAY_MS = 800;
    private static final int    TIMEOUT_SECS   = 12;

    @RequestMapping(value = "/iac/**",      method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<String> proxyIac(HttpServletRequest request, @RequestBody(required = false) String body) {
        return proxy(iacUrl, stripPrefix(request, "/proxy/iac"), request.getMethod(), body, request);
    }

    @RequestMapping(value = "/property/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<String> proxyProperty(HttpServletRequest request, @RequestBody(required = false) String body) {
        return proxy(propertyUrl, stripPrefix(request, "/proxy/property"), request.getMethod(), body, request);
    }

    @RequestMapping(value = "/pricing/**",  method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<String> proxyPricing(HttpServletRequest request, @RequestBody(required = false) String body) {
        return proxy(pricingUrl, stripPrefix(request, "/proxy/pricing"), request.getMethod(), body, request);
    }

    @RequestMapping(value = "/order/**",    method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<String> proxyOrder(HttpServletRequest request, @RequestBody(required = false) String body) {
        return proxy(orderUrl, stripPrefix(request, "/proxy/order"), request.getMethod(), body, request);
    }

    // ── Health check — shows status of all upstream services ─────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        var client = WebClient.create();
        Map<String, String> urls = Map.of(
                "iac", iacUrl, "property", propertyUrl, "pricing", pricingUrl, "order", orderUrl);
        Map<String, Object> result = new LinkedHashMap<>();
        urls.forEach((name, url) -> {
            try {
                client.get().uri(url + "/actuator/health")
                        .retrieve().bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(2)).block();
                result.put(name, "UP");
            } catch (Exception e) {
                result.put(name, "DOWN: " + e.getMessage());
            }
        });
        result.put("proxy", "UP");
        return ResponseEntity.ok(result);
    }

    // ── Core proxy with retry ─────────────────────────────────────────────

    private ResponseEntity<String> proxy(String baseUrl, String path,
                                         String method, String body,
                                         HttpServletRequest request) {
        String query   = request.getQueryString();
        String fullPath = path + (query != null ? "?" + query : "");
        log.debug("Proxy {} {} → {}{}", method, request.getRequestURI(), baseUrl, fullPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        FORWARD_HEADERS.forEach(h -> {
            String val = request.getHeader(h);
            if (val != null) headers.set(h, val);
        });

        Exception lastEx = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                ResponseEntity<String> result = doRequest(baseUrl, fullPath, method, body, headers);
                if (attempt > 1) log.info("Proxy succeeded on attempt {}", attempt);
                return result;
            } catch (Exception e) {
                lastEx = e;
                boolean isConnRefused = e.getMessage() != null &&
                        (e.getMessage().contains("Connection refused") ||
                         e.getMessage().contains("Connection reset") ||
                         e.getMessage().contains("Failed to connect"));

                if (!isConnRefused || attempt == MAX_RETRIES) break;

                log.warn("Attempt {}/{} failed ({}), retrying in {}ms...",
                        attempt, MAX_RETRIES, e.getMessage(), RETRY_DELAY_MS * attempt);
                try { Thread.sleep(RETRY_DELAY_MS * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }

        String errMsg = lastEx != null ? lastEx.getMessage() : "unknown error";
        log.error("All {} proxy attempts failed: {}", MAX_RETRIES, errMsg);

        boolean starting = errMsg != null && (errMsg.contains("Connection refused") || errMsg.contains("Failed to connect"));
        String userMsg = starting
                ? "Service is starting up, please wait 30–60 seconds and try again"
                : "Service unavailable: " + errMsg;

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("{\"success\":false,\"error\":\"" + userMsg.replace("\"","'") + "\"}");
    }

    private ResponseEntity<String> doRequest(String baseUrl, String path,
                                             String method, String body,
                                             HttpHeaders headers) {
        var client = WebClient.create(baseUrl);

        WebClient.RequestBodySpec spec = client
                .method(HttpMethod.valueOf(method))
                .uri(path)
                .headers(h -> h.addAll(headers));

        // Use exchangeToMono to get full control over response body handling
        // (toEntity can return null body on some WebClient versions)
        Mono<ResponseEntity<String>> mono = (body != null && !body.isBlank())
                ? spec.bodyValue(body)
                      .exchangeToMono(resp -> resp.bodyToMono(String.class)
                              .defaultIfEmpty("")
                              .map(b -> ResponseEntity.status(resp.statusCode())
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .body(b)))
                : spec.exchangeToMono(resp -> resp.bodyToMono(String.class)
                              .defaultIfEmpty("")
                              .map(b -> ResponseEntity.status(resp.statusCode())
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .body(b)));

        return mono
                .onErrorResume(WebClientResponseException.class, ex ->
                        Mono.just(ResponseEntity
                                .status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(ex.getResponseBodyAsString())))
                .timeout(Duration.ofSeconds(TIMEOUT_SECS))
                .block();
    }

    private String stripPrefix(HttpServletRequest request, String prefix) {
        String uri = request.getRequestURI();
        return uri.startsWith(prefix) ? uri.substring(prefix.length()) : uri;
    }
}
