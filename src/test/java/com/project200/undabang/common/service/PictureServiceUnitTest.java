package com.project200.undabang.common.service;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.entity.dto.PictureUploadParameters;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.exception.S3UploadFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PictureServiceUnitTest {

    @InjectMocks
    private PictureService pictureService;

    @Mock
    private S3Service s3Service;

    @Mock
    private PictureRepository pictureRepository;

    private Picture picture1;
    private Picture picture2;
    private List<Picture> pictureList;

    @BeforeEach
    void setUp() {
        // 테스트 전반에 사용될 공통 Picture 객체 초기화
        picture1 = Picture.builder().id(1L).pictureUrl("http://s3.com/uploads/pic1.jpg").build();
        picture2 = Picture.builder().id(2L).pictureUrl("http://s3.com/uploads/pic2.png").build();
        pictureList = List.of(picture1, picture2);
    }

    @Nested
    @DisplayName("사진 업로드 (자동 키 생성) 테스트")
    class UploadWithAutoKeyTests {
        private MockMultipartFile file1, file2;
        private List<MultipartFile> fileList;

        @BeforeEach
        void setUp() {
            file1 = new MockMultipartFile("f1", "img1.jpg", "image/jpeg", "c1".getBytes());
            file2 = new MockMultipartFile("f2", "img2.png", "image/png", "c2".getBytes());
            fileList = List.of(file1, file2);
        }

        @Test
        @DisplayName("성공: 모든 파일이 정상 업로드 및 저장된다")
        void upload_Success() {
            // given
            when(s3Service.generateObjectKey(anyString(), any(FileType.class))).thenReturn("key1", "key2");
            when(s3Service.uploadImage(any(), anyString())).thenReturn("url1", "url2");
            when(pictureRepository.save(any(Picture.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            List<Picture> result = pictureService.uploadPictureListToS3AndDB(fileList, FileType.PROFILE);

            // then
            assertThat(result).hasSize(2);
            verify(s3Service, times(2)).generateObjectKey(anyString(), any(FileType.class));
            verify(s3Service, times(2)).uploadImage(any(), anyString());
            verify(pictureRepository, times(2)).save(any(Picture.class));
            verify(s3Service, never()).deleteImage(anyString());
        }

        @Test
        @DisplayName("실패 및 롤백: DB 저장 실패 시 S3 업로드가 롤백된다")
        void upload_Fail_And_Rollback_On_DbError() {
            // given
            when(s3Service.generateObjectKey(anyString(), any(FileType.class))).thenReturn("key1");
            when(s3Service.uploadImage(any(), anyString())).thenReturn("url1");
            // pictureRepository.save()가 호출되는 즉시 예외를 던지도록 설정
            when(pictureRepository.save(any(Picture.class))).thenThrow(new RuntimeException("DB 강제 에러"));

            // when & then
            assertThatThrownBy(() -> pictureService.uploadPictureListToS3AndDB(fileList, FileType.PROFILE))
                    .isInstanceOf(CustomException.class);

            // then: 롤백 로직 검증
            verify(s3Service, times(1)).uploadImage(any(), anyString());
            verify(s3Service, times(1)).uploadImage(file1, "key1");

            verify(pictureRepository, times(1)).save(any());

            verify(s3Service, times(1)).deleteImage(anyString());
            verify(s3Service, times(1)).deleteImage("key1");

            verify(s3Service, never()).generateObjectKey(eq("img2.png"), any());
        }
    }

    @Nested
    @DisplayName("사진 업로드 (수동 키 지정) 테스트")
    class UploadWithManualKeyTests {
        private MockMultipartFile file1, file2;
        private List<PictureUploadParameters> params;

        @BeforeEach
        void setUp() {
            file1 = new MockMultipartFile("f1", "img1.jpg", "image/jpeg", "c1".getBytes());
            file2 = new MockMultipartFile("f2", "img2.png", "image/png", "c2".getBytes());
            params = List.of(
                    new PictureUploadParameters(file1, "manual/key1.jpg"),
                    new PictureUploadParameters(file2, "manual/key2.png")
            );
        }

        @Test
        @DisplayName("성공: 모든 파일이 지정된 키로 정상 업로드 및 저장된다")
        void upload_Success() {
            // given
            when(s3Service.uploadImage(any(), anyString())).thenReturn("url1", "url2");
            when(pictureRepository.save(any(Picture.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            List<Picture> result = pictureService.uploadPictureListToS3AndDB(params);

            // then
            assertThat(result).hasSize(2);
            verify(s3Service, never()).generateObjectKey(anyString(), any(FileType.class));
            verify(s3Service, times(1)).uploadImage(file1, "manual/key1.jpg");
            verify(s3Service, times(1)).uploadImage(file2, "manual/key2.png");
            verify(pictureRepository, times(2)).save(any(Picture.class));
        }

        @Test
        @DisplayName("실패 및 롤백: S3 업로드 실패 시 이전 작업이 롤백된다")
        void upload_Fail_And_Rollback_On_S3Error() {
            // given
            when(s3Service.uploadImage(file1, "manual/key1.jpg")).thenReturn("url1");
            when(s3Service.uploadImage(file2, "manual/key2.png")).thenThrow(new S3UploadFailedException("S3 에러"));
            when(pictureRepository.save(any(Picture.class))).thenAnswer(inv -> inv.getArgument(0));

            // when & then
            assertThatThrownBy(() -> pictureService.uploadPictureListToS3AndDB(params))
                    .isInstanceOf(CustomException.class);

            // then
            verify(pictureRepository, times(1)).save(any()); // 첫 번째 DB 저장은 성공
            verify(s3Service, times(1)).deleteImage("manual/key1.jpg"); // 성공했던 첫 번째 S3 업로드 롤백
        }
    }


    @Nested
    @DisplayName("사진 삭제 (deletePictureFromS3AndDB) 테스트")
    class DeletePictureTests {

        @Test
        @DisplayName("성공 케이스: S3 이동과 DB 업데이트가 모두 정상적으로 수행된다")
        void delete_Success() {
            // given: Mock 객체들의 정상 동작 설정
            // s3Service.extractObjectKeyFromUrl()가 호출되면 "uploads/..." 형태의 키를 반환하도록 설정
            when(s3Service.extractObjectKeyFromUrl(picture1.getPictureUrl())).thenReturn("uploads/pic1.jpg");
            when(s3Service.extractObjectKeyFromUrl(picture2.getPictureUrl())).thenReturn("uploads/pic2.png");

            // s3Service.moveImageToTrash()는 아무 예외도 발생시키지 않음 (성공)
            doNothing().when(s3Service).moveImageToTrash(anyString());

            // pictureRepository.save()는 전달된 객체를 그대로 반환
            when(pictureRepository.save(any(Picture.class))).thenAnswer(inv -> inv.getArgument(0));

            // when: 테스트 대상 메서드 호출
            pictureService.deletePictureFromS3AndDB(pictureList);

            // then: 결과 검증
            // S3의 moveImageToTrash가 각 사진에 대해 한 번씩, 총 2번 호출되었는지 확인
            verify(s3Service, times(1)).moveImageToTrash("uploads/pic1.jpg");
            verify(s3Service, times(1)).moveImageToTrash("uploads/pic2.png");

            // DB의 save가 각 사진에 대해 한 번씩, 총 2번 호출되었는지 확인
            verify(pictureRepository, times(2)).save(any(Picture.class));

            // 롤백 메서드는 절대 호출되지 않았는지 확인
            verify(s3Service, never()).restoreImageFromTrash(anyString());
        }

        @Test
        @DisplayName("실패 및 롤백 케이스 1: S3 파일 이동 중 예외가 발생한다")
        void delete_Fail_When_S3MoveFails() {
            // given: 두 번째 파일 이동 시 예외를 발생시키도록 설정
            when(s3Service.extractObjectKeyFromUrl(picture1.getPictureUrl())).thenReturn("uploads/pic1.jpg");
            when(s3Service.extractObjectKeyFromUrl(picture2.getPictureUrl())).thenReturn("uploads/pic2.png");

            // 첫 번째 파일은 성공
            doNothing().when(s3Service).moveImageToTrash("uploads/pic1.jpg");
            // 두 번째 파일 이동 시 S3UploadFailedException 발생
            doThrow(new S3UploadFailedException("S3 강제 에러")).when(s3Service).moveImageToTrash("uploads/pic2.png");

            // when & then: 예외 발생을 검증
            assertThatThrownBy(() -> pictureService.deletePictureFromS3AndDB(pictureList))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.PICTURE_DELETE_FAILED.getMessage());

            // then: 롤백 로직 검증
            // S3 moveImageToTrash는 2번 시도됨 (1번 성공, 1번 실패)
            verify(s3Service, times(2)).moveImageToTrash(anyString());

            // S3 restoreImageFromTrash는 성공했던 첫 번째 파일에 대해서만 1번 호출되어야 함
            verify(s3Service, times(1)).restoreImageFromTrash("uploads/pic1.jpg");
            verify(s3Service, never()).restoreImageFromTrash("uploads/pic2.png");

            // S3 작업이 중간에 실패했으므로 DB 작업(save)은 전혀 호출되지 않아야 함
            verify(pictureRepository, never()).save(any(Picture.class));
        }

        @Test
        @DisplayName("실패 및 롤백 케이스 2: DB 업데이트 중 예외가 발생한다")
        void delete_Fail_When_DbUpdateFails() {
            // given: S3 작업은 모두 성공하지만, DB 저장 시 예외 발생
            // "uploads/"가 시작되는 위치부터 잘라 실제 서비스와 동일하게 동작
            when(s3Service.extractObjectKeyFromUrl(anyString()))
                    .thenAnswer(inv -> {
                        String url = inv.getArgument(0).toString();
                        int idx = url.indexOf("uploads/");
                        return idx >= 0 ? url.substring(idx) : url;
                    });
            doNothing().when(s3Service).moveImageToTrash(anyString());

            // pictureRepository.save()가 호출되면 RuntimeException 발생
            when(pictureRepository.save(any(Picture.class))).thenThrow(new RuntimeException("DB 강제 에러"));

            // when & then: 예외 발생을 검증
            assertThatThrownBy(() -> pictureService.deletePictureFromS3AndDB(pictureList))
                    .isInstanceOf(CustomException.class);

            // then: 롤백 로직 검증
            // S3 moveImageToTrash는 두 파일 모두에 대해 성공적으로 호출되었음 (총 2번)
            verify(s3Service, times(2)).moveImageToTrash(anyString());

            // DB 작업이 실패했으므로, 성공했던 S3 작업 2건 모두 롤백(복원)되어야 함
            verify(s3Service, times(2)).restoreImageFromTrash(anyString());
            verify(s3Service, times(1)).restoreImageFromTrash("uploads/pic1.jpg");
            verify(s3Service, times(1)).restoreImageFromTrash("uploads/pic2.png");
        }

        @Test
        @DisplayName("롤백 중 S3 복원 실패 시, 에러를 로깅하고 계속 진행한다")
        void delete_Rollback_Continues_On_S3RestoreFailure() {
            // given: S3 이동은 성공, DB 저장은 실패, S3 롤백(복원) 중에도 실패
            when(s3Service.extractObjectKeyFromUrl(anyString()))
                    .thenAnswer(inv -> {
                        String url = inv.getArgument(0).toString();
                        int idx = url.indexOf("uploads/");
                        return idx >= 0 ? url.substring(idx) : url;
                    });
            doNothing().when(s3Service).moveImageToTrash(anyString());
            when(pictureRepository.save(any(Picture.class))).thenThrow(new RuntimeException("DB 강제 에러"));

            // 롤백 시, 첫 번째 파일 복원에서 예외를 던지도록 설정
            doThrow(new S3UploadFailedException("S3 복원 강제 에러")).when(s3Service).restoreImageFromTrash("uploads/pic1.jpg");
            // 두 번째 파일 복원은 성공
            doNothing().when(s3Service).restoreImageFromTrash("uploads/pic2.png");

            // when & then: 최종적으로는 CustomException이 발생해야 함
            assertThatThrownBy(() -> pictureService.deletePictureFromS3AndDB(pictureList))
                    .isInstanceOf(CustomException.class);

            // then: 롤백 로직 검증
            // 두 파일 모두에 대해 restoreImageFromTrash가 호출되었는지 확인 (첫 번째는 실패, 두 번째는 성공)
            verify(s3Service, times(1)).restoreImageFromTrash("uploads/pic1.jpg");
            verify(s3Service, times(1)).restoreImageFromTrash("uploads/pic2.png");
        }

        @Test
        @DisplayName("URL에서 ObjectKey 추출 실패 시, 해당 파일은 무시하고 진행한다")
        void delete_Ignores_File_If_ObjectKey_Is_Null() {
            // given
            when(s3Service.extractObjectKeyFromUrl(picture1.getPictureUrl())).thenReturn("uploads/pic1.jpg");
            // picture2에 대해서는 null을 반환하도록 설정
            when(s3Service.extractObjectKeyFromUrl(picture2.getPictureUrl())).thenReturn(null);

            // when
            pictureService.deletePictureFromS3AndDB(pictureList);

            // then
            // moveImageToTrash는 picture1에 대해서만 1번 호출되어야 함
            verify(s3Service, times(1)).moveImageToTrash("uploads/pic1.jpg");
            // picture2에 대해서는 호출되지 않아야 함
            verify(s3Service, never()).moveImageToTrash(null);
        }
    }

    @Nested
    @DisplayName("단일 사진 업로드 (uploadPictureToS3AndDB) 테스트")
    class UploadSinglePictureTests {

        private MockMultipartFile singleFile;
        private Picture expectedPicture;

        @BeforeEach
        void setUp() {
            singleFile = new MockMultipartFile("profile", "my-profile.jpg", "image/jpeg", "image_content".getBytes());
            expectedPicture = Picture.builder().id(1L).pictureName("my-profile.jpg").pictureUrl("http://s3.com/key/my-profile.jpg").build();
        }

        @Test
        @DisplayName("성공: 단일 파일이 정상적으로 업로드 및 저장된다")
        void upload_SingleFile_Success() {
            // given
            // uploadPictureToS3AndDB가 내부적으로 호출하는 uploadPictureListToS3AndDB의 의존성을 Mocking
            when(s3Service.generateObjectKey(anyString(), any(FileType.class))).thenReturn("some-key");
            when(s3Service.uploadImage(any(MultipartFile.class), anyString())).thenReturn("some-url");
            when(pictureRepository.save(any(Picture.class))).thenReturn(expectedPicture);

            // when
            Picture result = pictureService.uploadPictureToS3AndDB(singleFile, FileType.PROFILE);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(expectedPicture.getId());
            assertThat(result.getPictureName()).isEqualTo(expectedPicture.getPictureName());

            // 내부적으로 list 처리 메소드를 호출했는지, 그리고 그 의존성들이 1번씩만 호출되었는지 검증
            verify(s3Service, times(1)).uploadImage(eq(singleFile), anyString());
            verify(pictureRepository, times(1)).save(any(Picture.class));
        }

        @Test
        @DisplayName("실패: 업로드할 파일이 비어있으면 CustomException(PICTURE_IS_EMPTY)을 던진다")
        void upload_SingleFile_Fail_WhenFileIsEmpty() {
            // given
            MockMultipartFile emptyFile = new MockMultipartFile("profile", "empty.jpg", "image/jpeg", new byte[0]);

            // when & then
            assertThatThrownBy(() -> pictureService.uploadPictureToS3AndDB(emptyFile, FileType.PROFILE))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode") // CustomException의 errorCode 필드 검증
                    .isEqualTo(ErrorCode.PICTURE_IS_EMPTY);

            // 실패했으므로 S3나 DB와 상호작용이 전혀 없었는지 확인
            verify(s3Service, never()).uploadImage(any(), anyString());
            verify(pictureRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패[엣지케이스]: 내부 로직에서 빈 리스트를 반환하면 CustomException(PICTURE_UPLOAD_FAILED)을 던진다")
        void upload_SingleFile_Fail_WhenInternalMethodReturnsEmptyList() {
            // given
            // 이 테스트는 uploadPictureToS3AndDB 자체의 방어로직을 테스트하기 위함
            // Spy를 사용하여 실제 객체의 일부 메소드만 Mocking
            PictureService spiedPictureService = spy(new PictureService(s3Service, pictureRepository));
            doReturn(Collections.emptyList()).when(spiedPictureService).uploadPictureListToS3AndDB(anyList(), any(FileType.class));

            // when & then
            assertThatThrownBy(() -> spiedPictureService.uploadPictureToS3AndDB(singleFile, FileType.PROFILE))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PICTURE_UPLOAD_FAILED);
        }
    }
}