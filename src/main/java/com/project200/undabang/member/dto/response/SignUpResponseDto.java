package com.project200.undabang.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResponseDto {
    private UUID memberId;
    private String memberEmail;
    private String memberNickname;
    private String memberDesc;
    private char memberGender;
    private LocalDate memberBday;
    private int memberScore;
    private LocalDateTime memberCreatedAt;
}
