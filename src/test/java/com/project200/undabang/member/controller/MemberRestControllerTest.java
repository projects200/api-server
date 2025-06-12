package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.member.dto.response.MemberRegistrationStatusResponseDto;
import com.project200.undabang.member.service.MemberQueryService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
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
    @Disabled
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
        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/members/me/registration-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpectAll(
                        MockMvcResultMatchers.status().isOk()
                )
                // rest docs 문서화
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(RestDocsUtils.commonResponseFields(
                                fieldWithPath("data.memberId").type(JsonFieldType.STRING)
                                        .description("조회하기 위해 입력한 토큰에 포함된 회원 식별자입니다. " +
                                                "요청 헤더에 포함된 값을 반환하는 것이므로 이 값의 유무로 판단하지 마세요."),
                                fieldWithPath("data.isRegistered").type(JsonFieldType.BOOLEAN)
                                        .description("회원 등록 상태입니다. true면 회원이 등록되어 있고, false면 등록되지 않은 상태입니다.")
                        ))
                ))
                .andReturn().getResponse().getContentAsString();

        // then
        CommonResponse<MemberRegistrationStatusResponseDto> expectedData = CommonResponse.success(responseDto);
        String expected = objectMapper.writeValueAsString(expectedData);
        Assertions.assertThat(response).isEqualTo(expected);
    }
}