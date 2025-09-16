package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.member.controller.open.PublicMemberQueryController;
import com.project200.undabang.member.dto.response.CheckNicknameDuplicateResponse;
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

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicMemberQueryController.class)
@DisplayName("PublicMemberQueryController 테스트")
class PublicMemberQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private MemberQueryService memberQueryService;

    @Nested
    @DisplayName("닉네임 중복 검사 API")
    class CheckNicknameDuplicate {

        @Test
        @DisplayName("성공 - 사용 가능한 닉네임으로 요청 시 200 OK와 available true를 반환하고 API 문서를 생성한다")
        void checkNickname_Available_Success() throws Exception {
            // given
            String nickname = "availableNickname123";
            CheckNicknameDuplicateResponse responseDto = CheckNicknameDuplicateResponse.of(true);
            BDDMockito.given(memberQueryService.checkDuplicateNickname(nickname)).willReturn(responseDto);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/open/v1/nicknames/check")
                            .param("nickname", nickname)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andDo(document.document(
                            queryParameters(
                                    parameterWithName("nickname").attributes(getTypeFormat(JsonFieldType.STRING))
                                            .description("중복 검사를 위해 사용자가 입력한 닉네임입니다. 1~30자의 영문,숫자,한글로 이루어진 문자열이어야 합니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.available").type(JsonFieldType.BOOLEAN).description("요청받은 닉네임의 사용 가능 여부를 나타냅니다.")
                            ))
                    ));
        }

        @Test
        @DisplayName("실패하는 경우 - 이미 사용 중인 닉네임으로 요청 시 200 OK와 available false를 반환한다")
        void checkNickname_Duplicated_ReturnsOkWithFalse() throws Exception {
            // given
            String nickname = "existingUser";
            CheckNicknameDuplicateResponse responseDto = CheckNicknameDuplicateResponse.of(false);
            BDDMockito.given(memberQueryService.checkDuplicateNickname(nickname)).willReturn(responseDto);

            // when
            String response = mockMvc.perform(MockMvcRequestBuilders.get("/open/v1/nicknames/check")
                            .param("nickname", nickname)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // then
            CommonResponse<CheckNicknameDuplicateResponse> expectedData = CommonResponse.success(responseDto);
            String expected = objectMapper.writeValueAsString(expectedData);
            Assertions.assertThat(response).as("응답 본문 검증").isEqualTo(expected);
        }


        @Test
        @DisplayName("닉네임이 비어있을 경우(@NotBlank) 400 Bad Request를 반환한다")
        void checkNickname_Failed_BlankNickname() throws Exception {
            // given
            String blankNickname = " ";

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/open/v1/nicknames/check")
                            .param("nickname", blankNickname)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest())
                    .andDo(print());

            BDDMockito.then(memberQueryService).should(BDDMockito.never()).checkDuplicateNickname(BDDMockito.anyString());
        }


        @Test
        @DisplayName("닉네임 길이가 유효하지 않을 경우(@Size) 400 Bad Request를 반환한다")
        void checkNickname_Failed_InvalidLength() throws Exception {
            // given
            String longNickname = "a".repeat(31);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/open/v1/nicknames/check")
                            .param("nickname", longNickname)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest())
                    .andDo(print());

            BDDMockito.then(memberQueryService).should(BDDMockito.never()).checkDuplicateNickname(BDDMockito.anyString());
        }

        @Test
        @DisplayName("닉네임 형식이 유효하지 않을 경우(@Pattern) 400 Bad Request를 반환한다")
        void checkNickname_Failed_InvalidPattern() throws Exception {
            // given
            String invalidNickname = "invalid-nickname!";

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/open/v1/nicknames/check")
                            .param("nickname", invalidNickname)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isBadRequest())
                    .andDo(print());

            BDDMockito.then(memberQueryService).should(BDDMockito.never()).checkDuplicateNickname(BDDMockito.anyString());
        }
    }
}