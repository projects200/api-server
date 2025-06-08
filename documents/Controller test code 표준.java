@WebMvcTest(Controller.class)
public class JUnit5ExampleTests extended AbstractRestDocSupport {

    // 한국어로 주석 입력
    @Test
    @DisplayName("한국어로 입력")
    public void testExample() extends Exception {
        // given
        BDDMockito.given(mock.mock_method()).willReturn(...);    // 정상 반환 시
        BDDMockito.given(mock.mock_method()).willThrow(...);     // 에러 발생 시

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());
        headers.add("Authorization", "Bearer dummy-access-token-for-docs");

        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/items/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpectAll(
                        MockMvcResultMatchers.status().isOk(),
				...
			)
        // rest docs 문서화
			.andDo(this.document.document(
                pathParameters(
                    parameterWithName("id").description("아이디"),
                    ...
                ),
                requestHeaders(
                    RestDocsUtils.HEADER_ACCESS_TOKEN,
                    headerWithName("email").description("사용자 이메일"),
                            ...
                ),
                requestFields(
                    fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("리소스의 ID"),
                            ...
                ),

                // 1) data가 단일 객체
                responseFields(RestDocsUtils.commonResponseFields(
                    fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("리소스의 ID"),
                    fieldWithPath("data.name").type(JsonFieldType.STRING).description("리소스 이름")
                ))

                // 2) data가 리스트
                responseFields(RestDocsUtils.commonResponseFieldsForList( // 공통 응답 + data 리스트 내부 필드
                    fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("리소스 ID"),
                    fieldWithPath("data[].name").type(JsonFieldType.STRING).description("리소스 이름")
                ))

                // 3) data가 없음
                responseFields(RestDocsUtils.commonResponseFieldsOnly()) // 공통 응답 필드만 (data는 null)
            ))
            .andReturn().getResponse().getContentAsString();

        // then
        // 1) 정상 동작
        CommonResponse expectedData = CommonResponse.success(service);
        String expected = objectMapper.writeValueAsString(...);    // given에서 반환하도록 한 거
        Assertions.assertThat(response).isEqualTo(expected);

        // 2) 에러 발생
        BDDMockito.then(mock).should().mock_method()
        BDDMockito.then(mock).should(BDDMockito.times(1)).mock_method()
        BDDMockito.then(mock).shouldHaveNoMoreInteractions();
        BDDMockito.then(mock).shouldHaveNoInteractions();
    }
}