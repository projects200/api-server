package com.project200.undabang.feed.repository.impl;

import com.project200.undabang.comment.entity.Comment;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import com.project200.undabang.feed.dto.response.GetSpecificFeedResponse;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedPicture;
import com.project200.undabang.feed.entity.FeedType;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.like.entity.FeedLike;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class FeedRepositoryImplTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private FeedRepository feedRepository;

    @Nested
    @DisplayName("getSpecificFeed 메소드는")
    class Describe_getSpecificFeed {

        @Nested
        @DisplayName("존재하는 피드 ID와 로그인한 사용자가 주어지면")
        class Context_with_existing_feed_and_login_user {

            @Test
            @DisplayName("피드 상세 정보, 사진 목록, 좋아요/댓글 여부를 포함한 Optional 객체를 반환한다")
            void it_returns_feed_detail_with_full_info() {
                // given
                Member author = createAndSaveMember("author");
                Member viewer = createAndSaveMember("viewer"); // 조회자
                FeedType type = createAndSaveFeedType("상세조회용");

                // 피드 생성
                Feed feed = createAndSaveFeed(author, type, "Detail View Test");

                // 사진 2장 추가
                createAndSaveFeedPicture(feed, "http://img1.com");
                createAndSaveFeedPicture(feed, "http://img2.com");

                // 조회자가 좋아요 & 댓글 작성
                createAndSaveFeedLike(feed, viewer);
                createAndSaveComment(feed, viewer, "Nice feed!");

                flushAndClear();

                // when
                Optional<GetSpecificFeedResponse> result = feedRepository.getSpecificFeed(viewer, feed.getId());

                // then
                assertThat(result).isPresent();
                GetSpecificFeedResponse response = result.get();

                // 1. 기본 정보 검증
                assertThat(response.getFeedId()).isEqualTo(feed.getId());
                assertThat(response.getFeedContent()).isEqualTo("Detail View Test");
                assertThat(response.getNickname()).isEqualTo("author");

                // 2. 사진 목록 검증 (별도 쿼리 동작 확인)
                assertThat(response.getFeedPictures()).hasSize(2);
                assertThat(response.getFeedPictures())
                        .extracting("feedPictureUrl")
                        .containsExactlyInAnyOrder("http://img1.com", "http://img2.com");

                // 3. 좋아요/댓글 여부 검증 (viewer 기준)
                assertThat(response.isFeedIsLiked()).isTrue();
                assertThat(response.isFeedHasCommented()).isTrue();
            }
        }

        @Nested
        @DisplayName("존재하는 피드지만 사진이 없고, 비로그인(Guest) 사용자가 조회하면")
        class Context_with_existing_feed_and_guest_user {

            @Test
            @DisplayName("사진 목록은 비어있고, 좋아요/댓글 여부는 false인 Optional 객체를 반환한다")
            void it_returns_feed_detail_without_interaction_info() {
                // given
                Member author = createAndSaveMember("writer");
                FeedType type = createAndSaveFeedType("일반");
                Feed feed = createAndSaveFeed(author, type, "Guest View Test");

                // 사진 추가 X, 좋아요 X

                flushAndClear();

                // when (Member에 null 전달)
                Optional<GetSpecificFeedResponse> result = feedRepository.getSpecificFeed(null, feed.getId());

                // then
                assertThat(result).isPresent();
                GetSpecificFeedResponse response = result.get();

                assertThat(response.getFeedId()).isEqualTo(feed.getId());
                assertThat(response.getFeedPictures()).isEmpty(); // 사진 쿼리 결과 0건 확인
                assertThat(response.isFeedIsLiked()).isFalse();         // 게스트는 무조건 false
                assertThat(response.isFeedHasCommented()).isFalse();  // 게스트는 무조건 false
            }
        }

        @Nested
        @DisplayName("존재하지 않는 피드 ID가 주어지면")
        class Context_with_non_existing_feed {

            @Test
            @DisplayName("비어있는 Optional을 반환한다")
            void it_returns_empty_optional() {
                // given
                Member member = createAndSaveMember("tester");
                Long nonExistingFeedId = 99999L;

                flushAndClear();

                // when
                Optional<GetSpecificFeedResponse> result = feedRepository.getSpecificFeed(member, nonExistingFeedId);

                // then
                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("getAllFeedList 메소드는")
    class Describe_getAllFeedList {

        @Test
        @DisplayName("피드 목록을 최신순(ID 역순)으로 조회하고, 다음 페이지가 있다면 hasNext는 true이다")
        void it_returns_latest_feeds_with_has_next_true() {
            // given
            Member member = createAndSaveMember("runner");
            FeedType type = createAndSaveFeedType("러닝");

            // 20개의 피드 생성
            for (int i = 1; i <= 20; i++) {
                createAndSaveFeed(member, type, "Running Log " + i);
            }

            flushAndClear();

            // when (PageSize=10, 첫 페이지 요청, 비로그인 상태)
            Pageable pageable = PageRequest.of(0, 10);
            Slice<FeedDetailResponse> result = feedRepository.getAllFeedList(null, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(10);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.getContent().get(0).getFeedContent()).isEqualTo("Running Log 20"); // 최신글
        }

        @Test
        @DisplayName("마지막 페이지를 조회할 때 데이터 개수가 pageSize 이하라면 hasNext는 false이다")
        void it_returns_last_page_with_has_next_false() {
            // given
            Member member = createAndSaveMember("swimmer");
            FeedType type = createAndSaveFeedType("수영");

            List<Long> feedIds = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                Feed feed = createAndSaveFeed(member, type, "Swim Log " + i);
                feedIds.add(feed.getId());
            }

            flushAndClear();
            Long lastSeenFeedId = feedIds.get(4); // 가장 최신 ID (ID=5 가정)

            // when (PageSize=10, ID 5보다 작은 것 조회 -> 4,3,2,1)
            Pageable pageable = PageRequest.of(0, 10);
            Slice<FeedDetailResponse> result = feedRepository.getAllFeedList(null, lastSeenFeedId, pageable);

            // then
            assertThat(result.getContent()).hasSize(4);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("피드에 포함된 사진 목록을 정상적으로 매핑하여 조회한다")
        void it_returns_feeds_with_pictures_correctly_mapped() {
            // given
            Member member = createAndSaveMember("photographer");
            FeedType type = createAndSaveFeedType("사진");

            Feed feed1 = createAndSaveFeed(member, type, "Photo Day");
            createAndSaveFeedPicture(feed1, "http://img1.com");
            createAndSaveFeedPicture(feed1, "http://img2.com");

            Feed feed2 = createAndSaveFeed(member, type, "Just Text");

            flushAndClear();

            // when
            Slice<FeedDetailResponse> result = feedRepository.getAllFeedList(null, null, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(2); // 최신순이므로 feed2, feed1 순서

            // Feed 2 (최신, 사진 없음)
            FeedDetailResponse res2 = result.getContent().get(0);
            assertThat(res2.getFeedContent()).isEqualTo("Just Text");
            assertThat(res2.getFeedPictures()).isEmpty();

            // Feed 1 (과거, 사진 2장)
            FeedDetailResponse res1 = result.getContent().get(1);
            assertThat(res1.getFeedContent()).isEqualTo("Photo Day");
            assertThat(res1.getFeedPictures()).hasSize(2);
            assertThat(res1.getFeedPictures())
                    .extracting("feedPictureUrl")
                    .containsExactlyInAnyOrder("http://img1.com", "http://img2.com");
        }

        @Test
        @DisplayName("프로필 사진이 있는 회원의 피드는 프로필 URL을 포함한다")
        void it_returns_profile_url_when_member_has_picture() {
            // given
            Member member = createAndSaveMember("model");
            createAndSaveMemberProfilePicture(member, "http://my-profile.com/face.jpg");

            FeedType type = createAndSaveFeedType("일상");
            createAndSaveFeed(member, type, "Selfie");

            flushAndClear();

            // when
            Slice<FeedDetailResponse> result = feedRepository.getAllFeedList(null, null, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent().get(0).getProfileUrl()).isEqualTo("http://my-profile.com/face.jpg");
            assertThat(result.getContent().get(0).getNickname()).isEqualTo("model");
        }

        @Test
        @DisplayName("로그인한 사용자가 좋아요와 댓글을 단 피드는 isLiked, hasCommented가 true로 반환된다")
        void it_returns_isLiked_and_hasCommented_true_for_interacted_feeds() {
            // given
            Member author = createAndSaveMember("author"); // 작성자
            Member viewer = createAndSaveMember("viewer"); // 조회자 (로그인 유저)
            FeedType type = createAndSaveFeedType("테스트");

            // 피드 1: viewer가 좋아요 O, 댓글 O
            Feed feed1 = createAndSaveFeed(author, type, "Liked and Commented Feed");
            createAndSaveFeedLike(feed1, viewer);
            createAndSaveComment(feed1, viewer, "Good!");

            // 피드 2: viewer가 아무것도 안 함
            Feed feed2 = createAndSaveFeed(author, type, "No Interaction Feed");

            flushAndClear();

            // when (viewer로 로그인하여 조회)
            Slice<FeedDetailResponse> result = feedRepository.getAllFeedList(viewer, null, PageRequest.of(0, 10));

            // then
            List<FeedDetailResponse> content = result.getContent();

            // Feed 2 검증
            assertThat(content.get(0).getFeedContent()).isEqualTo("No Interaction Feed");
            assertThat(content.get(0).isFeedIsLiked()).isFalse();
            assertThat(content.get(0).isFeedHasCommented()).isFalse();

            // Feed 1 검증
            assertThat(content.get(1).getFeedContent()).isEqualTo("Liked and Commented Feed");
            assertThat(content.get(1).isFeedIsLiked()).isTrue();       // 좋아요 확인
            assertThat(content.get(1).isFeedHasCommented()).isTrue(); // 댓글 확인
        }

        @Test
        @DisplayName("등록된 피드가 하나도 없을 경우 빈 리스트를 반환하며, 사진 조회 쿼리는 실행되지 않는다")
        void it_returns_empty_slice_when_no_feed_exists() {
            // given
            // 피드 데이터를 아무것도 생성하지 않음 (DB가 비어있는 상태)
            flushAndClear();

            // when
            Pageable pageable = PageRequest.of(0, 10);
            Slice<FeedDetailResponse> result = feedRepository.getAllFeedList(null, null, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }
    }

    private Member createAndSaveMember(String nickname) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID()) // ID 직접 생성
                .memberEmail(nickname + "@test.com")
                .memberNickname(nickname)
                .memberGender(MemberGender.MALE) // Enum 사용
                .memberBday(LocalDate.of(1990, 1, 1)) // 생년월일 설정
                .memberScore((byte) 0)
                .build();

        em.persist(member);
        return member;
    }

    private void createAndSaveMemberProfilePicture(Member member, String url) {
        Picture pic = Picture.builder()
                .pictureUrl(url)
                .pictureName("profile.jpg")
                .build();
        em.persist(pic);

        MemberPicture mp = MemberPicture.builder()
                .member(member)
                .picture(pic)
                .memberPicturesUrl(url)
                .build();
        em.persist(mp);

        member.updateProfilePicture(mp);

        em.persist(member);
    }

    private FeedType createAndSaveFeedType(String name) {
        FeedType type = FeedType.builder()
                .feedTypeName(name)
                .feedTypeDesc(name + " 설명")
                .feedTypeIsActive(true)
                .build();
        em.persist(type);
        return type;
    }

    private Feed createAndSaveFeed(Member member, FeedType type, String content) {
        Feed feed = Feed.builder()
                .member(member)
                .feedType(type)
                .feedContent(content)
                .likesCount(0)
                .commentsCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        em.persist(feed);
        return feed;
    }

    private void createAndSaveFeedPicture(Feed feed, String url) {
        Picture pic = Picture.builder()
                .pictureUrl(url)
                .pictureName("feed_img.jpg")
                .build();
        em.persist(pic);

        FeedPicture fp = FeedPicture.builder()
                .feed(feed)
                .picture(pic)
                .build();
        em.persist(fp);
    }

    private void createAndSaveFeedLike(Feed feed, Member member) {
        FeedLike like = FeedLike.builder()
                .feed(feed)
                .member(member)
                .build();
        em.persist(like);
    }

    private void createAndSaveComment(Feed feed, Member member, String content) {
        Comment comment = Comment.builder()
                .feed(feed)
                .member(member)
                .content(content)
                .build();
        em.persist(comment);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}