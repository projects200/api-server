package com.project200.undabang.exercise.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.service.ExerciseCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExerciseCommandController.class)
class ExerciseCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private ExerciseCommandService exerciseCommandService;

    /*
    @Test
    @DisplayName("운동 생성 - 성공 케이스")
    void createExercise_Success() throws Exception {
        // given
        MockMultipartFile mockFile1 = new MockMultipartFile(
                "exercisePictureList",
                "test1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile mockFile2 = new MockMultipartFile(
                "exercisePictureList",
                "test2.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes(StandardCharsets.UTF_8));
        UUID testUserId = UUID.randomUUID();
        CreateExerciseRequestDto requestDto = new CreateExerciseRequestDto(
                "Test Title",
                "Personal",
                "Gym",
                "Detailed description",
                LocalDateTime.of(2025, 5, 20, 10, 0),
                LocalDateTime.of(2025, 5, 22, 11, 0),
                List.of(mockFile1, mockFile2)
        );

        CreateExerciseResponseDto responseDto = new CreateExerciseResponseDto(1L);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", testUserId.toString());
        headers.add("Authorization", "Bearer dummy-token-for-docs");

        given(exerciseService.uploadExerciseImages(BDDMockito.any(CreateExerciseRequestDto.class))).willReturn(responseDto);

        MultiValueMap<String, String> params = getParams(requestDto);

        // when
//        String response = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/v1/exercises")
//                        .file((MockMultipartFile) requestDto.getExercisePictureList().get(0))
//                        .file((MockMultipartFile) requestDto.getExercisePictureList().get(1))
//                        .params(queryParams)
////                        .with(request -> {
////                            request.setMethod("POST");
////                            request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
////                            return request;
////                        })
//                        .headers(headers)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.MULTIPART_FORM_DATA))
////                        .content(objectMapper.writeValueAsString(requestDto))

        String requestJson = """
                {
                    "exerciseTitle": "Test Title",
                    "exercisePersonalType": "Personal",
                    "exerciseLocation": "Gym",
                    "exerciseDetail": "Detailed description",
                    "exerciseStartedAt": "2025-05-20T10:00:00",
                    "exerciseEndedAt": "2025-05-22T11:00:00"
                }
                """;

        String response = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/v1/exercises")
                        .file(mockFile1)
                        .file(mockFile2)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .headers(headers)
                        .accept(MediaType.APPLICATION_JSON)
                        .params(params))
                .andExpect(status().isCreated())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        // .file()로 보낸 필드들은 requestParts()로 문서화
                        requestParts(
                                partWithName("exercisePictureList").description("업로드할 운동 사진 파일 목록 (List<MultipartFile>)")
                        ),
                        // .queryParams()로 보낸 필드들은 queryParameters()로 문서화
                        formParameters(
                                parameterWithName("exerciseTitle").description("운동 제목 (String)"),
                                parameterWithName("exercisePersonalType").description("운동 유형 (String)"),
                                parameterWithName("exerciseLocation").description("운동 장소(사용자 직접 입력) (String)"),
                                parameterWithName("exerciseDetail").description("운동 세부 설명 (String)"),
                                parameterWithName("exerciseStartedAt").description("운동 시작 시각 (ISO 8601 DateTime)"),
                                parameterWithName("exerciseEndedAt").description("운동 종료 시각(시작 시간보다 이후여야함) (ISO 8601 DateTime)")
                        ),
                        responseFields(commonResponseFields(
                                fieldWithPath("data.exerciseId").type(JsonFieldType.NUMBER).description("운동 ID")
                        ))
                ))
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(response).isEqualTo(objectMapper.writeValueAsString(CommonResponse.create(responseDto)));
        BDDMockito.then(exerciseService).should(BDDMockito.times(1))
                .uploadExerciseImages(BDDMockito.any(CreateExerciseRequestDto.class));
    }

    private static @NotNull MultiValueMap<String, String> getParams(CreateExerciseRequestDto requestDto) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("exerciseTitle", requestDto.getExerciseTitle());
        queryParams.add("exercisePersonalType", requestDto.getExercisePersonalType());
        queryParams.add("exerciseLocation", requestDto.getExerciseLocation());
        queryParams.add("exerciseDetail", requestDto.getExerciseDetail());
        queryParams.add("exerciseStartedAt", requestDto.getExerciseStartedAt().toString());
        queryParams.add("exerciseEndedAt", requestDto.getExerciseEndedAt().toString());
        return queryParams;
    }*/

    /*
    @Test
    @DisplayName("허용되지 않은 확장자 파일이 포함되면 유효성 검사 실패")
    void ValidationFailureWhenInvalidFileExtension() {
        // given
        MultipartFile validFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image".getBytes());
        MultipartFile invalidFile = new MockMultipartFile("file", "test.txt", "text/plain", "test text".getBytes());
        List<MultipartFile> files = Arrays.asList(validFile, invalidFile);

        CreateExerciseRequestDto dto = new CreateExerciseRequestDto(
                "운동 제목",
                "개인",
                "운동 장소",
                "운동 상세",
                LocalDateTime.of(2023, 10, 10, 9, 0),
                LocalDateTime.of(2023, 10, 10, 10, 0),
                files
        );

        // when
        Set<ConstraintViolation<CreateExerciseRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("허용되지 않은 파일 확장자입니다.");
    }

    @Test
    @DisplayName("허용된 확장자 파일만 포함되면 유효성 검사 성공")
    void ValidationSuccessWhenValidFileExtensions() {
        // given
        MultipartFile file1 = new MockMultipartFile("file", "test1.jpg", "image/jpeg", "test image1".getBytes());
        MultipartFile file2 = new MockMultipartFile("file", "test2.png", "image/png", "test image2".getBytes());
        List<MultipartFile> files = Arrays.asList(file1, file2);

        CreateExerciseRequestDto dto = new CreateExerciseRequestDto(
                "운동 제목",
                "개인",
                "운동 장소",
                "운동 상세",
                LocalDateTime.of(2023, 10, 10, 9, 0),
                LocalDateTime.of(2023, 10, 10, 10, 0)
        );

        // when
        Set<ConstraintViolation<CreateExerciseRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("파일 목록이 null이면 유효성 검사 성공")
    void ValidationSuccessWhenFileListIsNull() {
        // given
        CreateExerciseRequestDto dto = new CreateExerciseRequestDto(
                "운동 제목",
                "개인",
                "운동 장소",
                "운동 상세",
                LocalDateTime.of(2023, 10, 10, 9, 0),
                LocalDateTime.of(2023, 10, 10, 10, 0)
        );

        // when
        Set<ConstraintViolation<CreateExerciseRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("파일 목록이 비어있으면 유효성 검사 성공")
    void ValidationSuccessWhenFileListIsEmpty() {
        // given
        CreateExerciseRequestDto dto = new CreateExerciseRequestDto(
                "운동 제목",
                "개인",
                "운동 장소",
                "운동 상세",
                LocalDateTime.of(2023, 10, 10, 9, 0),
                LocalDateTime.of(2023, 10, 10, 10, 0),
        );

        // when
        Set<ConstraintViolation<CreateExerciseRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }
     */

    @Test
    @DisplayName("운동 생성 - 성공 케이스")
    void createExerciseSuccess() throws Exception {
        // given
        UUID testUserId = UUID.randomUUID();

        CreateExerciseRequestDto requestDto = CreateExerciseRequestDto.builder()
                .exerciseTitle("운동 1일차")
                .exerciseStartedAt(LocalDateTime.now().minusHours(3))
                .exerciseEndedAt(LocalDateTime.now().minusHours(1))
                .exerciseLocation("여의도 한강 공원")
                .exercisePersonalType("러닝")
                .exerciseDetail("10시간 동안 달리기")
                .build();

        ExerciseIdResponseDto responseDto = new ExerciseIdResponseDto(1L);
        given(exerciseCommandService.createExercise(BDDMockito.any(CreateExerciseRequestDto.class)))
                .willReturn(responseDto);

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", testUserId.toString());
        headers.add("Authorization", "Bearer dummy-access-token-for-docs");

        String actualResponse = this.mockMvc.perform(MockMvcRequestBuilders.post("/v1/exercises")
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(status().isCreated())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_ACCESS_TOKEN),
                        requestFields(
                                fieldWithPath("exerciseTitle").type(JsonFieldType.STRING)
                                        .description("운동 제목: 최대 255자"),
                                fieldWithPath("exerciseStartedAt").type(JsonFieldType.STRING)
                                        .description("운동 시작 일시: 현재 이전"),
                                fieldWithPath("exerciseEndedAt").type(JsonFieldType.STRING)
                                        .description("운동 종료 일시: 현재 이전, 시작일시 이후"),
                                fieldWithPath("exerciseDetail").type(JsonFieldType.STRING)
                                        .description("운동 상세 설명: 글자 수 제한 없음"),
                                fieldWithPath("exerciseLocation").type(JsonFieldType.STRING)
                                        .description("운동 장소(사용자 직접 입력): 최대 255자"),
                                fieldWithPath("exercisePersonalType").type(JsonFieldType.STRING)
                                        .description("운동 종류: 최대 255자")
                        ),
                        responseFields(RestDocsUtils.commonResponseFields(
                                fieldWithPath("data.exerciseId").type(JsonFieldType.NUMBER).description("생성된 운동의 ID")
                        ))
                ))
                .andReturn().getResponse().getContentAsString();

        // then
        CommonResponse<ExerciseIdResponseDto> expectedResponse = CommonResponse.create(responseDto);
        assertThat(actualResponse)
                .as("운동 생성 성공 응답 검증")
                .isEqualTo(objectMapper.writeValueAsString(expectedResponse));
        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1))
                .createExercise(BDDMockito.any(CreateExerciseRequestDto.class));
    }
 /*
    @Test
    @DisplayName("운동 생성 - 잘못된 입력")
    void createExerciseInvalidInput() throws Exception {
        // given
        UUID testUserId = UUID.randomUUID();
        try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
            given(UserContextHolder.getUserId()).willReturn(testUserId);

            CreateExerciseRequestDto invalidRequestDto = new CreateExerciseRequestDto(
                    "",  // 빈 제목
                    LocalDateTime.now().plusDays(1),  // 미래 시작 시간
                    LocalDateTime.now().plusDays(1).plusHours(1)
            );

            // when
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", testUserId.toString());

            this.mockMvc.perform(MockMvcRequestBuilders.post("/v1/exercises")
                            .content(objectMapper.writeValueAsString(invalidRequestDto))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(headers))
                    .andExpect(status().isBadRequest())
                    .andDo(this.document.document(
                            requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                            requestFields(
                                    fieldWithPath("exerciseTitle").type(JsonFieldType.STRING).description("운동 제목"),
                                    fieldWithPath("exerciseStartedAt").type(JsonFieldType.STRING).description("운동 시작 시간"),
                                    fieldWithPath("exerciseEndedAt").type(JsonFieldType.STRING).description("운동 종료 시간")
                            ),
                            responseFields(RestDocsUtils.commonResponseFieldsOnly())
                    ));

            // then
            BDDMockito.then(exerciseCommandService).shouldHaveNoInteractions();
        }
    }

    @Test
    @DisplayName("운동 생성 - 업로드 파일 갯수 초과")
    void createExerciseFailFilesExceeded() throws Exception {
        // given
        UUID testUserId = UUID.randomUUID();
        try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
            given(UserContextHolder.getUserId()).willReturn(testUserId);

            CreateExerciseRequestDto requestDto = new CreateExerciseRequestDto(
                    "운동 테스트 제목",
                    LocalDateTime.now().minusHours(3),
                    LocalDateTime.now()
            );

            List<MockMultipartFile> files = List.of(
                    new MockMultipartFile("pictures", "file1.jpg", MediaType.IMAGE_JPEG_VALUE, "test-content".getBytes()),
                    new MockMultipartFile("pictures", "file2.jpg", MediaType.IMAGE_JPEG_VALUE, "test-content".getBytes()),
                    new MockMultipartFile("pictures", "file3.jpg", MediaType.IMAGE_JPEG_VALUE, "test-content".getBytes()),
                    new MockMultipartFile("pictures", "file4.jpg", MediaType.IMAGE_JPEG_VALUE, "test-content".getBytes()),
                    new MockMultipartFile("pictures", "file5.jpg", MediaType.IMAGE_JPEG_VALUE, "test-content".getBytes()),
                    new MockMultipartFile("pictures", "file6.jpg", MediaType.IMAGE_JPEG_VALUE, "test-content".getBytes())
            );

            // when
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", testUserId.toString());

            this.mockMvc.perform(MockMvcRequestBuilders.multipart("/v1/exercises/{exerciseId}/pictures", 1L)
                            .file(files.get(0))
                            .file(files.get(1))
                            .file(files.get(2))
                            .file(files.get(3))
                            .file(files.get(4))
                            .file(files.get(5))
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(headers))
                    .andExpect(status().isBadRequest())
                    .andDo(this.document.document(
                            requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                            requestParts(
                                    partWithName("pictures").description("운동 사진 파일들 (최대 5개)")
                            ),
                            responseFields(RestDocsUtils.commonResponseFieldsOnly())
                    ));

            // then
            BDDMockito.then(exerciseCommandService).shouldHaveNoInteractions();
        }
    }

    @Test
    @DisplayName("운동 생성 - 잘못된 파일 확장자")
    void createExerciseFailInvalidFileExtension() throws Exception {
        // given
        UUID testUserId = UUID.randomUUID();
        try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
            given(UserContextHolder.getUserId()).willReturn(testUserId);

            MockMultipartFile invalidFile = new MockMultipartFile("pictures", "test.exe", MediaType.APPLICATION_OCTET_STREAM_VALUE, "test-content".getBytes());

            // when
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", testUserId.toString());

            this.mockMvc.perform(MockMvcRequestBuilders.multipart("/v1/exercises/{exerciseId}/pictures", 1L)
                            .file(invalidFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(headers))
                    .andExpect(status().isBadRequest())
                    .andDo(this.document.document(
                            requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                            requestParts(
                                    partWithName("pictures").description("운동 사진 파일들 (허용된 확장자: .jpg, .jpeg, .png)")
                            ),
                            responseFields(RestDocsUtils.commonResponseFieldsOnly())
                    ));

            // then
            BDDMockito.then(exerciseCommandService).shouldHaveNoInteractions();
        }
    }
 */
}