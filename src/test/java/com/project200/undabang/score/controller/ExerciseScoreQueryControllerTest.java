package com.project200.undabang.score.controller;

import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.DocumentFormatGenerator;
import com.project200.undabang.score.dto.response.EarnablePointsInfoResponseDto;
import com.project200.undabang.score.dto.response.ValidityWindowDto;
import com.project200.undabang.score.service.ExerciseScoreQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExerciseScoreQueryController.class)
public class ExerciseScoreQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private ExerciseScoreQueryService exerciseScoreQueryService;

    @Test
    @DisplayName("예상 점수 정보를 성공적으로 조회한다")
    public void shouldReturnEarnablePointsInfoSuccessfully() throws Exception {
        // given
        UUID memberId = UUID.randomUUID();

        EarnablePointsInfoResponseDto expectedResponse = new EarnablePointsInfoResponseDto(
                (byte) 3,
                (byte) 35,
                (byte) 100,
                new ValidityWindowDto(
                        LocalDateTime.of(2025, 7, 21, 0, 0, 0),
                        LocalDateTime.of(2025, 7, 23, 16, 7, 39, 489522000)
                ),
                List.of(
                        LocalDate.of(2025, 7, 21),
                        LocalDate.of(2025, 7, 22),
                        LocalDate.of(2025, 7, 23)
                )
        );

        given(exerciseScoreQueryService.getEarnablePointsInfo()).willReturn(expectedResponse);

        // when
        mockMvc.perform(get("/api/v1/scores/expected-points-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.succeed").value(true),
                        jsonPath("$.code").value("SUCCESS"),
                        jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."),
                        jsonPath("$.data.pointsPerExercise").value((int) expectedResponse.pointsPerExercise()),
                        jsonPath("$.data.currentUserScore").value((int) expectedResponse.currentUserScore()),
                        jsonPath("$.data.maxScore").value((int) expectedResponse.maxScore()),
                        jsonPath("$.data.validWindow.startDateTime").value("2025-07-21T00:00:00"),
                        jsonPath("$.data.validWindow.endDateTime").value("2025-07-23T16:07:39.489522"),
                        jsonPath("$.data.earnableScoreDates[0]").value(expectedResponse.earnableScoreDates().get(0).toString()),
                        jsonPath("$.data.earnableScoreDates[1]").value(expectedResponse.earnableScoreDates().get(1).toString())
                )
                .andDo(document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(commonResponseFields(
                                fieldWithPath("data.pointsPerExercise").type(NUMBER)
                                        .description("(정책 데이터) 운동 기록당 획득 가능한 점수입니다."),
                                fieldWithPath("data.currentUserScore").type(NUMBER)
                                        .description("현재 사용자의 운동 점수입니다."),
                                fieldWithPath("data.maxScore").type(NUMBER)
                                        .description("(정책 데이터) 운동 점수의 최대값입니다."),
                                fieldWithPath("data.validWindow.startDateTime").type("DateTime")
                                        .description("운동 점수를 획득할 수 있는 유효 시작 시간입니다. " +
                                                "이 시간 이후에 시작한 운동 기록부터 점수를 획득할 수 있습니다."),
                                fieldWithPath("data.validWindow.endDateTime").type("DateTime")
                                        .description("운동 점수를 획득할 수 있는 유효 종료 시간입니다. " +
                                                "이 시간 이전에 시작한 운동 기록까지 점수를 획득할 수 있습니다."),
                                fieldWithPath("data.earnableScoreDates")
                                        .type("Array")
                                        .description("운동 점수를 획득할 수 있는 날짜 목록입니다. " +
                                                "각 날짜는 'yyyy-MM-dd' 형식으로 표현됩니다.")
                                        .attributes(DocumentFormatGenerator.getTypeFormat("Date"))
                        ))
                ));

        // then
        BDDMockito.then(exerciseScoreQueryService).should().getEarnablePointsInfo();
        BDDMockito.then(exerciseScoreQueryService).shouldHaveNoMoreInteractions();
    }
}