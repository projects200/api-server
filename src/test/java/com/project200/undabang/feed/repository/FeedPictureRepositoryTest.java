package com.project200.undabang.feed.repository;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedPicture;
import com.project200.undabang.member.entity.Member;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class FeedPictureRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private FeedPictureRepository feedPictureRepository;

    @Nested
    @DisplayName("Describe: countByFeedAndPicture_PictureDeletedAtNull 메소드는")
    class Describe_CountByFeedAndPicture {

        @Test
        @DisplayName("삭제되지 않은 사진만 개수를 세어 반환하고, 타 피드 사진이나 삭제된 사진은 제외한다")
        void it_counts_only_active_pictures_of_target_feed() {
            // given: 작성자 및 피드 2개 생성
            Member author = createAndSaveMember("tester");
            Feed targetFeed = createAndSaveFeed(author, "대상 피드");
            Feed otherFeed = createAndSaveFeed(author, "다른 피드");

            // given: 대상 피드 - 활성 사진 2장, 삭제된 사진 1장
            createAndSaveFeedPicture(targetFeed, "active1.jpg", false);
            createAndSaveFeedPicture(targetFeed, "active2.jpg", false);
            createAndSaveFeedPicture(targetFeed, "deleted.jpg", true);

            // given: 다른 피드 - 활성 사진 1장 (카운트에 포함되면 안 됨)
            createAndSaveFeedPicture(otherFeed, "other.jpg", false);

            flushAndClear();

            // when: 대상 피드에 대해 카운트 조회
            long count = feedPictureRepository.countByFeedAndPicture_PictureDeletedAtNull(targetFeed);

            // then: 오직 대상 피드의 삭제되지 않은 2장만 카운트되어야 함
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("피드에 사진이 아예 없거나 모두 삭제된 상태라면 0을 반환한다")
        void it_returns_zero_when_no_active_pictures_exist() {
            // given
            Member author = createAndSaveMember("tester");
            Feed feed = createAndSaveFeed(author, "빈 피드");

            // 모두 삭제된 사진만 존재
            createAndSaveFeedPicture(feed, "deleted1.jpg", true);

            flushAndClear();

            // when
            long count = feedPictureRepository.countByFeedAndPicture_PictureDeletedAtNull(feed);

            // then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("Describe: findByFeedAndIdAndPicture_PictureDeletedAtNull 메소드는")
    class Describe_FindByFeedAndId {

        @Test
        @DisplayName("피드에 속하고 삭제되지 않은 사진이면 조회에 성공한다")
        void it_returns_picture_when_exists_and_active() {
            // given
            Member author = createAndSaveMember("tester");
            Feed feed = createAndSaveFeed(author, "내 피드");

            // 활성 사진 생성
            FeedPicture savedPic = createAndSaveFeedPicture(feed, "active.jpg", false);
            Long pictureId = savedPic.getId();

            flushAndClear();

            // when
            Optional<FeedPicture> result = feedPictureRepository.findByFeedAndIdAndPicture_PictureDeletedAtNull(feed, pictureId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(pictureId);
            assertThat(result.get().getPicture().getPictureUrl()).isEqualTo("active.jpg");
        }

        @Test
        @DisplayName("사진이 존재하지만 삭제된 상태라면 조회되지 않는다")
        void it_returns_empty_when_picture_is_deleted() {
            // given
            Member author = createAndSaveMember("tester");
            Feed feed = createAndSaveFeed(author, "내 피드");

            FeedPicture deletedPic = createAndSaveFeedPicture(feed, "deleted.jpg", true);
            Long pictureId = deletedPic.getId();

            flushAndClear();

            // when
            Optional<FeedPicture> result = feedPictureRepository.findByFeedAndIdAndPicture_PictureDeletedAtNull(feed, pictureId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("사진이 존재하고 활성 상태지만, 다른 피드의 사진이라면 조회되지 않는다")
        void it_returns_empty_when_picture_belongs_to_other_feed() {
            // given
            Member author = createAndSaveMember("tester");
            Feed myFeed = createAndSaveFeed(author, "내 피드");
            Feed otherFeed = createAndSaveFeed(author, "남의 피드");

            FeedPicture otherPic = createAndSaveFeedPicture(otherFeed, "other.jpg", false);
            Long otherPictureId = otherPic.getId();

            flushAndClear();

            Optional<FeedPicture> result = feedPictureRepository.findByFeedAndIdAndPicture_PictureDeletedAtNull(myFeed, otherPictureId);

            // then
            assertThat(result).isEmpty();
        }
    }

    private Member createAndSaveMember(String nickname) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@test.com")
                .memberNickname(nickname)
                .build();
        em.persist(member);
        return member;
    }

    private Feed createAndSaveFeed(Member member, String content) {
        Feed feed = Feed.builder()
                .member(member)
                .feedContent(content)
                .createdAt(LocalDateTime.now())
                .build();
        em.persist(feed);
        return feed;
    }

    private FeedPicture createAndSaveFeedPicture(Feed feed, String url, boolean isDeleted) {
        Picture picture = Picture.builder()
                .pictureUrl(url)
                .pictureName("file.jpg")
                .pictureDeletedAt(isDeleted ? LocalDateTime.now() : null)
                .build();
        em.persist(picture);

        FeedPicture feedPicture = FeedPicture.builder()
                .id(picture.getId())
                .picture(picture)
                .feed(feed)
                .build();
        em.persist(feedPicture);

        return feedPicture;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}