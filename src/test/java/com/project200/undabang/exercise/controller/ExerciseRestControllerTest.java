package com.project200.undabang.exercise.controller;

import com.project200.undabang.common.AbstractRestDocSupport;
import com.project200.undabang.common.RestDocsUtils;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
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

import java.time.LocalDateTime;
import java.util.*;

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
        List<String> urlList = new ArrayList<>();
        urlList.add("https://s3/picture/pic1.jpg");
        urlList.add("https://s3/picture/pic2.jpg");
        urlList.add("https://s3/picture/pic3.jpg");

        FindExerciseRecordResponseDto respDto = new FindExerciseRecordResponseDto();
        respDto.setExerciseTitle("운동제목");
        respDto.setExerciseDetail("운동내용");
        respDto.setExerciseLocation("운동위치");
        respDto.setExercisePersonalType("운동종류");
        respDto.setExerciseStartedAt(currentTime);
        respDto.setExerciseEndedAt(currentTime.plusHours(12));
        respDto.setExercisePictureUrls(Optional.of(urlList));

        BDDMockito.given(exerciseRecordService.findExerciseRecordByRecordId(recordId)).willReturn(respDto);

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());

        String response = this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exerciseRecords/{recordId}", recordId)
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
                                fieldWithPath("data.exercisePictureUrls").type(JsonFieldType.ARRAY).description("운동 사진 URL")
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

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exerciseRecords/{recordId}", recordId)
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

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exerciseRecords/{recordId}", nonExistentRecordId)
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

        this.mockMvc.perform(MockMvcRequestBuilders.get("/v1/exerciseRecords/{recordId}", anotherUserRecordID)
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
}