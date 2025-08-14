package com.project200.undabang.timer.simple.repository;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.timer.simple.entity.SimpleTimer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimpleTimerRepository extends JpaRepository<SimpleTimer, Long> {
    List<SimpleTimer> findByMemberAndSimpleTimerDeletedAtNull(Member member);
    Optional<SimpleTimer> findByIdAndMemberAndSimpleTimerDeletedAtNull(Long id, Member member);

    int countDistinctByMemberAndSimpleTimerDeletedAtNull(Member member);
}
