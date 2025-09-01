package com.project200.undabang.common.service;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.entity.dto.PictureUploadParameters;
import com.project200.undabang.common.entity.dto.PictureUploadWithKeyParameters;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.S3UploadFailedException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest
public class PictureServiceIntegrationTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:latest"))
            .withServices(LocalStackContainer.Service.S3)
            .withEnv("DEFAULT_REGION", "ap-northeast-2");
    @Autowired
    private PictureService pictureService;
    @Autowired
    private PictureRepository pictureRepository;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private S3Client s3Client;
    @Autowired
    private EntityManager entityManager;
    @Value("${app.s3.bucket-name}")
    private String BUCKET_NAME;
    @MockitoSpyBean
    private S3Service s3ServiceSpy;
    @MockitoSpyBean
    private PictureRepository pictureRepositorySpy;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.s3.endpoint", () -> localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
    }

    @BeforeEach
    void setUp() {
        reset(s3ServiceSpy, pictureRepositorySpy);

        pictureRepository.deleteAllInBatch();

        S3Client s3 = S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .region(Region.of(localStack.getRegion()))
                .build();
        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        } catch (S3Exception e) { /* 이미 존재하면 무시 */ }

        s3.listObjectsV2(ListObjectsV2Request.builder().bucket(BUCKET_NAME).build())
                .contents().forEach(obj -> s3Service.deleteImage(obj.key()));
        s3.listObjectsV2(ListObjectsV2Request.builder().bucket(BUCKET_NAME).prefix("trash/").build())
                .contents().forEach(obj -> s3Service.deleteImage(obj.key()));
    }


    @Nested
    @DisplayName("사진 업로드 통합 테스트 (uploadPictureListToS3AndDB)")
    class UploadPictureIntegrationTests {
        private MockMultipartFile file1;
        private MockMultipartFile file2;
        private PictureUploadParameters params;

        @BeforeEach
        void setUp() {
            file1 = new MockMultipartFile("files", "upload-success.jpg", "image/jpeg", "success content".getBytes());
            file2 = new MockMultipartFile("files", "upload-fail.png", "image/png", "fail content".getBytes());
            params = new PictureUploadParameters(List.of(file1, file2), FileType.PROFILE);
        }

        @Test
        @DisplayName("성공 시나리오: S3에 파일이 업로드되고 DB에 Picture 데이터가 저장된다")
        void upload_Success_Integration() {
            try (MockedStatic<UserContextHolder> mockedUser = mockStatic(UserContextHolder.class)) {
                mockedUser.when(UserContextHolder::getUserId).thenReturn(UUID.randomUUID());

                List<Picture> result = pictureService.uploadPictureListToS3AndDB(params);

                assertThat(result).hasSize(2);
                List<Picture> savedPictures = pictureRepository.findAll();
                assertThat(savedPictures).hasSize(2);
                assertThat(savedPictures.get(0).getPictureName()).isEqualTo("upload-success.jpg");
                assertThat(savedPictures.get(1).getPictureName()).isEqualTo("upload-fail.png");

                String objectKey1 = s3Service.extractObjectKeyFromUrl(savedPictures.get(0).getPictureUrl());
                String objectKey2 = s3Service.extractObjectKeyFromUrl(savedPictures.get(1).getPictureUrl());
                assertThat(s3Service.isFileExists(objectKey1)).isTrue();
                assertThat(s3Service.isFileExists(objectKey2)).isTrue();
            }
        }

        @Test
        @DisplayName("성공[수동 키 지정]: 미리 지정된 objectKey로 S3에 파일이 업로드되고 DB에 데이터가 저장된다")
        void upload_WithPredefinedKey_Success_Integration() {
            // given: 업로드할 파일과 미리 지정된 objectKey 준비
            MockMultipartFile file1 = new MockMultipartFile("files", "predefined1.txt", "text/plain", "predefined key content 1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("files", "predefined2.json", "application/json", "{}".getBytes());

            List<String> predefinedKeys = List.of("manual/uploads/file1.txt", "manual/uploads/data.json");
            PictureUploadWithKeyParameters params = new PictureUploadWithKeyParameters(List.of(file1, file2), predefinedKeys);

            // 이 메서드는 UserContextHolder를 사용하지 않으므로 모킹이 필요 없음

            // when: 테스트 대상 오버로딩 메서드 호출
            List<Picture> result = pictureService.uploadPictureListToS3AndDB(params);

            // then: 결과 검증
            // 1. DB 검증: 2개의 Picture 엔티티가 저장되었는지 확인
            assertThat(result).hasSize(2);
            List<Picture> savedPictures = pictureRepository.findAll();
            assertThat(savedPictures).hasSize(2);

            // 2. S3 검증: 우리가 직접 지정한 objectKey로 파일이 실제로 업로드되었는지 확인
            assertThat(s3Service.isFileExists("manual/uploads/file1.txt")).isTrue();
            assertThat(s3Service.isFileExists("manual/uploads/data.json")).isTrue();

            // 3. DB에 저장된 URL이 지정된 objectKey를 포함하는지 확인
            String url1 = savedPictures.stream()
                    .filter(p -> p.getPictureName().equals("predefined1.txt"))
                    .findFirst().get().getPictureUrl();
            String url2 = savedPictures.stream()
                    .filter(p -> p.getPictureName().equals("predefined2.json"))
                    .findFirst().get().getPictureUrl();

            assertThat(url1).endsWith("manual/uploads/file1.txt");
            assertThat(url2).endsWith("manual/uploads/data.json");
        }
    }

    @Nested
    @DisplayName("사진 삭제 통합 테스트")
    class DeletePictureIntegrationTests {

        @Test
        @DisplayName("성공: S3 파일이 휴지통으로 이동하고 DB 데이터가 soft-delete 된다")
        void delete_Success_Integration() {
            // given: 테스트 데이터 생성
            // 1. Picture 엔티티 2개를 DB에 저장
            Picture pic1 = pictureRepository.save(Picture.builder().pictureUrl("http://s3.com/uploads/success1.jpg").build());
            Picture pic2 = pictureRepository.save(Picture.builder().pictureUrl("http://s3.com/uploads/success2.png").build());

            // 2. 해당 Picture에 매칭되는 실제 파일을 LocalStack S3에 업로드
            s3Service.uploadImage(new MockMultipartFile("f", "s1.jpg", "image/jpeg", "c".getBytes()), "uploads/success1.jpg");
            s3Service.uploadImage(new MockMultipartFile("f", "s2.png", "image/png", "c".getBytes()), "uploads/success2.png");

            List<Picture> pictureList = List.of(pic1, pic2);

            // when: 테스트 대상 메서드 호출
            pictureService.deletePictureFromS3AndDB(pictureList);

            // then: 결과 검증
            // 1. DB 검증: deleted_at 컬럼이 null이 아닌지 확인
            Picture deletedPic1 = pictureRepository.findById(pic1.getId()).get();
            Picture deletedPic2 = pictureRepository.findById(pic2.getId()).get();
            assertThat(deletedPic1.getPictureDeletedAt()).isNotNull();
            assertThat(deletedPic2.getPictureDeletedAt()).isNotNull();

            // 2. S3 검증: 원본 파일은 없고, 휴지통에 파일이 있는지 확인
            assertThat(s3Service.isFileExists("uploads/success1.jpg")).isFalse();
            assertThat(s3Service.isFileExists("trash/uploads/success1.jpg")).isTrue();
            assertThat(s3Service.isFileExists("uploads/success2.png")).isFalse();
            assertThat(s3Service.isFileExists("trash/uploads/success2.png")).isTrue();
        }

        @Test
        @DisplayName("실패 및 롤백: S3 이동 실패 시 DB 트랜잭션도 롤백된다")
        void delete_Fail_And_Rollback_Integration() {
            // given: 테스트 데이터 생성
            Picture pic1 = pictureRepository.save(Picture.builder().pictureUrl("http://s3.com/uploads/rollback1.jpg").build());
            Picture pic2 = pictureRepository.save(Picture.builder().pictureUrl("http://s3.com/uploads/fail-on-this.png").build()); // 여기서 실패
            s3Service.uploadImage(new MockMultipartFile("f", "r1.jpg", "image/jpeg", "c".getBytes()), "uploads/rollback1.jpg");
            s3Service.uploadImage(new MockMultipartFile("f", "fail.png", "image/png", "c".getBytes()), "uploads/fail-on-this.png");

            List<Picture> pictureList = List.of(pic1, pic2);

            // S3Service의 실제 객체를 사용하되, 특정 키에 대해서만 예외를 발생시키도록 SpyBean 설정
            doThrow(new S3UploadFailedException("강제 S3 에러"))
                    .when(s3ServiceSpy).moveImageToTrash("uploads/fail-on-this.png");

            // when & then: 예외 발생 검증
            assertThatThrownBy(() -> pictureService.deletePictureFromS3AndDB(pictureList))
                    .isInstanceOf(CustomException.class);

            // then: 롤백 결과 검증
            // 1. DB 검증: 트랜잭션이 롤백되어 deleted_at이 여전히 null인지 확인
            Picture notDeletedPic1 = pictureRepository.findById(pic1.getId()).get();
            Picture notDeletedPic2 = pictureRepository.findById(pic2.getId()).get();
            assertThat(notDeletedPic1.getPictureDeletedAt()).isNull();
            assertThat(notDeletedPic2.getPictureDeletedAt()).isNull();

            // 2. S3 검증: 수동 롤백이 동작하여 모든 파일이 원본 위치에 그대로 있는지 확인
            assertThat(s3Service.isFileExists("uploads/rollback1.jpg")).isTrue();
            assertThat(s3Service.isFileExists("trash/uploads/rollback1.jpg")).isFalse();
            assertThat(s3Service.isFileExists("uploads/fail-on-this.png")).isTrue();
        }
    }
}
