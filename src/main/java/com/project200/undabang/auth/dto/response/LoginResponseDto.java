package com.project200.undabang.auth.dto.response;

import com.project200.undabang.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private UUID memberId;

    public static LoginResponseDto of(Member member) {
        return LoginResponseDto.builder()
                .memberId(member.getMemberId())
                .build();
    }
}
