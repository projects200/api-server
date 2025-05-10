package com.project200.undabang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<Map<String, String>> testEndpoint(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && !authHeader.isBlank()) {
            // 토큰이 존재할 경우
            Map<String, String> response = new HashMap<>();
            response.put("message", "Authorized");
            response.put("AUTH INPUT HEADER come", "Congrats!");
            return ResponseEntity.ok(response);
        } else {
            // 토큰이 없을 경우
            Map<String, String> response = new HashMap<>();
            response.put("error", "Unauthorized");
            response.put("AUTH INPUT Didn't come", "OOPS!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/open")
    public ResponseEntity<Map<String, String>> testOpenEndpoint() {
        Map<String, String> map = new HashMap<>();
        map.put("opened", "url");
        map.put("no token", "url");
        return ResponseEntity.ok().body(map);
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, String>> testUserEndPoint(@RequestHeader(value = "X-USER-ID", required = false) String userId) {
        // 토큰이 존재할 경우
        Map<String, String> response = new HashMap<>();
        response.put("message", "Authorized");
        response.put("이 메시지는 https://www.undabang.store", "/user - id token");
        response.put("X-USER-ID : ", userId);
        return ResponseEntity.ok(response);
    }
}
