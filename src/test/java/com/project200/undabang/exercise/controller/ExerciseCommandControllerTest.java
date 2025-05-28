package com.project200.undabang.exercise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.service.ExerciseCommandService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.restdocs.operation.preprocess.UriModifyingOperationPreprocessor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExerciseCommandController.class)
class ExerciseCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private ExerciseCommandService exerciseCommandService;

    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected RestDocumentationResultHandler document;

    @Value("${restdocs.uris.scheme}")
    private String scheme;

    @Value("${restdocs.uris.host}")
    private String host;

    @Value("${restdocs.uris.port}")
    private int port;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        boolean removePort = (scheme.equals("https") && port == 443) || (scheme.equals("http") && port == 80);

        UriModifyingOperationPreprocessor uriPreprocessor = Preprocessors.modifyUris()
                .scheme(scheme)
                .host(host);

        if (removePort) {
            uriPreprocessor = uriPreprocessor.removePort();
        } else {
            uriPreprocessor = uriPreprocessor.port(port);
        }

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(MockMvcRestDocumentation.documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(
                                uriPreprocessor, // 포트 443은 HTTPS 기본 포트
                                Preprocessors.modifyHeaders().remove("X-USER-ID"), // X-USER-ID 헤더 제거
                                Preprocessors.prettyPrint()
                        )
                        .withResponseDefaults(Preprocessors.prettyPrint())
                )
                .alwaysDo(MockMvcResultHandlers.print()) // 콘솔에 요청/응답 출력
                .alwaysDo(document)
                .addFilters(new CharacterEncodingFilter("UTF-8", true)) // 한글 깨짐 방지
                .build();
    }

    // createExercise 성공 테스트
    @Test
    @DisplayName("운동 정보를 성공적으로 생성하면 201 상태 코드와 응답 데이터를 반환한다")
    void createExerciseSuccess() throws Exception {
        // given
        UUID testUserId = UUID.randomUUID();

        // 요청 DTO
        CreateExerciseRequestDto requestDto = CreateExerciseRequestDto.builder()
                .exerciseTitle("운동 1일차")
                .exercisePersonalType("러닝")
                .exerciseLocation("여의도 한강 공원")
                .exerciseDetail("오늘은 5km 달리기를 하였다. 날씨가 맑고 기분이 좋았다.")
                .exerciseStartedAt(LocalDateTime.of(2023, 10, 1, 6, 0))
                .exerciseEndedAt(LocalDateTime.of(2023, 10, 1, 7, 0))
                .build();

        ExerciseIdResponseDto responseDto = new ExerciseIdResponseDto(1L);
        BDDMockito.given(exerciseCommandService.createExercise(BDDMockito.any(CreateExerciseRequestDto.class)))
                .willReturn(responseDto);

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", testUserId.toString());
        headers.add("Authorization", "Bearer dummy-access-token-for-docs");

        String response = this.mockMvc.perform(post("/v1/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isCreated() // 상태 코드 201 확인
                )
                .andDo(this.document.document(
                        requestHeaders(
                                RestDocsUtils.HEADER_ACCESS_TOKEN
                        ),
                        requestFields(
                                fieldWithPath("exerciseTitle").type(JsonFieldType.STRING).description("운동 제목: 최대 255자"),
                                fieldWithPath("exercisePersonalType").type(JsonFieldType.STRING).optional().description("운동 종류(사용자 입력): 최대 255자"),
                                fieldWithPath("exerciseLocation").type(JsonFieldType.STRING).optional().description("운동 장소: 최대 255자"),
                                fieldWithPath("exerciseDetail").type(JsonFieldType.STRING).optional().description("운동 상세 내용: 최대 65,535byte"),
                                fieldWithPath("exerciseStartedAt").type(JsonFieldType.STRING).description("운동 시작 일시: 오늘 이전"),
                                fieldWithPath("exerciseEndedAt").type(JsonFieldType.STRING).description("운동 종료 일시: 오늘 이전, 시작 일시 이후")
                        ),
                        responseFields(RestDocsUtils.commonResponseFields(
                                fieldWithPath("data.exerciseId").type(JsonFieldType.NUMBER).description("운동 ID: 이미지 업로드 시 사용")
                        ))
                ))
                .andReturn().getResponse().getContentAsString();

        // then
        Assertions.assertThat(response).isEqualTo(objectMapper.writeValueAsString(
                CommonResponse.create(new ExerciseIdResponseDto(1L))
        ));
        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1))
                .createExercise(BDDMockito.any(CreateExerciseRequestDto.class));
        BDDMockito.then(exerciseCommandService).shouldHaveNoMoreInteractions();
    }
}