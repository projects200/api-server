package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.response.GetProfilePictureResponse;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.repository.MemberPictureRepository;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberPictureQueryServiceImplTest {

    @Mock
    private MemberPictureRepository memberPictureRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberPictureQueryServiceImpl memberPictureQueryService;

    private Picture createPicture(Long id, String url) {
        return Picture.builder()
                .id(id)
                .pictureUrl(url)
                .pictureName("test_pic_" + id)
                .pictureExtension(".jpg")
                .build();
    }

    private MemberPicture createMemberPicture(Member member, Picture picture) {
        return MemberPicture.builder()
                .member(member)
                .picture(picture)
                .build();
    }

    @Nested
    @DisplayName("getProfilePictures() 메소드는")
    class Describe_getProfilePictures {

        @Test
        @DisplayName("대표 사진을 포함한 여러 프로필 사진이 있을 경우, 이를 올바르게 조합하여 반환한다")
        void it_returns_profile_pictures_when_representative_exists() { // ★ 메서드명 변경: 의도 명확화
            // given
            UUID testUserId = UUID.randomUUID();

            Member testUser = Member.builder().memberId(testUserId).build();

            Picture representativePic = createPicture(1L, "rep_url.jpg");
            Picture otherPic = createPicture(2L, "other_url.jpg");

            MemberPicture repMp = createMemberPicture(testUser, representativePic);
            MemberPicture otherMp = createMemberPicture(testUser, otherPic);

            Member testUserWithRepPic = Member.builder()
                    .memberId(testUserId)
                    .memberPicture(repMp)
                    .build();

            List<MemberPicture> allMemberPictures = List.of(repMp, otherMp);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(testUserWithRepPic));
                given(memberPictureRepository.findByMemberAndPicture_PictureDeletedAtNull(testUserWithRepPic)).willReturn(allMemberPictures);

                // when
                GetProfilePictureResponse response = memberPictureQueryService.getProfilePictures();

                // then
                assertThat(response).isNotNull();
                assertThat(response.getProfileImageCount()).isEqualTo(2);
                assertThat(response.getProfileImages()).hasSize(2);

                assertThat(response.getRepresentativeProfileImage()).isNotNull();
                assertThat(response.getRepresentativeProfileImage().profileImageId()).isEqualTo(representativePic.getId());
                assertThat(response.getRepresentativeProfileImage().profileImageUrl()).isEqualTo(representativePic.getPictureUrl());
            }
        }

        @Test
        @DisplayName("대표 사진이 설정됐지만 전체 목록에 포함되지 않을 경우, 대표 사진을 null로 반환한다")
        void it_returns_null_representative_when_it_is_not_in_the_main_list() {
            // given
            UUID testUserId = UUID.randomUUID();
            Member testUser = Member.builder().memberId(testUserId).build();

            Picture representativePic = createPicture(1L, "deleted_rep_url.jpg");
            Picture activePic = createPicture(2L, "active_pic.jpg");

            MemberPicture repMp = createMemberPicture(testUser, representativePic);
            MemberPicture activeMp = createMemberPicture(testUser, activePic);

            Member testUserWithRepPic = Member.builder()
                    .memberId(testUserId)
                    .memberPicture(repMp)
                    .build();

            List<MemberPicture> activePicturesOnly = List.of(activeMp);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(testUserWithRepPic));
                given(memberPictureRepository.findByMemberAndPicture_PictureDeletedAtNull(testUserWithRepPic)).willReturn(activePicturesOnly);

                // when
                GetProfilePictureResponse response = memberPictureQueryService.getProfilePictures();

                // then
                assertThat(response.getRepresentativeProfileImage()).isNull();
                assertThat(response.getProfileImages()).hasSize(1);
            }
        }

        @Test
        @DisplayName("항상 자신의 프로필 사진 정보만 조회한다")
        void it_always_retrieves_only_own_profile_pictures() {
            // given
            UUID loggedInUserId = UUID.randomUUID(); // 현재 로그인한 사용자
            UUID anotherUserId = UUID.randomUUID(); // 조회하려는 다른 사용자 (실제로는 이 파라미터가 API에 없음)

            Member loggedInUser = Member.builder().memberId(loggedInUserId).build();


            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                // UserContextHolder는 항상 로그인한 사용자의 ID를 반환하도록 설정
                given(UserContextHolder.getUserId()).willReturn(loggedInUserId);
                given(memberRepository.findById(loggedInUserId)).willReturn(Optional.of(loggedInUser));
                given(memberPictureRepository.findByMemberAndPicture_PictureDeletedAtNull(loggedInUser)).willReturn(Collections.emptyList());

                // when
                memberPictureQueryService.getProfilePictures();

                // then
                then(memberRepository).should(times(1)).findById(loggedInUserId);
                then(memberRepository).should(never()).findById(anotherUserId);
            }
        }

        @Test
        @DisplayName("프로필 사진이 없는 사용자로 호출 시, 빈 목록과 null 대표 사진을 반환한다")
        void it_returns_empty_response_for_user_with_no_pictures() {
            // given
            UUID testUserId = UUID.randomUUID();
            // 대표 사진(memberPicture)이 null인 사용자
            Member testUser = Member.builder().memberId(testUserId).build();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(testUser));
                // 사진 목록 조회 시 빈 리스트 반환
                given(memberPictureRepository.findByMemberAndPicture_PictureDeletedAtNull(testUser)).willReturn(Collections.emptyList());

                // when
                GetProfilePictureResponse response = memberPictureQueryService.getProfilePictures();

                // then
                assertThat(response).isNotNull();
                assertThat(response.getRepresentativeProfileImage()).isNull();
                assertThat(response.getProfileImageCount()).isEqualTo(0);
                assertThat(response.getProfileImages()).isEmpty();
            }
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 호출 시 CustomException(MEMBER_NOT_FOUND) 예외를 발생시킨다")
        void it_throws_exception_when_member_not_found() {
            // given
            UUID testUserId = UUID.randomUUID();
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);
                // 존재하지 않는 회원이므로 Optional.empty() 반환
                given(memberRepository.findById(testUserId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberPictureQueryService.getProfilePictures())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }
}