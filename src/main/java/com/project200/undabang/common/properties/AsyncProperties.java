package com.project200.undabang.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter@Setter
@ConfigurationProperties(prefix = "async.thread-pool.decrease-exercise-score")
public class AsyncProperties {

    private int corePoolSize = 10;
    private int maxPoolSize = 10;
    private int queueCapacity = 25;
    private String threadNamePrefix = "decreaseExerciseScoreThread-";
}