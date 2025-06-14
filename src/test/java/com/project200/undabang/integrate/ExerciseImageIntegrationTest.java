package com.project200.undabang.integrate;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.service.S3Service;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.member.entity.Member;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
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

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ExerciseImageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private S3Client s3Client;

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:latest"))
            .withServices(LocalStackContainer.Service.S3)
            .withEnv("DEFAULT_REGION", "ap-northeast-2");

    private static String BUCKET_NAME = "my-test-image-bucket";

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
            s3ClientForSetup.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        } catch (S3Exception e) {
            System.err.println("Bucket creation may have failed (already exists?): " + e.getMessage());
        }
        s3ClientForSetup.close();

        URI endpointOverride = localStack.getEndpointOverride(LocalStackContainer.Service.S3);
        String region = localStack.getRegion();
        String accessKey = localStack.getAccessKey();
        String secretKey = localStack.getSecretKey();

        System.out.println("LocalStack S3 endpoint: " + endpointOverride);
        System.out.println("LocalStack S3 region: " + region);
        System.out.println("LocalStack S3 access key: " + accessKey);
        System.out.println("LocalStack S3 secret key: " + secretKey);
        System.out.println("LocalStack S3 bucket name: " + BUCKET_NAME);

        System.out.printf("cli에서 S3 내의 파일을 확인하려면 다음 명령어를 사용하세요: aws s3 ls s3://%s --recursive --endpoint-url %s --region %s%n",
                BUCKET_NAME, endpointOverride, region);
    }

    /* issue: https://github.com/projects200/api-server/issues/142
    * 문제 상황
    * 현재 로직에서는 이미지의 버킷 키 값을 S3 URL에서 앞에 부분을 제거하여 추출하고 있습니다.
    * url 인코딩 문제로 인해 영어가 아닌 파일 이름을 가진 운동 이미지 업로드 시 url이 실제 버킷 키 값과 불일치하는 문제가 발생하였습니다.
    *
    * 해결 방법
    * S3에 원본 파일 이름을 저장하지 않고 uuid.png와 같은 고유한 이름으로 저장하고, 메타 데이터에 원본 파일 이름을 저장하는 방식으로 변경하였습니다.
    * S3에는 `uploads/exercises/{userId}/{year}/{month}/{랜덤 uuid}.{확장자}` 형식으로 저장됩니다.
     */
    @Test
    @DisplayName("url 인코딩 문제로 인해 영어가 아닌 파일 이름을 가진 운동 이미지 업로드 시 S3에서 파일을 찾지 못해 삭제가 안되는 문제 테스트")
    void uploadAndDeleteExerciseImageWithLongKoreanFileName() throws Exception {
        // given
        // 테스트용 회원, 운동 기록 데이터 DB에 저장
        UUID testMemberId = UUID.randomUUID();
        Member testMember = Member.builder()
                .memberId(testMemberId)
                .memberEmail("test@test.com")
                .memberNickname("테스트유저")
                .build();
        em.persist(testMember);

        Exercise testExercise = Exercise.builder()
                .member(testMember)
                .exerciseTitle("테스트용 운동 기록")
                .build();
        em.persist(testExercise);
        Long testExerciseId = testExercise.getId();

        // 테스트용 업로드 이미지 파일 생성
        String fileName = "운동.png";
        MockMultipartFile mockFile = new MockMultipartFile(
                "pictures", // API에서 파일을 받는 파라미터 이름
                fileName,
                MediaType.IMAGE_PNG_VALUE,
                "test image content".getBytes()
        );

        // 파일 업로드
        mockMvc.perform(multipart("/api/v1/exercises/{exerciseId}/pictures", testExerciseId) // 실제 업로드 API 엔드포인트로 변경해야 합니다.
                        .file(mockFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andDo(print())
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.data.exerciseId").value(testExerciseId)
                );


        List<Picture> exercisePictures = em.createQuery(
                "SELECT p FROM Picture p INNER JOIN ExercisePicture ep ON p.id = ep.picture.id WHERE ep.exercise.id = :exerciseId"
                        , Picture.class
                )
                .setParameter("exerciseId", testExerciseId)
                .getResultList();

        // 업로드된 이미지의 ID를 가져옴
        assertThat(exercisePictures).isNotEmpty();
        Picture exercisePicture = exercisePictures.getFirst();
        Long exercisePictureId = exercisePicture.getId();

        // S3에서 파일이 업로드되었는지 확인
        String s3ObjectKey = s3Service.extractObjectKeyFromUrl(exercisePicture.getPictureUrl());
        boolean fileExists = s3Service.isFileExists(s3ObjectKey);
        assertThat(fileExists).isTrue();

        // 업로드 된 S3 파일 메타데이터 확인
        Map<String, String> metadata = s3Client.headObject(builder -> builder.bucket(BUCKET_NAME).key(s3ObjectKey)).metadata();
        assertThat(metadata).containsKey("originalfilename");
        String originalFilenameInMetadata = URLDecoder.decode(metadata.get("originalfilename"), StandardCharsets.UTF_8);
        assertThat(originalFilenameInMetadata).isEqualTo(fileName);

        // when
        // 운동 기록의 이미지를 삭제하는 API 호출
        this.mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/exercises/{exerciseId}/pictures", testExerciseId)
                        .queryParam("pictureIds", String.valueOf(exercisePictureId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andDo(print())
                .andExpect(status().isOk());

        // then
        // S3에서 파일이 삭제되었는지 확인
        boolean fileDeleted = s3Service.isFileExists(s3ObjectKey);
        assertThat(fileDeleted).isFalse();

        // DB에서 운동 기록과 관련된 이미지가 삭제되었는지 확인
        List<Picture> remainingPictures = em.createQuery(
                "SELECT p FROM Picture p INNER JOIN ExercisePicture ep ON p.id = ep.picture.id WHERE ep.exercise.id = :exerciseId AND p.pictureDeletedAt IS NULL",
                Picture.class
        )
                .setParameter("exerciseId", testExerciseId)
                .getResultList();
        assertThat(remainingPictures).isEmpty();

        // 운동 기록이 여전히 존재하는지 확인
        Exercise remainingExercise = em.find(Exercise.class, testExerciseId);
        assertThat(remainingExercise).isNotNull();
    }

    /* issue: https://github.com/projects200/api-server/issues/142
     * 문제 상황
     * 현재 로직에서는 이미지의 버킷 키 값을 S3 URL에서 앞에 부분을 제거하여 추출하고 있습니다.
     * 그리고 DB에는 url이 최대 255자까지 저장할 수 있는 VARCHAR 타입으로 설정되어 있습니다.
     * 근데 url 인코딩 문제로 인해 영어가 아닌 파일 이름이 몇 자만 들어가도 url이 255자를 초과하는 문제가 발생하였습니다.
     * * 예시: "운동이미지_한글_특수문자_.,.,!@!#!$#$%.jpg" 파일을 업로드하면 url이 255자를 초과하여 DB에 저장할 수 없습니다.
     *
     * 해결 방법
     * S3에 원본 파일 이름을 저장하지 않고 uuid.png와 같은 고유한 이름으로 저장하고, 메타 데이터에 원본 파일 이름을 저장하는 방식으로 변경하였습니다.
     * S3에는 `uploads/exercises/{userId}/{year}/{month}/{랜덤 uuid}.{확장자}` 형식으로 저장됩니다.
     */
    @Test
    @DisplayName("url 인코딩으로 인하여 S3 url이 너무 길어져 DB에 운동 이미지 업로드가 실패하는 문제 테스트")
    void uploadExerciseImageWithLongFileName() throws Exception {
        // given
        // 테스트용 회원, 운동 기록 데이터 DB에 저장
        UUID testMemberId = UUID.randomUUID();
        Member testMember = Member.builder()
                .memberId(testMemberId)
                .memberEmail("test@test.com")
                .memberNickname("테스트유저")
                .build();
        em.persist(testMember);

        Exercise testExercise = Exercise.builder()
                .member(testMember)
                .exerciseTitle("테스트용 운동 기록")
                .build();
        em.persist(testExercise);
        Long testExerciseId = testExercise.getId();

        // 테스트용 업로드 이미지 파일 생성
        String fileName = "운동이미지_한글_특수문자_.,.,!@!#!$#$%.jpg";
        MockMultipartFile mockFile = new MockMultipartFile(
                "pictures", // API에서 파일을 받는 파라미터 이름
                fileName,
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        // when
        mockMvc.perform(multipart("/api/v1/exercises/{exerciseId}/pictures", testExerciseId) // 실제 업로드 API 엔드포인트로 변경해야 합니다.
                        .file(mockFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(getCommonApiHeaders(testMemberId)))
                .andDo(print())
                .andExpectAll(
                        status().isCreated(), // 또는 isCreated() 등 예상되는 상태 코드로 변경
                        jsonPath("$.data.exerciseId").value(testExerciseId) // 업로드된 운동 기록 ID를 응답으로 받는다고 가정
                );
    }
}
