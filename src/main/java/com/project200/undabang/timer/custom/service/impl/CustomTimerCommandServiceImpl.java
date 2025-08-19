package com.project200.undabang.timer.custom.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerStepCreateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import com.project200.undabang.timer.custom.repository.CustomTimerRepository;
import com.project200.undabang.timer.custom.repository.CustomTimerStepRepository;
import com.project200.undabang.timer.custom.service.CustomTimerCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CustomTimerCommandServiceImpl implements CustomTimerCommandService {
    private final CustomTimerRepository customTimerRepository;
    private final CustomTimerStepRepository customTimerStepRepository;
    private final MemberRepository memberRepository;
    private final PolicyService policyService;

    @Override
    public CustomTimerCreateResponse createCustomTimer(CustomTimerCreateRequest dto) {
        Member member = getMember(UserContextHolder.getUserId());

        checkCustomTimerCountLimit(dto.getCustomTimerSteps());
        validateCustomTimerStepOrder(dto.getCustomTimerSteps());

        CustomTimer customTimer = CustomTimer.of(member, dto.getCustomTimerName());
        CustomTimer savedTimer = customTimerRepository.save(customTimer);

        createStepTimerList(savedTimer, dto.getCustomTimerSteps());

        return new CustomTimerCreateResponse(savedTimer.getId());
    }

    private void validateCustomTimerStepOrder(List<CustomTimerStepCreateRequest> request) {
        List<Byte> orders = request.stream()
                .map(CustomTimerStepCreateRequest::getCustomTimerStepOrder)
                .toList();

        Set<Byte> uniqueOrders = new HashSet<>(orders);

        if (orders.size() != uniqueOrders.size()) {
            throw new CustomException(ErrorCode.CUSTOM_TIMER_STEP_ORDER_DUPLICATED);
        }
    }

    private void checkCustomTimerCountLimit(List<CustomTimerStepCreateRequest> request) {
        int minStepCount = policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MIN_COUNT);
        int maxStepCount = policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MAX_COUNT);

        if (request.size() < minStepCount) {
            throw new CustomException(ErrorCode.CUSTOM_TIMER_STEP_MIN_COUNT_VIOLATION);
        }
        if (request.size() > maxStepCount) {
            throw new CustomException(ErrorCode.CUSTOM_TIMER_STEP_MAX_COUNT_VIOLATION);
        }
    }

    private void createStepTimerList(CustomTimer customTimer, List<CustomTimerStepCreateRequest> requestList) {
        List<CustomTimerStep> customTimerStepList = requestList.stream()
                .map(req -> req.toEntity(customTimer))
                .toList();

        customTimerStepRepository.saveAll(customTimerStepList);
    }

    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
