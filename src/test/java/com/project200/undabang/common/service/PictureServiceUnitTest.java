package com.project200.undabang.common.service;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.entity.dto.PictureUploadParameters;
import com.project200.undabang.common.entity.dto.PictureUploadWithKeyParameters;
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
    @DisplayName("사진 업로드 (uploadPictureListToS3AndDB) 테스트")
    class UploadPictureTests {

        private MockMultipartFile file1;
        private MockMultipartFile file2;
        private PictureUploadParameters params;

        @BeforeEach
        void setUp() {
            file1 = new MockMultipartFile("files", "image1.jpg", "image/jpeg", "image1 content".getBytes());
            file2 = new MockMultipartFile("files", "image2.png", "image/png", "image2 content".getBytes());
            params = new PictureUploadParameters(List.of(file1, file2), FileType.PROFILE);
        }

        @Test
        @DisplayName("성공 케이스: 모든 파일이 정상적으로 업로드되고 DB에 저장된다")
        void upload_Success() {
            // given
            when(s3Service.generateObjectKey(anyString(), any(FileType.class)))
                    .thenReturn("uploads/key1.jpg", "uploads/key2.png");
            when(s3Service.uploadImage(any(MockMultipartFile.class), anyString()))
                    .thenReturn("http://s3.com/uploads/key1.jpg", "http://s3.com/uploads/key2.png");
            when(pictureRepository.save(any(Picture.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            List<Picture> result = pictureService.uploadPictureListToS3AndDB(params);

            // then
            assertThat(result).hasSize(2);
            verify(s3Service, times(2)).generateObjectKey(anyString(), any(FileType.class));
            verify(s3Service, times(2)).uploadImage(any(MockMultipartFile.class), anyString());
            verify(pictureRepository, times(2)).save(any(Picture.class));
            // S3 롤백(삭제) 메서드는 호출되지 않아야 함
            verify(s3Service, never()).deleteImage(anyString());
        }

        @Test
        @DisplayName("실패 및 롤백 케이스: S3 업로드 중 예외가 발생한다")
        void upload_Fail_When_S3UploadFails() {
            // given: 두 번째 파일 업로드 시 예외 발생 설정
            when(s3Service.generateObjectKey(anyString(), any(FileType.class)))
                    .thenReturn("uploads/key1.jpg", "uploads/key2.png");
            // 첫 번째 파일은 성공, URL 반환
            when(s3Service.uploadImage(eq(file1), eq("uploads/key1.jpg")))
                    .thenReturn("http://s3.com/uploads/key1.jpg");
            // 두 번째 파일은 실패, 예외 발생
            when(s3Service.uploadImage(eq(file2), eq("uploads/key2.png")))
                    .thenThrow(new S3UploadFailedException("S3 업로드 강제 에러"));

            // 첫 번째 파일은 DB 저장 성공
            when(pictureRepository.save(any(Picture.class))).thenAnswer(inv -> inv.getArgument(0));

            // when & then
            assertThatThrownBy(() -> pictureService.uploadPictureListToS3AndDB(params))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.PICTURE_UPLOAD_FAILED.getMessage());

            // then: 롤백 로직 검증
            // S3 업로드는 2번 시도됨
            verify(s3Service, times(2)).uploadImage(any(MockMultipartFile.class), anyString());
            // DB 저장은 첫 번째 파일에 대해서만 1번 성공함
            verify(pictureRepository, times(1)).save(any(Picture.class));

            // 성공했던 첫 번째 파일에 대해 S3 롤백(영구 삭제)이 1번 호출되어야 함
            verify(s3Service, times(1)).deleteImage("uploads/key1.jpg");
        }

        @Test
        @DisplayName("실패 및 롤백 케이스: DB 저장 중 예외가 발생한다")
        void upload_Fail_When_DbSaveFails() {
            // given: S3 업로드는 모두 성공, DB 저장은 두 번째 파일에서 실패
            when(s3Service.generateObjectKey(anyString(), any(FileType.class)))
                    .thenReturn("uploads/key1.jpg", "uploads/key2.png");
            when(s3Service.uploadImage(any(MockMultipartFile.class), anyString()))
                    .thenReturn("http://s3.com/uploads/key1.jpg", "http://s3.com/uploads/key2.png");


            // 첫 번째 파일 저장은 성공
            when(pictureRepository.save(any(Picture.class)))
                    .thenAnswer(inv -> inv.getArgument(0)) // 첫 번째 호출
                    .thenThrow(new RuntimeException("DB 저장 강제 에러")); // 두 번째 호출

            // when & then
            assertThatThrownBy(() -> pictureService.uploadPictureListToS3AndDB(params))
                    .isInstanceOf(CustomException.class);

            // then: 롤백 로직 검증
            // 성공적으로 S3에 업로드된 두 파일 모두에 대해 롤백(삭제)이 호출되어야 함
            verify(s3Service, times(2)).deleteImage(anyString());
            verify(s3Service, times(1)).deleteImage("uploads/key1.jpg");
            verify(s3Service, times(1)).deleteImage("uploads/key2.png");
        }
    }

    @Nested
    @DisplayName("사전 정의된 키로 사진 업로드 (uploadPictureListToS3AndDB with Keys) 테스트")
    class UploadPictureWithKeysTests {

        private MockMultipartFile file1;
        private MockMultipartFile file2;
        private List<String> objectKeys;
        private PictureUploadWithKeyParameters params;

        @BeforeEach
        void setUp() {
            file1 = new MockMultipartFile("files", "image1.jpg", "image/jpeg", "image1 content".getBytes());
            file2 = new MockMultipartFile("files", "image2.png", "image/png", "image2 content".getBytes());
            objectKeys = List.of("preset/key1.jpg", "preset/key2.png");
            params = new PictureUploadWithKeyParameters(List.of(file1, file2), objectKeys);
        }

        @Test
        @DisplayName("성공 케이스: 모든 파일이 사전 정의된 키로 정상 업로드되고 DB에 저장된다")
        void uploadWithKeys_Success() {
            // given
            when(s3Service.uploadImage(any(MockMultipartFile.class), anyString()))
                    .thenReturn("http://s3.com/preset/key1.jpg", "http://s3.com/preset/key2.png");
            when(pictureRepository.save(any(Picture.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            List<Picture> result = pictureService.uploadPictureListToS3AndDB(params);

            // then
            assertThat(result).hasSize(2);
            // generateObjectKey는 호출되지 않아야 함
            verify(s3Service, never()).generateObjectKey(anyString(), any(FileType.class));
            // uploadImage는 미리 정의된 키로 2번 호출되어야 함
            verify(s3Service, times(1)).uploadImage(file1, "preset/key1.jpg");
            verify(s3Service, times(1)).uploadImage(file2, "preset/key2.png");
            verify(pictureRepository, times(2)).save(any(Picture.class));
            verify(s3Service, never()).deleteImage(anyString()); // 롤백 없음
        }

        @Test
        @DisplayName("실패 케이스 1: 파일과 키의 개수가 맞지 않으면 예외가 발생한다")
        void uploadWithKeys_Fail_When_SizeMismatch() {
            // given: 파일은 2개, 키는 1개인 파라미터 생성
            PictureUploadWithKeyParameters mismatchParams = new PictureUploadWithKeyParameters(List.of(file1, file2), List.of("only-one-key.jpg"));

            // when & then
            assertThatThrownBy(() -> pictureService.uploadPictureListToS3AndDB(mismatchParams))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());

            // then: 예외 발생 전에 아무런 S3나 DB 작업이 없어야 함
            verify(s3Service, never()).uploadImage(any(), any());
            verify(pictureRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 및 롤백 케이스 2: S3 업로드 중 예외가 발생한다")
        void uploadWithKeys_Fail_And_Rollback_On_S3Error() {
            // given: 두 번째 파일 업로드 시 예외 발생
            when(s3Service.uploadImage(file1, "preset/key1.jpg")).thenReturn("http://s3.com/preset/key1.jpg");
            when(s3Service.uploadImage(file2, "preset/key2.png")).thenThrow(new S3UploadFailedException("S3 강제 에러"));
            when(pictureRepository.save(any(Picture.class))).thenAnswer(inv -> inv.getArgument(0));

            // when & then
            assertThatThrownBy(() -> pictureService.uploadPictureListToS3AndDB(params))
                    .isInstanceOf(CustomException.class);

            // then: 롤백 로직 검증
            // S3 업로드는 2번 시도됨
            verify(s3Service, times(2)).uploadImage(any(), any());
            // DB 저장은 첫 번째 파일에 대해서만 1번 성공함
            verify(pictureRepository, times(1)).save(any());

            // 롤백 핸들러가 handleUploadFailureForKeys를 호출하도록 수정했으므로,
            // 성공했던 첫 번째 파일의 objectKey로 롤백(삭제)이 호출되어야 함
            verify(s3Service, times(1)).deleteImage("preset/key1.jpg");
        }

        @Test
        @DisplayName("실패 및 롤백 케이스 3: DB 저장 중 예외가 발생한다")
        void uploadWithKeys_Fail_And_Rollback_On_DbError() {
            // given: S3 업로드는 성공하나, DB 저장은 첫 번째 시도부터 실패하도록 설정
            when(s3Service.uploadImage(any(MockMultipartFile.class), anyString()))
                    .thenReturn("http://s3.com/preset/key1.jpg"); // uploadImage는 한 번만 호출될 것이므로 한 번만 설정

            when(pictureRepository.save(any(Picture.class)))
                    .thenThrow(new RuntimeException("DB 강제 에러"));

            // when & then: 예외 발생 검증
            assertThatThrownBy(() -> pictureService.uploadPictureListToS3AndDB(params))
                    .isInstanceOf(CustomException.class);

            // then: 롤백 로직 검증
            verify(s3Service, times(1)).uploadImage(any(), any());
            verify(s3Service, times(1)).uploadImage(file1, "preset/key1.jpg");

            // 성공한 S3 업로드 1건에 대해서만 롤백(삭제)이 호출되어야 함
            verify(s3Service, times(1)).deleteImage(any());
            verify(s3Service, times(1)).deleteImage("preset/key1.jpg");

            // 두 번째 파일에 대한 업로드나 롤백은 시도되지 않았어야 함
            verify(s3Service, never()).uploadImage(file2, "preset/key2.png");
            verify(s3Service, never()).deleteImage("preset/key2.png");
        }
    }
}