package com.project200.undabang.openchat.entity;

import com.project200.undabang.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenChatRoom 엔티티 테스트")
class OpenChatRoomTest {

    private Member createTestMember() {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .build();
    }

    private OpenChatRoom createOpenChatRoomWithUrl(String url) {
        return OpenChatRoom.of(createTestMember(), url);
    }

    private OpenChatRoom createOpenChatRoomWithId(Long id) {
        return OpenChatRoom.builder()
                .id(id)
                .member(createTestMember())
                .url("https://open.kakao.com/o/chatroom-with-id")
                .build();
    }

    @Nested
    @DisplayName("of() 정적 팩토리 메소드는")
    class Describe_of {

        @Test
        @DisplayName("주어진 멤버와 URL로 OpenChatRoom 객체를 생성한다")
        void creates_instance_with_given_member_and_url() {
            // given
            Member member = createTestMember();
            String url = "https://open.kakao.com/o/initial";

            // when
            OpenChatRoom openChatRoom = OpenChatRoom.of(member, url);

            // then
            assertThat(openChatRoom).isNotNull();
            assertThat(openChatRoom.getMember()).isEqualTo(member);
            assertThat(openChatRoom.getUrl()).isEqualTo(url);
            assertThat(openChatRoom.getCreatedAt()).isNotNull();
            assertThat(openChatRoom.getMemberIdUniqueKey()).isEqualTo(0L);
            assertThat(openChatRoom.getUrlUniqueKey()).isEqualTo(0L);
            assertThat(openChatRoom.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("isSameUrl() 메소드는")
    class Describe_isSameUrl {

        @Test
        @DisplayName("같은 URL을 비교하면 true를 반환한다")
        void returns_true_for_same_url() {
            // given
            String url = "https://open.kakao.com/o/initial";
            OpenChatRoom openChatRoom = createOpenChatRoomWithUrl(url);

            // when
            boolean result = openChatRoom.isSameUrl(url);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("다른 URL을 비교하면 false를 반환한다")
        void returns_false_for_different_url() {
            // given
            OpenChatRoom openChatRoom = createOpenChatRoomWithUrl("https://open.kakao.com/o/initial");

            // when
            boolean result = openChatRoom.isSameUrl("https://open.kakao.com/o/different");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("updateOpenChatUrl() 메소드는")
    class Describe_updateOpenChatUrl {

        @Test
        @DisplayName("URL을 새로운 값으로 변경하고 updatedAt을 설정한다")
        void updates_url_and_sets_updatedAt() {
            // given
            OpenChatRoom openChatRoom = createOpenChatRoomWithUrl("https://open.kakao.com/o/initial");
            String newUrl = "https://open.kakao.com/o/updated";

            // when
            openChatRoom.updateOpenChatUrl(newUrl);

            // then
            assertThat(openChatRoom.getUrl()).isEqualTo(newUrl);
            assertThat(openChatRoom.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("softDelete() 메소드는")
    class Describe_softDelete {

        @Test
        @DisplayName("deletedAt을 설정하고 unique key들을 id 값으로 변경한다")
        void sets_deletedAt_and_updates_unique_keys() {
            // given
            Long chatRoomId = 123L;
            OpenChatRoom openChatRoom = createOpenChatRoomWithId(chatRoomId);

            // when
            openChatRoom.softDelete();

            // then
            assertThat(openChatRoom.getDeletedAt()).isNotNull();
            assertThat(openChatRoom.getMemberIdUniqueKey()).isEqualTo(chatRoomId);
            assertThat(openChatRoom.getUrlUniqueKey()).isEqualTo(chatRoomId);
        }

        @Test
        @DisplayName("ID가 null일 경우 unique key는 null로 변경된다")
        void updates_unique_keys_to_null_if_id_is_null() {
            // given
            // ID가 없는(JPA에 의해 할당받기 전) 객체
            OpenChatRoom openChatRoom = createOpenChatRoomWithUrl("https://open.kakao.com/o/initial");
            assertThat(openChatRoom.getId()).isNull(); // 전제 조건 확인

            // when
            openChatRoom.softDelete();

            // then
            assertThat(openChatRoom.getDeletedAt()).isNotNull();
            assertThat(openChatRoom.getMemberIdUniqueKey()).isNull(); // id가 null이므로 null이 됨
            assertThat(openChatRoom.getUrlUniqueKey()).isNull(); // id가 null이므로 null이 됨
        }
    }
}