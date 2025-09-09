package com.project200.undabang.common.service;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.entity.dto.PictureUploadParameters;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.exception.FileProcessingException;
import com.project200.undabang.common.web.exception.S3UploadFailedException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * S3에 이미지 파일을 업로드 및 삭제하고, DB에 해당 데이터를 관리하는 서비스를 제공합니다.
 * 주로 여러 이미지의 업로드, 삭제, 롤백 및 soft delete 처리를 지원합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PictureService {
    private final S3Service s3Service;
    private final PictureRepository pictureRepository;

    /**
     * 단일 MultipartFile을 S3에 업로드하고, 업로드된 파일 정보를 기반으로 Picture 엔티티를 생성하여
     * 데이터베이스에 저장합니다. 이 메소드는 내부적으로 리스트 처리 메소드를 호출하여 재사용합니다.
     *
     * @param multipartFile 업로드할 단일 파일
     * @param fileType      파일의 용도를 나타내는 Enum (예: PROFILE, POST)
     * @return 데이터베이스에 저장된 Picture 엔티티
     * @throws CustomException 파일이 비어있거나 업로드에 실패한 경우 발생
     */
    @Transactional
    public Picture uploadPictureToS3AndDB(@NotNull MultipartFile multipartFile, @NotNull FileType fileType) {
        // 파일이 비어있는지 먼저 확인
        if (multipartFile.isEmpty()) {
            throw new CustomException(ErrorCode.PICTURE_IS_EMPTY);
        }

        // 기존의 리스트 처리 메소드에 단일 파일을 리스트로 감싸서 전달
        List<Picture> pictureList = this.uploadPictureListToS3AndDB(List.of(multipartFile), fileType);

        // 결과 리스트가 비어있는 경우, 업로드 과정에서 알 수 없는 오류가 발생한 것으로 간주
        if (pictureList.isEmpty()) {
            // uploadPictureListToS3AndDB 내부에서 예외를 던지지 않고 비어있는 리스트를 반환하는 엣지 케이스 방어
            log.error("파일 업로드 후 Picture 객체를 생성하지 못했습니다. FileName: {}", multipartFile.getOriginalFilename());
            throw new CustomException(ErrorCode.PICTURE_UPLOAD_FAILED);
        }

        // 성공적으로 생성된 첫 번째(그리고 유일한) Picture 객체를 반환
        return pictureList.get(0);
    }

    /**
     * 단일 MultipartFile을 S3에 업로드하고, 업로드된 파일 정보를 기반으로 Picture 엔티티를 생성하여
     * 데이터베이스에 저장합니다. 내부적으로 리스트 처리 메소드를 재사용하며, 업로드 후 생성된 Picture 객체를 반환합니다.
     *
     * @param multipartFile 업로드할 파일. null이거나 비어있으면 CustomException이 발생합니다.
     * @param objectKey     업로드할 파일의 S3 객체 키. null일 수 없습니다.
     * @return S3에 업로드되고 데이터베이스에 저장된 Picture 엔티티
     * @throws CustomException 파일이 비어있거나 업로드에 실패한 경우 발생
     */
    @Transactional
    public Picture uploadPictureToS3AndDB(@NotNull MultipartFile multipartFile, @NotNull String objectKey) {
        // 파일이 비어있는지 우선 확인
        if (multipartFile.isEmpty()) {
            throw new CustomException(ErrorCode.PICTURE_IS_EMPTY);
        }

        // 기존의 리스트 처리 메소드에 단일 파일을 리스트로 감싸서 전달
        PictureUploadParameters parameter = new PictureUploadParameters(multipartFile, objectKey);
        List<Picture> pictureList = this.uploadPictureListToS3AndDB(List.of(parameter));

        // 결과 리스트가 비어있으면 오류가 발생한것으로 간주
        if (pictureList.isEmpty()) {
            log.error("파일 업로드 후 Picture 객체를 생성하지 못했습니다. ObjectKey: {}", objectKey);
            throw new CustomException(ErrorCode.PICTURE_UPLOAD_FAILED);
        }

        // 생성된 첫번째 Picture 객체 반환
        return pictureList.get(0);
    }


    /**
     * 주어진 MultipartFile 리스트를 S3에 업로드하고, 업로드된 파일 정보를 기반으로 Picture 엔티티를 생성하여
     * 데이터베이스에 저장합니다. S3 업로드 또는 데이터베이스 저장 중 오류가 발생하면 모든 작업을 롤백합니다.
     */
    @Transactional
    public List<Picture> uploadPictureListToS3AndDB(@NotNull List<MultipartFile> multipartFileList, @NotNull FileType fileType) {
        List<Picture> pictureList = new ArrayList<>();
        List<String> objectKeyList = new ArrayList<>();

        try {
            for (MultipartFile file : multipartFileList) {
                String objectKey = s3Service.generateObjectKey(file.getOriginalFilename(), fileType);
                String imageUrl = s3Service.uploadImage(file, objectKey);
                objectKeyList.add(objectKey);

                Picture picture = Picture.of(file, imageUrl);
                pictureList.add(pictureRepository.save(picture));
            }
        } catch (FileProcessingException | S3UploadFailedException e) {

            handleUploadFailure(objectKeyList, "S3에 이미지 파일 업로드시 에러 발생");
        } catch (Exception e) {

            handleUploadFailure(objectKeyList, "DB에 이미지 파일 저장시 에러 발생");
        }

        return pictureList;
    }

    /**
     * 주어진 PictureUploadParameters 리스트를 기반으로 파일을 S3에 업로드하고,
     * 업로드된 파일 정보를 토대로 Picture 엔티티를 생성하여 데이터베이스에 저장합니다.
     * 업로드 또는 저장 과정에서 예외가 발생하면 모든 작업을 롤백합니다.
     */
    @Transactional
    public List<Picture> uploadPictureListToS3AndDB(@NotNull List<PictureUploadParameters> parameterList) {
        List<Picture> pictureList = new ArrayList<>();
        List<String> uploadedObjectKeyList = new ArrayList<>();

        try {
            for (PictureUploadParameters parameter : parameterList) {
                MultipartFile file = parameter.getMultipartFile();
                String objectKey = parameter.getObjectKey();


                String imageUrl = s3Service.uploadImage(file, objectKey);
                uploadedObjectKeyList.add(objectKey);

                Picture picture = Picture.of(file, imageUrl);
                pictureList.add(pictureRepository.save(picture));
            }
        } catch (FileProcessingException | S3UploadFailedException e) {

            handleUploadFailure(uploadedObjectKeyList, "S3에 이미지 파일 업로드시 에러 발생");
        } catch (Exception e) {

            handleUploadFailure(uploadedObjectKeyList, "DB에 이미지 파일 저장시 에러 발생");
        }
        return pictureList;
    }

    /**
     * 주어진 Picture 리스트를 기반으로 S3에서 이미지를 삭제하고,
     * 데이터베이스에 해당 데이터를 논리적으로 삭제(soft delete)합니다.
     * 예외 상황 발생 시 S3 삭제나 DB 처리의 롤백을 수행합니다.
     */
    @Transactional
    public void deletePictureFromS3AndDB(@NotNull List<Picture> pictureList) {
        // 롤백을 위해 성공적으로 처리된 S3 객체 키를 기록하는 리스트
        List<String> processedObjectKeyList = new ArrayList<>();

        try {
            // S3 에서 삭제할 사진 객체는 /trash 폴더로 이동 (/trash/uploads...)
            moveImagesToTrashFolder(pictureList, processedObjectKeyList);
            // DB에 저장된 데이터 논리적 삭제
            softDeletePicturesInDB(pictureList);
        } catch (Exception e) {

            log.error("이미지 삭제 처리중 에러 발생. S3 롤백을 시도합니다. {}", e.getMessage(), e);
            rollBackS3MoveToTrash(processedObjectKeyList);
            throw new CustomException(ErrorCode.PICTURE_DELETE_FAILED);
        }
    }

    /**
     * 주어진 Picture 리스트에서 각 Picture의 URL을 기반으로 S3 객체 키를 추출한 후,
     * 해당 이미지를 S3의 "휴지통" 디렉터리로 이동시키고, 처리된 객체 키를 기록합니다.
     */
    public void moveImagesToTrashFolder(List<Picture> pictureList, List<String> processedObjectKeys) {
        for (Picture picture : pictureList) {
            String objectKey = s3Service.extractObjectKeyFromUrl(picture.getPictureUrl());
            if (objectKey != null) {
                s3Service.moveImageToTrash(objectKey);
                processedObjectKeys.add(objectKey);
            }
        }
    }

    /**
     * S3에 저장된 객체를 "휴지통"에서 원래 위치로 복원하여 이전 상태로 롤백합니다.
     * 롤백 중에 일부 객체에서 에러가 발생하더라도 다른 객체의 롤백 작업은 계속 시도합니다.
     */
    private void rollBackS3MoveToTrash(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return; // 롤백할 대상이 없으면 즉시 종료
        }
        log.info("S3 moveToTrash 롤백 시작. 대상 {}개", objectKeys.size());

        for (String objectKey : objectKeys) {
            try {
                s3Service.restoreImageFromTrash(objectKey);
            } catch (Exception e) {
                // 중요: 롤백 중 발생하는 에러는 다른 파일의 롤백을 막으면 안 됩니다.
                // 따라서 예외를 던지는 대신, 로그만 남기고 다음 파일의 롤백을 계속 시도합니다.
                log.error("S3 롤백 중 객체 복원 실패. Key: {}. 원인: {}", objectKey, e.getMessage());
            }
        }
    }

    /**
     * 주어진 Picture 리스트를 데이터베이스에서 논리적으로 삭제(soft delete)합니다.
     * 각 Picture 객체의 삭제된 시점을 기록한 후, 데이터베이스에 업데이트합니다.
     */
    public void softDeletePicturesInDB(List<Picture> pictureList) {
        for (Picture picture : pictureList) {
            picture.softDelete();
            pictureRepository.save(picture);
        }
    }

    /**
     * 주어진 Picture 리스트를 기반으로 S3에서 업로드된 이미지를 롤백(삭제)하는 메서드입니다.
     * 각 이미지의 URL에서 S3 객체 키를 추출한 후, 이를 이용하여 S3에서 이미지를 삭제합니다.
     * 삭제 도중 예외가 발생한 경우에도 작업이 중단되지 않고, 로그에 에러 메시지를 기록합니다.
     */
    private void rollBackS3Upload(List<String> uploadedObjectKeyList) {
        if (uploadedObjectKeyList == null || uploadedObjectKeyList.isEmpty()) {
            return;
        }

        for (String objectKey : uploadedObjectKeyList) {
            try {
                s3Service.deleteImage(objectKey);
            } catch (S3UploadFailedException e) {
                log.error("S3 롤백중 사진 삭제 실패", e.getMessage());
            }
        }
    }

    /**
     * S3 파일 업로드 실패 시 호출되는 메서드입니다.
     * 업로드된 S3 객체를 롤백 처리하고, 사용자 정의 예외를 발생시킵니다.
     */
    private void handleUploadFailure(List<String> uploadedObjectKeyList, String logMessage) {
        log.error(logMessage);
        rollBackS3Upload(uploadedObjectKeyList);
        throw new CustomException(ErrorCode.PICTURE_UPLOAD_FAILED);
    }
}