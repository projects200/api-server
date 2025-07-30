package com.project200.undabang.admin.component.dto.context;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserErrorContext {
    private final String requestUri; // 요청 URI
    private final String httpMethod; // HTTP METHOD
    private final UUID userIdentifier; // 유저 식별자 UUID
}
