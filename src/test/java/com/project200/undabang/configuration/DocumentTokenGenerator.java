package com.project200.undabang.configuration;

public interface DocumentTokenGenerator {
    static String getAccessToken(){
        return "Bearer {AccessToken}";
    }
    static String getIdToken(){
        return "Bearer {IdToken}";
    }
}
