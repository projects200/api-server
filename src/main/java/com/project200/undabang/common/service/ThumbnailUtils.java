package com.project200.undabang.common.service;

import com.project200.undabang.common.properties.ThumbnailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThumbnailUtils {
    private ThumbnailProperties thumbnailProperties;

    /**
     * 주어진 입력 스트림에서 이미지를 읽어 지정된 크기의 썸네일 프로필 이미지를 생성하고,
     * 바이트 배열 형태로 반환합니다.
     *
     * @param inputStream 썸네일 프로필 이미지를 생성하기 위한 입력 스트림
     * @return 생성된 썸네일 프로필 이미지의 바이트 배열
     * @throws IOException 입력 스트림을 처리하거나 이미지를 변환하는 동안 오류가 발생한 경우
     */
    public byte[] createThumbnailProfileImage(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int width = thumbnailProperties.width();
        int height = thumbnailProperties.height();

        // Thumbnailator를 사용하여 메모리 내에서 이미지 처리 수행
        Thumbnails.of(inputStream)
                .size(width, height)
                .crop(Positions.CENTER)
                .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }

}
