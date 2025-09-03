package com.project200.undabang.member.repository;

import com.project200.undabang.member.entity.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID>, MemberRepositoryCustom {

    boolean existsByMemberEmail(String memberEmail);

    boolean existsByMemberNickname(String memberNickname);

    boolean existsByMemberId(UUID memberId);

    Optional<Member> findByMemberIdAndMemberDeletedAtNull(UUID memberId);

    @EntityGraph(attributePaths = {"memberPicture", "memberPicture.picture", "preferredExercises", "preferredExercises.exercise"})
    Optional<Member> findMemberProfileByMemberIdAndMemberDeletedAtNull(UUID memberId);
}