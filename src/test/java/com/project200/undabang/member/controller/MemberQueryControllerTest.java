package com.project200.undabang.member.controller;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.member.dto.response.GetOtherMemberProfileResponse;
import com.project200.undabang.member.dto.response.MemberProfileResponse;
import com.project200.undabang.member.dto.response.MemberScoreResponseDto;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberQueryController.class)
@DisplayName("MemberRestController 테스트")
class MemberQueryControllerTest extends AbstractRestDocSupport {
    @MockitoBean
    private MemberQueryService memberQueryService;

    private MemberProfileResponse getMemberProfileResponse() {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail("test@gmail.com")
                .memberNickname("테스트유저")
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(1990, 1, 1))
                .memberScore((byte) 50)
                .memberDesc("테스트 자기소개입니다.")
                .build();

        Picture picture = Picture.builder()
                .id(1L)
                .pictureName("profile_image.jpg")
                .pictureExtension(".jpg")
                .pictureSize(1024)
                .pictureUrl("http://example.com/profile_image.jpg")
                .build();

        MemberPicture memberPicture = MemberPicture.builder()
                .id(1L)
                .memberPicturesUrl("http://example.com/profile_thumbnail.jpg")
                .picture(picture)
                .member(member)
                .memberPicturesName("profile_thumbnail.jpg")
                .memberPicturesUrl("http://example.com/profile_thumbnail.jpg")
                .build();

        member.updateProfilePicture(memberPicture);

        ExerciseType exerciseType1 = ExerciseType.builder()
                .id(1L)
                .exerciseName("헬스")
                .exerciseTypeImageUrl("http://example.com/exercise/weight_training.jpg")
                .build();

        ExerciseType exerciseType2 = ExerciseType.builder()
                .id(2L)
                .exerciseName("러닝")
                .exerciseTypeImageUrl("http://example.com/exercise/weight_training.jpg")
                .build();

        PreferredExercise preferredExercise1 = PreferredExercise.builder()
                .id(1L)
                .exercise(exerciseType1)
                .member(member)
                .preferredExerciseSkillLevel(ExerciseSkillLevel.INTERMEDIATE)
                .build();

        PreferredExercise preferredExercise2 = PreferredExercise.builder()
                .id(2L)
                .exercise(exerciseType2)
                .member(member)
                .preferredExerciseSkillLevel(ExerciseSkillLevel.PRO)
                .build();

        preferredExercise1.setDaysOfWeek(new boolean[]{true, true, true, true, true, true, true});

        List<PreferredExercise> preferredExercises = List.of(preferredExercise1, preferredExercise2);

        ReflectionTestUtils.setField(member, "preferredExercises", preferredExercises);

        return MemberProfileResponse.of(member, 365, 20);
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
                                    fieldWithPath("data.preferredExercises[].skillLevel").type(JsonFieldType.STRING).description("운동 수준: BEGINNER, ROOKIE, INTERMEDIATE, ADVANCED, SKILLED, PRO(입문자, 초급자, 중급자, 고급자, 숙련자, 선출)"),
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

    @Nested
    @DisplayName("getOtherMemberProfile 메소드는")
    class GetOtherMemberProfile {

        private GetOtherMemberProfileResponse getOtherMemberProfileResponse() {
            Member member = Member.builder()
                    .memberId(UUID.randomUUID())
                    .memberEmail("other@gmail.com")
                    .memberNickname("다른회원")
                    .memberGender(MemberGender.UNKNOWN)
                    .memberBday(LocalDate.of(1990, 1, 1))
                    .memberScore((byte) 50)
                    .memberDesc("다른 회원 자기소개입니다.")
                    .build();

            Picture picture = Picture.builder()
                    .id(1L)
                    .pictureName("profile_image.jpg")
                    .pictureExtension(".jpg")
                    .pictureSize(1024)
                    .pictureUrl("http://example.com/profile_image.jpg")
                    .build();

            MemberPicture memberPicture = MemberPicture.builder()
                    .id(1L)
                    .memberPicturesUrl("http://example.com/profile_thumbnail.jpg")
                    .picture(picture)
                    .member(member)
                    .memberPicturesName("profile_thumbnail.jpg")
                    .memberPicturesUrl("http://example.com/profile_thumbnail.jpg")
                    .build();

            member.updateProfilePicture(memberPicture);

            ExerciseType exerciseType = ExerciseType.builder()
                    .id(1L)
                    .exerciseName("헬스")
                    .exerciseTypeImageUrl("http://example.com/exercise/weight_training.jpg")
                    .build();

            PreferredExercise preferredExercise = PreferredExercise.builder()
                    .id(1L)
                    .exercise(exerciseType)
                    .member(member)
                    .preferredExerciseSkillLevel(ExerciseSkillLevel.INTERMEDIATE)
                    .build();

            preferredExercise.setDaysOfWeek(new boolean[]{true, true, true, true, true, true, true});

            List<PreferredExercise> preferredExercises = List.of(preferredExercise);
            ReflectionTestUtils.setField(member, "preferredExercises", preferredExercises);

            return GetOtherMemberProfileResponse.of(member, 15, 5);
        }

        @Test
        @DisplayName("다른 회원 프로필 조회를 성공한다")
        void getOtherMemberProfile_success() throws Exception {
            // given
            UUID currentMemberId = UUID.randomUUID();
            UUID otherMemberId = UUID.randomUUID();
            GetOtherMemberProfileResponse respDto = getOtherMemberProfileResponse();

            BDDMockito.given(memberQueryService.getOtherMemberProfile(otherMemberId)).willReturn(respDto);

            // when
            String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/{memberId}/profile", otherMemberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(currentMemberId)))
                    .andExpect(status().isOk())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("memberId").attributes(getTypeFormat(JsonFieldType.STRING)).description("조회할 회원의 식별자(UUID) 정보를 나타냅니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.profileThumbnailUrl").type(JsonFieldType.STRING).description("다른 회원 프로필 이미지의 썸네일 URL"),
                                    fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("다른 회원의 원본 이미지 URL(썸네일이 NULL이고 원본이 있으면 이거로. 없으면 기본 이미지로)"),
                                    fieldWithPath("data.nickname").type(JsonFieldType.STRING).description("다른 회원의 닉네임"),
                                    fieldWithPath("data.gender").type(JsonFieldType.STRING).description("다른 회원의 성별: MALE, FEMALE, UNKNOWN"),
                                    fieldWithPath("data.birthDate").type(JsonFieldType.STRING).description("다른 회원의 생년월일 (YYYY-MM-DD)"),
                                    fieldWithPath("data.bio").type(JsonFieldType.STRING).description("다른 회원의 자기소개(바이오는 인스타그램, 트위터 등 소셜 미디어에서 자신의 프로필 자기소개란에 적는 간략한 소개를 의미)"),
                                    fieldWithPath("data.yearlyExerciseDays").type(JsonFieldType.NUMBER).description("다른 회원의 올해 누적 운동 일수"),
                                    fieldWithPath("data.exerciseCountInLast30Days").type(JsonFieldType.NUMBER).description("다른 회원의 최근 30일간 운동 횟수"),
                                    fieldWithPath("data.exerciseScore").type(JsonFieldType.NUMBER).description("다른 회원의 현재 운동 점수"),
                                    fieldWithPath("data.preferredExercises[]").type(JsonFieldType.ARRAY).description("다른 회원이 선호하는 운동 목록"),
                                    fieldWithPath("data.preferredExercises[].preferredExerciseId").type(JsonFieldType.NUMBER).description("다른 회원의 선호 운동 ID").optional(),
                                    fieldWithPath("data.preferredExercises[].name").type(JsonFieldType.STRING).description("다른 회원의 선호 운동 이름"),
                                    fieldWithPath("data.preferredExercises[].skillLevel").type(JsonFieldType.STRING).description("다른 회원의 운동 수준: BEGINNER, ROOKIE, INTERMEDIATE, ADVANCED, SKILLED, PRO(입문자, 초급자, 중급자, 고급자, 숙련자, 선출)"),
                                    fieldWithPath("data.preferredExercises[].daysOfWeek").type(JsonFieldType.ARRAY).description("다른 회원의 운동 요일(월 ~ 일 순)").optional(),
                                    fieldWithPath("data.preferredExercises[].imageUrl").type(JsonFieldType.STRING).description("운동 이미지(최대 255자)").optional()
                            ))
                    )).andReturn().getResponse().getContentAsString();

            // then
            CommonResponse<GetOtherMemberProfileResponse> expectedData = CommonResponse.success(respDto);
            String expected = objectMapper.writeValueAsString(expectedData);
            Assertions.assertThat(response).as("응답 본문 검증").isEqualTo(expected);
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID로 조회 시 실패한다")
        void getOtherMemberProfile_Failed_MemberNotFound() throws Exception {
            // given
            UUID currentMemberId = UUID.randomUUID();
            UUID otherMemberId = UUID.randomUUID();

            BDDMockito.given(memberQueryService.getOtherMemberProfile(otherMemberId))
                    .willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/{memberId}/profile", otherMemberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(currentMemberId)))
                    .andExpect(status().isNotFound());

            // then
            BDDMockito.then(memberQueryService).should(BDDMockito.times(1)).getOtherMemberProfile(otherMemberId);
        }

        @Test
        @DisplayName("Access 토큰이 없는 경우 401 Unauthorized 오류를 반환한다")
        public void getOtherMemberProfile_Failed_Not_Having_Token() throws Exception {
            // given
            UUID otherMemberId = UUID.randomUUID();

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/{memberId}/profile", otherMemberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("getOtherMemberCalendars 메소드는")
    class GetOtherMemberCalendars {

        @Test
        @DisplayName("다른 회원의 운동 달력 조회를 성공한다")
        void getOtherMemberCalendars_success() throws Exception {
            // given
            UUID currentMemberId = UUID.randomUUID();
            UUID otherMemberId = UUID.randomUUID();
            LocalDate startDate = LocalDate.now().minusDays(10);
            LocalDate endDate = LocalDate.now();

            List<FindExerciseRecordByPeriodResponseDto> respDto = List.of(
                    new FindExerciseRecordByPeriodResponseDto(startDate, 1L),
                    new FindExerciseRecordByPeriodResponseDto(endDate, 2L)
            );

            BDDMockito.given(memberQueryService.getOtherMemberCalendars(otherMemberId, startDate, endDate))
                    .willReturn(respDto);

            // when
            String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/{memberId}/calendars", otherMemberId)
                            .param("start", startDate.toString())
                            .param("end", endDate.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(currentMemberId)))
                    .andExpect(status().isOk())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("memberId").attributes(getTypeFormat(JsonFieldType.STRING)).description("조회할 회원의 식별자(UUID) 정보를 나타냅니다.")
                            ),
                            queryParameters(
                                    parameterWithName("start").attributes(getTypeFormat(JsonFieldType.STRING)).description("운동 기록을 조회할 시작 날짜입니다. 형식은 ISO 8601 (YYYY-MM-DD)입니다. 시작 날짜는 오늘 이전이어야 합니다."),
                                    parameterWithName("end").attributes(getTypeFormat(JsonFieldType.STRING)).description("운동 기록을 조회할 종료 날짜입니다. 형식은 ISO 8601 (YYYY-MM-DD)입니다.")
                            ),
                            responseFields(commonResponseFieldsForList(
                                    fieldWithPath("data[].date").type(JsonFieldType.STRING).description("운동한 날짜를 의미합니다."),
                                    fieldWithPath("data[].exerciseCount").type(JsonFieldType.NUMBER).description("해당 날짜의 운동 횟수를 의미합니다.")
                            ))
                    )).andReturn().getResponse().getContentAsString();

            // then
            CommonResponse<List<FindExerciseRecordByPeriodResponseDto>> expectedData = CommonResponse.success(respDto);
            String expected = objectMapper.writeValueAsString(expectedData);
            Assertions.assertThat(response).as("응답 본문 검증").isEqualTo(expected);
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID로 조회 시 실패한다")
        void getOtherMemberCalendars_Failed_MemberNotFound() throws Exception {
            // given
            UUID currentMemberId = UUID.randomUUID();
            UUID otherMemberId = UUID.randomUUID();
            LocalDate startDate = LocalDate.now().minusDays(10);
            LocalDate endDate = LocalDate.now();

            BDDMockito.given(memberQueryService.getOtherMemberCalendars(otherMemberId, startDate, endDate))
                    .willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/{memberId}/calendars", otherMemberId)
                            .param("start", startDate.toString())
                            .param("end", endDate.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(currentMemberId)))
                    .andExpect(status().isNotFound());

            // then
            BDDMockito.then(memberQueryService).should(BDDMockito.times(1)).getOtherMemberCalendars(otherMemberId, startDate, endDate);
        }

        @Test
        @DisplayName("잘못된 날짜 범위로 조회 시 실패한다")
        void getOtherMemberCalendars_Failed_InvalidDateRange() throws Exception {
            // given
            UUID currentMemberId = UUID.randomUUID();
            UUID otherMemberId = UUID.randomUUID();
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().minusDays(1); // 시작 날짜가 종료 날짜보다 늦음

            BDDMockito.given(memberQueryService.getOtherMemberCalendars(otherMemberId, startDate, endDate))
                    .willThrow(new CustomException(ErrorCode.IMPOSSIBLE_INPUT_DATE));

            // when
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/{memberId}/calendars", otherMemberId)
                            .param("start", startDate.toString())
                            .param("end", endDate.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(currentMemberId)))
                    .andExpect(status().isBadRequest());

            // then
            BDDMockito.then(memberQueryService).should(BDDMockito.times(1)).getOtherMemberCalendars(otherMemberId, startDate, endDate);
        }

        @Test
        @DisplayName("Access 토큰이 없는 경우 401 Unauthorized 오류를 반환한다")
        public void getOtherMemberCalendars_Failed_Not_Having_Token() throws Exception {
            // given
            UUID otherMemberId = UUID.randomUUID();
            LocalDate startDate = LocalDate.now().minusDays(10);
            LocalDate endDate = LocalDate.now();

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/{memberId}/calendars", otherMemberId)
                            .param("start", startDate.toString())
                            .param("end", endDate.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }
}

