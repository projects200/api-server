package com.project200.undabang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@SpringBootApplication
public class UndabangApplication {

    public static void main(String[] args) {
        SpringApplication.run(UndabangApplication.class, args);
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> testEndpoint(@RequestHeader HttpHeaders headers) {
        Map<String, String> headersMap = new HashMap<>();

        headers.forEach((key, value) -> headersMap.put(key, value.getFirst()));

        return ResponseEntity.ok(headersMap);


    }
}
