package com.project200.undabang.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.member.controller.location.ExerciseLocationCommandController;
import com.project200.undabang.member.dto.request.CreateExerciseLocationRequest;
import com.project200.undabang.member.dto.request.UpdateExerciseLocationRequest;
import com.project200.undabang.member.dto.response.CreateExerciseLocationResponse;
import com.project200.undabang.member.dto.response.UpdateExerciseLocationResponse;
import com.project200.undabang.member.service.ExerciseLocationCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ExerciseLocationCommandController.class)
class ExerciseLocationCommandControllerTest extends AbstractRestDocSupport {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExerciseLocationCommandService exerciseLocationCommandService;

    @Nested
    @DisplayName("운동 위치 생성 API")
    class CreateExerciseLocation {

        @Test
        @DisplayName("유효한 정보로 운동 위치를 성공적으로 생성한다")
        void createExerciseLocation_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            CreateExerciseLocationRequest request = new CreateExerciseLocationRequest("새로운 헬스장", "서울시 강남구 테헤란로", 37.5080, 127.0564);
            CreateExerciseLocationResponse expectedResponse = new CreateExerciseLocationResponse(1L);

            given(exerciseLocationCommandService.createExerciseLocation(any(CreateExerciseLocationRequest.class)))
                    .willReturn(expectedResponse);

            // when & then
            mockMvc.perform(post("/api/v1/exercise-locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isCreated(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("CREATED"),
                            jsonPath("$.data.exerciseLocationId").value(expectedResponse.getExerciseLocationId())
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            requestFields(
                                    fieldWithPath("name").description("생성할 운동 장소의 이름(상호명)입니다."),
                                    fieldWithPath("address").description("운동 장소의 주소 정보를 나타냅니다. 도로명 주소 혹은 지번 주소 정보를 담아야 합니다."),
                                    fieldWithPath("latitude").description("운동 장소의 위도 값입니다."),
                                    fieldWithPath("longitude").description("운동 장소의 경도 값입니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.exerciseLocationId").description("새롭게 생성된 운동 장소의 고유 식별자 입니다.")
                            ))
                    ));

            then(exerciseLocationCommandService).should().createExerciseLocation(any(CreateExerciseLocationRequest.class));
            then(exerciseLocationCommandService).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("운동 장소 이름이 비어있으면(@Size) 400 Bad Request를 반환한다")
        void shouldFail_whenNameIsBlank() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            CreateExerciseLocationRequest request = new CreateExerciseLocationRequest("", "서울시 강남구", 37.5, 127.0);

            // when & then
            mockMvc.perform(post("/api/v1/exercise-locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            // 유효성 검증 단계에서 실패했으므로, 서비스는 절대 호출되면 안 됨
            then(exerciseLocationCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("위도 값이 null이면(@NotNull) 400 Bad Request를 반환한다")
        void shouldFail_whenLatitudeIsNull() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            CreateExerciseLocationRequest request = new CreateExerciseLocationRequest("헬스장", "서울시 강남구", null, 127.0);

            // when & then
            mockMvc.perform(post("/api/v1/exercise-locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            // 서비스는 절대 호출되면 안 됨
            then(exerciseLocationCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("서비스 계층에서 이름 중복 예외가 발생하면 409 Conflict를 반환한다")
        void shouldFail_whenServiceThrowsDuplicateNameException() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            CreateExerciseLocationRequest request = new CreateExerciseLocationRequest("이미 존재하는 헬스장", "서울시 강남구", 37.5, 127.0);

            // 서비스 메소드가 호출되면 CustomException을 던지도록 설정
            given(exerciseLocationCommandService.createExerciseLocation(any(CreateExerciseLocationRequest.class)))
                    .willThrow(new CustomException(ErrorCode.EXERCISE_LOCATION_NAME_DUPLICATED));

            // when & then
            mockMvc.perform(post("/api/v1/exercise-locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isConflict(), // 409 Conflict 상태 코드
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.EXERCISE_LOCATION_NAME_DUPLICATED.name())
                    );

            // 예외는 발생했지만, 서비스 메소드 자체는 호출되었음을 검증
            then(exerciseLocationCommandService).should().createExerciseLocation(any(CreateExerciseLocationRequest.class));
        }
    }

    @Nested
    @DisplayName("운동 위치 수정 API")
    class UpdateExerciseLocation {

        @Test
        @DisplayName("정상적으로 운동 위치 이름을 수정한다")
        void updateExerciseLocation_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long locationId = 1L;
            String newName = "수정된 헬스장";
            UpdateExerciseLocationRequest request = new UpdateExerciseLocationRequest(newName);
            UpdateExerciseLocationResponse expectedResponse = new UpdateExerciseLocationResponse(locationId);

            given(exerciseLocationCommandService.updateExerciseLocation(eq(locationId), any(UpdateExerciseLocationRequest.class)))
                    .willReturn(expectedResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/exercise-locations/{locationId}", locationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("UPDATED"),
                            jsonPath("$.message").value("리소스가 성공적으로 수정되었습니다."),
                            jsonPath("$.data.id").value(locationId)
                    ).andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            pathParameters(
                                    parameterWithName("locationId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                            .description("수정할 운동 장소의 고유 식별자 정보를 나타냅니다.")
                            ),
                            requestFields(
                                    fieldWithPath("exerciseLocationName").description("수정할 운동 장소의 상호명 혹은 본인이 설정한 이름 입니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.id").description("수정된 운동 장소의 고유 식별자 입니다.")
                            ))
                    ));


            then(exerciseLocationCommandService).should().updateExerciseLocation(eq(locationId), any(UpdateExerciseLocationRequest.class));
            then(exerciseLocationCommandService).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("운동 위치가 존재하지 않으면 404 Not Found를 반환한다")
        void updateExerciseLocation_NotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long locationId = 999L;
            UpdateExerciseLocationRequest request = new UpdateExerciseLocationRequest("없는 헬스장");

            given(exerciseLocationCommandService.updateExerciseLocation(eq(locationId), any(UpdateExerciseLocationRequest.class)))
                    .willThrow(new CustomException(ErrorCode.EXERCISE_LOCATION_NOT_FOUND));

            // when & then
            mockMvc.perform(patch("/api/v1/exercise-locations/{locationId}", locationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.EXERCISE_LOCATION_NOT_FOUND.name())
                    );

            then(exerciseLocationCommandService).should().updateExerciseLocation(eq(locationId), any(UpdateExerciseLocationRequest.class));
        }

        @Test
        @DisplayName("수정하려는 이름이 이미 존재하면 409 Conflict를 반환한다")
        void updateExerciseLocation_Conflict_WhenNameIsDuplicated() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long locationId = 1L;
            UpdateExerciseLocationRequest request = new UpdateExerciseLocationRequest("이미 있는 헬스장");

            given(exerciseLocationCommandService.updateExerciseLocation(eq(locationId), any(UpdateExerciseLocationRequest.class)))
                    .willThrow(new CustomException(ErrorCode.EXERCISE_LOCATION_NAME_DUPLICATED));

            // when & then
            mockMvc.perform(patch("/api/v1/exercise-locations/{locationId}", locationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isConflict(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.EXERCISE_LOCATION_NAME_DUPLICATED.name())
                    );

            then(exerciseLocationCommandService).should().updateExerciseLocation(eq(locationId), any(UpdateExerciseLocationRequest.class));
        }
    }

    @Nested
    @DisplayName("운동 위치 삭제 API")
    class DeleteExerciseLocation {

        @Test
        @DisplayName("운동 위치를 성공적으로 삭제한다")
        void deleteExerciseLocation_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long locationId = 1L;

            doNothing().when(exerciseLocationCommandService).deleteExerciseLocation(locationId);

            // when & then
            mockMvc.perform(delete("/api/v1/exercise-locations/{locationId}", locationId)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("DELETED"),
                            jsonPath("$.message").value("리소스가 성공적으로 삭제되었습니다.")
                    )
                    .andDo(document.document(
                            requestHeaders(
                                    HEADER_ACCESS_TOKEN
                            ),
                            pathParameters(
                                    parameterWithName("locationId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                            .description("삭제할 운동 장소의 고유 식별자 입니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data").description("삭제 성공 시 null을 반환합니다.").type(JsonFieldType.NULL)
                            ))
                    ));

            then(exerciseLocationCommandService).should().deleteExerciseLocation(locationId);
            then(exerciseLocationCommandService).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("삭제할 운동 위치가 존재하지 않으면 404 Not Found를 반환한다")
        void deleteExerciseLocation_NotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long locationId = 999L;

            doThrow(new CustomException(ErrorCode.EXERCISE_LOCATION_NOT_FOUND))
                    .when(exerciseLocationCommandService).deleteExerciseLocation(locationId);

            // when & then
            mockMvc.perform(delete("/api/v1/exercise-locations/{locationId}", locationId)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.EXERCISE_LOCATION_NOT_FOUND.name())
                    );

            then(exerciseLocationCommandService).should().deleteExerciseLocation(locationId);
        }

        @Test
        @DisplayName("다른 사람의 운동 위치를 삭제하려고 하면 403 Forbidden을 반환한다")
        void deleteExerciseLocation_Forbidden() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long locationId = 2L; // 다른 사람의 운동 위치 ID

            doThrow(new CustomException(ErrorCode.AUTHORIZATION_DENIED))
                    .when(exerciseLocationCommandService).deleteExerciseLocation(locationId);

            // when & then
            mockMvc.perform(delete("/api/v1/exercise-locations/{locationId}", locationId)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isForbidden(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.AUTHORIZATION_DENIED.name())
                    );

            then(exerciseLocationCommandService).should().deleteExerciseLocation(locationId);
        }
    }
}