package com.project200.undabang.batch.items;

import com.project200.undabang.common.batch.items.DeleteExpiredFcmToken.DeleteExpiredFcmTokenReader;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteExpiredFcmTokenReaderTest {
    private final int CHUNK_SIZE = 5;
    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Nested
    @DisplayName("read 메서드는")
    class Describe_read {

        @Nested
        @DisplayName("DB에 만료된 토큰 데이터가 아예 없다면")
        class Context_with_empty_data {

            @Test
            @DisplayName("DB 조회 후 즉시 null을 반환한다")
            void it_returns_null_immediately() throws Exception {
                // Given
                DeleteExpiredFcmTokenReader reader = new DeleteExpiredFcmTokenReader(fcmTokenRepository, CHUNK_SIZE);
                given(fcmTokenRepository.findAllExpiredTokenIdList(CHUNK_SIZE)).willReturn(Collections.emptyList());

                // When
                Long result = reader.read();

                // Then
                assertThat(result).isNull();
            }
        }

        @Nested
        @DisplayName("조회된 데이터가 Chunk Size보다 적게 남아있다면 (Last Batch)")
        class Context_with_partial_data {

            @Test
            @DisplayName("데이터를 모두 읽은 후, DB 재조회 없이 null을 반환한다 (무한 루프 방지)")
            void it_returns_null_without_requery() throws Exception {
                // Given
                DeleteExpiredFcmTokenReader reader = new DeleteExpiredFcmTokenReader(fcmTokenRepository, CHUNK_SIZE);

                // 3개만 반환 (Chunk 5보다 작음 -> Last Batch Flag On)
                List<Long> mockIds = List.of(1L, 2L, 3L);
                given(fcmTokenRepository.findAllExpiredTokenIdList(CHUNK_SIZE)).willReturn(mockIds);

                // When & Then
                // 1~3번째: 데이터 정상 반환
                assertThat(reader.read()).isEqualTo(1L);
                assertThat(reader.read()).isEqualTo(2L);
                assertThat(reader.read()).isEqualTo(3L);

                // 4번째: Flag가 켜져 있으므로 DB 조회 없이 바로 null
                assertThat(reader.read()).isNull();

                // Verify: DB 조회는 최초 1회만 발생해야 함
                verify(fcmTokenRepository, times(1)).findAllExpiredTokenIdList(CHUNK_SIZE);
            }
        }

        @Nested
        @DisplayName("조회된 데이터가 Chunk Size만큼 가득 차 있다면")
        class Context_with_full_data {

            @Test
            @DisplayName("데이터를 모두 읽은 후, 다시 DB를 조회하여 다음 데이터를 찾는다")
            void it_queries_db_again() throws Exception {
                // Given
                // 테스트를 위해 ChunkSize를 2로 줄여서 가정
                int smallChunkSize = 2;
                DeleteExpiredFcmTokenReader reader = new DeleteExpiredFcmTokenReader(fcmTokenRepository, smallChunkSize);

                // 첫 번째 호출: 2개 (꽉 참)
                // 두 번째 호출: 0개 (종료)
                given(fcmTokenRepository.findAllExpiredTokenIdList(smallChunkSize))
                        .willReturn(List.of(1L, 2L))
                        .willReturn(Collections.emptyList());

                // When & Then
                assertThat(reader.read()).isEqualTo(1L);
                assertThat(reader.read()).isEqualTo(2L);

                // Iterator 소진 -> DB 재조회 -> 빈 리스트 -> null
                assertThat(reader.read()).isNull();

                // Verify: 총 2번 호출됨
                verify(fcmTokenRepository, times(2)).findAllExpiredTokenIdList(smallChunkSize);
            }
        }
    }
}