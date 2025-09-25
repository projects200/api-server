package com.project200.undabang.member.service.impl;

import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ExerciseLocationQueryServiceImplTest {

    @InjectMocks
    private ExerciseLocationQueryServiceImpl exerciseLocationQueryService;

    @Mock
    private ExerciseLocationRepository exerciseLocationRepository;

    @Nested
    @DisplayName("getMembersExerciseLocations 메소드는")
    class Describe_getMembersExerciseLocations {

        @Test
        @DisplayName("레포지토리에서 받은 데이터를 그대로 반환한다")
        void it_returns_data_from_repository() {
            // given
            UUID memberId = UUID.randomUUID();
            LocalDate birthDate = LocalDate.of(1990, 1, 1);

            List<ExerciseLocationRecord> locations = List.of(
                    new ExerciseLocationRecord("헬스장A", 37.5, 127.0)
            );

            GetMembersExerciseLocationsResponse response = GetMembersExerciseLocationsResponse.builder()
                    .memberId(memberId)
                    .profileThumbnailUrl("url1")
                    .profileImageUrl("url1")
                    .nickname("user1")
                    .gender(MemberGender.MALE)
                    .birthDate(birthDate)
                    .locations(locations)
                    .build();

            List<GetMembersExerciseLocationsResponse> mockResponses = List.of(response);

            given(exerciseLocationRepository.getMembersExerciseLocations()).willReturn(mockResponses);

            // when
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationQueryService.getMembersExerciseLocations();

            // then
            assertThat(results).isEqualTo(mockResponses);
        }

        @Test
        @DisplayName("레포지토리에서 빈 리스트를 반환하면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_repository_returns_empty() {
            // given
            given(exerciseLocationRepository.getMembersExerciseLocations()).willReturn(List.of());

            // when
            List<GetMembersExerciseLocationsResponse> results = exerciseLocationQueryService.getMembersExerciseLocations();

            // then
            assertThat(results).isEmpty();
        }
    }
}