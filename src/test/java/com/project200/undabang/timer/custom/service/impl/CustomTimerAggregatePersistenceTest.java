package com.project200.undabang.timer.custom.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerNameUpdateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerStepCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerUpdateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;
import com.project200.undabang.timer.custom.dto.response.CustomTimerDetailResponse;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import com.project200.undabang.timer.custom.repository.CustomTimerRepository;
import com.project200.undabang.timer.custom.repository.CustomTimerStepRepository;
import com.project200.undabang.timer.custom.service.CustomTimerCommandService;
import com.project200.undabang.timer.custom.service.CustomTimerQueryService;
import com.project200.undabang.timer.sync.TimerDeleteRequest;
import com.project200.undabang.timer.sync.TimerEditOperation;
import com.project200.undabang.timer.sync.TimerMutationResponse;
import com.project200.undabang.timer.sync.TimerSyncService;
import com.project200.undabang.timer.sync.TimerType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@Import({TestQuerydslConfig.class, CustomTimerCommandServiceImpl.class, CustomTimerQueryServiceImpl.class, TimerSyncService.class})
class CustomTimerAggregatePersistenceTest {
    @Autowired
    private EntityManager em;
    @Autowired
    private CustomTimerCommandService commands;
    @Autowired
    private CustomTimerQueryService queries;
    @Autowired
    private CustomTimerRepository customTimerRepository;
    @Autowired
    private CustomTimerStepRepository customTimerStepRepository;
    @Autowired
    private TimerSyncService timerSyncService;
    @MockitoBean
    private PolicyService policyService;

    private UUID memberId;

    @BeforeEach
    void setUpMember() {
        memberId = UUID.randomUUID();
        Member member = Member.builder().memberId(memberId).memberEmail(memberId + "@timer.test")
                .memberNickname("aggregate").memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(2000, 1, 1)).build();
        em.persist(member);
        flushAndClear();
        UserContextHolder.setUserId(memberId);
        given(policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MIN_COUNT)).willReturn(1);
        given(policyService.getPolicyValueAsInt(PolicyKey.CUSTOM_TIMER_STEP_MAX_COUNT)).willReturn(50);
    }

    @AfterEach
    void resetUser() {
        UserContextHolder.reset();
    }

    @Test
    @DisplayName("실제 bulk clear 뒤에도 부모 이름, 기기 시각, 서버 시각과 교체 스텝 및 영수증을 함께 보존한다")
    void replacementPersistsAllParentFieldsAndReceiptAcrossBulkClear() {
        CustomTimerCreateResponse created = commands.createCustomTimer(new CustomTimerCreateRequest("최초", steps("기존", 60), UUID.randomUUID(), 100L));
        flushAndClear();
        UUID editId = UUID.randomUUID();
        CustomTimerUpdateRequest replacement = new CustomTimerUpdateRequest("교체", steps("새 스텝", 120), editId, 200L);

        TimerMutationResponse updated = commands.updateCustomTimer(created.customTimerId(), replacement);
        flushAndClear();

        CustomTimer stored = customTimerRepository.findById(created.customTimerId()).orElseThrow();
        assertThat(stored.getCustomTimerName()).isEqualTo("교체");
        assertThat(stored.getEditedAt()).isEqualTo(200L);
        assertThat(stored.getCustomTimerUpdatedAt()).isEqualTo(updated.updatedAt());
        assertThat(updated.updatedAt().getNano() % 1_000_000).isZero();
        List<CustomTimerStep> active = customTimerStepRepository.findAllByCustomTimerAndCustomTimerStepDeletedAtNull(stored);
        assertThat(active).hasSize(1);
        assertThat(active.getFirst().getCustomTimerStepName()).isEqualTo("새 스텝");
        assertThat(active.getFirst().getCustomTimerStepTime()).isEqualTo(120);
        Long stepId = active.getFirst().getId();
        assertThat(timerSyncService.findEdit(memberId, TimerType.CUSTOM, stored.getId(), editId, TimerEditOperation.REPLACE))
                .contains(updated);
        flushAndClear();

        TimerMutationResponse replayed = commands.updateCustomTimer(created.customTimerId(), replacement);
        flushAndClear();
        CustomTimerDetailResponse detail = queries.getCustomTimerDetail(created.customTimerId());
        assertThat(replayed).isEqualTo(updated);
        assertThat(detail.getUpdatedAt()).isEqualTo(updated.updatedAt());
        assertThat(detail.getCustomTimerSteps()).hasSize(1);
        assertThat(detail.getCustomTimerSteps().getFirst().customTimerStepId()).isEqualTo(stepId);
        assertThat(allSteps(created.customTimerId())).hasSize(2);
    }

    @Test
    @DisplayName("이후 이름 수정과 삭제가 있어도 생성 및 전체 교체 재전송은 원래 응답을 반환하고 스텝을 늘리지 않는다")
    void durableReplayAfterRenameAndDeletionDoesNotResurrectAggregate() {
        UUID createId = UUID.randomUUID();
        CustomTimerCreateRequest creation = new CustomTimerCreateRequest("최초", steps("기존", 60), createId, 100L);
        CustomTimerCreateResponse created = commands.createCustomTimer(creation);
        flushAndClear();
        UUID replaceId = UUID.randomUUID();
        CustomTimerUpdateRequest replacement = new CustomTimerUpdateRequest("교체", steps("교체 스텝", 120), replaceId, 200L);
        TimerMutationResponse replaced = commands.updateCustomTimer(created.customTimerId(), replacement);
        flushAndClear();
        commands.updateCustomTimerName(created.customTimerId(), new CustomTimerNameUpdateRequest("최신 이름", UUID.randomUUID(), 300L));
        flushAndClear();
        TimerMutationResponse deleted = commands.deleteCustomTimer(created.customTimerId(), new TimerDeleteRequest(UUID.randomUUID(), 1L));
        flushAndClear();

        assertThat(commands.createCustomTimer(creation)).isEqualTo(created);
        assertThat(commands.updateCustomTimer(created.customTimerId(), replacement)).isEqualTo(replaced);
        flushAndClear();
        CustomTimer stored = customTimerRepository.findById(created.customTimerId()).orElseThrow();
        assertThat(stored.getCustomTimerName()).isEqualTo("최신 이름");
        assertThat(stored.getCustomTimerDeletedAt()).isNotNull();
        assertThat(stored.getCustomTimerUpdatedAt()).isEqualTo(deleted.updatedAt());
        assertThat(customTimerStepRepository.findAllByCustomTimerAndCustomTimerStepDeletedAtNull(stored)).isEmpty();
        assertThat(allSteps(created.customTimerId())).hasSize(2);
        assertThat(customTimerRepository.findAllByMemberAndCustomTimerDeletedAtNull(stored.getMember())).isEmpty();
        assertThatThrownBy(() -> queries.getCustomTimerDetail(stored.getId()))
                .isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_NOT_FOUND);
    }

    private List<CustomTimerStepCreateRequest> steps(String name, int seconds) {
        return List.of(new CustomTimerStepCreateRequest(name, (byte) 0, seconds));
    }

    private List<CustomTimerStep> allSteps(Long timerId) {
        return em.createQuery("select step from CustomTimerStep step where step.customTimer.id = :timerId", CustomTimerStep.class)
                .setParameter("timerId", timerId).getResultList();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
