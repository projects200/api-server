package com.project200.undabang.common.service;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.web.exception.CustomException;
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
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@Testcontainers
@SpringBootTest
class PictureUtilsTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:latest"))
            .withServices(LocalStackContainer.Service.S3)
            .withEnv("DEFAULT_REGION", "ap-northeast-2");

    @Autowired
    private PictureUtils pictureUtils;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private PictureRepository pictureRepository;

    @Value("${app.s3.bucket-name}")
    private String BUCKET_NAME;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.s3.endpoint", () -> localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
    }

    @BeforeEach
    void setUpBucket() {
        // 컨테이너가 완전히 시작된 후에만 버킷 생성 시도
        S3Client s3Client = S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .region(Region.of(localStack.getRegion()))
                .build();
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        } catch (S3Exception e) {
            // 이미 존재하면 무시
        }
        s3Client.close();
    }

    @Nested
    @DisplayName("사진 업로드")
    class UploadPictures {

        @Test
        @DisplayName("여러 파일을 S3와 DB에 정상적으로 업로드")
        void uploadPicturesToS3AndDB_success() {
            UUID testUserId = UUID.randomUUID();
            MockMultipartFile file1 = new MockMultipartFile("images", "test1.jpg", "image/jpeg", "content1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("images", "test2.png", "image/png", "content2".getBytes());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);

                List<Picture> pictures = pictureUtils.uploadPicturesToS3AndDB(List.of(file1, file2), FileType.EXERCISE);

                assertThat(pictures).hasSize(2);
                for (Picture picture : pictures) {
                    assertThat(picture.getPictureUrl()).isNotBlank();
                    assertThat(s3Service.isFileExists(s3Service.extractObjectKeyFromUrl(picture.getPictureUrl()))).isTrue();
                    assertThat(pictureRepository.findById(picture.getId())).isPresent();
                }
            }
        }

        @Test
        @DisplayName("파일 업로드 중 예외 발생 시 롤백")
        void uploadPicturesToS3AndDB_rollbackOnException() {
            UUID testUserId = UUID.randomUUID();
            MockMultipartFile file1 = new MockMultipartFile("images", "ok.jpg", "image/jpeg", "ok".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("images", "fail.jpg", "image/jpeg", new byte[0]);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);

                assertThatThrownBy(() -> pictureUtils.uploadPicturesToS3AndDB(List.of(file1, file2), FileType.EXERCISE))
                        .isInstanceOf(CustomException.class);

                String objectKey = s3Service.generateObjectKey(file1.getOriginalFilename(), FileType.EXERCISE);
                assertThat(s3Service.isFileExists(objectKey)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("사진 삭제")
    class DeletePictures {

        @Test
        @DisplayName("S3와 DB에서 정상적으로 soft delete 처리")
        void deletePictureFromS3AndDB_success() {
            UUID testUserId = UUID.randomUUID();
            MockMultipartFile file = new MockMultipartFile("images", "delete_test.jpg", "image/jpeg", "delete-content".getBytes());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);

                List<Picture> pictures = pictureUtils.uploadPicturesToS3AndDB(List.of(file), FileType.EXERCISE);
                Picture picture = pictures.get(0);
                String objectKey = s3Service.extractObjectKeyFromUrl(picture.getPictureUrl());

                pictureUtils.deletePictureFromS3AndDB(pictures);

                assertThat(s3Service.isFileExists(objectKey)).isFalse();
                // soft delete된 row는 findById로 조회 불가할 수 있음 (엔티티 @Where 등 주의)
                Picture deleted = pictureRepository.findById(picture.getId()).orElseThrow();
                assertThat(deleted.getPictureDeletedAt()).isNotNull();
            }
        }
    }
}