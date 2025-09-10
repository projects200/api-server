package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.member.dto.request.UpdateMemberProfileRequest;
import com.project200.undabang.member.dto.response.UpdateMemberProfileResponse;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.service.MemberCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberCommandController.class)
class MemberCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private MemberCommandService memberCommandService;

    @Nested
    @DisplayName("회원 프로필 수정 API")
    class UpdateMemberProfileTest {

        private static final UUID TEST_UUID = UUID.randomUUID();
        private static final String NEW_NICKNAME = "새로운닉네임";

        @Test
        @DisplayName("정상적으로 회원 프로필을 수정한다")
        void updateMemberProfile_Success() throws Exception {
            // given
            // 1. 요청 DTO 생성
            UpdateMemberProfileRequest request = new UpdateMemberProfileRequest(
                    NEW_NICKNAME, MemberGender.FEMALE, "새로운 자기소개"
            );

            // 2. 서비스가 반환할 예상 응답 DTO 생성
            UpdateMemberProfileResponse expectedResponse = UpdateMemberProfileResponse.builder()
                    .nickname(NEW_NICKNAME)
                    .gender(MemberGender.FEMALE.toString())
                    .bio("새로운 자기소개")
                    .build();

            // 3. 서비스 메소드 Mocking: 어떤 요청이든 받으면 위에서 정의한 응답을 반환하도록 설정
            given(memberCommandService.updateMemberProfile(any(UpdateMemberProfileRequest.class)))
                    .willReturn(expectedResponse);

            // when & then
            mockMvc.perform(put("/api/v1/profile") // PUT 요청
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(TEST_UUID)) // AbstractRestDocSupport의 헬퍼 메소드 사용 가정
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isOk(), // 성공 시 200 OK
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("UPDATED"),
                            jsonPath("$.data.nickname").value(NEW_NICKNAME),
                            jsonPath("$.data.gender").value("FEMALE"),
                            jsonPath("$.data.bio").value("새로운 자기소개")
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN), // REST Docs 문서화
                            requestFields(
                                    fieldWithPath("nickname").description("변경할 닉네임 입니다. 한글, 영문, 숫자를 포함한 공백없는 30자가 필요합니다."),
                                    fieldWithPath("gender").description("변경할 성별 입니다. (MALE, FEMALE, UNKNOWN) 중에서 입력하셔야 합니다."),
                                    fieldWithPath("bio").description("변경할 자기소개 입니다. 문자열로 최대 500자 까지 입력하셔야 합니다.").optional()
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.nickname").description("수정된 닉네임을 보여줍니다. 한글, 영문, 숫자를 포함한 30자리 문자열입니다."),
                                    fieldWithPath("data.gender").description("수정된 성별 정보를 보여줍니다. 상태로는 MALE, FEMALE, UNKNOWN이 존재합니다."),
                                    fieldWithPath("data.bio").description("수정된 자기소개 정보를 보여줍니다. 최대 500글자까지 저장할 수 있습니다.")
                            ))
                    ));

            // 서비스 메소드가 정확히 1번 호출되었는지 검증
            then(memberCommandService).should().updateMemberProfile(any(UpdateMemberProfileRequest.class));
        }

        @Test
        @DisplayName("닉네임이 비어있으면(@NotBlank) 400 에러를 반환한다")
        void updateMemberProfile_ValidationFail_BlankNickname() throws Exception {
            // given
            UpdateMemberProfileRequest request = new UpdateMemberProfileRequest("", MemberGender.MALE, "자기소개");

            // when & then
            mockMvc.perform(put("/api/v1/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(TEST_UUID))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            // 유효성 검증에서 실패했으므로, 서비스는 호출되지 않아야 함
            then(memberCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 회원 정보 수정 시 404 에러를 반환한다")
        void updateMemberProfile_Fail_MemberNotFound() throws Exception {
            // given
            UpdateMemberProfileRequest request = new UpdateMemberProfileRequest(NEW_NICKNAME, MemberGender.FEMALE, "자기소개");

            // 서비스 계층에서 MEMBER_NOT_FOUND 예외가 발생하도록 설정
            given(memberCommandService.updateMemberProfile(any(UpdateMemberProfileRequest.class)))
                    .willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(put("/api/v1/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(TEST_UUID))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.getCode()));

            // 예외는 발생했지만, 서비스 메소드 자체는 호출되었음을 검증
            then(memberCommandService).should().updateMemberProfile(any(UpdateMemberProfileRequest.class));
        }
    }
}