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
import static org.mockito.Mockito.mockStatic;

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
        @DisplayName("유효한 사용자로 호출 시, 회원의 프로필 사진 정보를 조합하여 반환한다")
        void it_returns_profile_pictures_for_a_valid_user() {
            // given
            UUID testUserId = UUID.randomUUID();

            // 테스트용 사진 데이터 생성
            Picture representativePic = createPicture(1L, "rep_url.jpg");
            Picture otherPic1 = createPicture(2L, "other1_url.jpg");
            Picture otherPic2 = createPicture(3L, "other2_url.jpg");

            // 테스트용 MemberPicture 데이터 생성 (대표 사진 포함)
            MemberPicture repMp = createMemberPicture(null, representativePic);
            MemberPicture otherMp1 = createMemberPicture(null, otherPic1);
            MemberPicture otherMp2 = createMemberPicture(null, otherPic2);

            // testUser에 대표 사진 설정
            Member testUser = Member.builder()
                    .memberId(testUserId)
                    .memberPicture(repMp)
                    .build();

            // 이전에 생성한 MemberPicture 객체들에 완성된 Member 객체를 설정
            List<MemberPicture> allMemberPictures = List.of(
                    createMemberPicture(testUser, representativePic),
                    createMemberPicture(testUser, otherPic1),
                    createMemberPicture(testUser, otherPic2)
            );
            // try-with-resources 구문을 사용하여 static mock 관리
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {

                // Mockito BDD 스타일 given() 사용
                given(UserContextHolder.getUserId()).willReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(testUser));
                given(memberPictureRepository.findByMemberAndPicture_PictureDeletedAtNull(testUser)).willReturn(allMemberPictures);

                // when
                GetProfilePictureResponse response = memberPictureQueryService.getProfilePictures();

                // then
                assertThat(response).isNotNull();
                assertThat(response.getProfileImageCount()).isEqualTo(3);
                assertThat(response.getProfileImages()).hasSize(3);

                // 대표 사진 검증
                assertThat(response.getRepresentativeProfileImage()).isNotNull();
                assertThat(response.getRepresentativeProfileImage().profileImageId()).isEqualTo(representativePic.getId());
                assertThat(response.getRepresentativeProfileImage().profileImageUrl()).isEqualTo(representativePic.getPictureUrl());
            }
        }

        @Test
        @DisplayName("프로필 사진이 없는 사용자로 호출 시, 빈 목록과 null 대표 사진을 반환한다")
        void it_returns_empty_response_for_user_with_no_pictures() {
            // given
            UUID testUserId = UUID.randomUUID();
            // 대표 사진(memberPicture)이 null인 사용자
            Member testUser = Member.builder().memberId(testUserId).build();

            try (MockedStatic<UserContextHolder> mockedUserContextHolder = mockStatic(UserContextHolder.class)) {
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
            try (MockedStatic<UserContextHolder> mockedUserContextHolder = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);
                // 존재하지 않는 회원이므로 Optional.empty() 반환
                given(memberRepository.findById(testUserId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberPictureQueryService.getProfilePictures())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("대표 사진이 설정됐지만 전체 목록에 포함되지 않을 경우, 대표 사진을 null로 반환한다")
        void it_returns_null_representative_when_it_is_not_in_the_main_list() {
            // given: 이 테스트는 `getRepresentativeProfileImage`의 마지막 'return null' 분기를 검증합니다.
            UUID testUserId = UUID.randomUUID();
            Member testUser = Member.builder().memberId(testUserId).build();

            // 대표 사진으로 설정될 사진 (e.g. 삭제됨)
            Picture representativePic = createPicture(1L, "deleted_rep_url.jpg");
            // 현재 활성화된 사진
            Picture activePic = createPicture(2L, "active_pic.jpg");

            MemberPicture repMp = createMemberPicture(testUser, representativePic);
            MemberPicture activeMp = createMemberPicture(testUser, activePic);

            // Member에는 대표 사진이 설정되어 있음
            Member testUserWithRepPic = Member.builder()
                    .memberId(testUserId)
                    .memberPicture(repMp)
                    .build();

            // 하지만 Repository는 활성화된 사진 목록만 반환 (대표 사진은 필터링됨)
            List<MemberPicture> activePicturesOnly = List.of(activeMp);

            try (MockedStatic<UserContextHolder> mockedUserContextHolder = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(testUserWithRepPic));
                given(memberPictureRepository.findByMemberAndPicture_PictureDeletedAtNull(testUserWithRepPic)).willReturn(activePicturesOnly);

                // when
                GetProfilePictureResponse response = memberPictureQueryService.getProfilePictures();

                // then
                // 헬퍼 메서드의 for-loop가 끝까지 돌고 일치하는 것을 찾지 못해 null을 반환해야 합니다.
                assertThat(response.getRepresentativeProfileImage()).isNull();
                assertThat(response.getProfileImages()).hasSize(1);
            }
        }
    }
}
