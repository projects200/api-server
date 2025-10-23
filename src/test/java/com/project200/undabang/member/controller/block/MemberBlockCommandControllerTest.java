package com.project200.undabang.member.controller.block;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.member.dto.response.CreateMemberBlockResponse;
import com.project200.undabang.member.service.MemberBlockCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberBlockCommandController.class)
class MemberBlockCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private MemberBlockCommandService memberBlockCommandService;

    @Nested
    @DisplayName("회원 차단 API")
    class CreateMemberBlock {

        private final UUID MEMBER_ID = UUID.randomUUID();
        private final UUID BLOCKED_MEMBER_ID = UUID.randomUUID();

        @Test
        @DisplayName("[201 Created] 성공적으로 다른 회원을 차단한다")
        void createMemberBlock_Success() throws Exception {
            // given
            CreateMemberBlockResponse expectedResponse = new CreateMemberBlockResponse(1L);

            // 서비스 계층의 응답을 모킹(mocking)합니다.
            given(memberBlockCommandService.CreateMemberBlock(BLOCKED_MEMBER_ID))
                    .willReturn(expectedResponse);

            // when & then
            mockMvc.perform(post("/api/v1/members/{memberId}/block", BLOCKED_MEMBER_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(MEMBER_ID)))
                    .andExpectAll(
                            status().isCreated(),
                            jsonPath("$.message").value("리소스가 성공적으로 생성되었습니다."),
                            jsonPath("$.data.memberBlockId").value(expectedResponse.getMemberBlockId())
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("memberId").attributes(getTypeFormat(JsonFieldType.STRING)).description("차단할 대상 회원의 식별자 정보입니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.memberBlockId").type(JsonFieldType.NUMBER)
                                            .description("새롭게 생성된 차단 관계의 식별자 정보를 나타냅니다.")
                            ))
                    ));

            // 서비스 메소드가 정확히 1번 호출되었는지 검증합니다.
            then(memberBlockCommandService).should().CreateMemberBlock(BLOCKED_MEMBER_ID);
        }

        @Test
        @DisplayName("[400 Bad Request] 자기 자신을 차단하려고 하면 실패한다")
        void shouldReturn400_whenBlockingOneself() throws Exception {
            // given
            // 서비스 계층에서 '자기 자신 차단 시도' 예외가 발생하도록 설정
            given(memberBlockCommandService.CreateMemberBlock(MEMBER_ID))
                    .willThrow(new CustomException(ErrorCode.MEMBER_SELF_REQUEST_NOT_ALLOWED));

            // when & then
            mockMvc.perform(post("/api/v1/members/{memberId}/block", MEMBER_ID)
                            .headers(getCommonApiHeaders(MEMBER_ID)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(ErrorCode.MEMBER_SELF_REQUEST_NOT_ALLOWED.getMessage()));

            // 서비스 메소드가 호출되었는지 검증합니다.
            then(memberBlockCommandService).should().CreateMemberBlock(MEMBER_ID);
        }

        @Test
        @DisplayName("[401 Unauthorized] 인증되지 않은 사용자의 요청은 실패한다")
        void shouldReturn401_whenUnauthorized() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/members/{memberId}/block", BLOCKED_MEMBER_ID))
                    .andExpect(status().isUnauthorized());

            // 인증 필터에서 차단되므로 서비스는 호출되지 않아야 합니다.
            then(memberBlockCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("[404 Not Found] 존재하지 않는 회원을 차단하려고 하면 실패한다")
        void shouldReturn404_whenMemberNotFound() throws Exception {
            // given
            UUID nonExistentMemberId = UUID.randomUUID();
            // 서비스 계층에서 '회원 없음' 예외가 발생하도록 설정
            given(memberBlockCommandService.CreateMemberBlock(nonExistentMemberId))
                    .willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/v1/members/{memberId}/block", nonExistentMemberId)
                            .headers(getCommonApiHeaders(MEMBER_ID)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

            then(memberBlockCommandService).should().CreateMemberBlock(nonExistentMemberId);
        }

        @Test
        @DisplayName("[409 Conflict] 이미 차단한 회원을 다시 차단하려고 하면 실패한다")
        void shouldReturn409_whenBlockIsDuplicated() throws Exception {
            // given
            // 서비스 계층에서 '중복 차단' 예외가 발생하도록 설정
            given(memberBlockCommandService.CreateMemberBlock(BLOCKED_MEMBER_ID))
                    .willThrow(new CustomException(ErrorCode.MEMBER_BLOCK_DUPLICATED));

            // when & then
            mockMvc.perform(post("/api/v1/members/{memberId}/block", BLOCKED_MEMBER_ID)
                            .headers(getCommonApiHeaders(MEMBER_ID)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(ErrorCode.MEMBER_BLOCK_DUPLICATED.getMessage()));

            then(memberBlockCommandService).should().CreateMemberBlock(BLOCKED_MEMBER_ID);
        }
    }
}