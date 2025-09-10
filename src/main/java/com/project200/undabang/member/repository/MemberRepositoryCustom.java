package com.project200.undabang.member.repository;

import java.util.UUID;

public interface MemberRepositoryCustom {

    Long countMemberExerciseInLastDays(UUID memberId, int daysAgo);

    Long countMemberExerciseInThisYear(UUID memberId);
}
