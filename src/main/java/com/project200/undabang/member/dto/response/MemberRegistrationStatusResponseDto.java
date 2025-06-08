package com.project200.undabang.member.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;


@Builder
public class MemberRegistrationStatusResponseDto {
    @Getter
    private UUID memberId;

    private boolean isRegistered;

    @JsonProperty("isRegistered")
    public boolean isRegistered() {
        return isRegistered;
    }
}
