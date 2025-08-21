package com.project200.undabang.timer.custom.controller;

import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerStepCreateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;
import com.project200.undabang.timer.custom.service.CustomTimerCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomTimerCommandController.class)
class CustomTimerCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private CustomTimerCommandService customTimerCommandService;

    @Nested
    @DisplayName("커스텀 타이머 생성 성공")
    class CreateCustomTimer {

        @Test
        @DisplayName("커스텀 타이머를 성공적으로 생성한다")
        void createCustomTimer_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();

            // 요청 본문 DTO 생성
            List<CustomTimerStepCreateRequest> steps = List.of(
                    new CustomTimerStepCreateRequest("데드리프트", (byte) 1, 180),
                    new CustomTimerStepCreateRequest("휴식", (byte) 2, 60),
                    new CustomTimerStepCreateRequest("스쿼트", (byte) 3, 120)
            );
            CustomTimerCreateRequest request = new CustomTimerCreateRequest("3대 운동 루틴", steps);

            // 서비스 계층의 응답 객체 생성
            CustomTimerCreateResponse expectedResponse = new CustomTimerCreateResponse(1L);

            // 서비스 메소드가 어떤 요청이든(any) 받으면, 미리 정의된 응답을 반환하도록 설정
            given(customTimerCommandService.createCustomTimer(any(CustomTimerCreateRequest.class)))
                    .willReturn(expectedResponse);

            // when & then
            mockMvc.perform(post("/api/v1/custom-timers") // POST 요청
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request))) // 요청 본문 추가
                    .andExpectAll(
                            status().isCreated(), // 201 Created 상태 코드를 기대
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("CREATED"),
                            jsonPath("$.data.customTimerId").value(expectedResponse.customTimerId())
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            // 요청 본문의 필드를 문서화합니다.
                            requestFields(
                                    fieldWithPath("customTimerName").description("생성할 커스텀 타이머의 이름입니다."),
                                    fieldWithPath("customTimerSteps").description("커스텀 타이머에 포함될 스텝 목록입니다."),
                                    fieldWithPath("customTimerSteps[].customTimerStepName").description("스텝의 이름입니다."),
                                    fieldWithPath("customTimerSteps[].customTimerStepOrder").description("스텝의 순서입니다."),
                                    fieldWithPath("customTimerSteps[].customTimerStepTime").description("스텝의 시간(초)입니다.")
                            ),
                            // 응답 본문의 필드를 문서화합니다.
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.customTimerId").description("새롭게 생성된 커스텀 타이머의 ID입니다.")
                            ))
                    ));

            // 서비스 메소드가 정확히 1번 호출되었는지 검증
            then(customTimerCommandService).should().createCustomTimer(any(CustomTimerCreateRequest.class));
            then(customTimerCommandService).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("커스텀 타이머 생성 실패")
    class CreateFailure {

        @Test
        @DisplayName("타이머 이름이 비어있으면 400 에러를 반환한다")
        void shouldFail_whenNameIsBlank() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            List<CustomTimerStepCreateRequest> steps = List.of(new CustomTimerStepCreateRequest("스텝 1", (byte) 1, 30));
            CustomTimerCreateRequest request = new CustomTimerCreateRequest("", steps); // Invalid: blank name

            // when & then
            mockMvc.perform(post("/api/v1/custom-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            then(customTimerCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("타이머 스텝 리스트가 null이면 400 에러를 반환한다")
        void shouldFail_whenStepsListIsNull() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            CustomTimerCreateRequest request = new CustomTimerCreateRequest("유효한 이름", null); // Invalid: null steps

            // when & then
            mockMvc.perform(post("/api/v1/custom-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            then(customTimerCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("타이머 스텝의 이름이 비어있으면(@Valid) 400 에러를 반환한다")
        void shouldFail_whenNestedStepNameIsBlank() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            List<CustomTimerStepCreateRequest> steps = List.of(
                    new CustomTimerStepCreateRequest("", (byte) 1, 30) // Invalid: blank step name
            );
            CustomTimerCreateRequest request = new CustomTimerCreateRequest("유효한 이름", steps);

            // when & then
            mockMvc.perform(post("/api/v1/custom-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            then(customTimerCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("타이머 스텝의 시간이 1보다 작으면(@Min) 400 에러를 반환한다")
        void shouldFail_whenNestedStepTimeIsInvalid() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            List<CustomTimerStepCreateRequest> steps = List.of(
                    new CustomTimerStepCreateRequest("유효한 스텝 이름", (byte) 1, 0) // Invalid: time is 0
            );
            CustomTimerCreateRequest request = new CustomTimerCreateRequest("유효한 이름", steps);

            // when & then
            mockMvc.perform(post("/api/v1/custom-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            then(customTimerCommandService).shouldHaveNoInteractions();
        }
    }
}