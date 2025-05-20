package com.project200.undabang.common.service;

import com.project200.undabang.common.context.UserContextHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@Testcontainers
@SpringBootTest
class S3ServiceTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:s3-latest"))
            .withServices(LocalStackContainer.Service.S3)
            .withEnv("DEFAULT_REGION", "ap-northeast-2");
    @Autowired
    private S3Service s3Service;
    @Autowired
    private S3Client s3Client;
    @Value("${app.s3.bucket-name}")
    private String BUCKET_NAME;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.s3.endpoint", () -> localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
    }

    @BeforeAll
    static void beforeAll() {
        S3Client s3ClientForSetup = S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .region(Region.of(localStack.getRegion()))
                .build();
        try {
            s3ClientForSetup.createBucket(CreateBucketRequest.builder().bucket("my-test-image-bucket").build());
        } catch (S3Exception e) {
            System.err.println("Bucket creation may have failed (already exists?): " + e.getMessage());
        }
        s3ClientForSetup.close();
    }

    @Test
    @DisplayName("generateObjectKey: 메서드가 올바른 형식의 S3 객체 키를 생성하는지 확인")
    void generateObjectKey_shouldGenerateCorrectFormat() {
        // Given: 테스트에 필요한 값 설정
        UUID testUserId = UUID.randomUUID(); // UUID로 변경
        String originalFilename = "test_image.jpg";
        LocalDate now = LocalDate.now(); // 검증을 위해 현재 날짜 사용
        String expectedYear = String.valueOf(now.getYear());
        String expectedMonth = String.format("%02d", now.getMonthValue());
        String expectedPrefix = String.format("uploads/%s/%s/%s/images/", testUserId, expectedYear, expectedMonth); // testUserId.toString() 사용

        try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
            given(UserContextHolder.getUserId()).willReturn(testUserId);

            // When: 테스트 대상 메서드 호출
            String objectKey = s3Service.generateObjectKey(originalFilename);

            // Then: 결과 검증
            // 생성된 객체 키 형식 검증
            assertThat(objectKey).as("Object key prefix mismatch")
                    .startsWith(expectedPrefix);
            assertThat(objectKey).as("Object key suffix mismatch")
                    .endsWith("_" + originalFilename);

            // UUID 부분 추출 및 유효성 검사
            String pathSegment = objectKey.substring(objectKey.lastIndexOf("/images/") + "/images/".length());
            String uuidPart = pathSegment.substring(0, 36); // UUID는 36자
            assertThatCode(() -> UUID.fromString(uuidPart))
                    .withFailMessage("Object key의 UUID 부분이 유효한 UUID 형식이 아닙니다: " + uuidPart)
                    .doesNotThrowAnyException();
            assertThat(pathSegment).isEqualTo(uuidPart + "_" + originalFilename);
        }
    }

    @Test
    @DisplayName("uploadImage & getPublicUrl (with metadata): 메타데이터와 함께 이미지 업로드 및 공개 URL 반환 테스트")
    void uploadImageAndGetPublicUrlWithMetadata() throws IOException {
        // Given: 테스트 값 및 모킹 설정
        UUID testUserId = UUID.randomUUID(); // 일관성을 위해 UUID로 변경 (UserContextHolder 모킹과 맞춤)
        String originalFilename = "upload_test_with_meta.jpg";
        byte[] content = "Test image content for upload with metadata".getBytes();
        MockMultipartFile multipartFile = new MockMultipartFile(
                "images",
                originalFilename,
                "image/jpg",
                content
        );
        Map<String, String> metadata = Map.of("custom-uploader", "test-user", "source", "unit-test");

        try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
            given(UserContextHolder.getUserId()).willReturn(testUserId);
            String objectKey = s3Service.generateObjectKey(originalFilename);

            // When: 이미지 업로드 메서드 호출
            String publicUrl = s3Service.uploadImage(multipartFile, objectKey, metadata);

            // Then: 결과 검증
            assertThat(publicUrl).isNotNull();
            String expectedUrlPrefix = localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString() + "/" + BUCKET_NAME + "/";
            assertThat(publicUrl)
                    .startsWith(expectedUrlPrefix)
                    .endsWith(objectKey);

            // S3에 객체가 실제로 업로드되었는지, 메타데이터 및 Content-Type 검증
            HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(objectKey)
                    .build());

            assertThat(headObjectResponse.contentType()).isEqualTo("image/jpg");
            assertThat(headObjectResponse.contentLength()).isEqualTo(content.length);
            assertThat(headObjectResponse.metadata()).isNotNull()
                    .containsEntry("custom-uploader", "test-user")
                    .containsEntry("source", "unit-test");

            // 업로드된 객체의 내용 검증
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(BUCKET_NAME).key(objectKey).build());
            assertThat(objectBytes.asByteArray()).isEqualTo(content);
        }
    }

    @Test
    @DisplayName("uploadImage & getPublicUrl (without metadata): 메타데이터 없이 이미지 업로드 및 공개 URL 반환 테스트")
    void uploadImageAndGetPublicUrlWithoutMetadata() throws IOException {
        // Given: 테스트 값 및 모킹 설정
        UUID testUserId = UUID.randomUUID(); // 일관성을 위해 UUID로 변경
        String originalFilename = "upload_test_no_meta.png";
        byte[] content = "Test image content for upload without metadata".getBytes();
        MockMultipartFile multipartFile = new MockMultipartFile(
                "images",
                originalFilename,
                "image/png",
                content
        );
        String objectKey;

        try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
            given(UserContextHolder.getUserId()).willReturn(testUserId);
            objectKey = s3Service.generateObjectKey(originalFilename);

            // When: 메타데이터 없이 이미지 업로드 메서드 호출
            String publicUrl = s3Service.uploadImage(multipartFile, objectKey);

            // Then: 결과 검증
            assertThat(publicUrl).isNotNull();
            String expectedUrlPrefix = localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString() + "/" + BUCKET_NAME + "/";
            assertThat(publicUrl)
                    .startsWith(expectedUrlPrefix)
                    .endsWith(objectKey);

            HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(objectKey)
                    .build());

            assertThat(headObjectResponse.contentType()).isEqualTo("image/png");
            assertThat(headObjectResponse.contentLength()).isEqualTo(content.length);
            assertThat(headObjectResponse.metadata()).isNotNull().isEmpty(); // 메타데이터는 null이 아니고 비어있어야 함

            // 업로드된 객체의 내용 검증
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(BUCKET_NAME).key(objectKey).build());
            assertThat(objectBytes.asByteArray()).isEqualTo(content);
        }
    }

    @Test
    @DisplayName("deleteImage: S3에서 이미지 삭제 테스트")
    void deleteImage_shouldDeleteObjectFromS3() throws IOException {
        // Given: 삭제할 파일 준비 및 모킹 설정
        UUID testUserId = UUID.randomUUID(); // 일관성을 위해 UUID로 변경
        String originalFilename = "file_to_delete.jpeg";
        byte[] content = "This content is to be deleted.".getBytes();
        MockMultipartFile multipartFile = new MockMultipartFile(
                "images",
                originalFilename,
                "image/jpeg",
                content
        );
        String objectKey;

        try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
            given(UserContextHolder.getUserId()).willReturn(testUserId);
            objectKey = s3Service.generateObjectKey(originalFilename);

            // 파일을 먼저 업로드 (Given의 일부)
            s3Service.uploadImage(multipartFile, objectKey);

            // 파일이 존재하는지 사전 확인 (Given의 일부)
            assertThatCode(() -> s3Client.headObject(HeadObjectRequest.builder().bucket(BUCKET_NAME).key(objectKey).build()))
                    .doesNotThrowAnyException();

            // When: 이미지 삭제 메서드 호출
            s3Service.deleteImage(objectKey);

            // Then: 파일이 삭제되었는지 확인 (NoSuchKeyException 발생해야 함)
            assertThatThrownBy(() -> s3Client.headObject(HeadObjectRequest.builder().bucket(BUCKET_NAME).key(objectKey).build()))
                    .isInstanceOf(NoSuchKeyException.class);
        }
    }
}