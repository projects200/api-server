- Controller test는 @WebMvcTest, Service test는 @ExtendWith(MockitoExtension.class), Repository test는 @DataJpaTest 어노테이션을 사용해
- 주석, DisplayName 등의 설명을 한국어로 작성해
- given, when, then 구조로 작성해
- BDDMockito, AssertJ를 사용해
- AssertJ에서 as에 한국어로 테스트 실패 시 나타낼 메시지를 지정해
- assertThat이 여러개인데 독립적이라면 Soft assertions을 사용해. 이때, SoftAssertions.assertSoftly(softAssertions -> { ... }); 방식처럼 람다 방식으로 사용해
- UserContextHolder의 메소드는 전부 정적 메소드이므로 정적 메소드 모킹이 필요해. 다음 코드를 사용해 `import com.project200.undabang.common.context.UserContextHolder; try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) { BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId); ... }` 이 때, testUserId는 UUID야

- Controller test는 다음 코드를 참고해
```java
@WebMvcTest(Controller.class)
public class JUnit5ExampleTests extends AbstractRestDocSupport {

    // 한국어로 주석 입력
    @Test
    @DisplayName("한국어로 입력")
    public void testExample() throws Exception {
        // given
        UUID testMemberId = UUID.randomUUID();

        BDDMockito.given(mock.mock_method()).willReturn(...)// 정상 반환 시
        BDDMockito.given(mock.mock_method()).willThrow(...);     // 에러 발생 시

        // when
        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/items/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(HeadersGenerator.getCommonApiHeaders(testMemberId)))
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
        requestHeaders(RestDocsUtils.HEADER_ACCESS_TOKEN),
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
        BDDMockito.then(mock).should().mock_method();
        BDDMockito.then(mock).should(BDDMockito.times(1)).mock_method();
        BDDMockito.then(mock).shouldHaveNoMoreInteractions();
        BDDMockito.then(mock).shouldHaveNoInteractions();
    }
}
```