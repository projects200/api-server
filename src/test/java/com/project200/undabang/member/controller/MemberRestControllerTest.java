package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.member.dto.response.MemberRegistrationStatusResponseDto;
import com.project200.undabang.member.service.MemberQueryService;
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

import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

@WebMvcTest(MemberRestController.class)
class MemberRestControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private MemberQueryService memberQueryService;

    /**
     * 회원 등록 상태 조회 API를 테스트합니다.
     * 회원의 등록 상태 정보를 성공적으로 반환하는지 확인합니다.
     */
    @Test
    @DisplayName("회원 등록 상태 조회 - 회원 맞음")
    public void getRegistrationStatus_Success() throws Exception {
        // given
        UUID memberId = UUID.randomUUID();
        MemberRegistrationStatusResponseDto responseDto = MemberRegistrationStatusResponseDto.builder()
                .memberId(memberId)
                .isRegistered(true)
                .build();
        BDDMockito.given(memberQueryService.getRegistrationStatus()).willReturn(responseDto);

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/me/registration-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpectAll(
                        MockMvcResultMatchers.status().isOk()
                )
                // rest docs 문서화
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(RestDocsUtils.commonResponseFields(
                                fieldWithPath("data.memberId").type(JsonFieldType.STRING).description("회원 식별자"),
                                fieldWithPath("data.isRegistered").type(JsonFieldType.BOOLEAN).description("회원 등록 상태")
                        ))
                ))
                .andReturn().getResponse().getContentAsString();

        // then
        CommonResponse<MemberRegistrationStatusResponseDto> expectedData = CommonResponse.success(responseDto);
        String expected = objectMapper.writeValueAsString(expectedData);
        Assertions.assertThat(response).isEqualTo(expected);
    }
}