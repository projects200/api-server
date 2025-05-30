package com.project200.undabang.configuration;

import org.springframework.http.HttpHeaders;

public interface DocumentTokenGenerator {
    static HttpHeaders getAccessToken(){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer {{AccessToken}}");
        return httpHeaders;
    }
    static HttpHeaders getIdToken(){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer {{IdToken}}");
        return httpHeaders;
    }
}
