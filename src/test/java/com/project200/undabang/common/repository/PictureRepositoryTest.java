package com.project200.undabang.common.repository;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class PictureRepositoryTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private PictureRepository pictureRepository;

    private Picture createAndSavePicture(String url, boolean isDeleted) {
        Picture picture = Picture.builder()
                .pictureUrl(url)
                .build();
        if (isDeleted) {
            picture.softDelete();
        }
        em.persist(picture);
        return picture;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("existsByIdAndPictureDeletedAtNull 메소드 테스트")
    class ExistsByIdAndPictureDeletedAtNullTest {

        @Test
        @DisplayName("성공: 존재하고 삭제되지 않은 사진ID로 조회 시 true를 반환한다")
        void givenExistingAndNotDeletedPicture_whenExists_thenReturnsTrue() {
            // given
            Picture savedPicture = createAndSavePicture("https://example.com/image.jpg", false);
            flushAndClear();

            // when
            boolean result = pictureRepository.existsByIdAndPictureDeletedAtNull(savedPicture.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("실패: 삭제된 사진ID로 조회 시 false를 반환한다")
        void givenDeletedPicture_whenExists_thenReturnsFalse() {
            // given
            Picture savedPicture = createAndSavePicture("https://example.com/deleted.jpg", true);
            flushAndClear();

            // when
            boolean result = pictureRepository.existsByIdAndPictureDeletedAtNull(savedPicture.getId());

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사진ID로 조회 시 false를 반환한다")
        void givenNonExistentPictureId_whenExists_thenReturnsFalse() {
            // given
            Long nonExistentId = 9999L;

            // when
            boolean result = pictureRepository.existsByIdAndPictureDeletedAtNull(nonExistentId);

            // then
            assertThat(result).isFalse();
        }
    }
}