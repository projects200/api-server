@WebMvcTest(Controller.class)
public class JUnit5ExampleTests extended AbstractRestDocSupport {

    /**
     * 한국어로 Javadoc 입력
     */
    @Test
    @DisplayName("한국어로 입력")
    public void testExample() extends

    Exception {
        // given
        BDDMockito.given(...).willReturn(...);    // 정상 반환 시
        BDDMockito.given(...).willThrow(...);     // 에러 발생 시

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

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
                    RestDocsUtils.HEADER_X_USER_ID,
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
        Mockito.verify(service, never()).method();
    }
}