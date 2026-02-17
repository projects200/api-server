package com.project200.undabang.feed.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.service.FileType;
import com.project200.undabang.common.service.PictureService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.dto.response.CreateFeedPictureResponse;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedPicture;
import com.project200.undabang.feed.repository.FeedPictureRepository;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedPictureServiceImplTest {

    @InjectMocks
    private FeedPictureServiceImpl feedPictureService;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PictureService pictureService;

    @Mock
    private FeedPictureRepository feedPictureRepository;

    @Mock
    private PolicyService policyService;

    @Nested
    @DisplayName("Describe: createFeedPictures 메소드는")
    class Describe_CreateFeedPictures {

        @Test
        @DisplayName("유효한 사진 리스트와 정책 범위 내의 개수가 주어지면 사진을 업로드하고 저장한다")
        void it_uploads_and_saves_pictures_successfully() {
            // given
            UUID memberId = UUID.randomUUID();
            Long feedId = 1L;
            List<MultipartFile> files = List.of(mock(MultipartFile.class));

            Member member = mock(Member.class);
            Feed feed = mock(Feed.class);
            Picture picture = mock(Picture.class);
            FeedPicture feedPicture = FeedPicture.of(picture, feed);

            try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
                userContext.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(memberId)).willReturn(Optional.of(member));
                given(feedRepository.findByIdAndMemberAndDeletedAtNull(feedId, member)).willReturn(Optional.of(feed));

                // 정책 및 기존 개수 설정 (최대 5개, 기존 0개)
                given(feedPictureRepository.countByFeedAndPicture_PictureDeletedAtNull(feed)).willReturn(0L);
                given(policyService.getPolicyValueAsInt(PolicyKey.FEED_PICTURE_MAX_COUNT)).willReturn(5);

                // 사진 서비스 동작 정의
                given(pictureService.uploadPictureListToS3AndDB(anyList(), eq(FileType.FEED))).willReturn(List.of(picture));
                given(feedPictureRepository.saveAll(anyList())).willReturn(List.of(feedPicture));

                // when
                List<CreateFeedPictureResponse> responses = feedPictureService.createFeedPictures(feedId, files);

                // then
                assertThat(responses).hasSize(1);
                verify(pictureService, times(1)).uploadPictureListToS3AndDB(anyList(), eq(FileType.FEED));
                verify(feedPictureRepository, times(1)).saveAll(anyList());
            }
        }

        @Test
        @DisplayName("업로드하려는 사진 개수가 정책상 최대 개수를 초과하면 예외를 던진다")
        void it_throws_exception_when_count_exceeds_policy() {
            // given
            UUID memberId = UUID.randomUUID();
            Long feedId = 1L;
            List<MultipartFile> files = List.of(mock(MultipartFile.class), mock(MultipartFile.class)); // 2개 추가 시도

            Member member = mock(Member.class);
            Feed feed = mock(Feed.class);

            try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
                userContext.when(UserContextHolder::getUserId).thenReturn(memberId);

                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(memberId)).willReturn(Optional.of(member));
                given(feedRepository.findByIdAndMemberAndDeletedAtNull(feedId, member)).willReturn(Optional.of(feed));

                // 기존 4개 + 신규 2개 > 최대 5개 시나리오
                given(feedPictureRepository.countByFeedAndPicture_PictureDeletedAtNull(feed)).willReturn(4L);
                given(policyService.getPolicyValueAsInt(PolicyKey.FEED_PICTURE_MAX_COUNT)).willReturn(5);

                // when & then
                assertThatThrownBy(() -> feedPictureService.createFeedPictures(feedId, files))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_PICTURE_MAX_COUNT_EXCEED);

                verify(pictureService, never()).uploadPictureListToS3AndDB(any(), any());
            }
        }

        @Test
        @DisplayName("존재하지 않거나 본인의 피드가 아니면 FEED_NOT_FOUND 예외를 던진다")
        void it_throws_exception_when_feed_not_found() {
            // given
            UUID memberId = UUID.randomUUID();
            Long feedId = 999L;
            Member member = mock(Member.class);

            // 실제 객체 생성 (anyList() 대신 빈 리스트나 mock 리스트 사용)
            List<MultipartFile> emptyList = List.of();

            try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
                userContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(memberId)).willReturn(Optional.of(member));
                given(feedRepository.findByIdAndMemberAndDeletedAtNull(feedId, member)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> feedPictureService.createFeedPictures(feedId, emptyList))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID면 MEMBER_NOT_FOUND 예외를 던진다")
        void it_throws_member_not_found() {
            UUID invalidId = UUID.randomUUID();
            try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
                userContext.when(UserContextHolder::getUserId).thenReturn(invalidId);
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(invalidId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> feedPictureService.createFeedPictures(1L, List.of()))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("Describe: deleteFeedPictures 메소드는")
    class Describe_DeleteFeedPictures {

        @Test
        @DisplayName("본인의 피드에 존재하고 삭제되지 않은 사진 ID가 주어지면 사진을 삭제한다")
        void it_deletes_picture_successfully() {
            // given
            UUID memberId = UUID.randomUUID();
            Long feedId = 1L;
            Long pictureId = 100L;

            // Mock 객체 생성
            Member member = mock(Member.class);
            Feed feed = mock(Feed.class);
            Picture picture = mock(Picture.class);
            FeedPicture feedPicture = mock(FeedPicture.class);

            try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
                // 1. 로그인 유저 ID 설정
                userContext.when(UserContextHolder::getUserId).thenReturn(memberId);

                // 2. Member & Feed 조회 성공 설정
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(memberId)).willReturn(Optional.of(member));
                given(feedRepository.findByIdAndMemberAndDeletedAtNull(feedId, member)).willReturn(Optional.of(feed));

                // 3. FeedPicture 조회 성공 설정 (Stubbing)
                given(feedPictureRepository.findByFeedAndIdAndPicture_PictureDeletedAtNull(feed, pictureId))
                        .willReturn(Optional.of(feedPicture));

                // 4. feedPicture.getPicture() 호출 시 반환할 Picture 설정
                given(feedPicture.getPicture()).willReturn(picture);

                // when
                feedPictureService.deleteFeedPictures(feedId, pictureId);

                // then
                // PictureService의 삭제 메서드가 올바른 인자(picture)로 호출되었는지 검증
                verify(pictureService, times(1)).deletePictureFromS3AndDB(picture);
            }
        }

        @Test
        @DisplayName("사진이 해당 피드에 없거나 이미 삭제된 경우 FEED_PICTURE_NOT_FOUND 예외를 던진다")
        void it_throws_exception_when_picture_not_found() {
            // given
            UUID memberId = UUID.randomUUID();
            Long feedId = 1L;
            Long pictureId = 999L; // 존재하지 않거나 삭제된 ID

            Member member = mock(Member.class);
            Feed feed = mock(Feed.class);

            try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
                // 1. 로그인 유저 및 피드 정상 조회
                userContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(memberId)).willReturn(Optional.of(member));
                given(feedRepository.findByIdAndMemberAndDeletedAtNull(feedId, member)).willReturn(Optional.of(feed));

                // 2. FeedPicture 조회 실패 (Optional.empty 반환)
                given(feedPictureRepository.findByFeedAndIdAndPicture_PictureDeletedAtNull(feed, pictureId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> feedPictureService.deleteFeedPictures(feedId, pictureId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_PICTURE_NOT_FOUND);

                verify(pictureService, never()).deletePictureFromS3AndDB(any(Picture.class));
            }
        }

        @Test
        @DisplayName("요청한 피드가 존재하지 않거나 본인의 것이 아니면 FEED_NOT_FOUND 예외를 던진다")
        void it_throws_exception_when_feed_not_found() {
            // given
            UUID memberId = UUID.randomUUID();
            Long feedId = 999L;
            Long pictureId = 100L;
            Member member = mock(Member.class);

            try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
                userContext.when(UserContextHolder::getUserId).thenReturn(memberId);
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(memberId)).willReturn(Optional.of(member));

                // 피드 조회 실패 설정
                given(feedRepository.findByIdAndMemberAndDeletedAtNull(feedId, member)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> feedPictureService.deleteFeedPictures(feedId, pictureId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);

                verify(pictureService, never()).deletePictureFromS3AndDB(any(Picture.class));
            }
        }
    }
}