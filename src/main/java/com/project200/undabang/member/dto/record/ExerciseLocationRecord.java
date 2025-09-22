package com.project200.undabang.member.dto.record;

import java.time.LocalDate;
import java.util.UUID;

public record ExerciseLocationRecord(UUID memberId, String profileImageUrl, String nickname, String gender,
                                     LocalDate birthDate, String exerciseLocationName, double latitude,
                                     double longitude) {
}
