package com.project200.undabang.common.service;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.FileProcessingException;
import com.project200.undabang.common.web.exception.S3UploadFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3Service {
    private final S3Client s3Client;

    @Value("${app.s3.bucket-name}") // 프로퍼티에서 값 주입
    private String bucketName;

    private static final String TRASH_PREFIX = "trash/";

    /**
    * 사용자 ID와 파일 이름을 기반으로 S3 객체 키를 생성합니다.
    * 형식: uploads/{category}/{userId}/{year}/{month}/{uuid}.{extension}
    */
    public String generateObjectKey(String originalFilename, FileType category) {
        String userId = UserContextHolder.getUserId().toString();
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String uuid = UUID.randomUUID().toString();

        String extension =  originalFilename.substring(originalFilename.lastIndexOf('.') + 1);

        return String.format("uploads/%s/%s/%s/%s/%s.%s",
                category.getPath(),
                userId,
                year,
                month,
                uuid,
                extension);
    }

    /**
     * DB에 저장된 URL에서 ObjectKey 추출하는 기능을 가진 메소드
     */
    public String extractObjectKeyFromUrl(String url){
        if(Objects.isNull(url) || url.isEmpty()){
            return null;
        }

        // "uploads/"로 시작하는 부분을 찾아서 그 부분부터 끝까지를 객체 키로 반환
        int uploadsIndex = url.indexOf("uploads/");
        if (uploadsIndex >= 0) {
            return url.substring(uploadsIndex);
        }

        // "uploads/"를 찾을 수 없는 경우 기존 방식 시도
        int bucketEndIndex = url.indexOf(bucketName);
        if (bucketEndIndex >= 0) {
            bucketEndIndex += bucketName.length();
            // URL에 버킷 이름 다음에 / 문자가 있는지 확인
            if (bucketEndIndex < url.length() && url.charAt(bucketEndIndex) == '/') {
                bucketEndIndex += 1;
            }
            return url.substring(bucketEndIndex);
        }

        log.warn("객체 키를 추출할 수 없습니다: {}", url);
        // 추출 실패시 원래 url 반환
        return url;
    }

    /**
     * 전달된 파일을 S3 버킷의 지정된 경로로 업로드합니다.
     *
     * @param multipartFile 업로드할 파일을 나타내는 객체
     * @param objectKey     S3에 저장될 객체의 키(경로)
     * @return 업로드된 객체의 S3 공개 URL
     * @throws FileProcessingException 파일 처리 중 오류가 발생한 경우
     * @throws S3UploadFailedException S3 업로드 중 오류가 발생한 경우
     */
    public String uploadImage(MultipartFile multipartFile, String objectKey)
            throws FileProcessingException, S3UploadFailedException {
        // S3에 업로드할 이미지 메타데이터에 원본 파일 이름 추가
        Map<String, String> metadata = new HashMap<>();
        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename != null) {
            String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8);
            metadata.put("originalfilename", encodedFilename);
        }

        // S3에 업로드할 객체 요청 생성
        return uploadImage(multipartFile, objectKey, metadata);
    }


    /**
     * S3 버킷에 파일을 업로드합니다.
     *
     * @param multipartFile 업로드할 파일을 나타내는 MultipartFile 객체
     * @param objectKey     S3에 저장될 객체의 키(경로)
     * @param metadata      파일과 함께 저장할 메타데이터
     * @return 업로드된 파일의 S3 공개 URL
     * @throws FileProcessingException 파일 처리 과정에서 오류가 발생한 경우
     * @throws S3UploadFailedException S3 업로드 중 오류가 발생한 경우
     */
    public String uploadImage(MultipartFile multipartFile, String objectKey, Map<String, String> metadata)
            throws FileProcessingException, S3UploadFailedException {
        // 업로드할 파일이 비어있지 않은지 확인
        validateFile(multipartFile, objectKey);
        // S3에 업로드할 객체 요청 생성
        PutObjectRequest putObjectRequest = createPutObjectRequest(multipartFile, objectKey, metadata);
        // S3에 파일 업로드
        return uploadToS3(multipartFile, objectKey, putObjectRequest);
    }

    /**
     * S3 객체 키를 기반으로 해당 객체의 공개 URL을 반환합니다.
     *
     * @param objectKey S3에 저장된 객체의 키(경로)
     * @return S3 객체의 공개 URL
     */
    public String getPublicUrl(String objectKey) {
        // S3Client가 구성된 리전 정보를 사용
        try {
            return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(objectKey)).toString();
        } catch (SdkClientException e) {
            log.error("생성 된 URL이 잘못되었습니다. Key: {}, 메시지: {}", objectKey, e.getMessage(), e);
            throw new S3UploadFailedException("생성 된 URL이 잘못되었습니다: " + objectKey, e);
        }
    }

    /**
     * 지정된 S3 객체 키를 기반으로 S3 버킷에서 이미지를 삭제합니다.
     *
     * @param objectKey 삭제할 이미지의 S3 객체 키
     */
    public void deleteImage(String objectKey) throws S3UploadFailedException {
        // S3 객체 삭제 요청 생성
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        try {
            // S3에서 객체 삭제
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception ex) {
            handleS3Exception(objectKey, ex);
        } catch (SdkException ex) {
            handleSdkException(objectKey, ex);
        } catch (Exception ex) {
            handleGenericException(objectKey, ex);
        }
    }

    /**
     * 주어진 S3 객체를 휴지통 경로로 이동합니다.
     * 원본 객체를 휴지통 경로로 복사한 후, 원본 객체를 삭제합니다.
     *
     * @param objectKey 이동하려는 S3 객체의 고유 키. null이거나 비어있는 값은 허용되지 않습니다.
     * @throws S3UploadFailedException S3 객체 복사 또는 삭제 작업 중 실패가 발생한 경우 예외를 던집니다.
     */
    public void moveImageToTrash(String objectKey) throws S3UploadFailedException {
        if (objectKey == null || objectKey.isBlank()) {
            log.warn("moveToTrash: objectKey가 null이거나 비어있습니다.");
            return;
        }

        // DB에는 데이터가 있는데 S3에 객체가 없을경우 체크
        if (!isFileExists(objectKey)) {
            log.warn("휴지통으로 이동할 원본 S3 객체가 존재하지 않습니다. Key: {}", objectKey);
            return;
        }

        String trashObjectKey = getTrashKeyFromOriginal(objectKey);

        try {
            copyObject(objectKey, trashObjectKey);
            deleteImage(objectKey);
            log.info("S3 객체를 휴지통으로 이동했습니다: 원본 '{}' -> 휴지통 '{}'", objectKey, trashObjectKey);
        } catch (S3UploadFailedException e) {
            log.error("S3 객체를 휴지통으로 이동하는 중 오류 발생. 원본 Key: {}", objectKey, e);
            throw e;
        }
    }

    /**
     * 휴지통에 있는 이미지를 원래 위치로 복원합니다.
     */
    public void restoreImageFromTrash(String originalObjectKey) throws S3UploadFailedException {
        if (originalObjectKey == null || originalObjectKey.isBlank()) {
            log.warn("restoreFromTrash: originalObjectKey가 null이거나 비어있습니다.");
            return;
        }
        String trashObjectKey = getTrashKeyFromOriginal(originalObjectKey);

        try {
            if (!isFileExists(trashObjectKey)) {
                log.warn("휴지통에 복원할 파일이 없습니다: {}", trashObjectKey);
                return;
            }
            copyObject(trashObjectKey, originalObjectKey);
            deleteImage(trashObjectKey);
            log.info("S3 객체를 휴지통에서 복원했습니다: 휴지통 '{}' -> 원본 '{}'", trashObjectKey, originalObjectKey);
        } catch (S3UploadFailedException e) {
            log.error("S3 객체를 휴지통에서 복원하는 중 오류 발생. 원본 Key: {}", originalObjectKey, e);
            throw e;
        }
    }

    /**
     * 주어진 소스 객체 키를 대상 객체 키로 복사합니다. 복사는 동일한 S3 버킷 내에서 수행됩니다.
     */
    private void copyObject(String sourceKey, String destinationKey) throws S3UploadFailedException {
        try {
            CopyObjectRequest copyReq = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
                    .build();
            s3Client.copyObject(copyReq);
        } catch (S3Exception ex) {
            handleS3Exception(sourceKey, ex);
        } catch (SdkException ex) {
            handleSdkException(sourceKey, ex);
        } catch (Exception ex) {
            handleGenericException(sourceKey, ex);
        }
    }

    /**
     * 원본 키로부터 휴지통 키를 생성합니다.
     */
    public String getTrashKeyFromOriginal(String originalKey) {
        return TRASH_PREFIX + originalKey;
    }

    // 업로드할 파일이 비어있는지 확인하는 메서드
    private void validateFile(MultipartFile multipartFile, String objectKey) throws FileProcessingException {
        if (multipartFile.isEmpty()) {
            log.error("업로드할 파일이 비어 있습니다. Key: {}", objectKey);
            throw new FileProcessingException("업로드할 파일이 비어 있습니다: " + objectKey);
        }
    }

    // S3에 업로드할 객체 요청을 생성하는 메서드
    private PutObjectRequest createPutObjectRequest(MultipartFile multipartFile, String objectKey, Map<String, String> metadata) {
        return PutObjectRequest.builder()
                .bucket(bucketName) // application-{profile}.yml 에서 주입된 버킷 이름
                .key(objectKey)
                .contentType(multipartFile.getContentType()) // Content-Type 설정
                .metadata(metadata) // 사용자 정의 메타데이터 설정
                .build();
    }

    // S3에 파일을 업로드하는 메서드
    private String uploadToS3(MultipartFile multipartFile, String objectKey, PutObjectRequest putObjectRequest)
            throws FileProcessingException, S3UploadFailedException {
        try {
            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));
            return getPublicUrl(objectKey);
        } catch (IOException ex) {
            handleIOException(objectKey, ex);
        } catch (S3Exception ex) {
            handleS3Exception(objectKey, ex);
        } catch (SdkException ex) {
            handleSdkException(objectKey, ex);
        } catch (Exception ex) {
            handleGenericException(objectKey, ex);
        }
        return null; // 이 줄은 도달하지 않음
    }

    // 예외 처리 메서드들
    private void handleIOException(String objectKey, IOException ex) throws FileProcessingException {
        log.error("MultipartFile에서 InputStream을 얻는 중 오류 발생 (임시 저장소 접근 오류 가능성). Key: '{}'. 메시지: {}",
                objectKey, ex.getMessage(), ex);
        throw new FileProcessingException("업로드된 파일 데이터를 읽는 중 오류 발생: " + objectKey, ex);
    }

    private void handleS3Exception(String objectKey, S3Exception ex) throws S3UploadFailedException {
        log.error("S3 서비스 오류 발생. Key: {}, ErrorCode: {}",
                objectKey, ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorCode() : "N/A", ex);
        throw new S3UploadFailedException("S3 업로드 중 서비스 오류 발생: " + objectKey, ex);
    }

    private void handleSdkException(String objectKey, SdkException ex) throws S3UploadFailedException {
        log.error("S3 SDK 오류 발생. Key: {}", objectKey, ex);
        throw new S3UploadFailedException("S3 업로드 중 클라이언트 오류 발생: " + objectKey, ex);
    }

    private void handleGenericException(String objectKey, Exception ex) throws S3UploadFailedException {
        log.error("S3 업로드 중 예기치 못한 오류 발생. Key: {}", objectKey, ex);
        throw new S3UploadFailedException("S3 업로드 중 알 수 없는 오류 발생: " + objectKey, ex);
    }

    public boolean isFileExists(String objectKey) {
        try {
            s3Client.headObject(builder -> builder.bucket(bucketName).key(objectKey));
            return true; // 객체가 존재하는 경우
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false; // 객체가 존재하지 않는 경우
            }
            log.error("S3 객체 존재 여부 확인 중 오류 발생. Key: {}, 메시지: {}", objectKey, e.getMessage(), e);
            throw new S3UploadFailedException("S3 객체 존재 여부 확인 중 오류 발생: " + objectKey, e);
        }
    }
}
