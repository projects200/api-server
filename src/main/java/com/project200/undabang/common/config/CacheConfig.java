package com.project200.undabang.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.project200.undabang.chat.entity.TicketInfoRecord;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean(name = "chatTicketCache")
    public Cache<UUID, TicketInfoRecord> chatTicketCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS) // 캐시에 작성 후 30초 뒤에 삭제되도록 설정
                .maximumSize(10000) // 최대 10000개의 캐시까지 저장 가능
                .build();
    }
}