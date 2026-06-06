package com.finadvise.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.async.io-executor")
public record AsyncExecutorProperties(
        int corePoolSize,
        int maxPoolSize,
        int queueCapacity,
        String threadNamePrefix
) {}
