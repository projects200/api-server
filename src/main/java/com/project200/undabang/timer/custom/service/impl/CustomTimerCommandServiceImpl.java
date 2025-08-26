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

        validateCustomTimerSteps(dto.getCustomTimerSteps());

        CustomTimer customTimer = CustomTimer.of(member, dto.getCustomTimerName());
        CustomTimer savedTimer = customTimerRepository.save(customTimer);

        createStepTimerList(savedTimer, dto.getCustomTimerSteps());

        return new CustomTimerCreateResponse(savedTimer.getId());
    }

    /**
     * 주어진 사용자와 타이머 ID를 기반으로 커스텀 타이머를 삭제합니다.
     * 타이머와 연관된 모든 스텝도 함께 삭제됩니다.
     */
    @Override
    public void deleteCustomTimer(Long customTimerId) {
        Member member = getMember(UserContextHolder.getUserId());
        CustomTimer customTimer = getCustomTimer(member, customTimerId);

        customTimer.deleteCustomTimer();
        // 벌크 연산을 사용하여 DB 접근 최소화 및 성능 향상
        customTimerStepRepository.softDeleteAllByCustomTimer(customTimer);
    }

    /**
     * 주어진 커스텀 타이머 ID와 요청 데이터로 커스텀 타이머를 업데이트합니다.
     * 요청된 타이머 스텝의 유효성을 검사하고, 기존 스텝을 삭제한 후 새로운 스텝을 추가합니다.
     */
    @Override
    public void updateCustomTimer(Long customTimerId, CustomTimerCreateRequest request) {
        Member member = getMember(UserContextHolder.getUserId());
        CustomTimer customTimer = getCustomTimer(member, customTimerId);

        // 커스텀 타이머 스텝 제약사항 검사
        validateCustomTimerSteps(request.getCustomTimerSteps());

        // 커스텀 타이머 이름 변경
        customTimer.updateCustomTimerName(request.getCustomTimerName());

        // 커스텀 타이머 스텝 전체 논리적 삭제
        customTimerStepRepository.softDeleteAllByCustomTimer(customTimer);

        // 커스텀 타이머 스텝 추가
        createStepTimerList(customTimer, request.getCustomTimerSteps());
    }

    /**
     * 주어진 사용자와 커스텀 타이머 ID를 기반으로 커스텀 타이머의 이름을 업데이트합니다.
     */
    @Override
    public void updateCustomTimerName(Long customTimerId, CustomTimerNameUpdateRequest request) {
        Member member = getMember(UserContextHolder.getUserId());
        CustomTimer customTimer = getCustomTimer(member, customTimerId);

        customTimer.updateCustomTimerName(request.getCustomTimerName());
    }

    /**
     * 스텝 관련 유효성 검사를 하나의 메서드로 묶음
     * create, update에서 재활용 가능한 형태로 변경
     */
    private void validateCustomTimerSteps(List<CustomTimerStepCreateRequest> steps) {
        validateCustomTimerStepOrder(steps);
        checkCustomTimerCountLimit(steps);
    }

    /**
     * 커스텀 타이머 스텝의 순서가 정확한지 검증합니다.
     */
    private void validateCustomTimerStepOrder(List<CustomTimerStepCreateRequest> request) {
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

    /**
     * 주어진 사용자와 타이머 ID를 기반으로 커스텀 타이머를 조회합니다.
     * 타이머가 존재하지 않거나 삭제된 상태일 경우 {@link CustomException}을 발생시킵니다.
     */
    private CustomTimer getCustomTimer(Member member, Long customTimerId) {
        return customTimerRepository.findByIdAndMemberAndCustomTimerDeletedAtNull(customTimerId, member)
                .orElseThrow(() -> new CustomException(ErrorCode.CUSTOM_TIMER_NOT_FOUND));
    }
}
