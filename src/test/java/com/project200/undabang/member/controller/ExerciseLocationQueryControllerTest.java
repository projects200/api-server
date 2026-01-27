package com.project200.undabang.member.controller;

import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.member.controller.location.ExerciseLocationQueryController;
import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
import com.project200.undabang.member.dto.record.PreferredExerciseRecord;
import com.project200.undabang.member.dto.record.Viewport;
import com.project200.undabang.member.dto.response.GetExerciseLocationsResponse;
import com.project200.undabang.member.dto.response.GetOtherMemberExerciseLocationsResponse;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.service.ExerciseLocationQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFieldsForList;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
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

            PreferredExerciseRecord preferredExercise = new PreferredExerciseRecord(
                    1L,
                    "다이어트 복싱",
                    ExerciseSkillLevel.BEGINNER,
                    (byte) 3,
                    "https://www.example.com/image.jpg"
            );

            List<GetOtherMemberExerciseLocationsResponse> responseList = List.of(
                    GetOtherMemberExerciseLocationsResponse.builder()
                            .memberId(memberId1)
                            .profileThumbnailUrl("https://www.example.com/thumbnail1.jpg")
                            .profileImageUrl("https://www.example.com/profile1.jpg")
                            .nickname("운동맨")
                            .gender(MemberGender.MALE)
                            .birthDate(LocalDate.of(1990, 1, 15))
                            .memberScore((byte) 50)
                            .locations(java.util.Set.of(
                                    new ExerciseLocationRecord(1L, "헬스장 A", 37.5665, 126.9780),
                                    new ExerciseLocationRecord(2L, "수영장 B", 37.5796, 126.9770)))
                            .preferredExercises(java.util.Set.of(preferredExercise))
                            .build());

            Viewport expectedViewport = new Viewport(37.7, 126.8, 37.4, 127.2);

            BDDMockito.given(exerciseLocationQueryService.getMembersExerciseLocations(BDDMockito.refEq(expectedViewport))).willReturn(responseList);

            // when & then
            mockMvc.perform(get("/api/v1/members")
                            .queryParam("leftTopLatitude", "37.7")
                            .queryParam("leftTopLongitude", "126.8")
                            .queryParam("rightBottomLatitude", "37.4")
                            .queryParam("rightBottomLongitude", "127.2")
                            .headers(getCommonApiHeaders(memberId1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.data[0].memberId").value(memberId1.toString()),
                            jsonPath("$.data[0].nickname").value("운동맨"),
                            // 선호 운동 검증
                            jsonPath("$.data[0].preferredExercises[0].name").value("다이어트 복싱"),
                            jsonPath("$.data[0].preferredExercises[0].skillLevel").value("BEGINNER"),
                            // boolean[] 배열로 변환되어 내려오는지 확인
                            jsonPath("$.data[0].preferredExercises[0].daysOfWeek").isArray(),
                            jsonPath("$.data[0].preferredExercises[0].daysOfWeek[0]").value(true)
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            queryParameters(
                                    parameterWithName("leftTopLatitude").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("좌상단 위도 (-90 ~ 90)를 나타냅니다."),
                                    parameterWithName("leftTopLongitude").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("좌상단 경도 (-180 ~ 180)를 나타냅니다."),
                                    parameterWithName("rightBottomLatitude").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("우하단 위도 (-90 ~ 90)를 나타냅니다."),
                                    parameterWithName("rightBottomLongitude").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("우하단 경도 (-180 ~ 180)를 나타냅니다.")
                            ),
                            responseFields(commonResponseFieldsForList(
                                    fieldWithPath("data[].memberId").type(STRING).description("다른 회원의 식별자(UUID)를 나타냅니다."),
                                    fieldWithPath("data[].profileThumbnailUrl").type(STRING).description("다른 회원의 썸네일 이미지 URL 정보입니다."),
                                    fieldWithPath("data[].profileImageUrl").type(STRING).description("다른 회원의 프로필 이미지 URL 정보입니다."),
                                    fieldWithPath("data[].nickname").type(STRING).description("다른 회원의 닉네임을 나타냅니다."),
                                    fieldWithPath("data[].gender").type(STRING).description("다른 회원의 성별 (MALE, FEMALE, UNKNOWN) 입니다."),
                                    fieldWithPath("data[].birthDate").type(STRING).description("다른 회원의 생년월일 정보입니다."),
                                    fieldWithPath("data[].memberScore").type(NUMBER).optional().description("다른 회원의 운동 점수 (0~100) 입니다."),

                                    fieldWithPath("data[].locations[]").type(ARRAY).description("다른 회원이 저장한 운동 위치 목록입니다."),
                                    fieldWithPath("data[].locations[].exerciseLocationId").type(NUMBER).description("다른 회원이 저장한 운동 위치 목록의 식별자 정보 입니다."),
                                    fieldWithPath("data[].locations[].exerciseLocationName").type(STRING).description("운동 장소의 이름입니다."),
                                    fieldWithPath("data[].locations[].latitude").type(NUMBER).description("운동 장소의 위도 정보입니다."),
                                    fieldWithPath("data[].locations[].longitude").type(NUMBER).description("운동 장소의 경도 정보입니다."),

                                    fieldWithPath("data[].preferredExercises[]").type(ARRAY).optional().description("다른 회원의 선호 운동 목록입니다."),
                                    fieldWithPath("data[].preferredExercises[].preferredExerciseId").type(NUMBER).optional().description("선호 운동의 ID입니다."),
                                    fieldWithPath("data[].preferredExercises[].name").type(STRING).optional().description("선호 운동의 종목 이름입니다."),
                                    fieldWithPath("data[].preferredExercises[].skillLevel").type(STRING).optional().description("숙련도 (BEGINNER, ROOKIE, INTERMEDIATE, ADVANCED, SKILLED, PRO) 정보를 나타냅니다."),
                                    fieldWithPath("data[].preferredExercises[].daysOfWeek").type(ARRAY).optional().description("운동 요일 정보 (boolean 배열, 인덱스 0부터 월요일~일요일)를 의미합니다."),
                                    fieldWithPath("data[].preferredExercises[].imageUrl").type(STRING).optional().description("선호 운동 종목의 대표 이미지 URL입니다.")
                            ))));

            BDDMockito.then(exerciseLocationQueryService).should(BDDMockito.times(1))
                    .getMembersExerciseLocations(BDDMockito.refEq(expectedViewport));
        }

        @Test
        @DisplayName("조회된 데이터가 없으면 빈 리스트를 반환한다")
        void getMembersExerciseLocations_Success_EmptyList() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            BDDMockito.given(exerciseLocationQueryService.getMembersExerciseLocations(
                            BDDMockito.any(Viewport.class)))
                    .willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/v1/members")
                            .queryParam("leftTopLatitude", "37.5")
                            .queryParam("leftTopLongitude", "127.0")
                            .queryParam("rightBottomLatitude", "37.4")
                            .queryParam("rightBottomLongitude", "127.1")
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.data").isArray(),
                            jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("유효하지 않은 좌표 범위가 입력되면 예외가 발생한다")
        void getMembersExerciseLocations_Fail_InvalidRange() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/members")
                            .queryParam("leftTopLatitude", "100.0")   // 오류: 90보다 큼
                            .queryParam("leftTopLongitude", "126.8")
                            .queryParam("rightBottomLatitude", "37.4")
                            .queryParam("rightBottomLongitude", "127.2")
                            .headers(getCommonApiHeaders(UUID.randomUUID()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest()); // 400 에러 예상
        }

        @Test
        @DisplayName("필수 파라미터가 누락되면 예외가 발생한다")
        void getMembersExerciseLocations_Fail_MissingParam() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/members")
                            // .queryParam("leftTopLatitude", "37.7") // 누락 시킴
                            .queryParam("leftTopLongitude", "126.8")
                            .queryParam("rightBottomLatitude", "37.4")
                            .queryParam("rightBottomLongitude", "127.2")
                            .headers(getCommonApiHeaders(UUID.randomUUID()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/exercise-locations API는")
    class GetExerciseLocations {

        @Test
        @DisplayName("현재 로그인한 사용자의 운동 장소 목록을 성공적으로 조회한다")
        void getExerciseLocations_Success() throws Exception {
            // given
            UUID currentMemberId = UUID.randomUUID();
            List<GetExerciseLocationsResponse> responseList = List.of(
                    GetExerciseLocationsResponse.builder()
                            .id(1L)
                            .name("강남 스포애니")
                            .address("서울 강남구 테헤란로 123")
                            .latitude(37.5017)
                            .longitude(127.0396)
                            .build(),
                    GetExerciseLocationsResponse.builder()
                            .id(2L)
                            .name("우리집 홈짐")
                            .address("서울 서초구 반포대로 456")
                            .latitude(37.4923)
                            .longitude(127.0086)
                            .build());

            BDDMockito.given(exerciseLocationQueryService.getExerciseLocations()).willReturn(responseList);

            // when & then
            mockMvc.perform(get("/api/v1/exercise-locations")
                            .headers(getCommonApiHeaders(currentMemberId)) // 인증 토큰 포함
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.data").isArray(),
                            jsonPath("$.data[0].id").value(1L),
                            jsonPath("$.data[0].name").value("강남 스포애니"),
                            jsonPath("$.data[1].latitude").value(37.4923))
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFieldsForList(
                                    fieldWithPath("data[].id").type(NUMBER).description("운동 장소의 고유 식별자 정보를 나타냅니다."),
                                    fieldWithPath("data[].name").type(STRING).description("운동 장소의 이름(상호명) 입니다."),
                                    fieldWithPath("data[].address").type(STRING).description("운동 장소의 도로명 주소(혹은 지번주소) 입니다."),
                                    fieldWithPath("data[].latitude").type(NUMBER).description("운동 장소의 위도 정보 입니다."),
                                    fieldWithPath("data[].longitude").type(NUMBER).description("운동 장소의 경도 정보 입니다.")))));

            BDDMockito.then(exerciseLocationQueryService)
                    .should(BDDMockito.times(1))
                    .getExerciseLocations();
        }

        @Test
        @DisplayName("조회된 운동 장소가 없으면 빈 리스트를 반환한다")
        void getExerciseLocations_Success_EmptyList() throws Exception {
            // given
            UUID currentMemberId = UUID.randomUUID();
            BDDMockito.given(exerciseLocationQueryService.getExerciseLocations()).willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/v1/exercise-locations")
                            .headers(getCommonApiHeaders(currentMemberId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.data").isArray(),
                            jsonPath("$.data").isEmpty());

            BDDMockito.then(exerciseLocationQueryService)
                    .should(BDDMockito.times(1))
                    .getExerciseLocations();
        }
    }
}