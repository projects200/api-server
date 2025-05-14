package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {
    @InjectMocks
    private MemberServiceImpl memberService;
    @Mock
    private MemberRepository memberRepository;

    private MockedStatic<UserContextHolder> userContextHolderMock;
    private final UUID TEST_UUID = UUID.randomUUID();
    private final String TEST_EMAIL = "test@email.com";
    private final String TEST_NICKNAME = "테스트닉네임";

    @BeforeEach
    void setUp(){
        userContextHolderMock = Mockito.mockStatic(UserContextHolder.class);
        userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(TEST_UUID);
        userContextHolderMock.when(UserContextHolder::getUserEmail).thenReturn(TEST_EMAIL);
    }

    @AfterEach
    void tearDown(){
        userContextHolderMock.close();
    }

    @Test
    @DisplayName("멤버 ID 중복 체크 - 중복인 경우")
    public void duplicated_checkMemberId() {
        // given
        UUID testUuid = UUID.randomUUID();
        Mockito.when(memberRepository.existsByMemberId(testUuid)).thenReturn(true);

        // when
        boolean result = memberService.checkMemberId(testUuid);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("멤버 ID 중복 체크 - 중복이 아닌 경우")
    public void not_Duplicated_checkMemberId() {
        // given
        UUID testUuid = UUID.randomUUID();
        Mockito.when(memberRepository.existsByMemberId(testUuid)).thenReturn(false);

        // when
        boolean result = memberService.checkMemberId(testUuid);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("이메일 중복 체크 - 중복인 경우")
    public void duplicated_checkMemberEmail() {
        //given
        Mockito.when(memberRepository.existsByMemberEmail(TEST_EMAIL)).thenReturn(true);
        //when
        boolean result = memberService.checkMemberEmail(TEST_EMAIL);
        //then
        assertTrue(result);
    }

    @Test
    @DisplayName("이메일 중복 체크 - 중복이 아닌 경우")
    public void not_Duplicated_checkMemberEmail() {
        //given
        Mockito.when(memberRepository.existsByMemberEmail(TEST_EMAIL)).thenReturn(false);
        //when
        boolean result = memberService.checkMemberEmail(TEST_EMAIL);
        //then
        assertFalse(result);
    }

    @Test
    @DisplayName("닉네임 중복 체크 - 중복인 경우")
    public void duplicated_checkMemberNickname() {
        Mockito.when(memberRepository.existsByMemberNickname(TEST_NICKNAME)).thenReturn(true);
        boolean result = memberService.checkMemberNickname(TEST_NICKNAME);
        assertTrue(result);
    }
    @Test
    @DisplayName("닉네임 중복 체크 - 중복이 아닌 경우")
    public void not_Duplicated_checkMemberNickname() {
        Mockito.when(memberRepository.existsByMemberNickname(TEST_NICKNAME)).thenReturn(false);
        boolean result = memberService.checkMemberNickname(TEST_NICKNAME);
        assertFalse(result);
    }

    @Test
    @DisplayName("회원 가입 성공 테스트")
    public void success_memberSignUpTest(){
        // given
        SignUpRequestDto requestDto = SignUpRequestDto.builder()
                .memberNickname(TEST_NICKNAME)
                .memberGender(MemberGender.M)
                .memberBday(LocalDate.parse("2010-01-01"))
                .build();

        Mockito.when(memberRepository.existsByMemberEmail(TEST_EMAIL)).thenReturn(false);
        Mockito.when(memberRepository.existsByMemberNickname(TEST_NICKNAME)).thenReturn(false);

        Member member = Member.builder()
                .memberId(TEST_UUID)
                .memberEmail(TEST_EMAIL)
                .memberNickname(TEST_NICKNAME)
                .memberGender(MemberGender.M)
                .memberScore((byte) 35)
                .memberBday(LocalDate.parse("2010-01-01"))
                .build();

        Mockito.when(memberRepository.save(Mockito.any(Member.class))).thenReturn(member);

        //when
        SignUpResponseDto result = memberService.memberSignUp(requestDto);

        //then
        // 특정 조건을 검증하기 위해 사용되며, 예상되는 결과와 실제 결과를 비교하여 테스트 수행
        // 단위테스트 / 통합 테스트에서 사용됨
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(TEST_UUID);
        assertThat(result.getMemberEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.getMemberNickname()).isEqualTo(TEST_NICKNAME);
        assertThat(result.getMemberGender()).isEqualTo('m');
    }

    @Test
    @DisplayName("회원 가입 실패 - 이미 존재하는 멤버 ID")
    public void fail_memberSignUp_duplicateMemberId() {
        // given
        SignUpRequestDto requestDto = SignUpRequestDto.builder()
                .memberNickname(TEST_NICKNAME)
                .memberGender(MemberGender.M)
                .memberBday(LocalDate.parse("2010-01-01"))
                .build();

        Mockito.when(memberRepository.existsByMemberId(TEST_UUID)).thenReturn(true);

        // when & then
        assertThrows(CustomException.class, () -> memberService.memberSignUp(requestDto));
    }

    @Test
    @DisplayName("회원 가입 실패 - 이미 존재하는 이메일")
    public void fail_memberSignUp_duplicateEmail() {
        // given
        SignUpRequestDto requestDto = SignUpRequestDto.builder()
                .memberNickname(TEST_NICKNAME)
                .memberGender(MemberGender.M)
                .memberBday(LocalDate.parse("2010-01-01"))
                .build();

        // when
        Mockito.when(memberRepository.existsByMemberEmail(TEST_EMAIL)).thenReturn(true);

        // then
        assertThrows(CustomException.class, () -> memberService.memberSignUp(requestDto));
    }

    @Test
    @DisplayName("회원 가입 실패 - 이미 존재하는 닉네임")
    public void fail_memberSignUp_duplicateNickname() {
        // given
        SignUpRequestDto requestDto = SignUpRequestDto.builder()
                .memberNickname(TEST_NICKNAME)
                .memberGender(MemberGender.M)
                .memberBday(LocalDate.parse("2010-01-01"))
                .build();

        //when
        Mockito.when(memberRepository.existsByMemberNickname(TEST_NICKNAME)).thenReturn(true);

        // then
        assertThrows(CustomException.class, () -> memberService.memberSignUp(requestDto));
    }

    @Test
    @DisplayName("회원 가입 실패 - 잘못된 성별 정보")
    public void fail_memberSignUp_invalidGender() {
        // given
        SignUpRequestDto requestDto = SignUpRequestDto.builder()
                .memberNickname(TEST_NICKNAME)
                .memberGender(null)
                .memberBday(LocalDate.parse("2010-01-01"))
                .build();

        Mockito.when(memberRepository.existsByMemberId(TEST_UUID)).thenReturn(false);
        Mockito.when(memberRepository.existsByMemberEmail(TEST_EMAIL)).thenReturn(false);
        Mockito.when(memberRepository.existsByMemberNickname(TEST_NICKNAME)).thenReturn(false);

        // when & then
        assertThrows(CustomException.class, () -> memberService.memberSignUp(requestDto));
    }

    @Test
    @DisplayName("생일 체크 - 미래 날짜인 경우 (유효하지 않음)")
    public void invalid_checkMemberBday() {
        // given
        LocalDate futureBday = LocalDate.now().plusDays(1);

        // when
        boolean result = memberService.checkMemberBday(futureBday);

        // then
        assertTrue(result); // 미래 날짜이면 true 반환 (유효하지 않은 생일)
    }

    @Test
    @DisplayName("생일 체크 - 과거 날짜인 경우 (유효함)")
    public void valid_checkMemberBday() {
        // given
        LocalDate pastBday = LocalDate.now().minusDays(1);

        // when
        boolean result = memberService.checkMemberBday(pastBday);

        // then
        assertFalse(result); // 과거 날짜이면 false 반환 (유효한 생일)
    }

    @Test
    @DisplayName("회원 가입 실패 - 미래 날짜의 생일")
    public void fail_memberSignUp_invalidBirthday() {
        // given
        LocalDate futureBday = LocalDate.now().plusDays(1);
        SignUpRequestDto requestDto = SignUpRequestDto.builder()
                .memberNickname(TEST_NICKNAME)
                .memberGender(MemberGender.M)
                .memberBday(futureBday)
                .build();

        // 다른 검증은 통과하도록 설정
        Mockito.when(memberRepository.existsByMemberId(TEST_UUID)).thenReturn(false);
        Mockito.when(memberRepository.existsByMemberEmail(TEST_EMAIL)).thenReturn(false);
        Mockito.when(memberRepository.existsByMemberNickname(TEST_NICKNAME)).thenReturn(false);

        // when & then
        assertThrows(CustomException.class, () -> memberService.memberSignUp(requestDto));
    }
}