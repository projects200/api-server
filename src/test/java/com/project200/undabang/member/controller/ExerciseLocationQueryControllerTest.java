package com.project200.undabang.member.controller;

import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.member.controller.location.ExerciseLocationQueryController;
import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.service.ExerciseLocationQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFieldsForList;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExerciseLocationQueryController.class)
class ExerciseLocationQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private ExerciseLocationQueryService exerciseLocationQueryService;

    @Nested
    @DisplayName("GET /api/v1/members API는")
    class GetMembersExerciseLocations {

        @Test
        @DisplayName("회원들의 운동 장소 목록을 성공적으로 조회한다")
        void getMembersExerciseLocations_Success() throws Exception {
            // given
            UUID memberId1 = UUID.randomUUID();
            List<GetMembersExerciseLocationsResponse> responseList = List.of(
                    GetMembersExerciseLocationsResponse.builder()
                            .memberId(memberId1)
                            .profileImageUrl("http://example.com/profile1.jpg")
                            .nickname("운동맨")
                            .gender(MemberGender.MALE)
                            .birthDate(LocalDate.of(1990, 1, 15))
                            .locations(List.of(
                                    new ExerciseLocationRecord("헬스장 A", 37.5665, 126.9780),
                                    new ExerciseLocationRecord("수영장 B", 37.5796, 126.9770)
                            ))
                            .build()
            );

            BDDMockito.given(exerciseLocationQueryService.getMembersExerciseLocations()).willReturn(responseList);

            // when & then
            mockMvc.perform(get("/api/v1/members")
                            .headers(getCommonApiHeaders(memberId1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.data").isArray(),
                            jsonPath("$.data[0].memberId").value(memberId1.toString()),
                            jsonPath("$.data[0].nickname").value("운동맨"),
                            jsonPath("$.data[0].locations[0].exerciseLocationName").value("헬스장 A")
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFieldsForList(
                                    fieldWithPath("data[].memberId").type(STRING).description("다른 회원의 식별자(UUID)를 나타냅니다. "),
                                    fieldWithPath("data[].profileImageUrl").type(STRING).description("다른 회원의 썸네일 프로필 이미지 URL 정보입니다."),
                                    fieldWithPath("data[].nickname").type(STRING).description("다른 회원의 닉네임값을 나타냅니다."),
                                    fieldWithPath("data[].gender").type(STRING).description("다른 회원의 성별 (MALE, FEMALE, UNKNOWN) 정보를 나타냅니다."),
                                    fieldWithPath("data[].birthDate").type(STRING).description("다른 회원의 생년월일 (YYYY-MM-DD) 정보를 나타냅니다."),
                                    fieldWithPath("data[].locations[]").type(ARRAY).description("다른 회원이 저장한 운동 위치 목록 입니다."),
                                    fieldWithPath("data[].locations[].exerciseLocationName").type(STRING).description("카카오맵 API 에서 반환하거나 본인이 저장한 운동 장소 이름 입니다."),
                                    fieldWithPath("data[].locations[].latitude").type(NUMBER).description("운동 장소의 위도 정보 입니다."),
                                    fieldWithPath("data[].locations[].longitude").type(NUMBER).description("운동 장소의 경도 정보 입니다.")
                            ))
                    ));

            BDDMockito.then(exerciseLocationQueryService).should(BDDMockito.times(1)).getMembersExerciseLocations();
        }

        @Test
        @DisplayName("조회된 데이터가 없으면 빈 리스트를 반환한다")
        void getMembersExerciseLocations_Success_EmptyList() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            BDDMockito.given(exerciseLocationQueryService.getMembersExerciseLocations()).willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/v1/members")
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.data").isArray(),
                            jsonPath("$.data").isEmpty()
                    );

            BDDMockito.then(exerciseLocationQueryService).should(BDDMockito.times(1)).getMembersExerciseLocations();
        }
    }
}