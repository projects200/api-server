package com.project200.undabang.member.dto.record;

import com.project200.undabang.member.enums.MemberGender;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MemberProfileRecord(UUID memberId,
                                  String nickname,
                                  String bio,
                                  MemberGender gender,
                                  LocalDate birthDate,
                                  String profileImageUrl,
                                  String profileThumbnailUrl,
                                  Byte memberScore,
                                  List<PreferredExerciseRecord> preferredExerciseRecordList) {
}
