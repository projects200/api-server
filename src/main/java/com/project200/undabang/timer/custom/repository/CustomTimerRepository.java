package com.project200.undabang.timer.custom.repository;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomTimerRepository extends JpaRepository<CustomTimer, Long> {
    List<CustomTimer> findByMemberAndCustomTimerDeletedAtNull(Member member);
}
