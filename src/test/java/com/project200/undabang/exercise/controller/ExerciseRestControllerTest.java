package com.project200.undabang.exercise.controller;

import com.project200.undabang.common.AbstractRestDocSupport;
import com.project200.undabang.common.RestDocsUtils;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.dto.response.PictureDataResponse;
import com.project200.undabang.exercise.service.ExerciseRecordService;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

@WebMvcTest(ExerciseRestController.class)
class ExerciseRestControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private ExerciseRecordService exerciseRecordService;

    @Test
    @DisplayName("운동기록 상세조회 - 자신의 운동기록 상세조회")
    void findMemberExerciseRecord() throws Exception{
        //given
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


        BDDMockito.given(exerciseRecordService.findExerciseRecordByRecordId(recordId)).willReturn(respDto);

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises/{recordId}", recordId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers))
                .andExpectAll(MockMvcResultMatchers.status().isOk())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(RestDocsUtils.commonResponseFields(
                                fieldWithPath("data.exerciseTitle").type(JsonFieldType.STRING).description("운동 제목"),
                                fieldWithPath("data.exerciseDetail").type(JsonFieldType.STRING).description("운동 내용"),
                                fieldWithPath("data.exercisePersonalType").type(JsonFieldType.STRING).description("운동 종류"),
                                fieldWithPath("data.exerciseStartedAt").type(JsonFieldType.STRING).description("운동 시작 시간"),
                                fieldWithPath("data.exerciseEndedAt").type(JsonFieldType.STRING).description("운동 종료 시간"),
                                fieldWithPath("data.exerciseLocation").type(JsonFieldType.STRING).description("운동 장소 제목"),
                                fieldWithPath("data.pictureDataList").type(JsonFieldType.ARRAY).description("운동 사진 관련 필드"),
                                fieldWithPath("data.pictureDataList[].pictureId").type(JsonFieldType.NUMBER).description("사진 식별자 ID"),
                                fieldWithPath("data.pictureDataList[].pictureUrl").type(JsonFieldType.STRING).description("사진 URL"),
                                fieldWithPath("data.pictureDataList[].pictureName").type(JsonFieldType.STRING).description("사진 이름"),
                                fieldWithPath("data.pictureDataList[].pictureExtension").type(JsonFieldType.STRING).description("사진 확장자")
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
    void findMemberExerciseRecord_NoPicture() throws Exception{
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


        BDDMockito.given(exerciseRecordService.findExerciseRecordByRecordId(recordId)).willReturn(respDto);

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises/{recordId}", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpectAll(MockMvcResultMatchers.status().isOk())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(RestDocsUtils.commonResponseFields(
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
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises/{recordId}", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(RestDocsUtils.commonResponseFields(
                                fieldWithPath("data['findMemberExerciseRecord.recordId']").type(JsonFieldType.STRING).description("올바른 Record를 다시 입력해주세요")
                        ))
                ));

        //then
        BDDMockito.then(exerciseRecordService).should(BDDMockito.never()).findExerciseRecordByRecordId(BDDMockito.anyLong());
    }


    @Test
    @DisplayName("운동기록 상세조회 - 존재하지 않는 기록 조회 시 예외 발생")
    void findMemberExerciseRecord_notFoundRecord() throws Exception {
        // given
        UUID memberId = UUID.randomUUID();
        Long nonExistentRecordId = 123456789L;

        BDDMockito.given(exerciseRecordService.findExerciseRecordByRecordId(nonExistentRecordId))
                .willThrow(new CustomException(ErrorCode.EXERCISE_RECORD_NOT_FOUND));

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises/{recordId}", nonExistentRecordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                        .andExpect(MockMvcResultMatchers.status().isNotFound())
                        .andDo(this.document.document(
                                requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                                responseFields(RestDocsUtils.commonResponseFieldsOnly())
                        ));

        //then
        BDDMockito.then(exerciseRecordService).should(BDDMockito.times(1)).findExerciseRecordByRecordId(BDDMockito.anyLong());
    }

    @Test
    @DisplayName("운동기록 상세조회 - 다른 회원 기록 조회 상황")
    void findMemberExerciseRecord_findAnotherUserRecord() throws Exception {
        //given
        UUID memberId = UUID.randomUUID();
        Long anotherUserRecordID = 1L;

        BDDMockito.given(exerciseRecordService.findExerciseRecordByRecordId(anotherUserRecordID))
                .willThrow(new CustomException(ErrorCode.AUTHORIZATION_DENIED));


        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises/{recordId}", anotherUserRecordID)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(RestDocsUtils.commonResponseFieldsOnly())
                ));

        //then
        BDDMockito.then(exerciseRecordService).should(BDDMockito.times(1)).findExerciseRecordByRecordId(anotherUserRecordID);
    }

    @Test
    @DisplayName("특정 날짜의 운동기록 조회_성공")
    void findMemberExerciseRecordByDate_Success() throws Exception{
        //given
        UUID memberId = UUID.randomUUID();
        LocalDateTime testDateTime = LocalDateTime.of(2021,1,1, 00,00,00);
        String picureUrl = "https://s3.urlpage/test/pic1.jpg";
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
        BDDMockito.given(exerciseRecordService.findExerciseRecordByDate(testDateTime.toLocalDate())).willReturn(Optional.of(responseDtoList));

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises/dates")
                .param("date", testDateTime.toLocalDate().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("운동 기록 목록"),
                                fieldWithPath("data[].exerciseId").type(JsonFieldType.NUMBER).description("운동 ID"),
                                fieldWithPath("data[].exerciseTitle").type(JsonFieldType.STRING).description("운동 제목"),
                                fieldWithPath("data[].exercisePersonalType").type(JsonFieldType.STRING).description("운동 종류"),
                                fieldWithPath("data[].exerciseStartedAt").type(JsonFieldType.STRING).description("운동 시작시간"),
                                fieldWithPath("data[].exerciseEndedAt").type(JsonFieldType.STRING).description("운동 종료시간"),
                                fieldWithPath("data[].pictureUrl").type(JsonFieldType.STRING).description("운동 사진 URL")
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
    void findMemberExerciseRecordByDate_NotFound() throws Exception{
        //given
        UUID memberId = UUID.randomUUID();
        LocalDateTime testDateTime = LocalDateTime.of(2021,1,1, 00,00,00);

        //given
        BDDMockito.given(exerciseRecordService.findExerciseRecordByDate(testDateTime.toLocalDate())).willReturn(Optional.empty());

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises/dates")
                        .param("date", testDateTime.toLocalDate().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("운동 기록 목록")
                        )
                ))
                .andReturn().getResponse().getContentAsString();

        //then
        CommonResponse<List<FindExerciseRecordDateResponseDto>> expectedData = CommonResponse.success(null);
        String expected = objectMapper.writeValueAsString(expectedData);
        Assertions.assertEquals(response, expected);
    }

    @Test
    @DisplayName("날짜 입력 형식 오류")
    void findMemberRecordByDate_WrongInput() throws Exception{
        //given
        UUID memberId = UUID.randomUUID();
        String date = "20111111";

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises/dates")
                .param("date", date)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data.date").type(JsonFieldType.STRING).description("오류 메시지 출력")
                        )
                ));

        //then
        BDDMockito.then(exerciseRecordService).should(BDDMockito.never()).findExerciseRecordByDate(BDDMockito.any());
    }

    @Test
    @DisplayName("날짜 입력 누락")
    void findMemberRecordByDate_EmptyInput() throws Exception{
        //given
        UUID memberId = UUID.randomUUID();

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises/dates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(RestDocsUtils.commonResponseFieldsOnly())
                ));

        //then
        BDDMockito.then(exerciseRecordService).should(BDDMockito.never()).findExerciseRecordByDate(BDDMockito.any());
    }

    @Test
    @DisplayName("과거 날짜 입력")
    void findMemberRecordByDate_PastInput() throws Exception{
        //given
        UUID memberId = UUID.randomUUID();
        LocalDate date = LocalDate.of(1945,8,14);

        BDDMockito.given(exerciseRecordService.findExerciseRecordByDate(date)).willThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises/dates")
                        .param("date", date.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("오류 메시지 출력")
                        )
                ));

        //then
        BDDMockito.then(exerciseRecordService).should(BDDMockito.times(1)).findExerciseRecordByDate(BDDMockito.any());
    }

    @Test
    @DisplayName("미래 날짜 입력")
    void findMemberRecordByDate_FutureInput() throws Exception{
        //given
        UUID memberId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        BDDMockito.given(exerciseRecordService.findExerciseRecordByDate(date)).willThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises/dates")
                        .param("date", date.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("오류 메시지 출력")
                        )
                ));

        //then
        BDDMockito.then(exerciseRecordService).should(BDDMockito.times(1)).findExerciseRecordByDate(BDDMockito.any());
    }

    @Test
    @DisplayName("구간별 운동기록 조회_성공")
    void findExerciseRecordsByPeriod() throws Exception{
        UUID memberId = UUID.randomUUID();
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now();

        List<FindExerciseRecordByPeriodResponseDto> respDtoList = new ArrayList<>();
        respDtoList.add(new FindExerciseRecordByPeriodResponseDto(LocalDate.now().minusDays(1), 0L));
        respDtoList.add(new FindExerciseRecordByPeriodResponseDto(LocalDate.now(), 2L));

        // given
        BDDMockito.given(exerciseRecordService.findExerciseRecordsByPeriod(start, end)).willReturn(respDtoList);

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises")
                .param("start", start.toString())
                .param("end", end.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("운동 기록 목록"),
                                fieldWithPath("data[].date").type(JsonFieldType.STRING).description("운동 날짜"),
                                fieldWithPath("data[].exerciseCount").type(JsonFieldType.NUMBER).description("운동 횟수")
                        )
                ));

        //then
        BDDMockito.then(exerciseRecordService).should(BDDMockito.times(1)).findExerciseRecordsByPeriod(start, end);
    }

    @Test
    @DisplayName("구간별 운동종목 조회 실패 _ 날짜 입력 형식 오류")
    void findExerciseRecordsByPeriod_FailedInputType() throws  Exception{
        //given
        UUID memberId = UUID.randomUUID();
        String start = "20250520";
        LocalDate end = LocalDate.now();

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises")
                .param("start", start)
                .param("end", end.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("운동 기록 목록"),
                                fieldWithPath("data.start").type(JsonFieldType.STRING).description("기록 구간 입력 오류")
                        )
                ));

        BDDMockito.then(exerciseRecordService).should(BDDMockito.never()).findExerciseRecordsByPeriod(BDDMockito.any(), BDDMockito.any());
    }


    @Test
    @DisplayName("구간별 운동종목 조회 실패 _ 날짜 입력 누락")
    void findExerciseRecordsByPeriod_FailedInputLost() throws  Exception{
        //given
        UUID memberId = UUID.randomUUID();
        LocalDate start = LocalDate.now();

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises")
                        .param("start", start.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("운동 기록 목록")
                        )
                ));

        BDDMockito.then(exerciseRecordService).should(BDDMockito.never()).findExerciseRecordsByPeriod(BDDMockito.any(), BDDMockito.any());
    }


    @Test
    @DisplayName("구간별 운동종목 조회 실패 _ 과거 날짜 조회")
    void findExerciseRecordsByPeriod_FailedPastInput() throws  Exception{
        //given
        UUID memberId = UUID.randomUUID();
        LocalDate start = LocalDate.of(1945,8,14);
        LocalDate end = LocalDate.of(1945,8,15);

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        BDDMockito.given(exerciseRecordService.findExerciseRecordsByPeriod(start,end)).willThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises")
                        .param("start", start.toString())
                        .param("end", end.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("운동 기록 목록")
                        )
                ));

        BDDMockito.then(exerciseRecordService).should(BDDMockito.times(1)).findExerciseRecordsByPeriod(start,end);
    }


    @Test
    @DisplayName("구간별 운동종목 조회 실패 _ 미래 날짜 조회")
    void findExerciseRecordsByPeriod_FailedFutureInput() throws  Exception{
        //given
        UUID memberId = UUID.randomUUID();
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(1);

        BDDMockito.given(exerciseRecordService.findExerciseRecordsByPeriod(start,end)).willThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises")
                        .param("start", start.toString())
                        .param("end", end.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("운동 기록 목록")
                        )
                ));

        BDDMockito.then(exerciseRecordService).should(BDDMockito.times(1)).findExerciseRecordsByPeriod(start, end);
    }


    @Test
    @DisplayName("구간별 운동종목 조회 실패 _ 순서 입력 오류")
    void findExerciseRecordsByPeriod_FailedOrderInput() throws  Exception{
        //given
        UUID memberId = UUID.randomUUID();
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(1);

        BDDMockito.given(exerciseRecordService.findExerciseRecordsByPeriod(start,end)).willThrow(new CustomException(ErrorCode.IMPOSSIBLE_INPUT_DATE));

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exercises")
                        .param("start", start.toString())
                        .param("end", end.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_X_USER_ID),
                        responseFields(
                                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("응답 상태"),
                                fieldWithPath("code").type(JsonFieldType.STRING).description("상태 코드"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("운동 기록 목록")
                        )
                ));

        BDDMockito.then(exerciseRecordService).should(BDDMockito.times(1)).findExerciseRecordsByPeriod(start, end);
    }


}