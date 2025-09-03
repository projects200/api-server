package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.member.dto.response.MemberProfileResponse;
import com.project200.undabang.member.dto.response.MemberScoreResponseDto;
import com.project200.undabang.member.dto.response.PreferredExercisesOfMemberResponse;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.service.MemberQueryService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberRestController.class)
@DisplayName("MemberRestController 테스트")
class MemberRestControllerTest extends AbstractRestDocSupport {
    @MockitoBean
    private MemberQueryService memberQueryService;


    private MemberProfileResponse getMemberProfileResponse() {
        MemberProfileResponse response = new MemberProfileResponse();
        response.setProfileThumbnailUrl("https://example.com/thumbnail.jpg");
        response.setProfileImageUrl("https://example.com/profile.jpg");
        response.setNickname("운동매니아");
        response.setGender(MemberGender.UNKNOWN);
        response.setBirthDate("1995-05-10");
        response.setBio("안녕하세요! 운동을 사랑하는 개발자입니다.");
        response.setYearlyExerciseDays(150);
        response.setExerciseCountInLast30Days(20);
        response.setExerciseScore(85);

        PreferredExercisesOfMemberResponse exercise1 = new PreferredExercisesOfMemberResponse();
        exercise1.setName("헬스");
        exercise1.setSkillLevel("중급");

        PreferredExercisesOfMemberResponse exercise2 = new PreferredExercisesOfMemberResponse();
        exercise2.setName("조깅");
        exercise2.setSkillLevel("상급");

        response.setPreferredExercises(List.of(exercise1, exercise2));
        return response;
    }

    @Nested
    @DisplayName("getMemberScore 메소드는")
    class GetMemberScore {
        @Test
        @DisplayName("회원 운동점수 조회를 성공한다")
        void getMemberScore_success() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Byte expectedScore = 55;
            int exerciseScoreMaxPoints = 100;
            int exerciseScoreMinPoints = 0;

            MemberScoreResponseDto respDto = MemberScoreResponseDto.builder()
                    .memberId(testMemberId)
                    .memberScore(expectedScore)
                    .policyMaxScore(exerciseScoreMaxPoints)
                    .policyMinScore(exerciseScoreMinPoints)
                    .build();

            BDDMockito.given(memberQueryService.getMemberScore()).willReturn(respDto);

            // when
            String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/score")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpect(status().isOk())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("회원 식별자"),
                                    fieldWithPath("data.memberScore").type(JsonFieldType.NUMBER).description("회원 점수"),
                                    fieldWithPath("data.policyMaxScore").type(JsonFieldType.NUMBER).description("회원이 가질 수 있는 최대 점수"),
                                    fieldWithPath("data.policyMinScore").type(JsonFieldType.NUMBER).description("회원이 가질 수 있는 최소 점수")
                            ))
                    )).andReturn().getResponse().getContentAsString();

            // then
            CommonResponse<MemberScoreResponseDto> expectedData = CommonResponse.success(respDto);
            String expected = objectMapper.writeValueAsString(expectedData);
            Assertions.assertThat(response).as("응답 본문 검증").isEqualTo(expected);
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID로 조회 시 실패한다")
        void getMemberScore_Failed_MemberNotFound() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();

            BDDMockito.given(memberQueryService.getMemberScore()).willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/score")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpect(status().isNotFound());

            // then
            BDDMockito.then(memberQueryService).should(BDDMockito.times(1)).getMemberScore();
        }

        @Test
        @DisplayName("Access 토큰이 없는 경우 401 Unauthorized 오류를 반환한다")
        public void getMemberScore_Failed_Not_Having_Token() throws Exception {
            // given, when, then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/score")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("getProfile 메소드는")
    class GetProfile {
        @Test
        @DisplayName("회원 프로필 조회를 성공한다")
        void getProfile_success() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            MemberProfileResponse respDto = getMemberProfileResponse();

            BDDMockito.given(memberQueryService.getMemberProfile()).willReturn(respDto);

            // when
            String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpect(status().isOk())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.profileThumbnailUrl").type(JsonFieldType.STRING).description("프로필 이미지의 썸네일 URL"),
                                    fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("원본 이미지 URL(썸네일이 NULL이고 원본이 있으면 이거로. 없으면 기본 이미지로)"),
                                    fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("닉네임"),
                                    fieldWithPath("data.gender").type(JsonFieldType.STRING).description("성별: MALE, FEMALE, UNKNOWN"),
                                    fieldWithPath("data.birthDate").type(JsonFieldType.STRING).description("생년월일 (YYYY-MM-DD)"),
                                    fieldWithPath("data.bio").type(JsonFieldType.STRING).description("자기소개(바이오는 인스타그램, 트위터 등 소셜 미디어에서 자신의 프로필 자기소개란에 적는 간략한 소개를 의미)"),
                                    fieldWithPath("data.yearlyExerciseDays").type(JsonFieldType.NUMBER).description("올해 누적 운동 일수"),
                                    fieldWithPath("data.exerciseCountInLast30Days").type(JsonFieldType.NUMBER).description("최근 30일간 운동 횟수"),
                                    fieldWithPath("data.exerciseScore").type(JsonFieldType.NUMBER).description("현재 운동 점수"),
                                    fieldWithPath("data.preferredExercises[]").type(JsonFieldType.ARRAY).description("사용자가 선호하는 운동 목록"),
                                    fieldWithPath("data.preferredExercises[].preferredExerciseId").type(JsonFieldType.NUMBER).description("선호 운동 ID").optional(),
                                    fieldWithPath("data.preferredExercises[].name").type(JsonFieldType.STRING).description("선호 운동 이름"),
                                    fieldWithPath("data.preferredExercises[].skillLevel").type(JsonFieldType.STRING).description("운동 수준: NOVICE, BEGINNER, INTERMEDIATE, ADVANCED, EXPERT, PROFESSIONAL(입문자, 초급자, 중급자, 고급자, 숙련자, 선출)"),
                                    fieldWithPath("data.preferredExercises[].daysOfWeek").type(JsonFieldType.ARRAY).description("운동 요일(월 ~ 일 순)").optional(),
                                    fieldWithPath("data.preferredExercises[].imageUrl").type(JsonFieldType.STRING).description("운동 이미지(최대 255자)").optional()
                            ))
                    )).andReturn().getResponse().getContentAsString();

            // then
            CommonResponse<MemberProfileResponse> expectedData = CommonResponse.success(respDto);
            String expected = objectMapper.writeValueAsString(expectedData);
            Assertions.assertThat(response).as("응답 본문 검증").isEqualTo(expected);
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID로 조회 시 실패한다")
        void getProfile_Failed_MemberNotFound() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            BDDMockito.given(memberQueryService.getMemberProfile()).willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpect(status().isNotFound());

            // then
            BDDMockito.then(memberQueryService).should(BDDMockito.times(1)).getMemberProfile();
        }

        @Test
        @DisplayName("Access 토큰이 없는 경우 401 Unauthorized 오류를 반환한다")
        public void getProfile_Failed_Not_Having_Token() throws Exception {
            // given, when, then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }
}

