package com.project200.undabang.member.repository;

import com.project200.undabang.member.dto.record.MemberProfileRecord;
import com.project200.undabang.member.entity.Member;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepositoryCustom {

    Long countMemberExerciseInLastDays(UUID memberId, int daysAgo);

    Long countMemberExerciseInThisYear(UUID memberId);

    List<Member> findAllByIdWithPessimisticLock(List<UUID> sortedMemberIdList);

    Optional<Member> findMemberWithProfileImage(UUID memberId);

    Optional<MemberProfileRecord> findMemberProfileWithPreferredExerciseActiveByMemberId(UUID memberId);
}