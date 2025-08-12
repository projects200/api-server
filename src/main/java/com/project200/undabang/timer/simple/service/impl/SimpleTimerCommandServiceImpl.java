package com.project200.undabang.timer.simple.service.impl;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.simple.service.SimpleTimerCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SimpleTimerCommandServiceImpl implements SimpleTimerCommandService {
    private final PolicyService policyService;

    @Override
    public void createDefaultSimpleTimer() {
        int simpleTimerCount = policyService.getPolicyValueAsInt(PolicyKey.SIMPLE_TIMER_INIT_COUNT);
        String simpleTimerStr = policyService.getPolicyValueAsString(PolicyKey.SIMPLE_TIMER_INIT_VALUES);

        List<Integer> simpleTimerList = List.of(simpleTimerStr.split(",")).stream()
                .map(Integer::parseInt)
                .toList();

        if(!checkSimpleTimerApproved(simpleTimerCount, simpleTimerList)){
            log.error("DB에서 심플 타이머 목록을 조회하는데 실패하였습니다.");
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }






    }

    private boolean checkSimpleTimerApproved(int simpleTimerCount, List<Integer> simpleTimerList){
        if(simpleTimerCount == simpleTimerList.size()){
            return true;
        }
        return false;
    }

}
