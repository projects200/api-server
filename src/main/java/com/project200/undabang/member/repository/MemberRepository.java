package com.project200.undabang.member.repository;

import com.project200.undabang.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, String> {
    boolean existsByMemberEmail(String memberEmail);
    boolean existsByMemberNickname(String memberNickname);
}
