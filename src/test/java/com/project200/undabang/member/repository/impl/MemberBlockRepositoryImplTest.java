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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class MemberBlockRepositoryImplTest {
    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private TestEntityManager em;

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

    @Nested
    @DisplayName("findAllBlockedMemberIdsByMember 메소드는")
    class Describe_findAllBlockedMemberIdsByMember {

        @Test
        @DisplayName("나 자신, 내가 차단한 회원, 나를 차단한 회원의 ID를 모두 포함한 Set을 반환한다")
        void it_returns_set_containing_self_i_blocked_and_blocked_by_members() {
            // given
            Member currentUser = createMember("currentUser");
            Member blockedByUser = createMember("blockedByUser");        // 내가 차단할 사용자
            Member blockedByOther = createMember("blockedByOther");      // 나를 차단할 사용자
            Member otherUser = createMember("otherUser");               // 관계 없는 사용자

            MemberBlock block1 = MemberBlock.of(currentUser, blockedByUser);  // 내가 차단
            MemberBlock block2 = MemberBlock.of(blockedByOther, currentUser); // 나를 차단

            saveAndFlush(currentUser, blockedByUser, blockedByOther, otherUser, block1, block2);

            // when
            Set<UUID> exclusionIds = memberBlockRepository.findAllBlockedMemberIdsByMember(currentUser);

            // then
            assertThat(exclusionIds).hasSize(3)
                    .containsExactlyInAnyOrder(
                            currentUser.getMemberId(),
                            blockedByUser.getMemberId(),
                            blockedByOther.getMemberId()
                    );
        }

        @Test
        @DisplayName("차단 관계가 전혀 없으면 자기 자신의 ID만 포함한 Set을 반환한다")
        void it_returns_set_with_only_self_id_when_no_blocks_exist() {
            // given
            Member currentUser = createMember("currentUser");
            Member otherUser = createMember("otherUser");
            saveAndFlush(currentUser, otherUser);

            // when
            Set<UUID> exclusionIds = memberBlockRepository.findAllBlockedMemberIdsByMember(currentUser);

            // then
            assertThat(exclusionIds).hasSize(1)
                    .containsExactly(currentUser.getMemberId());
        }

        @Test
        @DisplayName("차단했다가 해제한 회원은 결과 Set에 포함하지 않는다")
        void it_excludes_unblocked_members_from_the_set() {
            // given
            Member currentUser = createMember("currentUser");
            Member unblockedUser = createMember("unblockedUser");

            MemberBlock unblockedRelation = MemberBlock.of(currentUser, unblockedUser);
            unblockedRelation.unBlock(); // 차단 해제 (soft-delete)

            saveAndFlush(currentUser, unblockedUser, unblockedRelation);

            // when
            Set<UUID> exclusionIds = memberBlockRepository.findAllBlockedMemberIdsByMember(currentUser);

            // then
            // 내가 차단했던 unblockedUser는 제외되고, 나 자신만 포함되어야 함
            assertThat(exclusionIds).hasSize(1)
                    .containsExactly(currentUser.getMemberId());
        }

        @Test
        @DisplayName("나를 차단했다가 해제한 회원도 결과 Set에 포함하지 않는다")
        void it_excludes_members_who_unblocked_me_from_the_set() {
            // given
            Member currentUser = createMember("currentUser");
            Member userWhoUnblockedMe = createMember("userWhoUnblockedMe");

            MemberBlock unblockedRelation = MemberBlock.of(userWhoUnblockedMe, currentUser);
            unblockedRelation.unBlock(); // 차단 해제

            saveAndFlush(currentUser, userWhoUnblockedMe, unblockedRelation);

            // when
            Set<UUID> exclusionIds = memberBlockRepository.findAllBlockedMemberIdsByMember(currentUser);

            // then
            // 나를 차단했던 userWhoUnblockedMe는 제외되고, 나 자신만 포함되어야 함
            assertThat(exclusionIds).hasSize(1)
                    .containsExactly(currentUser.getMemberId());
        }
    }

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
    @DisplayName("checkMemberBlockExists 메소드는")
    class Describe_checkMemberBlockExists {

        @Test
        @DisplayName("내가 상대방을 차단한 경우 true를 반환한다")
        void it_returns_true_when_currentMember_blocked_target() {
            // given
            Member currentUser = createMember("currentUser");
            Member targetMember = createMember("targetMember");
            MemberBlock block = MemberBlock.of(currentUser, targetMember); // 내가 상대를 차단
            saveAndFlush(currentUser, targetMember, block);

            // when
            boolean result = memberBlockRepository.checkMemberBlockExists(currentUser, targetMember);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("상대방이 나를 차단한 경우 true를 반환한다")
        void it_returns_true_when_target_blocked_currentMember() {
            // given
            Member currentUser = createMember("currentUser");
            Member targetMember = createMember("targetMember");
            MemberBlock block = MemberBlock.of(targetMember, currentUser); // 상대가 나를 차단
            saveAndFlush(currentUser, targetMember, block);

            // when
            boolean result = memberBlockRepository.checkMemberBlockExists(currentUser, targetMember);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("상호 차단한 경우 true를 반환한다")
        void it_returns_true_when_mutual_block_exists() {
            // given
            Member currentUser = createMember("currentUser");
            Member targetMember = createMember("targetMember");
            MemberBlock block1 = MemberBlock.of(currentUser, targetMember); // 내가 상대를 차단
            MemberBlock block2 = MemberBlock.of(targetMember, currentUser); // 상대가 나를 차단
            saveAndFlush(currentUser, targetMember, block1, block2);

            // when
            boolean result = memberBlockRepository.checkMemberBlockExists(currentUser, targetMember);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("차단 관계가 전혀 없으면 false를 반환한다")
        void it_returns_false_when_no_block_exists() {
            // given
            Member currentUser = createMember("currentUser");
            Member targetMember = createMember("targetMember");
            saveAndFlush(currentUser, targetMember);

            // when
            boolean result = memberBlockRepository.checkMemberBlockExists(currentUser, targetMember);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("내가 상대방을 차단했다가 해제한 경우 false를 반환한다")
        void it_returns_false_when_currentMember_unblocked_target() {
            // given
            Member currentUser = createMember("currentUser");
            Member targetMember = createMember("targetMember");
            MemberBlock block = MemberBlock.of(currentUser, targetMember);
            block.unBlock(); // 차단 해제 (soft-delete)
            saveAndFlush(currentUser, targetMember, block);

            // when
            boolean result = memberBlockRepository.checkMemberBlockExists(currentUser, targetMember);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("상대방이 나를 차단했다가 해제한 경우 false를 반환한다")
        void it_returns_false_when_target_unblocked_currentMember() {
            // given
            Member currentUser = createMember("currentUser");
            Member targetMember = createMember("targetMember");
            MemberBlock block = MemberBlock.of(targetMember, currentUser); // 상대가 나를 차단
            block.unBlock(); // 차단 해제 (soft-delete)
            saveAndFlush(currentUser, targetMember, block);

            // when
            boolean result = memberBlockRepository.checkMemberBlockExists(currentUser, targetMember);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("다른 사람과의 차단 관계는 false를 반환한다")
        void it_returns_false_for_unrelated_blocks() {
            // given
            Member currentUser = createMember("currentUser");
            Member targetMember = createMember("targetMember");
            Member otherMember = createMember("otherMember");
            MemberBlock block = MemberBlock.of(currentUser, otherMember); // 나는 다른 사람을 차단
            saveAndFlush(currentUser, targetMember, otherMember, block);

            // when
            boolean result = memberBlockRepository.checkMemberBlockExists(currentUser, targetMember);

            // then
            assertThat(result).isFalse();
        }
    }
}