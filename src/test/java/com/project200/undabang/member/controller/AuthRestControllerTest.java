package com.project200.undabang.member.controller;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.MemberRegistrationStatusResponseDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.service.MemberQueryService;
import com.project200.undabang.member.service.MemberService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonAuthHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ID_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthRestController.class)
public class AuthRestControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberQueryService memberQueryService;

    private final UUID memberTestId = UUID.randomUUID();
    private final String memberTestEmail = "naturalPlayer@steroid.com";
    private final String memberTestNickname = "25년생프로헬창지망생";
    private final LocalDate memberBday = LocalDate.of(2025, 1, 1);
    private final char memberGender = MemberGender.M.getCode();

    @Test
    @DisplayName("회원가입 성공 케이스")
    public void signUpMember_Succeed() throws Exception {
        // given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        SignUpResponseDto respDto = SignUpResponseDto.builder()
                .memberId(memberTestId)
                .memberEmail(memberTestEmail)
                .memberNickname(memberTestNickname)
                .memberDesc("자칭내추럴헬스인의첫발지금그시작을확인하세요")
                .memberScore((byte) 35)
                .memberBday(memberBday)
                .memberGender(memberGender)
                .memberCreatedAt(LocalDateTime.now())
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any(SignUpRequestDto.class))).willReturn(respDto);

        // when
        String response = this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDto))
                        .headers(getCommonAuthHeaders(memberTestId, memberTestEmail)))
                .andDo(this.document.document(
                                requestHeaders(HEADER_ID_TOKEN),
                                requestFields(
                                        fieldWithPath("memberNickname").type(JsonFieldType.STRING)
                                                .description("회원 닉네임입니다. 최대 30자 이내로 한글, 영문, 숫자만 사용 가능합니다. " +
                                                        "닉네임은 공백일 수 없으며 중복될 수 없습니다."),
                                        fieldWithPath("memberGender").type(JsonFieldType.STRING)
                                                .description("회원 성별입니다. 회원 성별은 M(남성), F(여성), U(알수없음) 중 하나로 입력해주세요."),
                                        fieldWithPath("memberBday").type("Date")
                                                .description("회원 생년월일 입니다. ISO 8601 형식으로 입력해주세요. 예: 2025-01-01")
                                ),
                                responseFields(commonResponseFields(
                                        fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("회원 식별자"),
                                        fieldWithPath("data.memberEmail").type(JsonFieldType.STRING).description("회원 이메일"),
                                        fieldWithPath("data.memberNickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                        fieldWithPath("data.memberGender").type(JsonFieldType.STRING).description("회원 성별 M,F,U"),
                                        fieldWithPath("data.memberBday").type(JsonFieldType.STRING).description("회원 생년 월일"),
                                        fieldWithPath("data.memberDesc").type(JsonFieldType.STRING).description("회원 자기 소개"),
                                        fieldWithPath("data.memberScore").type(JsonFieldType.NUMBER).description("회원 점수"),
                                        fieldWithPath("data.memberCreatedAt").type(JsonFieldType.STRING).description("회원 가입 일시"))
                                )
                        )
                ).andReturn().getResponse().getContentAsString();

        // then
        CommonResponse<SignUpResponseDto> expectedData = CommonResponse.success(respDto);
        String expected = objectMapper.writeValueAsString(expectedData);
        Assertions.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("회원가입 살패 케이스 - memberId 없음")
    public void signUpMember_Failed_memberId_notExist() throws Exception {
        // given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any(SignUpRequestDto.class)))
                .willThrow(new CustomException(ErrorCode.USER_ID_HEADER_MISSING));

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDto))
                        .headers(getCommonAuthHeaders(null, memberTestEmail)))
                .andExpect(status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_ID_HEADER_MISSING"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ID_TOKEN),
                        requestFields(
                                fieldWithPath("memberNickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                fieldWithPath("memberGender").type(JsonFieldType.STRING).description("회원 성별 (M,F,U)"),
                                fieldWithPath("memberBday").type(JsonFieldType.STRING).description("회원 생년월일")
                        ),
                        responseFields(RestDocsUtils.commonResponseFieldsOnly()))
                );

        // then
        BDDMockito.then(memberService).should(BDDMockito.never()).memberSignUp(BDDMockito.any(SignUpRequestDto.class));
    }

    @Test
    @DisplayName("회원가입 실패 케이스 - memberEmail 중복")
    public void emailDuplicated() throws Exception {
        //given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any())).willThrow(new CustomException(ErrorCode.MEMBER_EMAIL_DUPLICATED));

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDto))
                        .headers(getCommonAuthHeaders(memberTestId, memberTestEmail)))
                .andExpect(status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_EMAIL_DUPLICATED"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ID_TOKEN),
                        requestFields(
                                fieldWithPath("memberNickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                fieldWithPath("memberGender").type(JsonFieldType.STRING).description("회원 성별 (M,F,U)"),
                                fieldWithPath("memberBday").type(JsonFieldType.STRING).description("회원 생년월일")
                        ),
                        responseFields(RestDocsUtils.commonResponseFieldsOnly())
                ));

        //then
        BDDMockito.then(memberService).should(BDDMockito.times(1)).memberSignUp(BDDMockito.any(SignUpRequestDto.class));
    }

    @Test
    @DisplayName("사용자 이메일 없음")
    public void emailNotExist() throws Exception {
        //when
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any(SignUpRequestDto.class))).willThrow(new CustomException(ErrorCode.USER_EMAIL_HEADER_MISSING));

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDto))
                        .headers(getCommonAuthHeaders(memberTestId, null)))
                .andExpect(status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_EMAIL_HEADER_MISSING"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ID_TOKEN),
                        requestFields(
                                fieldWithPath("memberNickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                fieldWithPath("memberGender").type(JsonFieldType.STRING).description("회원 성별 (M,F,U)"),
                                fieldWithPath("memberBday").type(JsonFieldType.STRING).description("회원 생년월일")
                        ),
                        responseFields(RestDocsUtils.commonResponseFieldsOnly())
                ));

        //then
        BDDMockito.then(memberService).should(BDDMockito.never()).memberSignUp(BDDMockito.any(SignUpRequestDto.class));
    }

    @Test
    @DisplayName("회원 가입시 닉네임 중복")
    public void nicknameDuplicated() throws Exception {
        //given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any(SignUpRequestDto.class))).willThrow(new CustomException(ErrorCode.MEMBER_NICKNAME_DUPLICATED));

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonAuthHeaders(memberTestId, memberTestEmail))
                        .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_NICKNAME_DUPLICATED"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ID_TOKEN),
                        requestFields(
                                fieldWithPath("memberNickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                fieldWithPath("memberGender").type(JsonFieldType.STRING).description("회원 성별 (M,F,U)"),
                                fieldWithPath("memberBday").type(JsonFieldType.STRING).description("회원 생년월일")
                        ),
                        responseFields(RestDocsUtils.commonResponseFieldsOnly())
                ));

        //then
        BDDMockito.then(memberService).should(BDDMockito.times(1)).memberSignUp(BDDMockito.any(SignUpRequestDto.class));
    }

    @Test
    @DisplayName("생년월일 입력오류")
    public void bDayFailed() throws Exception {
        //given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any(SignUpRequestDto.class))).willThrow(new CustomException(ErrorCode.MEMBER_BDAY_ERROR));

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonAuthHeaders(memberTestId, memberTestEmail))
                        .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_BDAY_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ID_TOKEN),
                        requestFields(
                                fieldWithPath("memberNickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                fieldWithPath("memberGender").type(JsonFieldType.STRING).description("회원 성별 (M,F,U)"),
                                fieldWithPath("memberBday").type(JsonFieldType.STRING).description("회원 생년월일")
                        ),
                        responseFields(RestDocsUtils.commonResponseFieldsOnly())
                ));

        //then
        BDDMockito.then(memberService).should(BDDMockito.times(1)).memberSignUp(BDDMockito.any(SignUpRequestDto.class));
    }

    @Test
    @DisplayName("성별입력오류")
    public void genderFailed() throws Exception {
        //given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any(SignUpRequestDto.class))).willThrow(new CustomException(ErrorCode.MEMBER_GENDER_ERROR));

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonAuthHeaders(memberTestId, memberTestEmail))
                        .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_GENDER_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ID_TOKEN),
                        requestFields(
                                fieldWithPath("memberNickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                fieldWithPath("memberGender").type(JsonFieldType.STRING).description("회원 성별 (M,F,U)"),
                                fieldWithPath("memberBday").type(JsonFieldType.STRING).description("회원 생년월일")
                        ),
                        responseFields(RestDocsUtils.commonResponseFieldsOnly())
                ));
        //then
        BDDMockito.then(memberService).should(BDDMockito.times(1)).memberSignUp(BDDMockito.any(SignUpRequestDto.class));
    }

    @Test
    @DisplayName("유효성 검증 실패 - 필수 필드 누락")
    public void validationFailedMissingField() throws Exception {
        // given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .build();

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonAuthHeaders(memberTestId, memberTestEmail))
                        .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ID_TOKEN),
                        requestFields(
                                fieldWithPath("memberNickname").type(JsonFieldType.NULL).description("회원 닉네임"),
                                fieldWithPath("memberGender").type(JsonFieldType.STRING).description("회원 성별 (M,F,U)"),
                                fieldWithPath("memberBday").type(JsonFieldType.STRING).description("회원 생년월일")
                        ),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("유효성 검증 오류 정보"),
                                fieldWithPath("data.memberNickname").type(JsonFieldType.STRING).description("닉네임 필드 오류 메시지")
                        )
                ));

        //then
        BDDMockito.then(memberService).should(BDDMockito.never()).memberSignUp(BDDMockito.any(SignUpRequestDto.class));
    }

    @Test
    @DisplayName("잘못된 요청 본문 형식")
    public void invalidJsonFormat() throws Exception {
        // given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberNickname("aslenfwgnlsdrjgbsldkjrgbslekrjgbslekrjgbslekrjgbselkrjgbslekrjgbselkjrgblskjerbgslkejrbgslkjerbg")
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .build();
        // when
        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonAuthHeaders(memberTestId, memberTestEmail))
                        .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ID_TOKEN),
                        requestFields(
                                fieldWithPath("memberNickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                fieldWithPath("memberGender").type(JsonFieldType.STRING).description("회원 성별 (M,F,U)"),
                                fieldWithPath("memberBday").type(JsonFieldType.STRING).description("회원 생년월일")
                        ),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("유효성 검증 오류 정보"),
                                fieldWithPath("data.memberNickname").type(JsonFieldType.STRING).description("닉네임 필드 오류 메시지")
                        )
                ));
        //then
        BDDMockito.then(memberService).should(BDDMockito.never()).memberSignUp(BDDMockito.any(SignUpRequestDto.class));
    }

    @Test
    @DisplayName("회원 저장 실패")
    public void saveMemberFailed() throws Exception {
        // given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberNickname(memberTestNickname)
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any(SignUpRequestDto.class)))
                .willThrow(new CustomException(ErrorCode.MEMBER_SAVE_FAILED_ERROR));

        // when & then
        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonAuthHeaders(memberTestId, memberTestEmail))
                        .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_SAVE_FAILED_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ID_TOKEN),
                        requestFields(
                                fieldWithPath("memberNickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                fieldWithPath("memberGender").type(JsonFieldType.STRING).description("회원 성별 (M,F,U)"),
                                fieldWithPath("memberBday").type(JsonFieldType.STRING).description("회원 생년월일")
                        ),
                        responseFields(RestDocsUtils.commonResponseFieldsOnly())
                ));

        BDDMockito.then(memberService).should(BDDMockito.times(1)).memberSignUp(BDDMockito.any(SignUpRequestDto.class));
    }

    /**
     * /api/v1/... -> /auth/v1/...으로 url prefix 변경
     * Authorization 헤더에 Access token 대신 Id token 필요, 이에 API 명세서에 별도 내용 명시
     */
    @Test
    @DisplayName("회원 등록 상태 조회 - 회원 맞음")
    public void getRegistrationStatus_Success() throws Exception {
        // given
        MemberRegistrationStatusResponseDto respDto = MemberRegistrationStatusResponseDto.builder()
                .memberId(memberTestId)
                .isRegistered(true)
                .build();

        BDDMockito.given(memberQueryService.getRegistrationStatus()).willReturn(respDto);

        try (MockedStatic<UserContextHolder> mockedStatic = Mockito.mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(memberTestId);
            mockedStatic.when(UserContextHolder::getUserEmail).thenReturn(memberTestEmail);

            // when
            String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/auth/v1/registration-status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonAuthHeaders(memberTestId, memberTestEmail)))
                    .andDo(this.document.document(
                            requestHeaders(HEADER_ID_TOKEN),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.memberId").type(JsonFieldType.STRING)
                                            .description("조회하기 위해 입력한 토큰에 포함된 회원 식별자입니다. " +
                                                    "요청 헤더에 포함된 값을 반환하는 것이므로 이 값의 유무로 판단하지 마세요."),
                                    fieldWithPath("data.isRegistered").type(JsonFieldType.BOOLEAN)
                                            .description("회원 등록 상태입니다. true면 회원이 등록되어 있고, false면 등록되지 않은 상태입니다.")
                            ))
                    ))
                    .andReturn().getResponse().getContentAsString();

            // then
            CommonResponse<MemberRegistrationStatusResponseDto> expectedData = CommonResponse.success(respDto);
            String expected = objectMapper.writeValueAsString(expectedData);
            Assertions.assertThat(response).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("토큰은 유효하지만, 가입되지 않은 회원일 경우")
    public void getRegistrationStatus_Succeed_NotExistUser() throws Exception {
        // given
        MemberRegistrationStatusResponseDto respDto = MemberRegistrationStatusResponseDto.builder()
                .memberId(memberTestId)
                .isRegistered(false)
                .build();

        BDDMockito.given(memberQueryService.getRegistrationStatus()).willReturn(respDto);

        try (MockedStatic<UserContextHolder> mockedStatic = Mockito.mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(memberTestId);
            mockedStatic.when(UserContextHolder::getUserEmail).thenReturn(memberTestEmail);
            // when
            String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/auth/v1/registration-status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonAuthHeaders(memberTestId, memberTestEmail)))
                    .andExpect(status().isOk()) // 요청은 성공적으로 처리되어야 합니다.
                    .andReturn().getResponse().getContentAsString();

            CommonResponse<MemberRegistrationStatusResponseDto> expectedData = CommonResponse.success(respDto);
            String expected = objectMapper.writeValueAsString(expectedData);
            Assertions.assertThat(response).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("ID 토큰이 없는 경우의 회원정보 조회")
    public void getRegistrationStatus_Failed() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.get("/auth/v1/registration-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }
}