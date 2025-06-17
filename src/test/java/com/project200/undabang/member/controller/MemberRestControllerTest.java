package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.member.dto.response.MemberScoreResponseDto;
import com.project200.undabang.member.service.MemberQueryService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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
class MemberRestControllerTest extends AbstractRestDocSupport {
    @MockitoBean
    private MemberQueryService memberQueryService;


    @Test
    @DisplayName("회원 운동점수 조회 API")
    void getMemberScore_success() throws Exception {
        // given
        UUID testMemberId = UUID.randomUUID();
        Byte expectedScore = 55;

        MemberScoreResponseDto respDto = MemberScoreResponseDto.builder()
                .memberId(testMemberId)
                .memberScore(expectedScore)
                .build();

        BDDMockito.given(memberQueryService.getMemberScore()).willReturn(respDto);

        // when
        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andDo(this.document.document(
                                requestHeaders(HEADER_ACCESS_TOKEN),
                                responseFields(commonResponseFields(
                                        fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("회원 식별자"),
                                        fieldWithPath("data.memberScore").type(JsonFieldType.NUMBER).description("회원 점수")
                                ))
                )).andReturn().getResponse().getContentAsString();

        // then
        CommonResponse<MemberScoreResponseDto> expectedData = CommonResponse.success(respDto);
        String expected = objectMapper.writeValueAsString(expectedData);
        Assertions.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("회원 운동점수 조회 실패")
    void getMemberScore_Failed() throws Exception{
        // given
        UUID testMemberId = UUID.randomUUID();
        Byte expectedScore = 55;

        BDDMockito.given(memberQueryService.getMemberScore()).willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // when
        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpectAll(
                        status().isNotFound()
                )
                .andReturn().getResponse().getContentAsString();

        BDDMockito.then(memberQueryService).should(BDDMockito.times(1)).getMemberScore();
    }

    @Test
    @DisplayName("Access 토큰이 없는 경우의 회원 운동 점수 조회")
    public void getMemberScore_Failed_Not_Having_Token() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }
}