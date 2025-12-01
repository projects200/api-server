package com.project200.undabang.common.batch.items.DeleteExpiredFcmToken;

import com.project200.undabang.notification.fcm.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.util.Iterator;
import java.util.List;

/**
 * DeleteExpiredFcmTokenReader 클래스는 만료된 FCM 토큰 ID를 읽어오는 역할을 수행합니다.
 * Spring Batch의 ItemReader 인터페이스를 구현하여 배치 작업에서 사용됩니다.
 * <p>
 * 주요 기능:
 * - 주어진 chunkSize만큼 만료된 FCM 토큰 ID 목록을 FcmTokenRepository를 통해 조회합니다.
 * - 조회된 FCM 토큰 ID를 Iterator로 하나씩 반환하며, 처리할 데이터가 없으면 null을 반환하여 Step의 종료를 나타냅니다.
 * <p>
 * 종속성:
 * - FcmTokenRepository: 만료된 FCM 토큰 ID 리스트를 조회합니다.
 * - chunkSize: 한 번의 조회에서 가져올 데이터의 크기를 정의합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class DeleteExpiredFcmTokenReader implements ItemReader<Long> {
    private final FcmTokenRepository fcmTokenRepository;
    private final int chunkSize;
    private Iterator<Long> iterator;
    private boolean lastBatchFlag = false;

    /**
     * 만료된 FCM 토큰 ID를 하나씩 반환합니다.
     * 배치의 크기(chunk size)만큼 만료된 FCM 토큰 ID를 조회하고,
     * Iterator를 통해 하나씩 반환합니다. 만료된 토큰이 더 이상 없을 경우 null을 반환하여 처리 단계 종료를 나타냅니다.
     */
    @Override
    public Long read() throws Exception {
        // 반복자가 비어있거나 전부 사용한 경우만 동작하도록 설정
        if (iterator == null || !iterator.hasNext()) {
            if (lastBatchFlag) {
                return null;
            }
            // 항상 Limit 만큼만 조회
            List<Long> nextBatch = fcmTokenRepository.findAllExpiredTokenIdList(chunkSize);

            if (nextBatch.isEmpty()) {
                return null; // 더 이상 지울 데이터가 없을 경우 step 종료
            }

            if (nextBatch.size() < chunkSize) {
                lastBatchFlag = true;
            }

            iterator = nextBatch.iterator();
        }

        // iterator 에서 하나씩 반환
        return iterator.next();
    }
}
