package com.project200.undabang.exercise.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.dto.response.PictureDataResponse;
import com.project200.undabang.exercise.service.ExerciseCommandService;
import com.project200.undabang.exercise.service.ExerciseQueryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.*;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFieldsForList;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExerciseQueryController.class)
class ExerciseQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private ExerciseQueryService exerciseQueryService;

    @MockitoBean
    private ExerciseCommandService exerciseCommandService;

    @Test
    @DisplayName("운동기록 상세조회 - 자신의 운동기록 상세조회")
    void findMemberExerciseRecord() throws Exception {
        //givenexerciseService
        UUID memberId = UUID.randomUUID();
        Long recordId = 1L;
        LocalDateTime currentTime = LocalDateTime.now().minusHours(12);

        PictureDataResponse data = new PictureDataResponse();
        data.setPictureId(1L);
        data.setPictureName("find");
        data.setPictureExtension("jpg");
        data.setPictureUrl("https://s3/picture/pic1.jpg");

        PictureDataResponse data2 = new PictureDataResponse();
        data2.setPictureId(2L);
        data2.setPictureName("find2");
        data2.setPictureExtension("jpg");
        data2.setPictureUrl("https://s3/picture/pic2.jpg");

        PictureDataResponse data3 = new PictureDataResponse();
        data3.setPictureId(3L);
        data3.setPictureName("find3");
        data3.setPictureExtension("jpg");
        data3.setPictureUrl("https://s3/picture/pic3.jpg");

        List<PictureDataResponse> pictureDataResponseList = new ArrayList<>();
        pictureDataResponseList.add(data);
        pictureDataResponseList.add(data2);
        pictureDataResponseList.add(data3);

        FindExerciseRecordResponseDto respDto = new FindExerciseRecordResponseDto();
        respDto.setExerciseTitle("운동제목");
        respDto.setExerciseDetail("운동내용");
        respDto.setExerciseLocation("운동위치");
        respDto.setExercisePersonalType("운동종류");
        respDto.setExerciseStartedAt(currentTime);
        respDto.setExerciseEndedAt(currentTime.plusHours(12));
        respDto.setPictureDataList(Optional.of(pictureDataResponseList));


        given(exerciseQueryService.findExerciseRecordByRecordId(recordId)).willReturn(respDto);

        //when

        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises/{exerciseId}", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpectAll(status().isOk())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        pathParameters(
                                parameterWithName("exerciseId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                        .description("운동 ID입니다. 조회할 운동 기록 ID를 입력하시면 됩니다.")
                        ),
                        responseFields(commonResponseFields(
                                fieldWithPath("data.exerciseTitle").type(JsonFieldType.STRING)
                                        .description("운동 제목입니다."),
                                fieldWithPath("data.exerciseDetail").type(JsonFieldType.STRING)
                                        .description("운동 내용입니다."),
                                fieldWithPath("data.exercisePersonalType").type(JsonFieldType.STRING)
                                        .description("운동 종류입니다."),
                                fieldWithPath("data.exerciseStartedAt").type(JsonFieldType.STRING)
                                        .description("운동 시작 시간입니다."),
                                fieldWithPath("data.exerciseEndedAt").type(JsonFieldType.STRING)
                                        .description("운동 종료 시간입니다."),
                                fieldWithPath("data.exerciseLocation").type(JsonFieldType.STRING)
                                        .description("운동 장소 제목입니다."),
                                fieldWithPath("data.pictureDataList[]").type(JsonFieldType.ARRAY)
                                        .description("운동 사진 리스트입니다. 사진이 없으면 null로 반환됩니다. " +
                                                "사진은 이미지 업로드할 때, 저장된 순서로 반환됩니다."),
                                fieldWithPath("data.pictureDataList[].pictureId").type(JsonFieldType.NUMBER)
                                        .description("사진 식별자 ID입니다. 삭제할 때 사용 가능합니다."),
                                fieldWithPath("data.pictureDataList[].pictureUrl").type(JsonFieldType.STRING)
                                        .description("사진 URL입니다."),
                                fieldWithPath("data.pictureDataList[].pictureName").type(JsonFieldType.STRING)
                                        .description("사진 이름입니다."),
                                fieldWithPath("data.pictureDataList[].pictureExtension").type(JsonFieldType.STRING)
                                        .description("사진 확장자입니다.")
                        ))
                ))
                .andReturn().getResponse().getContentAsString();


        //then
        CommonResponse<FindExerciseRecordResponseDto> expectedData = CommonResponse.success(respDto);
        String expected = objectMapper.writeValueAsString(expectedData);
        Assertions.assertEquals(response, expected);
    }

    @Test
    @DisplayName("사진 없는 운동기록 상세조회 - 자신의 운동기록 상세조회")
    void findMemberExerciseRecord_NoPicture() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        Long recordId = 1L;
        LocalDateTime currentTime = LocalDateTime.now().minusHours(12);


        FindExerciseRecordResponseDto respDto = new FindExerciseRecordResponseDto();
        respDto.setExerciseTitle("운동제목");
        respDto.setExerciseDetail("운동내용");
        respDto.setExerciseLocation("운동위치");
        respDto.setExercisePersonalType("운동종류");
        respDto.setExerciseStartedAt(currentTime);
        respDto.setExerciseEndedAt(currentTime.plusHours(12));
        respDto.setPictureDataList(Optional.empty());


        given(exerciseQueryService.findExerciseRecordByRecordId(recordId)).willReturn(respDto);

        //when
        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises/{recordId}", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpectAll(status().isOk())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(commonResponseFields(
                                fieldWithPath("data.exerciseTitle").type(JsonFieldType.STRING).description("운동 제목"),
                                fieldWithPath("data.exerciseDetail").type(JsonFieldType.STRING).description("운동 내용"),
                                fieldWithPath("data.exercisePersonalType").type(JsonFieldType.STRING).description("운동 종류"),
                                fieldWithPath("data.exerciseStartedAt").type(JsonFieldType.STRING).description("운동 시작 시간"),
                                fieldWithPath("data.exerciseEndedAt").type(JsonFieldType.STRING).description("운동 종료 시간"),
                                fieldWithPath("data.exerciseLocation").type(JsonFieldType.STRING).description("운동 장소 제목"),
                                fieldWithPath("data.pictureDataList").type(JsonFieldType.NULL).description("운동 사진 관련 필드")
                        ))
                ))
                .andReturn().getResponse().getContentAsString();


        //then
        CommonResponse<FindExerciseRecordResponseDto> expectedData = CommonResponse.success(respDto);
        String expected = objectMapper.writeValueAsString(expectedData);
        Assertions.assertEquals(response, expected);
    }

    @Test
    @DisplayName("운동기록 상세조회 - 운동기록ID 음수 입력 테스트")
    void findMemberExerciseRecord_invalidRecordId() throws Exception {
        // given
        UUID memberId = UUID.randomUUID();
        Long recordId = -1L;

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises/{recordId}", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(commonResponseFields(
                                fieldWithPath("data['recordId']").type(JsonFieldType.STRING).description("올바른 Record를 다시 입력해주세요")
                        ))
                ));

        //then
        BDDMockito.then(exerciseQueryService).should(BDDMockito.never()).findExerciseRecordByRecordId(BDDMockito.anyLong());
    }


    @Test
    @DisplayName("운동기록 상세조회 - 존재하지 않는 기록 조회 시 예외 발생")
    void findMemberExerciseRecord_notFoundRecord() throws Exception {
        // given
        UUID memberId = UUID.randomUUID();
        Long nonExistentRecordId = 123456789L;

        given(exerciseQueryService.findExerciseRecordByRecordId(nonExistentRecordId))
                .willThrow(new CustomException(ErrorCode.EXERCISE_RECORD_NOT_FOUND));

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises/{recordId}", nonExistentRecordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isNotFound())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(RestDocsUtils.commonResponseFieldsOnly())
                ));

        //then
        BDDMockito.then(exerciseQueryService).should(BDDMockito.times(1)).findExerciseRecordByRecordId(BDDMockito.anyLong());
    }

    @Test
    @DisplayName("운동기록 상세조회 - 다른 회원 기록 조회 상황")
    void findMemberExerciseRecord_findAnotherUserRecord() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        Long anotherUserRecordID = 1L;

        given(exerciseQueryService.findExerciseRecordByRecordId(anotherUserRecordID))
                .willThrow(new CustomException(ErrorCode.AUTHORIZATION_DENIED));


        //when
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises/{recordId}", anotherUserRecordID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isForbidden())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(commonResponseFieldsOnly())
                ));

        //then
        BDDMockito.then(exerciseQueryService).should(BDDMockito.times(1)).findExerciseRecordByRecordId(anotherUserRecordID);
    }

    @Test
    @DisplayName("특정 날짜의 운동기록 조회_성공")
    void findMemberExerciseRecordByDate_Success() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        LocalDateTime testDateTime = LocalDateTime.of(2021, 1, 1, 00, 00, 00);
        List<String> picureUrl = List.of("https://s3.urlpage/test/pic1.jpg");
        Long exerciseId = 1L;

        List<FindExerciseRecordDateResponseDto> responseDtoList = new ArrayList<>();
        FindExerciseRecordDateResponseDto dto = new FindExerciseRecordDateResponseDto();
        dto.setExerciseId(exerciseId);
        dto.setExerciseTitle("운동제목");
        dto.setExercisePersonalType("운동기록");
        dto.setExerciseStartedAt(testDateTime);
        dto.setExerciseEndedAt(testDateTime.plusHours(1));
        dto.setPictureUrl(picureUrl);

        responseDtoList.add(dto);

        //given
        given(exerciseQueryService.findExerciseRecordByDate(testDateTime.toLocalDate())).willReturn(responseDtoList);

        //when
        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises")
                        .queryParam("date", testDateTime.toLocalDate().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        queryParameters(
                                parameterWithName("date")
                                        .attributes(getTypeFormat("Date"))
                                        .description("운동 기록을 조회할 날짜입니다. 형식은 ISO 8601 (YYYY-MM-DD)입니다.")
                        ),
                        responseFields(
                                commonResponseFieldsForList(
                                    fieldWithPath("data[].exerciseId").type(JsonFieldType.NUMBER)
                                            .description("운동 ID입니다."),
                                    fieldWithPath("data[].exerciseTitle").type(JsonFieldType.STRING)
                                            .description("운동 제목입니다."),
                                    fieldWithPath("data[].exercisePersonalType").type(JsonFieldType.STRING)
                                            .description("사용자가 입력한 운동 종류입니다."),
                                    fieldWithPath("data[].exerciseStartedAt").type(JsonFieldType.STRING)
                                            .description("운동 시작시간입니다."),
                                    fieldWithPath("data[].exerciseEndedAt").type(JsonFieldType.STRING)
                                            .description("운동 종료시간입니다."),
                                    fieldWithPath("data[].pictureUrl").type(JsonFieldType.ARRAY)
                                            .description("운동 사진 URL 목록입니다. 사진은 저장된 순서로 반환됩니다.")
                                )
                        )
                ))
                .andReturn().getResponse().getContentAsString();

        //then
        CommonResponse<List<FindExerciseRecordDateResponseDto>> expectedData = CommonResponse.success(responseDtoList);
        String expected = objectMapper.writeValueAsString(expectedData);
        Assertions.assertEquals(response, expected);
    }

    @Test
    @DisplayName("특정 날짜 운동기록 조회결과 없음")
    void findMemberExerciseRecordByDate_NotFound() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        LocalDateTime testDateTime = LocalDateTime.of(2021, 1, 1, 00, 00, 00);

        //given
        given(exerciseQueryService.findExerciseRecordByDate(testDateTime.toLocalDate())).willReturn(Collections.emptyList());

        //when
        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises")
                        .queryParam("date", testDateTime.toLocalDate().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(
                                commonResponseFieldsForList()
                        )
                ))
                .andReturn().getResponse().getContentAsString();

        //then
        CommonResponse<List<FindExerciseRecordDateResponseDto>> expectedData = CommonResponse.success(Collections.emptyList());
        String expected = objectMapper.writeValueAsString(expectedData);
        Assertions.assertEquals(response, expected);
    }

    @Test
    @DisplayName("날짜 입력 형식 오류")
    void findMemberRecordByDate_WrongInput() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        String date = "20111111";

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises")
                        .queryParam("date", date)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data.date").type(JsonFieldType.STRING).description("오류 메시지 출력")
                        )
                ));

        //then
        BDDMockito.then(exerciseQueryService).should(BDDMockito.never()).findExerciseRecordByDate(BDDMockito.any());
    }

    @Test
    @DisplayName("날짜 입력 누락")
    void findMemberRecordByDate_EmptyInput() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data.date").type(JsonFieldType.STRING).description("필수 파라미터 누락 메시지")
                        )
                ));

        //then
        BDDMockito.then(exerciseQueryService).should(BDDMockito.never()).findExerciseRecordByDate(BDDMockito.any());
    }

    @Test
    @DisplayName("과거 날짜 입력")
    void findMemberRecordByDate_PastInput() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        LocalDate date = LocalDate.of(1945, 8, 14);

        given(exerciseQueryService.findExerciseRecordByDate(date)).willThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises")
                        .queryParam("date", date.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("오류 메시지 출력")
                        )
                ));

        //then
        BDDMockito.then(exerciseQueryService).should(BDDMockito.times(1)).findExerciseRecordByDate(BDDMockito.any());
    }

    @Test
    @DisplayName("미래 날짜 입력")
    void findMemberRecordByDate_FutureInput() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        given(exerciseQueryService.findExerciseRecordByDate(date)).willThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises")
                        .queryParam("date", date.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("오류 메시지 출력")
                        )
                ));

        //then
        BDDMockito.then(exerciseQueryService).should(BDDMockito.times(1)).findExerciseRecordByDate(BDDMockito.any());
    }

    @Test
    @DisplayName("구간별 운동기록 조회_성공")
    void findExerciseRecordsByPeriod() throws Exception {
        UUID memberId = UUID.randomUUID();
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now();

        List<FindExerciseRecordByPeriodResponseDto> respDtoList = new ArrayList<>();
        respDtoList.add(new FindExerciseRecordByPeriodResponseDto(LocalDate.now().minusDays(1), 0L));
        respDtoList.add(new FindExerciseRecordByPeriodResponseDto(LocalDate.now(), 2L));

        // given
        given(exerciseQueryService.findExerciseRecordsByPeriod(start, end)).willReturn(respDtoList);

        // when
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises/count")
                        .queryParam("start", start.toString())
                        .queryParam("end", end.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        queryParameters(
                                parameterWithName("start").attributes(getTypeFormat(JsonFieldType.STRING))
                                        .description("운동 기록을 조회할 시작 날짜입니다. 형식은 ISO 8601 (YYYY-MM-DD)입니다. " +
                                                "시작 날짜는 오늘 이전이어야 합니다."),
                                parameterWithName("end").attributes(getTypeFormat(JsonFieldType.STRING))
                                        .description("운동 기록을 조회할 종료 날짜입니다. 형식은 ISO 8601 (YYYY-MM-DD)입니다.")
                        ),
                        responseFields(
                                commonResponseFieldsForList(
                                    fieldWithPath("data[].date").type(JsonFieldType.STRING)
                                            .description("운동한 날짜입니다."),
                                    fieldWithPath("data[].exerciseCount").type(JsonFieldType.NUMBER)
                                            .description("운동 기록 수 입니다.")
                                )
                        )
                ));

        //then
        BDDMockito.then(exerciseQueryService).should(BDDMockito.times(1)).findExerciseRecordsByPeriod(start, end);
    }

    @Test
    @DisplayName("구간별 운동종목 조회 실패 _ 날짜 입력 형식 오류")
    void findExerciseRecordsByPeriod_FailedInputType() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        String start = "20250520";
        LocalDate end = LocalDate.now();

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises/count")
                        .queryParam("start", start)
                        .queryParam("end", end.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("운동 기록 목록"),
                                fieldWithPath("data.start").type(JsonFieldType.STRING).description("기록 구간 입력 오류")
                        )
                ));

        BDDMockito.then(exerciseQueryService).should(BDDMockito.never()).findExerciseRecordsByPeriod(BDDMockito.any(), BDDMockito.any());
    }


    @Test
    @DisplayName("구간별 운동종목 조회 실패 _ 날짜 입력 누락")
    void findExerciseRecordsByPeriod_FailedInputLost() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        LocalDate start = LocalDate.now();

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises/count")
                        .queryParam("start", start.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data.end").type(JsonFieldType.STRING).description("필수 파라미터 누락 메시지")
                        )
                ));

        BDDMockito.then(exerciseQueryService).should(BDDMockito.never()).findExerciseRecordsByPeriod(BDDMockito.any(), BDDMockito.any());
    }


    @Test
    @DisplayName("구간별 운동종목 조회 실패 _ 과거 날짜 조회")
    void findExerciseRecordsByPeriod_FailedPastInput() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        LocalDate start = LocalDate.of(1945, 8, 14);
        LocalDate end = LocalDate.of(1945, 8, 15);

        //when
        given(exerciseQueryService.findExerciseRecordsByPeriod(start, end)).willThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises/count")
                        .queryParam("start", start.toString())
                        .queryParam("end", end.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("운동 기록 목록")
                        )
                ));

        BDDMockito.then(exerciseQueryService).should(BDDMockito.times(1)).findExerciseRecordsByPeriod(start, end);
    }

    @Test
    @DisplayName("구간별 운동종목 조회 실패 _ 미래 날짜 조회")
    void findExerciseRecordsByPeriod_FailedFutureInput() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(1);

        given(exerciseQueryService.findExerciseRecordsByPeriod(start, end)).willThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises/count")
                        .queryParam("start", start.toString())
                        .queryParam("end", end.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("운동 기록 목록")
                        )
                ));

        BDDMockito.then(exerciseQueryService).should(BDDMockito.times(1)).findExerciseRecordsByPeriod(start, end);
    }


    @Test
    @DisplayName("구간별 운동종목 조회 실패 _ 순서 입력 오류")
    void findExerciseRecordsByPeriod_FailedOrderInput() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(1);

        given(exerciseQueryService.findExerciseRecordsByPeriod(start, end)).willThrow(new CustomException(ErrorCode.IMPOSSIBLE_INPUT_DATE));

        //when
        this.mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercises/count")
                        .queryParam("start", start.toString())
                        .queryParam("end", end.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(memberId)))
                .andExpect(status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(HEADER_ACCESS_TOKEN),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("운동 기록 목록")
                        )
                ));

        BDDMockito.then(exerciseQueryService).should(BDDMockito.times(1)).findExerciseRecordsByPeriod(start, end);
    }
}