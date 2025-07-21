package com.project200.undabang.policy.service.impl;

import com.project200.undabang.policy.dto.record.PolicyGroupItemRecord;
import com.project200.undabang.policy.dto.response.PolicyResponseDto;
import com.project200.undabang.policy.repository.PolicyGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PolicyGroupServiceImplTest {
    @Mock
    private PolicyGroupRepository policyGroupRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache policyCache;

    @InjectMocks
    private PolicyGroupServiceImpl policyGroupService;

    @DisplayName("캐시 예열 시, DB에서 데이터를 조회하여 올바른 DTO로 가공 후 캐시에 저장한다")
    @Test
    void loadAllPoliciesIntoCache_ShouldProcessAndPutDataIntoCache() {
        when(cacheManager.getCache("policyGroups")).thenReturn(policyCache);

        // given
        List<PolicyGroupItemRecord> records = createRecordFromTestData();
        when(policyGroupRepository.findAllPoliciesWithGroupName()).thenReturn(records);

        // when
        // @PostConstruct는 자동으로 호출되지 않으므로, 테스트에서는 직접 메서드를 호출
        policyGroupService.loadAllPoliciesIntoCache();

        // then - 검증
        // 1. Repository가 정확히 1번 호출되었는지 확인
        verify(policyGroupRepository, times(1)).findAllPoliciesWithGroupName();

        // 2. ArgumentCaptor로 캐시에 저장된 실제 DTO를 체크
        ArgumentCaptor<String> groupNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Optional<PolicyResponseDto>> dtoCaptor = ArgumentCaptor.forClass(Optional.class);

        // 'exercise-score' 그룹 하나만 있으므로 put은 1번만 호출되어야 합니다.
        verify(policyCache, times(1)).put(groupNameCaptor.capture(), dtoCaptor.capture());

        // 3. 캡쳐된 데이터가 우리의 기대와 일치하는지 상세하게 검증
        String capturedGroupName = groupNameCaptor.getValue();
        Optional<PolicyResponseDto> capturedDtoOpt = dtoCaptor.getValue();

        assertThat(capturedGroupName).isEqualTo("exercise-score");
        assertThat(capturedDtoOpt).isPresent();

        PolicyResponseDto resultDto = capturedDtoOpt.get();
        assertThat(resultDto.getGroupName()).isEqualTo("exercise-score");
        // SQL 데이터에 정의된 정책은 총 8개입니다.
        assertThat(resultDto.getSize()).isEqualTo(8);
        assertThat(resultDto.getPolicies()).hasSize(8);

        // 4. (핵심) 특정 정책 키가 올바른 값으로 포함되어 있는지 샘플링하여 확인
        assertThat(resultDto.getPolicies()).anyMatch(policy ->
                policy.policyKey().equals("SIGNUP_INITIAL_POINTS") &&
                        policy.policyValue().equals("35")
        );
        assertThat(resultDto.getPolicies()).anyMatch(policy ->
                policy.policyKey().equals("PENALTY_SCORE_DECREMENT_POINTS") &&
                        policy.policyValue().equals("1")
        );
    }

    private List<PolicyGroupItemRecord> createRecordFromTestData() {
        // 'exercise-score' 그룹에 속한 정책들
        return List.of(
                new PolicyGroupItemRecord("exercise-score", "EXERCISE_SCORE_MAX_POINTS", "100", "POINTS", "회원이 가질 수 있는 최대 운동 점수"),
                new PolicyGroupItemRecord("exercise-score", "EXERCISE_SCORE_MIN_POINTS", "0", "POINTS", "회원이 가질 수 있는 최소 운동 점수"),
                new PolicyGroupItemRecord("exercise-score", "SIGNUP_INITIAL_POINTS", "35", "POINTS", "회원 가입 시 기본으로 부여되는 점수"),
                new PolicyGroupItemRecord("exercise-score", "POINTS_PER_EXERCISE", "3", "POINTS", "운동 기록 1회당 부여되는 점수 (일 1회)"),
                new PolicyGroupItemRecord("exercise-score", "EXERCISE_RECORD_VALIDITY_PERIOD", "2", "DAYS", "점수 획득이 가능한 운동 기록의 유효 기간. (단위: DAYS, HOURS, MINUTES)"),
                new PolicyGroupItemRecord("exercise-score", "EXERCISE_RECORD_MAX_PER_DAY", "1", "COUNT", "하루에 기록할 수 있는 최대 운동 횟수"),
                new PolicyGroupItemRecord("exercise-score", "PENALTY_INACTIVITY_THRESHOLD_DAYS", "7", "DAYS", "페널티가 시작되는 비활성 기준일 (이 기간 이상 운동 기록이 없을 경우)"),
                new PolicyGroupItemRecord("exercise-score", "PENALTY_SCORE_DECREMENT_POINTS", "1", "POINTS", "비활성 상태일 때 매일 차감되는 점수")
        );
    }

    @DisplayName("@Cacheable 메서드에서 Cache Miss가 발생했을 때의 로직을 검증한다")
    @Test
    void getByGroupName_ShouldReturnEmpty_WhenCacheMiss() {
        /*
         * [중요] 단위 테스트에서는 @Cacheable AOP 프록시가 동작하지 않습니다.
         * 따라서 getByGroupName()을 호출하면 항상 메서드 내부의 로직이 실행됩니다.
         * 우리는 이 "캐시가 없을 때의 동작(cache-miss 시나리오)"이 올바른지만 검증하면 됩니다.
         * 캐시 히트(Cache-hit) 시나리오(메서드 본문이 실행되지 않는 것)는 통합 테스트의 영역입니다.
         */

        // given
        String nonExistentGroup = "non_existent_group";

        // when
        Optional<PolicyResponseDto> result = policyGroupService.getByGroupName(nonExistentGroup);

        // then
        assertThat(result).isEmpty();
    }
}
