package com.project200.undabang.batch.items;

import com.project200.undabang.common.batch.items.DeleteExpiredFcmToken.DeleteExpiredFcmTokenWriter;
import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteExpiredFcmTokenWriter 단위 테스트")
class DeleteExpiredFcmTokenWriterTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Nested
    @DisplayName("write 메서드는")
    class Describe_write {

        @Nested
        @DisplayName("삭제할 ID가 담긴 Chunk가 전달되면")
        class Context_with_valid_chunk {

            @Test
            @DisplayName("Repository의 일괄 삭제 메서드를 호출한다")
            void it_calls_delete_all_by_id_in_batch() throws Exception {
                // Given
                DeleteExpiredFcmTokenWriter writer = new DeleteExpiredFcmTokenWriter(fcmTokenRepository);
                List<Long> ids = List.of(1L, 2L, 3L);
                Chunk<Long> chunk = new Chunk<>(ids);

                // When
                writer.write(chunk);

                // Then
                verify(fcmTokenRepository, times(1)).deleteAllByIdInBatch(ids);
            }
        }

        @Nested
        @DisplayName("비어있는 Chunk가 전달되면")
        class Context_with_empty_chunk {

            @Test
            @DisplayName("삭제 메서드를 호출하지 않는다")
            void it_does_not_call_delete_method() throws Exception {
                // Given
                DeleteExpiredFcmTokenWriter writer = new DeleteExpiredFcmTokenWriter(fcmTokenRepository);
                Chunk<Long> emptyChunk = new Chunk<>(List.of());

                // When
                writer.write(emptyChunk);

                // Then
                verify(fcmTokenRepository, never()).deleteAllByIdInBatch(any());
            }
        }
    }
}