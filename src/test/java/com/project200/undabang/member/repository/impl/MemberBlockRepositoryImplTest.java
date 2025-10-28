package com.project200.undabang.member.repository.impl;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.dto.record.MemberBlockRecord;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberBlock;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.repository.MemberBlockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class MemberBlockRepositoryImplTest {
    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private TestEntityManager em;

    private Member createMember(String nickname) {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@test.com")
                .memberNickname(nickname)
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();
    }

    private MemberPicture createMemberPicture(Member member, Picture picture) {
        return MemberPicture.builder()
                .member(member)
                .picture(picture)
                .memberPicturesUrl(picture.getPictureUrl())
                .build();
    }

    private Picture createPicture(String url) {
        return Picture.builder().pictureUrl(url).build();
    }

    private void saveAndFlush(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("findAllMemberBlockRecordsByMember 메소드는")
    class Describe_findAllMemberBlockRecordsByMember {

        @Test
        @DisplayName("사용자가 차단한 회원 목록을 DTO 레코드로 정확하게 반환한다")
        void it_returns_list_of_blocked_members() {
            // given
            Member blocker = createMember("차단하는사람");
            Member blocked1 = createMember("차단당한사람1"); // 사진 있음
            Member blocked2 = createMember("차단당한사람2"); // 사진 없음
            Member notBlocked = createMember("관계없는사람");

            Picture pic1 = createPicture("url1");
            // [중요] 수정된 헬퍼 메소드가 호출됩니다.
            MemberPicture mp1 = createMemberPicture(blocked1, pic1);
            blocked1.updateProfilePicture(mp1);

            MemberBlock block1 = MemberBlock.of(blocker, blocked1);
            MemberBlock block2 = MemberBlock.of(blocker, blocked2);

            // Picture, MemberPicture도 영속화 대상에 포함되어야 합니다.
            saveAndFlush(blocker, blocked1, blocked2, notBlocked, pic1, mp1, block1, block2);

            // when
            List<MemberBlockRecord> records = memberBlockRepository.findAllMemberBlockRecordsByMember(blocker);

            // then
            assertThat(records).hasSize(2);
            assertThat(records).extracting(MemberBlockRecord::nickname)
                    .containsExactlyInAnyOrder("차단당한사람1", "차단당한사람2");

            // 순서에 의존하지 않는 검증 (이전과 동일)
            MemberBlockRecord recordForBlocked1 = records.stream()
                    .filter(r -> r.nickname().equals("차단당한사람1"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("차단당한사람1의 레코드를 찾을 수 없습니다."));

            assertThat(recordForBlocked1.profileImageUrl()).isEqualTo("url1"); // 이제 통과할 것입니다.

            MemberBlockRecord recordForBlocked2 = records.stream()
                    .filter(r -> r.nickname().equals("차단당한사람2"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("차단당한사람2의 레코드를 찾을 수 없습니다."));

            assertThat(recordForBlocked2.profileImageUrl()).isNull();
        }

        @Test
        @DisplayName("차단한 회원이 없으면 비어있는 리스트를 반환한다")
        void it_returns_empty_list_when_no_blocks_exist() {
            // given
            Member user = createMember("차단안하는사람");
            saveAndFlush(user);

            // when
            List<MemberBlockRecord> records = memberBlockRepository.findAllMemberBlockRecordsByMember(user);

            // then
            assertThat(records).isEmpty();
        }

        @Test
        @DisplayName("차단했다가 해제한 회원은 결과에 포함하지 않는다")
        void it_excludes_soft_deleted_blocks() {
            // given
            Member blocker = createMember("차단하는사람");
            Member blocked = createMember("차단했다해제된사람");

            MemberBlock block = MemberBlock.of(blocker, blocked);
            block.unBlock(); // 차단 해제 (soft-delete)

            saveAndFlush(blocker, blocked, block);

            // when
            List<MemberBlockRecord> records = memberBlockRepository.findAllMemberBlockRecordsByMember(blocker);

            // then
            assertThat(records).isEmpty();
        }
    }
}