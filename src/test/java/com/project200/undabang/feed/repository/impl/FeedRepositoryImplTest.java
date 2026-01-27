package com.project200.undabang.feed.repository.impl;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedPicture;
import com.project200.undabang.feed.entity.FeedType;
import com.project200.undabang.feed.repository.FeedRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    @DisplayName("getAllFeedList 메소드는")
    class Describe_getAllFeedList {

        @Test
        @DisplayName("피드 목록을 최신순(ID 역순)으로 조회하고, 다음 페이지가 있다면 hasNext는 true이다")
        void it_returns_latest_feeds_with_has_next_true() {
            // given
            Member member = createAndSaveMember("runner");
            FeedType type = createAndSaveFeedType("러닝");

            // 20개의 피드 생성 (ID 1 ~ 20)
            for (int i = 1; i <= 20; i++) {
                createAndSaveFeed(member, type, "Running Log " + i);
            }

            flushAndClear();

            // when (PageSize=10, 첫 페이지 요청)
            Pageable pageable = PageRequest.of(0, 10);
            Slice<FeedDetailResponse> result = feedRepository.getAllFeedList(null, pageable);

            // then
            assertThat(result.getContent()).hasSize(10);
            assertThat(result.hasNext()).isTrue(); // 20개 중 10개만 가져왔으므로 true

            // 최신순 검증 (20번 피드가 가장 먼저 나와야 함)
            assertThat(result.getContent().get(0).getFeedContent()).isEqualTo("Running Log 20");
            assertThat(result.getContent().get(9).getFeedContent()).isEqualTo("Running Log 11");
        }

        @Test
        @DisplayName("마지막 페이지를 조회할 때 데이터 개수가 pageSize 이하라면 hasNext는 false이다")
        void it_returns_last_page_with_has_next_false() {
            // given
            Member member = createAndSaveMember("swimmer");
            FeedType type = createAndSaveFeedType("수영");

            // 5개의 피드 생성 (ID 1 ~ 5)
            List<Long> feedIds = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                Feed feed = createAndSaveFeed(member, type, "Swim Log " + i);
                feedIds.add(feed.getId());
            }

            flushAndClear();
            Long lastSeenFeedId = feedIds.get(4); // ID: 5 (가장 최신)

            // when (PageSize=10, ID 5보다 작은 것 조회 -> 4,3,2,1 총 4개)
            Pageable pageable = PageRequest.of(0, 10);
            Slice<FeedDetailResponse> result = feedRepository.getAllFeedList(lastSeenFeedId, pageable);

            // then
            assertThat(result.getContent()).hasSize(4);
            assertThat(result.hasNext()).isFalse(); // 더 이상 데이터가 없으므로 false
            assertThat(result.getContent().get(0).getFeedContent()).isEqualTo("Swim Log 4");
        }

        @Test
        @DisplayName("피드에 포함된 사진 목록을 정상적으로 매핑하여 조회한다")
        void it_returns_feeds_with_pictures_correctly_mapped() {
            // given
            Member member = createAndSaveMember("photographer");
            FeedType type = createAndSaveFeedType("사진");

            // 피드 1: 사진 2장
            Feed feed1 = createAndSaveFeed(member, type, "Photo Day");
            createAndSaveFeedPicture(feed1, "http://img1.com");
            createAndSaveFeedPicture(feed1, "http://img2.com");

            // 피드 2: 사진 0장
            Feed feed2 = createAndSaveFeed(member, type, "Just Text");

            flushAndClear();

            // when
            Pageable pageable = PageRequest.of(0, 10);
            Slice<FeedDetailResponse> result = feedRepository.getAllFeedList(null, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);

            // Feed 2 (최신글, 사진 없음) 확인
            FeedDetailResponse res2 = result.getContent().get(0);
            assertThat(res2.getFeedContent()).isEqualTo("Just Text");
            assertThat(res2.getFeedPictures()).isEmpty();

            // Feed 1 (과거글, 사진 2장) 확인
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
            Slice<FeedDetailResponse> result = feedRepository.getAllFeedList(null, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent().get(0).getProfileUrl()).isEqualTo("http://my-profile.com/face.jpg");
            assertThat(result.getContent().get(0).getNickname()).isEqualTo("model");
        }

        @Test
        @DisplayName("등록된 피드가 하나도 없을 경우 빈 리스트를 반환하며, 사진 조회 쿼리는 실행되지 않는다")
        void it_returns_empty_slice_when_no_feed_exists() {
            // given
            // 피드 데이터를 아무것도 생성하지 않음 (DB가 비어있는 상태)
            flushAndClear();

            // when
            Pageable pageable = PageRequest.of(0, 10);
            Slice<FeedDetailResponse> result = feedRepository.getAllFeedList(null, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }
    }

    private Member createAndSaveMember(String nickname) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@test.com")
                .memberNickname(nickname)
                .memberScore((byte) 50)
                .memberDesc("Intro")
                .memberGender(MemberGender.MALE) // Enum 필요시 수정
                .build();
        em.persist(member);
        return member;
    }

    private void createAndSaveMemberProfilePicture(Member member, String url) {
        Picture pic = Picture.builder()
                .pictureUrl(url) // 실제 URL 입력
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

        em.flush();
        em.clear();
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

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}