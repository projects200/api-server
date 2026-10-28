package com.project200.undabang.exercise.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.dto.response.ExercisePicturesResponse;
import com.project200.undabang.exercise.service.ExerciseCommandService;
import com.project200.undabang.exercise.sync.ExerciseSyncConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExerciseCommandController.class)
class ExerciseSyncControllerTest extends AbstractRestDocSupport {
    @MockitoBean ExerciseCommandService service;
    private final UUID memberId = UUID.randomUUID();
    private static final LocalDateTime START = LocalDateTime.of(2025, 5, 1, 10, 0);

    @Test
    void syncCreation() throws Exception {
        var request = CreateExerciseRequestDto.builder().createRequestId(UUID.randomUUID()).exerciseTitle("오프라인 운동")
                .exerciseStartedAt(START).exerciseEndedAt(START.plusHours(1)).build();
        when(service.createExercise(any())).thenReturn(new CreateExerciseResponseDto(1L, (byte) 3, 0));
        mockMvc.perform(post("/api/v1/exercises").headers(getCommonApiHeaders(memberId))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andExpect(jsonPath("data.earnedPoints").value(3))
                .andExpect(jsonPath("data.version").value(0))
                .andDo(document.document(requestFields(textFields(
                                fieldWithPath("createRequestId").type(JsonFieldType.STRING).description("불변 생성 UUID. 동일 본문 재시도는 원본 응답 재생"))),
                        responseFields(RestDocsUtils.commonResponseFields(
                                fieldWithPath("data.exerciseId").description("서버 기록 ID"),
                                fieldWithPath("data.earnedPoints").description("이 생성에서 확정된 실제 점수"),
                                fieldWithPath("data.version").description("초기 버전 0")))));
        verify(service).createExercise(argThat(dto -> dto.getCreateRequestId().equals(request.getCreateRequestId())));
    }

    @Test
    void syncEdit() throws Exception {
        var request = edit(UUID.randomUUID(), 2L);
        when(service.updateExercise(eq(1L), any())).thenReturn(new ExerciseIdResponseDto(1L, 3));
        mockMvc.perform(patch("/api/v1/exercises/{exerciseId}", 1).headers(getCommonApiHeaders(memberId))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("data.version").value(3))
                .andDo(document.document(requestFields(textFields(
                                fieldWithPath("editId").description("불변 편집 UUID"),
                                fieldWithPath("baseVersion").description("편집 기준 버전. 서버와 일치해야 함"))), responseFields(mutationFields())));
        verify(service).updateExercise(eq(1L), argThat(dto -> dto.getBaseVersion() == 2L && dto.getEditId().equals(request.getEditId())));
    }

    @Test
    void staleVersionConflict() throws Exception {
        when(service.updateExercise(eq(1L), any())).thenThrow(new ExerciseSyncConflictException(1L, 4));
        mockMvc.perform(patch("/api/v1/exercises/{exerciseId}", 1).headers(getCommonApiHeaders(memberId))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(edit(UUID.randomUUID(), 2L))))
                .andExpect(status().isConflict()).andExpect(jsonPath("code").value("EXERCISE_EDIT_CONFLICT"))
                .andExpect(jsonPath("data.version").value(4)).andExpect(jsonPath("succeed").value(false))
                .andDo(document.document(responseFields(mutationFields())));
    }

    @Test
    void reusedKeyAndDeletedRecordAreExplicitErrors() throws Exception {
        when(service.updateExercise(eq(1L), any()))
                .thenThrow(new CustomException(ErrorCode.EXERCISE_REQUEST_ID_REUSED))
                .thenThrow(new CustomException(ErrorCode.EXERCISE_DELETED));
        String body = objectMapper.writeValueAsString(edit(UUID.randomUUID(), 0L));
        mockMvc.perform(patch("/api/v1/exercises/1").headers(getCommonApiHeaders(memberId))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("EXERCISE_REQUEST_ID_REUSED"));
        mockMvc.perform(patch("/api/v1/exercises/1").headers(getCommonApiHeaders(memberId))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isGone()).andExpect(jsonPath("code").value("EXERCISE_DELETED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"editOnly", "versionOnly", "negative"})
    void incompleteEditMetadataIsRejectedBeforeCallingService(String mode) throws Exception {
        var request = edit(mode.equals("versionOnly") ? null : UUID.randomUUID(),
                mode.equals("editOnly") ? null : mode.equals("negative") ? -1L : 0L);
        mockMvc.perform(patch("/api/v1/exercises/1").headers(getCommonApiHeaders(memberId))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_INPUT_VALUE"));
        verifyNoInteractions(service);
    }

    @Test
    void syncUpload() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        when(service.uploadExerciseImages(eq(1L), anyList(), eq(requestId), eq(List.of(imageId))))
                .thenReturn(new ExercisePicturesResponse(1L, 1,
                        List.of(new ExercisePicturesResponse.UploadedPicture(imageId, 10L, "https://s3/uploads/photo"))));
        mockMvc.perform(multipart("/api/v1/exercises/{exerciseId}/pictures", 1)
                        .file(file()).queryParam("uploadRequestId", requestId.toString()).queryParam("clientImageIds", imageId.toString())
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("data.pictures[0].clientImageId").value(imageId.toString()))
                .andExpect(jsonPath("data.pictures[0].pictureId").value(10))
                .andDo(document.document(queryParameters(
                                parameterWithName("uploadRequestId").description("불변 배치 UUID"),
                                parameterWithName("clientImageIds").description("pictures 순서와 1:1 대응하는 고유 이미지 UUID 목록")),
                        requestParts(partWithName("pictures").description("1..5개 jpg/jpeg/png 파일. 재시도 시 바이너리/메타데이터 불변")),
                        responseFields(RestDocsUtils.commonResponseFields(
                                fieldWithPath("data.exerciseId").description("서버 기록 ID"),
                                fieldWithPath("data.version").description("서버 버전"),
                                fieldWithPath("data.pictures[]").description("요청 순서의 확정 이미지 매핑"),
                                fieldWithPath("data.pictures[].clientImageId").description("입력 이미지 UUID"),
                                fieldWithPath("data.pictures[].pictureId").description("사진 삭제와 상세 조회에 사용하는 Picture ID"),
                                fieldWithPath("data.pictures[].pictureUrl").description("저장된 사진 URL")))));
    }

    @Test
    void incompleteOrDuplicateUploadMetadataIsRejectedBeforeCallingService() throws Exception {
        UUID imageId = UUID.randomUUID();
        mockMvc.perform(multipart("/api/v1/exercises/1/pictures").file(file())
                        .param("uploadRequestId", UUID.randomUUID().toString()).headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/v1/exercises/1/pictures").file(file())
                        .param("clientImageIds", imageId.toString()).headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/v1/exercises/1/pictures").file(file()).file(file())
                        .param("uploadRequestId", UUID.randomUUID().toString())
                        .param("clientImageIds", imageId.toString(), imageId.toString()).headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/v1/exercises/1/pictures").file(file()).file(file())
                        .param("uploadRequestId", UUID.randomUUID().toString())
                        .param("clientImageIds", imageId.toString()).headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void syncDelete() throws Exception {
        UUID key = UUID.randomUUID();
        when(service.deleteExercise(1L, key)).thenReturn(new ExerciseIdResponseDto(1L, 4));
        mockMvc.perform(delete("/api/v1/exercises/{exerciseId}", 1).queryParam("editId", key.toString()).headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isOk()).andExpect(jsonPath("data.version").value(4))
                .andDo(document.document(queryParameters(parameterWithName("editId").description("불변 삭제 UUID. baseVersion 불필요")),
                        responseFields(mutationFields())));
    }

    @Test
    void syncDeletePictures() throws Exception {
        UUID key = UUID.randomUUID();
        when(service.deleteImages(1L, List.of(10L, 11L), key)).thenReturn(new ExerciseIdResponseDto(1L, 4));
        mockMvc.perform(delete("/api/v1/exercises/{exerciseId}/pictures", 1).queryParam("editId", key.toString())
                        .queryParam("pictureIds", "10", "11").headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isOk()).andExpect(jsonPath("data.exerciseId").value(1)).andExpect(jsonPath("data.version").value(4))
                .andDo(document.document(queryParameters(
                                parameterWithName("editId").description("불변 사진 삭제 UUID"),
                                parameterWithName("pictureIds").description("이 운동에 속한 사진 ID 목록. 순서/중복은 무시")),
                        responseFields(mutationFields())));
    }

    @Test
    void malformedUuidMetadataIsRejected() throws Exception {
        mockMvc.perform(delete("/api/v1/exercises/1").param("editId", "invalid").headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    private FieldDescriptor[] textFields(FieldDescriptor... metadata) {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(metadata));
        fields.add(fieldWithPath("exerciseTitle").description("운동 제목"));
        fields.add(fieldWithPath("exerciseDetail").optional().description("상세 내용"));
        fields.add(fieldWithPath("exercisePersonalType").optional().description("운동 종류"));
        fields.add(fieldWithPath("exerciseLocation").optional().description("운동 장소"));
        fields.add(fieldWithPath("exerciseStartedAt").description("운동 시작 시각"));
        fields.add(fieldWithPath("exerciseEndedAt").description("운동 종료 시각"));
        return fields.toArray(FieldDescriptor[]::new);
    }

    private FieldDescriptor[] mutationFields() {
        return RestDocsUtils.commonResponseFields(fieldWithPath("data.exerciseId").description("서버 기록 ID"),
                fieldWithPath("data.version").description("현재/원본 성공 응답의 서버 버전"));
    }

    private UpdateExerciseRequestDto edit(UUID key, Long version) {
        return UpdateExerciseRequestDto.builder().editId(key).baseVersion(version).exerciseTitle("편집")
                .exerciseStartedAt(START).exerciseEndedAt(START.plusHours(1)).build();
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("pictures", "사진.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }
}
