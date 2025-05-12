package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        SignUpRequestDto requestDto = new SignUpRequestDto();
        requestDto.setMemberNickname(TEST_NICKNAME);
        requestDto.setMemberGender(MemberGender.valueOf("M"));
        requestDto.setMemberBday(LocalDate.parse("2010-01-01"));
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
        assertThat(result.getMemberGender()).isEqualTo("남");
    }

}