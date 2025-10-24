package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.member.controller.block.MemberBlockQueryController;
import com.project200.undabang.member.dto.response.GetBlockedMembersResponse;
import com.project200.undabang.member.service.MemberBlockQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFieldsForList;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberBlockQueryController.class)
class MemberBlockQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private MemberBlockQueryService memberBlockQueryService;

    @Nested
    @DisplayName("차단 목록 조회 API (GET /api/v1/members/blocks)")
    class GetBlockedMembers {
        private final UUID MEMBER_ID = UUID.randomUUID();

        @Test
        @DisplayName("[200 OK] 성공적으로 차단한 회원 목록을 조회한다")
        void getBlockedMembers_Success() throws Exception {
            // given
            List<GetBlockedMembersResponse> responseList = List.of(
                    new GetBlockedMembersResponse(1L, UUID.randomUUID(), "차단된사람1", "url1", "thumb1", LocalDateTime.now()),
                    new GetBlockedMembersResponse(2L, UUID.randomUUID(), "차단된사람2", null, null, LocalDateTime.now())
            );
            given(memberBlockQueryService.getBlockedMembers()).willReturn(responseList);

            // when & then
            mockMvc.perform(get("/api/v1/members/blocks")
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(MEMBER_ID)))
                    .andExpectAll(
                            status().isOk()
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFieldsForList(
                                    fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("차단된 회원 목록 입니다."),
                                    fieldWithPath("data[].memberBlockId").type(JsonFieldType.NUMBER).description("회원이 차단한 관계 식별자 입니다."),
                                    fieldWithPath("data[].memberId").type(JsonFieldType.STRING).description("차단된 회원의 식별자 (UUID)를 나타냅니다."),
                                    fieldWithPath("data[].nickname").type(JsonFieldType.STRING).description("차단된 회원의 닉네임을 의미합니다."),
                                    fieldWithPath("data[].profileImageUrl").type(JsonFieldType.STRING).optional().description("차단된 회원의 프로필 사진 URL입니다. 없으면 기본 프로필 사진을 사용하시면 됩니다."),
                                    fieldWithPath("data[].thumbnailImageUrl").type(JsonFieldType.STRING).optional().description("차단된 회원의 썸네일 사진 URL입니다. 없으면 회원 프로필 사진을 사용하시면 됩니다."),
                                    fieldWithPath("data[].blockedAt").type(JsonFieldType.STRING).description("회원이 다른 회원을 차단한 시간을 의미합니다.")
                            ))
                    ));
        }

        @Test
        @DisplayName("[404 Not Found] 요청한 회원이 존재하지 않으면 실패한다")
        void shouldReturn404_whenMemberNotFound() throws Exception {
            // given
            given(memberBlockQueryService.getBlockedMembers())
                    .willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/members/blocks")
                            .headers(getCommonApiHeaders(MEMBER_ID)))
                    .andExpect(status().isNotFound());
        }
    }
}