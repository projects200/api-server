package com.project200.undabang.member.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class MemberRegistrationStatusResponseDto {
    private UUID memberId;
    private boolean isRegistered;
}
