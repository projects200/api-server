package com.project200.undabang.configuration;

import org.springframework.http.HttpHeaders;

import java.util.UUID;

public interface HeadersGenerator {
    static HttpHeaders getCommonApiHeaders(UUID xUserId) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer {{AccessToken}}");
        httpHeaders.add("X-USER-ID", xUserId.toString());
        return httpHeaders;
    }
    static HttpHeaders getCommonAuthHeaders(UUID xUserId, String xUserEmail) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer {{IdToken}}");
        httpHeaders.add("X-USER-ID", xUserId.toString());
        httpHeaders.add("X-USER-EMAIL", xUserEmail);
        return httpHeaders;
    }
}
