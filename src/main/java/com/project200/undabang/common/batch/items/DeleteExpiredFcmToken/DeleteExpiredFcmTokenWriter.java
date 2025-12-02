package com.project200.undabang.common.batch.items.DeleteExpiredFcmToken;

import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * DeleteExpiredFcmTokenWriter 클래스는 만료된 FCM 토큰 데이터를 삭제하는 기능을 제공합니다.
 * Spring Batch의 ItemWriter 인터페이스 구현체로, 배치 작업에서 사용됩니다.
 * <p>
 * 주요 역할:
 * - 제공된 만료된 FCM 토큰 ID 목록을 데이터베이스에서 삭제합니다.
 * - 삭제된 토큰 수를 로그에 기록합니다.
 * <p>
 * 생성자 주입을 통해 FcmTokenRepository를 받아 데이터 조작을 수행합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class DeleteExpiredFcmTokenWriter implements ItemWriter<Long> {
    private final FcmTokenRepository fcmTokenRepository;

    /**
     * 만료된 FCM 토큰 ID 목록을 삭제합니다.
     * 제공된 토큰 ID 목록이 비어 있지 않을 경우, 해당 ID에 해당하는 FCM 토큰 데이터를 배치 삭제 처리하고
     * 삭제된 항목 수를 로그에 기록합니다.
     */
    @Override
    public void write(Chunk<? extends Long> chunk) throws Exception {
        List<Long> tokenIdList = (List<Long>) chunk.getItems();

        if (!tokenIdList.isEmpty()) {
            fcmTokenRepository.deleteAllByIdInBatch(tokenIdList);
            log.info("만료된 FCM 토큰 삭제 : {} 건 삭제 완료", tokenIdList.size());
        }
    }
}
