package com.project200.undabang.exercise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.restdocs.operation.preprocess.UriModifyingOperationPreprocessor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
        UUID testMemberId = UUID.randomUUID();

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
        String response = this.mockMvc.perform(post("/api/v1/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId))
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpectAll(
                        status().isCreated() // 상태 코드 201 확인
                )
                .andDo(this.document.document(
                        requestHeaders(
                                RestDocsUtils.HEADER_ACCESS_TOKEN
                        ),
                        requestFields(
                                fieldWithPath("exerciseTitle").type(JsonFieldType.STRING)
                                        .description("운동 제목입니다. 최대 255자까지 입력 가능합니다. null, 빈 문자열, 공백을 받지 않습니다"),
                                fieldWithPath("exercisePersonalType").type(JsonFieldType.STRING).optional()
                                        .description("운동 종류입니다. 사용자가 입력하는 값이며 최대 255자까지 입력 가능합니다."),
                                fieldWithPath("exerciseLocation").type(JsonFieldType.STRING).optional()
                                        .description("운동 장소 입니다. 최대 255자까지 입력 가능합니다."),
                                fieldWithPath("exerciseDetail").type(JsonFieldType.STRING).optional()
                                        .description("운동에 대한 상세 내용입니다. 최대 65,535byte 까지 입력 가능합니다. 글자 수 기준이 아닙니다."),
                                fieldWithPath("exerciseStartedAt").type("Datetime")
                                        .description("시작일시 입니다. ISO 8601 형식으로 입력 받으며 오늘 이전 일시여야 합니다."),
                                fieldWithPath("exerciseEndedAt").type("Datetime")
                                        .description("종료일시 입니다.ISO 8601 형식으로 입력 받습니다. 오늘 이전 일시여야 하며 시작 일시 이후여야 합니다.")
                        ),
                        responseFields(RestDocsUtils.commonResponseFields(
                                fieldWithPath("data.exerciseId").type(JsonFieldType.NUMBER)
                                        .description("운동 ID입니다. 이미지 업로드 시 사용 가능합니다.")
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

    // uploadExerciseImages 성공 테스트
    @Test
    @DisplayName("운동 이미지 업로드가 성공하면 201 상태 코드와 응답 데이터를 반환한다")
    void uploadExerciseImagesSuccess() throws Exception {
        // given
        UUID testMemberId = UUID.randomUUID();
        Long exerciseId = 1L;

        MockMultipartFile file1 = new MockMultipartFile(
                "pictures",
                "image1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "DummyImage1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "pictures",
                "image2.png",
                MediaType.IMAGE_PNG_VALUE,
                "DummyImage2".getBytes()
        );

        ExerciseIdResponseDto responseDto = new ExerciseIdResponseDto(exerciseId);
        BDDMockito.given(exerciseCommandService.uploadExerciseImages(BDDMockito.eq(exerciseId), BDDMockito.anyList()))
                .willReturn(responseDto);

        // when
        String response = this.mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/api/v1/exercises/{exerciseId}/pictures", exerciseId)
                        .file(file1)
                        .file(file2)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpectAll(
                        status().isCreated()
                )
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("exerciseId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                        .description("운동 ID입니다. 이미지 업로드 시 사용 가능합니다.")
                        ),
                        requestHeaders(
                                RestDocsUtils.HEADER_ACCESS_TOKEN
                        ),
                        requestParts(
                                partWithName("pictures").attributes(getTypeFormat("FILE"))
                                        .description("운동 이미지 파일들입니다. " +
                                                "운동 이미지는 최대 5개까지 업로드 가능합니다. " +
                                                "각 파일 크기는 10MB 이하여야 합니다. " +
                                                "파일의 확장자는 .jpg, .jpeg, .png만 허용됩니다.")
                        ),
                        responseFields(RestDocsUtils.commonResponseFields(
                                fieldWithPath("data.exerciseId").type(JsonFieldType.NUMBER)
                                        .description("이미지가 업로드 된 운동 ID입니다.")
                        ))
                ))
                .andReturn().getResponse().getContentAsString();

        // then
        Assertions.assertThat(response).isEqualTo(objectMapper.writeValueAsString(
                CommonResponse.create(new ExerciseIdResponseDto(exerciseId))
        ));
        BDDMockito.then(exerciseCommandService).should()
                .uploadExerciseImages(BDDMockito.eq(exerciseId), BDDMockito.anyList());
        BDDMockito.then(exerciseCommandService).shouldHaveNoMoreInteractions();
    }

    // uploadExerciseImages 실패 테스트
    @Test
    @DisplayName("지원하지 않는 파일 확장자 업로드 시 에러 발생")
    void uploadExerciseImagesUnsupportedFileExtension() throws Exception {
        // given
        UUID testMemberId = UUID.randomUUID();
        Long exerciseId = 1L;

        MockMultipartFile firstFile = new MockMultipartFile("pictures", "invalid_file.exe", MediaType.APPLICATION_OCTET_STREAM_VALUE, "InvalidContent".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("pictures", "image2.png", MediaType.IMAGE_PNG_VALUE, "DummyImage2".getBytes());

        // when
        String response = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/exercises/{exerciseId}/pictures", exerciseId)
                        .file(firstFile)
                        .file(secondFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpectAll(
                        status().isBadRequest() // 상태 코드 400 확인
                )
                .andReturn().getResponse().getContentAsString();

        // then
        Assertions.assertThat(response).contains("허용되지 않은 파일 확장자입니다.");
        BDDMockito.then(exerciseCommandService).should(BDDMockito.never())
                .uploadExerciseImages(BDDMockito.eq(exerciseId), BDDMockito.anyList());
        BDDMockito.then(exerciseCommandService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("파일이 5개를 초과할 경우 에러 발생")
    void uploadExerciseImagesExceedFileLimit() throws Exception {
        // given
        UUID testMemberId = UUID.randomUUID();
        Long exerciseId = 1L;

        MockMultipartFile[] files = new MockMultipartFile[6];
        for (int i = 0; i < 6; i++) {
            files[i] = new MockMultipartFile("pictures", "image" + (i + 1) + ".jpg", MediaType.IMAGE_JPEG_VALUE, ("DummyImage" + (i + 1)).getBytes());
        }

        // when
        MockMultipartHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.multipart("/api/v1/exercises/{exerciseId}/pictures", exerciseId);
        for (MockMultipartFile file : files) {
            requestBuilder.file(file);
        }

        String response = this.mockMvc.perform(requestBuilder
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpectAll(
                        status().isBadRequest() // 상태 코드 400 확인
                )
                .andReturn().getResponse().getContentAsString();


        // then
        Assertions.assertThat(response).contains("최대 5개의 파일만 업로드할 수 있습니다.");
        BDDMockito.then(exerciseCommandService).should(BDDMockito.never())
                .uploadExerciseImages(BDDMockito.eq(exerciseId), BDDMockito.anyList());
        BDDMockito.then(exerciseCommandService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("운동기록 변경 테스트 _ 성공")
    void exerciseUpdate_Success() throws Exception {
        // given
        UUID testMemberId = UUID.randomUUID();
        Long exerciseId = 1L;
        UpdateExerciseRequestDto requestDto = UpdateExerciseRequestDto.builder()
                .exerciseTitle("운동제목")
                .exerciseDetail("운동내용")
                .exerciseLocation("운동위치")
                .exercisePersonalType("운동종류")
                .exerciseStartedAt(LocalDateTime.of(2025, 5, 1, 0, 0, 0))
                .exerciseEndedAt(LocalDateTime.of(2025, 5, 1, 1, 0, 0))
                .build();

        ExerciseIdResponseDto responseDto = new ExerciseIdResponseDto(1L);

        BDDMockito.given(exerciseCommandService.updateExercise(exerciseId, requestDto)).willReturn(responseDto);

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/exercises/{exerciseId}", exerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId))
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("exerciseId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                        .description("운동 ID입니다. 수정할 운동 기록 ID를 입력하시면 됩니다.")
                        ),
                        requestHeaders(RestDocsUtils.HEADER_ACCESS_TOKEN),
                        requestFields(
                                fieldWithPath("exerciseTitle").type(JsonFieldType.STRING)
                                        .description("운동 제목입니다. 최대 255자까지 입력 가능합니다. null, 빈 문자열, 공백을 받지 않습니다"),
                                fieldWithPath("exercisePersonalType").type(JsonFieldType.STRING).optional()
                                        .description("운동 종류입니다. 사용자가 입력하는 값이며 최대 255자까지 입력 가능합니다."),
                                fieldWithPath("exerciseLocation").type(JsonFieldType.STRING).optional()
                                        .description("운동 장소 입니다. 최대 255자까지 입력 가능합니다."),
                                fieldWithPath("exerciseDetail").type(JsonFieldType.STRING).optional()
                                        .description("운동에 대한 상세 내용입니다. 최대 65,535byte 까지 입력 가능합니다. 글자 수 기준이 아닙니다."),
                                fieldWithPath("exerciseStartedAt").type("Datetime")
                                        .description("시작일시 입니다. ISO 8601 형식으로 입력 받으며 오늘 이전 일시여야 합니다."),
                                fieldWithPath("exerciseEndedAt").type("Datetime")
                                        .description("종료일시 입니다.ISO 8601 형식으로 입력 받습니다. 오늘 이전 일시여야 하며 시작 일시 이후여야 합니다.")
                        ),
                        responseFields(RestDocsUtils.commonResponseFields(
                                        fieldWithPath("data.exerciseId").type(JsonFieldType.NUMBER)
                                                .description("운동 ID 입니다. 이미지 업로드 시 사용 가능합니다.")
                                )
                        )
                ))
                .andReturn().getResponse().getContentAsString();

        // then
        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1))
                .updateExercise(BDDMockito.eq(exerciseId), BDDMockito.any(UpdateExerciseRequestDto.class));
    }

    @Test
    @DisplayName("운동 기록 수정 - 실패 (exerciseId 음수)")
    void updateExercise_Fail_NegativeExerciseId() throws Exception {
        // given
        long invalidExerciseId = -1L;
        UUID testMemberId = UUID.randomUUID();
        UpdateExerciseRequestDto requestDto = UpdateExerciseRequestDto.builder()
                .exerciseTitle("수정된 운동 제목")
                .exerciseStartedAt(LocalDateTime.now().minusHours(2))
                .exerciseEndedAt(LocalDateTime.now().minusHours(1))
                .build();

        // when & then
        this.mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/exercises/{exerciseId}", invalidExerciseId)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("message").value("요청 파라미터 유효성 검증에 실패했습니다."));

        BDDMockito.then(exerciseCommandService).should(BDDMockito.never())
                .updateExercise(BDDMockito.anyLong(), BDDMockito.any(UpdateExerciseRequestDto.class));
    }

    @Test
    @DisplayName("운동 기록 수정 - 실패 (요청 DTO 유효성 위반 - 제목 공백)")
    void exerciseUpdate_validateFailed_BlankTitle() throws Exception {
        // given
        long exerciseId = 1L;
        UUID testMemberId = UUID.randomUUID();
        UpdateExerciseRequestDto requestDto = UpdateExerciseRequestDto.builder()
                .exerciseTitle(" ") // 유효성 위반: 제목 공백
                .exerciseStartedAt(LocalDateTime.now().minusHours(2))
                .exerciseEndedAt(LocalDateTime.now().minusHours(1))
                .exerciseLocation("운동 장소")
                .exercisePersonalType("운동 종류")
                .exerciseDetail("운동 상세")
                .build();

        // when & then
        this.mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/exercises/{exerciseId}", exerciseId)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("message").isNotEmpty());

        BDDMockito.then(exerciseCommandService).should(BDDMockito.never())
                .updateExercise(BDDMockito.anyLong(), BDDMockito.any(UpdateExerciseRequestDto.class));
    }

    @Test
    @DisplayName("운동 기록 수정 - 실패 (요청 DTO 유효성 위반 - 시작 시간이 종료 시간 이후)")
    void exerciseUpdate_validateFailed_InvalidStartEndTime() throws Exception {
        // given
        long exerciseId = 1L;
        UUID testMemberId = UUID.randomUUID();
        UpdateExerciseRequestDto requestDto = UpdateExerciseRequestDto.builder()
                .exerciseTitle("유효한 제목")
                .exerciseStartedAt(LocalDateTime.now().minusHours(1)) // 시작 시간이 종료 시간보다 이후
                .exerciseEndedAt(LocalDateTime.now().minusHours(2))
                .exerciseLocation("운동 장소")
                .exercisePersonalType("운동 종류")
                .exerciseDetail("운동 상세")
                .build();

        // when & then
        this.mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/exercises/{exerciseId}", exerciseId)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("message").isNotEmpty());

        BDDMockito.then(exerciseCommandService).should(BDDMockito.never())
                .updateExercise(BDDMockito.anyLong(), BDDMockito.any(UpdateExerciseRequestDto.class));
    }

    @Test
    @DisplayName("운동기록 이미지 삭제 성공 케이스")
    void deleteExerciseImages() throws Exception {
        // given
        Long testExerciseId = 1L;
        List<Long> pictureIds = List.of(1L, 2L, 3L);
        UUID testMemberID = UUID.randomUUID();

        String[] queryParamPictureIdArr = pictureIds.stream().map(String::valueOf).toArray(String[]::new);

        // void 니까 willDoNothing()
        BDDMockito.willDoNothing().given(exerciseCommandService).deleteImages(testExerciseId, pictureIds);

        this.mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/exercises/{exerciseId}/pictures", testExerciseId)
                        .queryParam("pictureIds", queryParamPictureIdArr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberID)))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("exerciseId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                        .description("운동 ID입니다. 수정할 운동 기록 ID를 입력하시면 됩니다.")
                        ),
                        queryParameters(
                                parameterWithName("pictureIds").attributes(getTypeFormat(JsonFieldType.STRING))
                                        .description("사진 ID 리스트 입니다. 삭제할 사진 ID 리스트를 입력하시면 됩니다.")
                        ),
                        requestHeaders(RestDocsUtils.HEADER_ACCESS_TOKEN),
                        responseFields(RestDocsUtils.commonResponseFieldsOnly())
                ))
                .andReturn().getResponse().getContentAsString();

        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1)).deleteImages(testExerciseId, pictureIds);
    }

    @Test
    @DisplayName("운동기록 이미지 삭제 - 실패 (exerciseId 음수)")
    void deleteExerciseImages_Fail_NegativeExerciseId() throws Exception {
        // given
        long invalidExerciseId = -1L;
        List<Long> pictureIds = List.of(1L, 2L, 3L);
        UUID testMemberID = UUID.randomUUID();
        String[] queryParamPictureIdArr = pictureIds.stream().map(String::valueOf).toArray(String[]::new);

        // when & then
        this.mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/exercises/{exerciseId}/pictures", invalidExerciseId)
                        .queryParam("pictureIds", queryParamPictureIdArr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("message").value("요청 파라미터 유효성 검증에 실패했습니다."));


        BDDMockito.then(exerciseCommandService).should(BDDMockito.never()).deleteImages(BDDMockito.anyLong(), BDDMockito.anyList());
    }

    @Test
    @DisplayName("운동기록 이미지 삭제 - 실패 (pictureIds 파라미터 누락)")
    void deleteExerciseImages_Fail_MissingPictureIds() throws Exception {
        // given
        Long testExerciseId = 1L;
        UUID testMemberID = UUID.randomUUID();

        // when & then
        this.mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/exercises/{exerciseId}/pictures", testExerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("message").value("필수 파라미터를 입력해주세요."));

        BDDMockito.then(exerciseCommandService).should(BDDMockito.never()).deleteImages(BDDMockito.anyLong(), BDDMockito.anyList());
    }


    @Test
    @DisplayName("운동기록 이미지 삭제 - 실패 (권한 없음)")
    void deleteExerciseImages_Fail_AuthorizationDenied() throws Exception {
        // given
        Long testExerciseId = 1L;
        List<Long> pictureIds = List.of(1L, 2L, 3L);
        UUID testMemberID = UUID.randomUUID();
        String[] queryParamPictureIdArr = pictureIds.stream().map(String::valueOf).toArray(String[]::new);

        BDDMockito.willThrow(new CustomException(ErrorCode.AUTHORIZATION_DENIED))
                .given(exerciseCommandService).deleteImages(testExerciseId, pictureIds);

        // when & then
        this.mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/exercises/{exerciseId}/pictures", testExerciseId)
                        .queryParam("pictureIds", queryParamPictureIdArr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberID)))
                .andExpect(status().isForbidden()) // 403
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("code").value(ErrorCode.AUTHORIZATION_DENIED.getCode()))
                .andExpect(jsonPath("message").value(ErrorCode.AUTHORIZATION_DENIED.getMessage()));

        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1)).deleteImages(testExerciseId, pictureIds);
    }

    @Test
    @DisplayName("운동기록 이미지 삭제 - 실패 (사용자 없음)")
    void deleteExerciseImages_Fail_MemberNotFound() throws Exception {
        // given
        Long testExerciseId = 1L;
        List<Long> pictureIds = List.of(1L, 2L, 3L);
        UUID testMemberID = UUID.randomUUID(); // 이 ID로 사용자를 찾을 수 없다고 가정
        String[] queryParamPictureIdArr = pictureIds.stream().map(String::valueOf).toArray(String[]::new);

        BDDMockito.willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND))
                .given(exerciseCommandService).deleteImages(testExerciseId, pictureIds);

        // when & then
        this.mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/exercises/{exerciseId}/pictures", testExerciseId)
                        .queryParam("pictureIds", queryParamPictureIdArr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberID))) // UserContextHolder가 이 ID를 사용한다고 가정
                .andExpect(status().isNotFound()) // 404
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("code").value(ErrorCode.MEMBER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1)).deleteImages(testExerciseId, pictureIds);
    }

    @Test
    @DisplayName("운동기록 삭제 _ 성공케이스")
    void deleteExercise() throws Exception {
        // given
        Long testExerciseId = 1L;
        UUID testMemberId = UUID.randomUUID();

        BDDMockito.willDoNothing().given(exerciseCommandService).deleteExercise(testExerciseId);

        this.mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/exercises/{exerciseId}", testExerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("exerciseId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                        .description("운동 ID입니다. 운동기록 삭제시 사용 가능합니다.")
                        ),
                        requestHeaders(
                                RestDocsUtils.HEADER_ACCESS_TOKEN
                        ),
                        responseFields(
                                RestDocsUtils.commonResponseFieldsOnly()
                        )
                )).andReturn().getResponse().getContentAsString();

        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1)).deleteExercise(testExerciseId);
    }

    @Test
    @DisplayName("운동기록 삭제실패 _ exerciseId 음수입력")
    void deleteExerciseFailed_negativePathVariable() throws Exception {
        Long testExerciseId = -1L;
        UUID testMemberId = UUID.randomUUID();

        this.mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/exercises/{exerciseId}", testExerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("message").value("요청 파라미터 유효성 검증에 실패했습니다."))
                .andExpect(jsonPath("code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
                .andReturn().getResponse().getContentAsString();

        BDDMockito.then(exerciseCommandService).should(BDDMockito.never()).deleteExercise(testExerciseId);
    }

    @Test
    @DisplayName("운동기록 삭제실패 _ 권한없는 사용자의 삭제요청")
    void deleteExerciseFailed_AuthorizationFailed() throws Exception {
        Long testExerciseId = 1L;
        UUID testMemberId = UUID.randomUUID();

        BDDMockito.willThrow(new CustomException(ErrorCode.AUTHORIZATION_DENIED)).given(exerciseCommandService).deleteExercise(testExerciseId);

        this.mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/exercises/{exerciseId}", testExerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("code").value(ErrorCode.AUTHORIZATION_DENIED.getCode()))
                .andExpect(jsonPath("message").value(ErrorCode.AUTHORIZATION_DENIED.getMessage()))
                .andReturn().getResponse().getContentAsString();

        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1)).deleteExercise(testExerciseId);
    }

    @Test
    @DisplayName("운동기록 삭제실패 _ 회원정보 없음")
    void deleteExerciseFailed_NoMemberExist() throws Exception {
        Long testExerciseId = 1L;
        UUID testMemberId = UUID.randomUUID();

        BDDMockito.willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND)).given(exerciseCommandService).deleteExercise(testExerciseId);

        this.mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/exercises/{exerciseId}", testExerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("code").value(ErrorCode.MEMBER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage()))
                .andReturn().getResponse().getContentAsString();

        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1)).deleteExercise(testExerciseId);
    }

    @Test
    @DisplayName("운동기록 삭제실패 _ 운동기록 없음")
    void deleteExerciseFailed_NoExerciseRecordExist() throws Exception {
        Long testExerciseId = 1L;
        UUID testMemberId = UUID.randomUUID();

        BDDMockito.willThrow(new CustomException(ErrorCode.EXERCISE_RECORD_NOT_FOUND)).given(exerciseCommandService).deleteExercise(testExerciseId);

        this.mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/exercises/{exerciseId}", testExerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("code").value(ErrorCode.EXERCISE_RECORD_NOT_FOUND.getCode()))
                .andExpect(jsonPath("message").value(ErrorCode.EXERCISE_RECORD_NOT_FOUND.getMessage()))
                .andReturn().getResponse().getContentAsString();

        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1)).deleteExercise(testExerciseId);
    }

    @Test
    @DisplayName("운동기록 삭제실패 _ 이미지 삭제 실패")
    void deleteExerciseFailed_ImageDeletionFailed() throws Exception {
        Long testExerciseId = 1L;
        UUID testMemberId = UUID.randomUUID();

        BDDMockito.willThrow(new CustomException(ErrorCode.EXERCISE_PICTURE_DELETE_FAILED))
                .given(exerciseCommandService).deleteExercise(testExerciseId);

        this.mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/exercises/{exerciseId}", testExerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("code").value(ErrorCode.EXERCISE_PICTURE_DELETE_FAILED.getCode()))
                .andExpect(jsonPath("message").value(ErrorCode.EXERCISE_PICTURE_DELETE_FAILED.getMessage()))
                .andReturn().getResponse().getContentAsString();

        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1)).deleteExercise(testExerciseId);
    }
}