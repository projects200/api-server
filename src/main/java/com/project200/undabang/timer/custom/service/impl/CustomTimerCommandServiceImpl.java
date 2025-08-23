package com.project200.undabang.timer.custom.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerNameUpdateRequest;
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

import java.util.List;
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
    public void updateCustomTimerName(Long customTimerId, CustomTimerNameUpdateRequest request) {
        Member member = getMember(UserContextHolder.getUserId());
    }

    /**
     * 이 구현체는 다음의 순서로 커스텀 타이머를 생성합니다:
     * 요청한 사용자의 유효성을 확인합니다.
     * 커스텀 타이머 스텝의 개수와 순서의 유효성을 검증합니다.
     * {@link CustomTimer} 엔티티를 먼저 저장합니다.
     * 연관된 {@link CustomTimerStep} 엔티티들을 일괄 저장합니다.
     */
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

    /**
     * 커스텀 타이머 스텝의 순서가 정확한지 검증합니다.
     */
    private void validateCustomTimerStepOrder(List<CustomTimerStepCreateRequest> request) {
        // CustomTimerStepOrder 순서검사
        for (int i = 0; i < request.size(); i++) {
            if (request.get(i).getCustomTimerStepOrder() != i) {
                throw new CustomException(ErrorCode.CUSTOM_TIMER_STEP_ORDER_INVALID);
            }
        }
    }

    /**
     * 커스텀 타이머 스텝의 개수가 정책에서 정의한 범위 내에 있는지 검증합니다.
     */
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

    /**
     * DTO 리스트를 {@link CustomTimerStep} 엔티티 리스트로 변환하고 데이터베이스에 일괄 저장합니다.
     */
    private void createStepTimerList(CustomTimer customTimer, List<CustomTimerStepCreateRequest> requestList) {
        List<CustomTimerStep> customTimerStepList = requestList.stream()
                .map(req -> req.toEntity(customTimer))
                .toList();

        customTimerStepRepository.saveAll(customTimerStepList);
    }

    /**
     * 주어진 ID로 회원 엔티티를 조회합니다. 회원이 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
