package com.example.demo.auth.vendor;

import com.example.demo.auth.config.Msg91Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class Msg91OtpClient {
    
    private final Msg91Properties msg91Properties;
    private final WebClient webClient;
    
    public Msg91OtpClient(Msg91Properties msg91Properties) {
        this.msg91Properties = msg91Properties;
        this.webClient = WebClient.builder()
                .baseUrl(msg91Properties.getBaseUrl())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .filter((request, next) -> {
                    log.info("=== WebClient Request Debug ===");
                    log.info("Request Method: {}", request.method());
                    log.info("Request URL: {}", request.url());
                    log.info("Request Headers: {}", request.headers());
                    log.info("===============================");
                    
                    return next.exchange(request)
                            .doOnNext(response -> {
                                log.info("=== WebClient Response Debug ===");
                                log.info("Response Status: {}", response.statusCode());
                                log.info("Response Headers: {}", response.headers().asHttpHeaders());
                                log.info("=================================");
                            });
                })
                .build();
    }
    
    public Mono<Msg91OtpResponse> generateOtp(String email) {
        Msg91OtpRequest request = new Msg91OtpRequest(
                msg91Properties.getWidgetId(),
                email
        );
        
        String fullUrl = msg91Properties.getBaseUrl() + msg91Properties.getOtp().getGeneratePath();
        
        log.info("=== MSG91 OTP Widget Request Debug ===");
        log.info("Identifier (Email): {}", email);
        log.info("Widget ID: {}", msg91Properties.getWidgetId());
        log.info("Auth Key: {}", msg91Properties.getAuthKey());
        log.info("Full URL: {}", fullUrl);
        log.info("Request Body: {}", request);
        log.info("=====================================");
        
        return webClient.post()
                .uri(msg91Properties.getOtp().getGeneratePath())
                .header("authkey", msg91Properties.getAuthKey())
                .header("content-type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Msg91OtpResponse.class)
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(response -> {
                    log.info("=== MSG91 OTP Widget Response Debug ===");
                    log.info("Response: {}", response);
                    log.info("Response Type: {}", response.getType());
                    log.info("Response Message (Request ID): {}", response.getMessage());
                    log.info("Request ID: {}", response.getRequestId());
                    log.info("=====================================");
                })
                .doOnError(error -> {
                    log.error("=== MSG91 OTP Error Debug ===");
                    log.error("Error for email: {}", email, error);
                    log.error("=============================");
                })
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.error("=== MSG91 API Error Debug ===");
                    log.error("Status Code: {}", ex.getStatusCode());
                    log.error("Response Body: {}", ex.getResponseBodyAsString());
                    log.error("Request URL: {}", ex.getRequest().getURI());
                    log.error("Request Method: {}", ex.getRequest().getMethod());
                    log.error("=============================");
                    return new RuntimeException("Failed to send OTP: " + ex.getResponseBodyAsString());
                });
    }
    
    public Mono<Msg91OtpVerifyResponse> verifyOtp(String requestId, String code) {
        Msg91OtpVerifyRequest request = new Msg91OtpVerifyRequest(
                msg91Properties.getWidgetId(),
                requestId,
                code
        );
        
        String fullUrl = msg91Properties.getBaseUrl() + msg91Properties.getOtp().getVerifyPath();
        
        log.info("=== MSG91 OTP Widget Verify Request Debug ===");
        log.info("Widget ID: {}", msg91Properties.getWidgetId());
        log.info("Request ID: {}", requestId);
        log.info("OTP Code: {}", code);
        log.info("Auth Key: {}", msg91Properties.getAuthKey());
        log.info("Full URL: {}", fullUrl);
        log.info("Request Body: {}", request);
        log.info("===========================================");
        
        return webClient.post()
                .uri(msg91Properties.getOtp().getVerifyPath())
                .header("authkey", msg91Properties.getAuthKey())
                .header("content-type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Msg91OtpVerifyResponse.class)
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(response -> {
                    log.info("=== MSG91 OTP Verify Response Debug ===");
                    log.info("Response: {}", response);
                    log.info("======================================");
                })
                .doOnError(error -> {
                    log.error("=== MSG91 OTP Verify Error Debug ===");
                    log.error("Error for requestId: {}", requestId, error);
                    log.error("===================================");
                })
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.error("=== MSG91 API Verify Error Debug ===");
                    log.error("Status Code: {}", ex.getStatusCode());
                    log.error("Response Body: {}", ex.getResponseBodyAsString());
                    log.error("Request URL: {}", ex.getRequest().getURI());
                    log.error("Request Method: {}", ex.getRequest().getMethod());
                    log.error("===================================");
                    return new RuntimeException("Failed to verify OTP: " + ex.getResponseBodyAsString());
                });
    }
}
