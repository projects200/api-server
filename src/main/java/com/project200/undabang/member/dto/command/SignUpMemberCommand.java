package com.project200.undabang.member.dto.command;

import com.project200.undabang.member.enums.MemberGender;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record SignUpMemberCommand(UUID memberId, String memberEmail, String memberNickname, MemberGender memberGender,
                                  byte initialSignupPoints, LocalDate memberBday) {
}
