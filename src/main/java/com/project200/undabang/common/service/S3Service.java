package com.project200.undabang.common.service;

import com.project200.undabang.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class S3Service {
    private final S3Client s3Client;

    @Value("${app.s3.bucket-name}") // 프로퍼티에서 값 주입
    private String bucketName;

    /**
     * 사용자 ID와 파일 이름을 기반으로 S3 객체 키를 생성합니다.
     * 형식: uploads/{userId}/{year}/{month}/images/{uuid}_{originalFilename}
     */
    public String generateObjectKey(String originalFilename) {
        String userId = UserContextHolder.getUserId().toString();
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String uuid = UUID.randomUUID().toString();

        return String.format("uploads/%s/%s/%s/images/%s_%s",
                userId, year, month, uuid, originalFilename);
    }

    public String uploadImage(MultipartFile multipartFile, String objectKey) throws IOException {
        return uploadImage(multipartFile, objectKey, null);
    }

    public String uploadImage(MultipartFile multipartFile, String objectKey, Map<String, String> metadata) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName) // application-{profile}.yml 에서 주입된 버킷 이름
                .key(objectKey)
                .contentType(multipartFile.getContentType()) // Content-Type 설정
                .metadata(metadata) // 사용자 정의 메타데이터 설정
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));

        return getPublicUrl(objectKey); // 업로드 후 공개 URL 반환
    }

    public String getPublicUrl(String objectKey) {
        // S3Client가 구성된 리전 정보를 사용
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(objectKey)).toString();
        // 또는 수동 구성:
        // return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, s3Client.serviceClientConfiguration().region().id(), objectKey);
    }

    public void deleteImage(String objectKey) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }
}
