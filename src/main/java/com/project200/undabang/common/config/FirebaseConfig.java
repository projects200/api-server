package com.project200.undabang.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.project200.undabang.common.properties.FirebaseProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
public class FirebaseConfig {

    private final FirebaseProperties firebaseProperties;

    public FirebaseConfig(FirebaseProperties firebaseProperties) {
        this.firebaseProperties = firebaseProperties;
    }

    /**
     * 'firebase.enabled=true'일 때만 FirebaseApp을 초기화하고 FirebaseMessaging 인스턴스를 Bean으로 등록합니다.
     *
     * @return FirebaseMessaging 인스턴스
     */
    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        log.info("FCM 설정을 초기화합니다. (firebase.enabled=true)");

        System.exit(1);

        String path = firebaseProperties.credentials().path();
        if (path == null || path.isBlank()) {
            throw new IOException("FCM 초기화 오류: 'firebase.credentials.path' 속성이 설정되지 않았습니다.");
        }

        ClassPathResource resource = new ClassPathResource(path.replace("classpath:", ""));

        try (InputStream credentials = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();

            // 중복 초기화 방지
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp이 성공적으로 초기화되었습니다.");
            }

            return FirebaseMessaging.getInstance();
        } catch (IOException e) {
            log.error("FCM 설정 초기화에 실패했습니다. 키 파일 경로: {}", path, e);
            throw new IOException("FCM 초기화 오류: 서비스 계정 키 파일을 찾거나 읽을 수 없습니다.", e);
        }
    }
}