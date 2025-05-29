package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.service.MemberService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;

@WebMvcTest(AuthRestController.class)
public class AuthRestControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private MemberService memberService;

    private final UUID memberTestId = UUID.randomUUID();
    private final String memberTestEmail = "naturalPlayer@steroid.com";
    private final String memberTestNickname = "25년생프로헬창지망생";
    private final LocalDate memberBday = LocalDate.of(2025,1,1);
    private final char memberGender = MemberGender.M.getCode();

    @Test
    @DisplayName("회원가입 성공 케이스")
    public void signUpMember_Succeed() throws Exception{
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
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-USER-ID", memberTestId.toString());
        httpHeaders.add("X-USER-EMAIL", memberTestEmail);

        String response = this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqDto))
                .headers(httpHeaders))
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID, RestDocsUtils.HEADER_X_USER_EMAIL),
                            requestFields(
                                    fieldWithPath("memberNickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                    fieldWithPath("memberGender").type(JsonFieldType.STRING).description("회원 성별 (M,F,U)"),
                                    fieldWithPath("memberBday").type(JsonFieldType.STRING).description("회원 생년월일")
                            ),
                            responseFields(RestDocsUtils.commonResponseFields(
                                fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("회원 식별자"),
                                fieldWithPath("data.memberEmail").type(JsonFieldType.STRING).description("회원 이메일"),
                                fieldWithPath("data.memberNickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                fieldWithPath("data.memberGender").type(JsonFieldType.STRING).description("회원 성별 M,F,U"),
                                fieldWithPath("data.memberBday").type(JsonFieldType.STRING).description("회원 생년 월일"),
                                fieldWithPath("data.memberDesc").type(JsonFieldType.STRING).description("회원 자기 소개").optional(),
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
    public void signUpMember_Failed_memberId_notExist() throws  Exception{
        // given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any(SignUpRequestDto.class)))
                .willThrow(new CustomException(ErrorCode.USER_ID_HEADER_MISSING));

        // when
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-USER-EMAIL", memberTestEmail);

        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDto))
                        .headers(httpHeaders))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_ID_HEADER_MISSING"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_EMAIL),
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
    public void emailDuplicated() throws Exception{
        //given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any())).willThrow(new CustomException(ErrorCode.MEMBER_EMAIL_DUPLICATED));

        //when
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-USER-ID", memberTestId.toString());
        httpHeaders.add("X-USER-EMAIL", memberTestEmail);

        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqDto))
                .headers(httpHeaders))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_EMAIL_DUPLICATED"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID, RestDocsUtils.HEADER_X_USER_EMAIL),
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
    public void emailNotExist() throws Exception{
        //when
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any(SignUpRequestDto.class))).willThrow(new CustomException(ErrorCode.USER_EMAIL_HEADER_MISSING));

        //when
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-USER-ID", memberTestId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqDto))
                .headers(httpHeaders))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("USER_EMAIL_HEADER_MISSING"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
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
    public void nicknameDuplicated() throws Exception{
        //given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any(SignUpRequestDto.class))).willThrow(new CustomException(ErrorCode.MEMBER_NICKNAME_DUPLICATED));

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberTestId.toString());
        headers.add("X-USER-EMAIL", memberTestEmail);

        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers)
                .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_NICKNAME_DUPLICATED"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID, RestDocsUtils.HEADER_X_USER_EMAIL),
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
    public void bDayFailed() throws Exception{
        //given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any(SignUpRequestDto.class))).willThrow(new CustomException(ErrorCode.MEMBER_BDAY_ERROR));

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberTestId.toString());
        headers.add("X-USER-EMAIL", memberTestEmail);

        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers)
                .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_BDAY_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID, RestDocsUtils.HEADER_X_USER_EMAIL),
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
    public void genderFailed() throws Exception{
        //given
        SignUpRequestDto reqDto = SignUpRequestDto.builder()
                .memberBday(memberBday)
                .memberGender(MemberGender.M)
                .memberNickname(memberTestNickname)
                .build();

        BDDMockito.given(memberService.memberSignUp(BDDMockito.any(SignUpRequestDto.class))).willThrow(new CustomException(ErrorCode.MEMBER_GENDER_ERROR));

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberTestId.toString());
        headers.add("X-USER-EMAIL", memberTestEmail);

        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers)
                .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_GENDER_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID, RestDocsUtils.HEADER_X_USER_EMAIL),
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
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberTestId.toString());
        headers.add("X-USER-EMAIL", memberTestEmail);

        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers)
                .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID, RestDocsUtils.HEADER_X_USER_EMAIL),
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
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberTestId.toString());
        headers.add("X-USER-EMAIL", memberTestEmail);

        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers)
                .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").exists())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID, RestDocsUtils.HEADER_X_USER_EMAIL),
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
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberTestId.toString());
        headers.add("X-USER-EMAIL", memberTestEmail);

        this.mockMvc.perform(MockMvcRequestBuilders.post("/auth/v1/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers)
                .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("MEMBER_SAVE_FAILED_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID, RestDocsUtils.HEADER_X_USER_EMAIL),
                        requestFields(
                                fieldWithPath("memberNickname").type(JsonFieldType.STRING).description("회원 닉네임"),
                                fieldWithPath("memberGender").type(JsonFieldType.STRING).description("회원 성별 (M,F,U)"),
                                fieldWithPath("memberBday").type(JsonFieldType.STRING).description("회원 생년월일")
                        ),
                        responseFields(RestDocsUtils.commonResponseFieldsOnly())
                ));

        BDDMockito.then(memberService).should(BDDMockito.times(1)).memberSignUp(BDDMockito.any(SignUpRequestDto.class));
    }
}