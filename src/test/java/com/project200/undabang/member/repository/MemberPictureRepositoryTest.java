package com.project200.undabang.member.repository;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class MemberPictureRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberPictureRepository memberPictureRepository;

    @Nested
    @DisplayName("findByMemberAndPicture_PictureDeletedAtNull 메소드는")
    class Describe_findByMemberAndPicture_PictureDeletedAtNull {

        @Test
        @DisplayName("특정 회원이 가지고 있는, 삭제되지 않은 모든 프로필 사진 목록을 조회한다")
        void it_returns_all_non_deleted_pictures_for_the_given_member() {
            // given
            Member member1 = createAndSaveMember("testMember1");
            Member member2 = createAndSaveMember("testMember2");

            MemberPicture mp1 = createAndSaveMemberPicture(member1);
            MemberPicture mp2 = createAndSaveMemberPicture(member1);
            MemberPicture mp3 = createAndSaveMemberPicture(member1);
            createAndSaveMemberPicture(member2); // 다른 회원의 사진

            flushAndClear();

            // when
            List<MemberPicture> foundPictures = memberPictureRepository.findByMemberAndPicture_PictureDeletedAtNull(member1);

            // then
            assertThat(foundPictures).hasSize(3);
            assertThat(foundPictures).extracting(MemberPicture::getId)
                    .containsExactlyInAnyOrder(mp1.getId(), mp2.getId(), mp3.getId());
        }

        @Test
        @DisplayName("연결된 Picture가 삭제된(soft-deleted) MemberPicture는 조회 결과에서 제외한다")
        void it_excludes_pictures_that_are_soft_deleted() {
            // given
            Member member = createAndSaveMember("testMember");

            MemberPicture activeMp = createAndSaveMemberPicture(member);
            MemberPicture deletedMp = createAndSaveMemberPicture(member);

            // Picture 엔티티를 soft-delete 처리
            deletedMp.getPicture().softDelete();
            em.persist(deletedMp.getPicture()); // 변경된 Picture 상태를 영속화

            flushAndClear();

            // when
            List<MemberPicture> foundPictures = memberPictureRepository.findByMemberAndPicture_PictureDeletedAtNull(member);

            // then
            assertThat(foundPictures).hasSize(1);
            assertThat(foundPictures.get(0).getId()).isEqualTo(activeMp.getId());
        }

        @Test
        @DisplayName("프로필 사진이 없는 회원을 조회하면 빈 리스트를 반환한다")
        void it_returns_an_empty_list_when_no_pictures_exist_for_the_member() {
            // given
            Member member = createAndSaveMember("testMember");
            flushAndClear();

            // when
            List<MemberPicture> foundPictures = memberPictureRepository.findByMemberAndPicture_PictureDeletedAtNull(member);

            // then
            assertThat(foundPictures).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("findByMemberAndPicture_IdAndPicture_PictureDeletedAtNull 메소드는")
    class Describe_findByMemberAndPicture_Id_AndPicture_PictureDeletedAtNull {

        @Test
        @DisplayName("유효한 회원과 사진ID로 조회 시 해당 MemberPicture 객체를 Optional에 담아 반환한다")
        void it_returns_optional_of_member_picture_for_valid_input() {
            // given
            Member member = createAndSaveMember("testMember");
            MemberPicture expectedMemberPicture = createAndSaveMemberPicture(member);
            Long pictureId = expectedMemberPicture.getPicture().getId();

            flushAndClear();

            // when
            Optional<MemberPicture> foundOpt = memberPictureRepository.findByMemberAndPicture_IdAndPicture_PictureDeletedAtNull(member, pictureId);

            // then
            assertThat(foundOpt).isPresent();
            assertThat(foundOpt.get().getId()).isEqualTo(expectedMemberPicture.getId());
        }

        @Test
        @DisplayName("사진이 다른 회원의 소유일 경우 빈 Optional을 반환한다 (인가 실패 케이스)")
        void it_returns_empty_optional_when_picture_owned_by_another_member() {
            // given
            Member requestingMember = createAndSaveMember("requestingMember");
            Member ownerMember = createAndSaveMember("ownerMember");
            MemberPicture ownedMemberPicture = createAndSaveMemberPicture(ownerMember);
            Long pictureIdOwnedByOther = ownedMemberPicture.getPicture().getId();

            flushAndClear();

            // when
            Optional<MemberPicture> result = memberPictureRepository.findByMemberAndPicture_IdAndPicture_PictureDeletedAtNull(requestingMember, pictureIdOwnedByOther);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("사진이 soft-delete된 경우 빈 Optional을 반환한다")
        void it_returns_empty_optional_when_picture_is_soft_deleted() {
            // given
            Member member = createAndSaveMember("testMember");
            MemberPicture memberPicture = createAndSaveMemberPicture(member);
            Picture picture = memberPicture.getPicture();
            picture.softDelete(); // Picture를 soft-delete
            em.persist(picture);

            flushAndClear();

            // when
            Optional<MemberPicture> result = memberPictureRepository.findByMemberAndPicture_IdAndPicture_PictureDeletedAtNull(member, picture.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    private Member createAndSaveMember(String nickname) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@email.com")
                .memberNickname(nickname)
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();
        em.persist(member);
        return member;
    }

    private Picture createAndSavePicture(String pictureName, LocalDateTime createdAt) {
        Picture picture = Picture.builder()
                .pictureName(pictureName)
                .pictureUrl("/test/" + pictureName)
                .pictureExtension(".jpg")
                .pictureSize(1024)
                .pictureCreatedAt(createdAt) // 생성 시간 제어를 위해 파라미터 추가
                .build();
        em.persist(picture);
        return picture;
    }

    private MemberPicture createAndSaveMemberPicture(Member member) {
        return createAndSaveMemberPicture(member, LocalDateTime.now());
    }

    private MemberPicture createAndSaveMemberPicture(Member member, LocalDateTime pictureCreatedAt) {
        Picture picture = createAndSavePicture(UUID.randomUUID().toString(), pictureCreatedAt);
        MemberPicture memberPicture = MemberPicture.builder()
                .member(member)
                .picture(picture)
                .build();
        em.persist(memberPicture);
        return memberPicture;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("findFirstByMemberAndMemberPicturesDeletedAtNullAndPicture_PictureDeletedAtNullOrderByPicture_PictureCreatedAtDesc 메소드는")
    class Describe_findFirstByMember {

        @Test
        @DisplayName("삭제되지 않은 사진들 중 가장 최근에 생성된 사진 하나를 Optional에 담아 반환한다")
        void it_returns_the_most_recently_created_picture() throws InterruptedException {
            // given
            Member member = createAndSaveMember("testMember");

            // 생성 시간 순서를 보장하기 위해 시간 간격을 둠
            createAndSaveMemberPicture(member, LocalDateTime.now().minusDays(2)); // 가장 오래된 사진
            MemberPicture middlePicture = createAndSaveMemberPicture(member, LocalDateTime.now().minusDays(1)); // 중간
            MemberPicture latestPicture = createAndSaveMemberPicture(member, LocalDateTime.now()); // 가장 최신

            flushAndClear();

            // when
            Optional<MemberPicture> foundOpt = memberPictureRepository.findFirstByMemberAndMemberPicturesDeletedAtNullAndPicture_PictureDeletedAtNullOrderByPicture_PictureCreatedAtDesc(member);

            // then
            assertThat(foundOpt).isPresent();
            assertThat(foundOpt.get().getId()).isEqualTo(latestPicture.getId());
            assertThat(foundOpt.get().getPicture().getId()).isEqualTo(latestPicture.getPicture().getId());
        }

        @Test
        @DisplayName("가장 최신 사진의 Picture가 soft-delete된 경우, 그 다음으로 최신인 사진을 반환한다")
        void it_returns_the_next_latest_picture_if_the_latest_one_is_deleted() {
            // given
            Member member = createAndSaveMember("testMember");

            createAndSaveMemberPicture(member, LocalDateTime.now().minusDays(2));
            MemberPicture expectedPicture = createAndSaveMemberPicture(member, LocalDateTime.now().minusDays(1)); // 차순위 최신
            MemberPicture latestButDeleted = createAndSaveMemberPicture(member, LocalDateTime.now()); // 가장 최신이지만 삭제될 사진

            // 가장 최신 Picture를 soft-delete
            latestButDeleted.getPicture().softDelete();
            em.persist(latestButDeleted.getPicture());

            flushAndClear();

            // when
            Optional<MemberPicture> foundOpt = memberPictureRepository.findFirstByMemberAndMemberPicturesDeletedAtNullAndPicture_PictureDeletedAtNullOrderByPicture_PictureCreatedAtDesc(member);

            // then
            assertThat(foundOpt).isPresent();
            assertThat(foundOpt.get().getId()).isEqualTo(expectedPicture.getId());
        }

        @Test
        @DisplayName("가장 최신 사진의 MemberPicture가 soft-delete된 경우, 그 다음으로 최신인 사진을 반환한다")
        void it_returns_the_next_latest_picture_if_the_latest_member_picture_is_deleted() {
            // given
            Member member = createAndSaveMember("testMember");

            createAndSaveMemberPicture(member, LocalDateTime.now().minusDays(2));
            MemberPicture expectedPicture = createAndSaveMemberPicture(member, LocalDateTime.now().minusDays(1));
            MemberPicture latestButDeleted = createAndSaveMemberPicture(member, LocalDateTime.now());

            // 가장 최신 MemberPicture를 soft-delete
            latestButDeleted.deleteMemberPicture();
            em.persist(latestButDeleted);

            flushAndClear();

            // when
            Optional<MemberPicture> foundOpt = memberPictureRepository.findFirstByMemberAndMemberPicturesDeletedAtNullAndPicture_PictureDeletedAtNullOrderByPicture_PictureCreatedAtDesc(member);

            // then
            assertThat(foundOpt).isPresent();
            assertThat(foundOpt.get().getId()).isEqualTo(expectedPicture.getId());
        }

        @Test
        @DisplayName("삭제되지 않은 사진이 하나도 없는 경우 빈 Optional을 반환한다")
        void it_returns_empty_optional_when_no_active_pictures_exist() {
            // given
            Member member = createAndSaveMember("testMember");
            MemberPicture mp1 = createAndSaveMemberPicture(member);
            MemberPicture mp2 = createAndSaveMemberPicture(member);

            // 모든 사진을 삭제 처리
            mp1.getPicture().softDelete();
            em.persist(mp1.getPicture());
            mp2.deleteMemberPicture();
            em.persist(mp2);

            flushAndClear();

            // when
            Optional<MemberPicture> foundOpt = memberPictureRepository.findFirstByMemberAndMemberPicturesDeletedAtNullAndPicture_PictureDeletedAtNullOrderByPicture_PictureCreatedAtDesc(member);

            // then
            assertThat(foundOpt).isEmpty();
        }
    }
}