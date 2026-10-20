package com.project200.undabang.member.repository;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID>, MemberRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.memberId = :memberId")
    Optional<Member> findByIdForUpdate(@Param("memberId") UUID memberId);

    boolean existsByMemberEmail(String memberEmail);

    boolean existsByMemberNickname(String memberNickname);

    boolean existsByMemberId(UUID memberId);

    boolean existsByMemberIdAndMemberDeletedAtNull(UUID memberId);

    Optional<Member> findByMemberIdAndMemberDeletedAtNull(UUID memberId);

    Optional<Member> findByMemberEmailAndMemberDeletedAtNull(String memberEmail);

    @EntityGraph(attributePaths = {"memberPicture", "memberPicture.picture", "preferredExercises", "preferredExercises.exercise"})
    Optional<Member> findMemberProfileByMemberIdAndMemberDeletedAtNull(UUID memberId);
}