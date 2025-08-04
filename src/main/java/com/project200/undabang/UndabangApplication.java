package com.project200.undabang;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan("com.project200.undabang.common.properties")
public class UndabangApplication {

    public static void main(String[] args) {
        SpringApplication.run(UndabangApplication.class, args);
    }

    @Bean
    public CommandLineRunner classExistenceCheck() {
        return args -> {
            System.out.println("### [최후 진단] 클래스패스 존재 여부 검사를 시작합니다...");
            try {
                // 1단계: FirebaseConfig 클래스 자체를 로드 시도
                Class.forName("com.project200.undabang.common.config.FirebaseConfig");
                System.out.println("### [최후 진단] OK: FirebaseConfig.class는 클래스패스에 존재합니다.");

                // 2단계: FirebaseConfig가 의존하는 핵심 클래스를 로드 시도
                Class.forName("com.google.firebase.FirebaseApp");
                System.out.println("### [최후 진단] OK: FirebaseApp.class도 클래스패스에 존재합니다. 문제는 더 복잡한 곳에 있습니다.");

            } catch (ClassNotFoundException e) {
                // 이 예외가 발생하면 범인을 잡은 것입니다.
                System.err.println("### [최후 진단] 결정적 단서 발견: 클래스를 찾을 수 없습니다!");
                e.printStackTrace(); // 어떤 클래스가 없는지 정확한 에러 메시지를 출력합니다.

                // 서버를 강제 종료시켜서라도 이 문제를 확실히 인지시킵니다.
                System.exit(1);
            }
        };
    }
}
