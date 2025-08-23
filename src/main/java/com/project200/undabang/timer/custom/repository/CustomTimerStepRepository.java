package com.project200.undabang.timer.custom.repository;

import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomTimerStepRepository extends JpaRepository<CustomTimerStep, Long> {
    List<CustomTimerStep> findAllByCustomTimerAndCustomTimerStepDeletedAtNull(CustomTimer customTimer);
=======
        import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import org.springframework.data.jpa.repository.JpaRepository;

    interface CustomTimerStepRepository extends JpaRepository<CustomTimerStep, Long> {
>>>>>>>16715

                커스텀타이머 생성
b1(feat(커스텀타이 :
        기능 기본머)

        클래스 생성(#229))
}
