package com.project200.undabang.member.repository;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberBlockRepository extends JpaRepository<MemberBlock, Long>, MemberBlockRepositoryCustom {
    Optional<MemberBlock> findByBlockerAndBlocked(Member blocker, Member blocked);
    Optional<MemberBlock> findByBlockerAndBlockedAndMemberBlockDeletedAtNull(Member blocker, Member blocked);
}
