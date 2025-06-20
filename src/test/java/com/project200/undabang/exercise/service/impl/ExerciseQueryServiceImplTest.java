package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class ExerciseQueryServiceImplTest {
    @InjectMocks
    private ExerciseQueryServiceImpl service;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private MemberRepository memberRepository;


    @Test
    @DisplayName("회원의 UUID가 DB에 없는 경우 에러 반환")
    void checkMemberId() {
        UUID testUserId = UUID.randomUUID();

        // given
        given(memberRepository.existsByMemberId(testUserId)).willReturn(false);

        //when
        boolean result = service.checkMemberId(testUserId);

        //then
        assertFalse(result);
        then(memberRepository).should().existsByMemberId(testUserId);
    }

    @Test
    @DisplayName("회원의 운동기록이 DB에 없는 경우 에러 반환")
    void checkExerciseRecordId() {
        Long testRecordId = 1L;

        //given
        given(exerciseRepository.existsById(testRecordId)).willReturn(false);

        //when
        boolean result = service.checkExerciseRecordId(testRecordId);

        //then
        assertFalse(result);
        then(exerciseRepository).should().existsById(testRecordId);
    }

    @Test
    @DisplayName("회원이 자신의 운동 기록을 조회하는지 확인")
    void validateMemberRecordAccess_correct() {
        UUID testUserId = UUID.randomUUID();
        Long testRecord = 1L;

        //given
        given(exerciseRepository.existsByRecordIdAndMemberId(testUserId, testRecord)).willReturn(true);

        //when
        boolean result = service.validateMemberRecordAccess(testUserId, testRecord);

        //then
        assertTrue(result);
        then(exerciseRepository).should().existsByRecordIdAndMemberId(testUserId, testRecord);
    }

    @Test
    @DisplayName("회원이 타인의 운동기록을 조회하는 경우")
    void validateMemberRecordAccess_wrong() {
        UUID testUserId = UUID.randomUUID();
        Long testAnotherUserRecord = 1L;

        //given
        given(exerciseRepository.existsByRecordIdAndMemberId(testUserId, testAnotherUserRecord)).willReturn(false);

        //when
        boolean result = service.validateMemberRecordAccess(testUserId,testAnotherUserRecord);

        //then
        assertFalse(result);
        then(exerciseRepository).should().existsByRecordIdAndMemberId(testUserId,testAnotherUserRecord);
    }

    @Test
    @DisplayName("운동기록 조회_성공")
    void findExerciseRecordByRecordId() {
        UUID testUserId = UUID.randomUUID();
        Long testRecordId = 1L;
        FindExerciseRecordResponseDto mockDto = new FindExerciseRecordResponseDto();

        //given
        try(MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)){
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(testUserId);

            given(memberRepository.existsByMemberId(testUserId)).willReturn(true);
            given(exerciseRepository.existsById(testRecordId)).willReturn(true);

            given(exerciseRepository.existsByRecordIdAndMemberId(testUserId,testRecordId)).willReturn(true);
            given(exerciseRepository.findExerciseByExerciseId(testUserId,testRecordId)).willReturn(mockDto);

            //when
            FindExerciseRecordResponseDto result = service.findExerciseRecordByRecordId(testRecordId);

            //then
            assertNotNull(result);
            assertSame(mockDto,result);
            then(exerciseRepository).should().existsByRecordIdAndMemberId(testUserId,testRecordId);
            then(exerciseRepository).should().findExerciseByExerciseId(testUserId,testRecordId);
        }
    }

    @Test
    @DisplayName("운동기록 조회_실패_타인의 운동기록 접근")
    void findExerciseRecordByRecordId_Fail(){
        UUID testUserId = UUID.randomUUID();
        Long testRecord = 1L;

        try(MockedStatic<UserContextHolder> mockedStstic = mockStatic(UserContextHolder.class)){
            mockedStstic.when(UserContextHolder::getUserId).thenReturn(testUserId);

            given(exerciseRepository.existsById(testRecord)).willReturn(true);
            given(memberRepository.existsByMemberId(testUserId)).willReturn(true);
            given(exerciseRepository.existsByRecordIdAndMemberId(testUserId,testRecord)).willReturn(false);

            CustomException exception = assertThrows(CustomException.class,
                    () -> service.findExerciseRecordByRecordId(testRecord));
            assertEquals(ErrorCode.AUTHORIZATION_DENIED, exception.getErrorCode());
        }
    }

    @Test
    @DisplayName("특정 날짜의 운동기록 조회_조회성공")
    void findExerciseRecordByDate_Success(){
        UUID testUserId = UUID.randomUUID();
        LocalDate testDate = LocalDate.of(2020,1,1);
        List<FindExerciseRecordDateResponseDto> mockedListDto = new ArrayList<>();
        mockedListDto.add(new FindExerciseRecordDateResponseDto());

        try(MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)){
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(testUserId);

            given(exerciseRepository.findExerciseRecordByDate(testUserId, testDate)).willReturn(mockedListDto);

            //when
            List<FindExerciseRecordDateResponseDto> result = service.findExerciseRecordByDate(testDate);

            //then
            assertNotNull(result);
            assertTrue(!result.isEmpty());
            assertEquals(1, result.size());
            then(exerciseRepository).should().findExerciseRecordByDate(testUserId, testDate);
        }
    }

    @Test
    @DisplayName("과거 날짜의 운동기록 조회_실패")
    void findExerciseRecordByDate_PrevFailed(){
        UUID testUserId = UUID.randomUUID();
        LocalDate testDate = LocalDate.of(1900,1,1);
        List<FindExerciseRecordDateResponseDto> mockedListDto = new ArrayList<>();
        mockedListDto.add(new FindExerciseRecordDateResponseDto());

        try(MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)){
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(testUserId);

            CustomException exception = assertThrows(CustomException.class,
                    () -> service.findExerciseRecordByDate(testDate));

            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        }
    }

    @Test
    @DisplayName("미래 날짜의 운동기록 조회_실패")
    void findExerciseRecordByDate_FutureFailed(){
        UUID testUserId = UUID.randomUUID();
        LocalDate testDate = LocalDate.now().plusDays(1);
        List<FindExerciseRecordDateResponseDto> mockedListDto = new ArrayList<>();
        mockedListDto.add(new FindExerciseRecordDateResponseDto());

        try(MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)){
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(testUserId);

            CustomException exception = assertThrows(CustomException.class,
                    () -> service.findExerciseRecordByDate(testDate));
            assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        }
    }

    @Test
    @DisplayName("특정 기간의 운동기록목록조회 _ 성공")
    void findExerciseRecordsByPeriods(){
        UUID memberId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2025,5,1);
        LocalDate endDate = LocalDate.of(2025,5,20);
        List<FindExerciseRecordByPeriodResponseDto> mockedDto = new ArrayList();

        for (int i = 0; i <= (int) ChronoUnit.DAYS.between(startDate,endDate); i++) {
            mockedDto.add(new FindExerciseRecordByPeriodResponseDto());
        }

        given(exerciseRepository.findExercisesByPeriod(memberId, startDate, endDate)).willReturn(mockedDto);

        try(MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)){
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(memberId);

            List<FindExerciseRecordByPeriodResponseDto> dtoList = service.findExerciseRecordsByPeriod(startDate, endDate);

            long size = ChronoUnit.DAYS.between(startDate, endDate)+1;
            assertEquals((int)size, dtoList.size());
            assertNotNull(dtoList);
            then(exerciseRepository).should().findExercisesByPeriod(memberId,startDate,endDate);
        }
    }

    @Test
    @DisplayName("특정기간의 운동기록목록조회 _ 실패_ 과거 기간 조회")
    void findExerciseRecordsByPeriods_Failed_PastPeriod(){
        UUID memberId = UUID.randomUUID();
        LocalDate endDate = LocalDate.of(1945, 8,15);
        LocalDate startDate = endDate.minusDays(1);

        try(MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)){
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(memberId);

            CustomException exception = assertThrows(CustomException.class,
                    () -> service.findExerciseRecordsByPeriod(startDate,endDate));

            assertEquals(exception.getErrorCode(), ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Test
    @DisplayName("특정기간의 운동기록목록조회 _ 실패_미래기간 조회")
    void findExerciseRecordsByPeriods_Failed_futurePeriod(){
        UUID memberId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2025,5,1);
        LocalDate endDate = LocalDate.now().plusDays(1);

        try(MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)){
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(memberId);

            CustomException exception = assertThrows(CustomException.class,
                    () -> service.findExerciseRecordsByPeriod(startDate,endDate));

            assertEquals(exception.getErrorCode(), ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Test
    @DisplayName("특정기간의 운동기록목록조회 _ 실패_기간 입력 실수(end < start)")
    void findExerciseRecordsByPeriods_Failed_EndComesFasterThenStart(){
        UUID memberId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2025,5,1);
        LocalDate endDate = startDate.minusDays(1);

        try(MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)){
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(memberId);

            CustomException exception = assertThrows(CustomException.class,
                    () -> service.findExerciseRecordsByPeriod(startDate,endDate));

            assertEquals(exception.getErrorCode(), ErrorCode.IMPOSSIBLE_INPUT_DATE);
        }
    }
}