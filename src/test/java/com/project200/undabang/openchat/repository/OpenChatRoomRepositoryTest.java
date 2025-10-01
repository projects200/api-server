package com.project200.undabang.openchat.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.openchat.entity.OpenChatRoom;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class OpenChatRoomRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private OpenChatRoomRepository openChatRoomRepository;

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

    private OpenChatRoom createAndSaveOpenChatRoom(Member member, String url) {
        OpenChatRoom room = OpenChatRoom.builder()
                .member(member)
                .url(url)
                .build();
        em.persist(room);
        return room;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("existsByMemberAndDeletedAtNull 메소드는")
    class Describe_existsByMemberAndDeletedAtNull {

        @Test
        @DisplayName("회원이 비삭제된 오픈채팅 방을 가지고 있으면 true 를 반환한다")
        void it_returns_true_when_member_has_active_room() {
            Member member = createAndSaveMember("member1");
            createAndSaveOpenChatRoom(member, "url1");
            flushAndClear();

            boolean exists = openChatRoomRepository.existsByMemberAndDeletedAtNull(member);

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("회원의 방이 모두 삭제되어 있으면 false 를 반환한다")
        void it_returns_false_when_member_only_has_deleted_rooms() {
            Member member = createAndSaveMember("member2");
            OpenChatRoom deleted = OpenChatRoom.builder()
                    .member(member)
                    .url("deleted-url")
                    .deletedAt(LocalDateTime.now())
                    .build();
            em.persist(deleted);
            flushAndClear();

            boolean exists = openChatRoomRepository.existsByMemberAndDeletedAtNull(member);

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByUrlAndDeletedAtNull 메소드는")
    class Describe_existsByUrlAndDeletedAtNull {

        @Test
        @DisplayName("같은 url의 비삭제된 방이 존재하면 true 를 반환한다")
        void it_returns_true_when_active_room_with_url_exists() {
            Member member = createAndSaveMember("member3");
            createAndSaveOpenChatRoom(member, "unique-url");
            flushAndClear();

            boolean exists = openChatRoomRepository.existsByUrlAndDeletedAtNull("unique-url");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("해당 url이 삭제된 방만 있으면 false 를 반환한다")
        void it_returns_false_when_only_deleted_rooms_exist_for_url() {
            Member member = createAndSaveMember("member4");
            OpenChatRoom deleted = OpenChatRoom.builder()
                    .member(member)
                    .url("deleted-only-url")
                    .deletedAt(LocalDateTime.now())
                    .build();
            em.persist(deleted);
            flushAndClear();

            boolean exists = openChatRoomRepository.existsByUrlAndDeletedAtNull("deleted-only-url");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByUrlAndIdNotAndDeletedAtNull 메소드는")
    class Describe_existsByUrlAndIdNotAndDeletedAtNull {

        @Test
        @DisplayName("같은 url을 가진 다른(다른 id) 비삭제 방을 만들면 DB 제약으로 예외가 발생한다")
        void it_throws_when_attempt_to_create_another_active_room_with_same_url() {
            Member m1 = createAndSaveMember("m5");
            Member m2 = createAndSaveMember("m6");
            OpenChatRoom roomA = createAndSaveOpenChatRoom(m1, "conflict-url");
            flushAndClear();

            // H2의 활성 URL 유니크 제약으로 동일 url을 가진 두 번째 active 엔티티 삽입 시 예외 발생
            assertThrows(Exception.class, () -> createAndSaveOpenChatRoom(m2, "conflict-url"));
        }

        @Test
        @DisplayName("같은 url이지만 자신의 id만 존재하면 false 를 반환한다")
        void it_returns_false_when_only_same_room_exists() {
            Member m = createAndSaveMember("m7");
            OpenChatRoom room = createAndSaveOpenChatRoom(m, "solo-url");
            flushAndClear();

            boolean exists = openChatRoomRepository.existsByUrlAndIdNotAndDeletedAtNull("solo-url", room.getId());

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findByIdAndDeletedAtNull 메소드는")
    class Describe_findByIdAndDeletedAtNull {

        @Test
        @DisplayName("비삭제된 id 로 조회하면 Optional 에 값이 담겨 반환된다")
        void it_returns_room_when_id_is_active() {
            Member m = createAndSaveMember("m8");
            OpenChatRoom room = createAndSaveOpenChatRoom(m, "find-url");
            flushAndClear();

            var found = openChatRoomRepository.findByIdAndDeletedAtNull(room.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(room.getId());
        }

        @Test
        @DisplayName("삭제된 방은 조회되지 않는다")
        void it_does_not_return_deleted_room() {
            Member m = createAndSaveMember("m9");
            OpenChatRoom deleted = OpenChatRoom.builder()
                    .member(m)
                    .url("deleted-find-url")
                    .deletedAt(LocalDateTime.now())
                    .build();
            em.persist(deleted);
            flushAndClear();

            var found = openChatRoomRepository.findByIdAndDeletedAtNull(deleted.getId());

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByMember_MemberIdAndDeletedAtNull 메소드는")
    class Describe_findByMemberMemberIdAndDeletedAtNull {

        @Test
        @DisplayName("회원의 비삭제 방을 memberId 로 조회하면 Optional 에 값이 담겨 반환된다")
        void it_returns_room_by_memberId_when_active() {
            Member m = createAndSaveMember("m10");
            OpenChatRoom room = createAndSaveOpenChatRoom(m, "memberid-url");
            flushAndClear();

            var found = openChatRoomRepository.findByMember_MemberIdAndDeletedAtNull(m.getMemberId());

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(room.getId());
        }

        @Test
        @DisplayName("회원의 방이 삭제되어 있으면 빈 Optional 을 반환한다")
        void it_returns_empty_when_member_room_deleted() {
            Member m = createAndSaveMember("m11");
            OpenChatRoom deleted = OpenChatRoom.builder()
                    .member(m)
                    .url("member-deleted-url")
                    .deletedAt(LocalDateTime.now())
                    .build();
            em.persist(deleted);
            flushAndClear();

            var found = openChatRoomRepository.findByMember_MemberIdAndDeletedAtNull(m.getMemberId());

            assertThat(found).isEmpty();
        }
    }
}