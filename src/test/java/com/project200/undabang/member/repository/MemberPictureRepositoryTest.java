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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class MemberPictureRepositoryTest {


    @Autowired
    private EntityManager em;

    @Autowired
    private MemberPictureRepository memberPictureRepository;

    private Member createAndSaveMember(String nickname) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@email.com")
                .memberNickname(nickname)
                .memberGender(MemberGender.UNKNOWN) // Enum이 있다면 실제 값으로
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();
        em.persist(member);
        return member;
    }

    private Picture createAndSavePicture(String pictureName) {
        Picture picture = Picture.builder()
                .pictureName(pictureName)
                .pictureUrl("/test/" + pictureName)
                .pictureExtension(".jpg")
                .pictureSize(1024)
                .build();
        em.persist(picture);
        return picture;
    }

    private MemberPicture createAndSaveMemberPicture(Member member) {
        // Picture를 먼저 생성하고 저장
        Picture picture = createAndSavePicture(UUID.randomUUID().toString());

        // MemberPicture 생성 및 저장
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
}