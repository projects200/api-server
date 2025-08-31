package com.project200.undabang.common.service;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.entity.dto.PictureUploadParameters;
import com.project200.undabang.common.entity.dto.PictureUploadWithKeysParameters;
import com.project200.undabang.common.entity.dto.PictureWithKeyRecord;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.exception.FileProcessingException;
import com.project200.undabang.common.web.exception.S3UploadFailedException;
import io.awspring.cloud.s3.S3Exception;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;

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
     * 주어진 PictureUploadParameters를 사용하여 이미지 파일 리스트를 S3에 업로드하고,
     * 업로드된 이미지 URL을 사용하여 Picture 엔티티를 생성한 후 데이터베이스에 저장합니다.
     * 업로드 중 오류가 발생하면 처리된 데이터가 롤백됩니다.
     */
    @Transactional
    public List<Picture> uploadPictureListToS3AndDB(PictureUploadParameters parameters) {
        List<Picture> pictureList = new ArrayList<>();

        try {
            for (MultipartFile file : parameters.getPictureList()) {
                String objectKey = s3Service.generateObjectKey(file.getOriginalFilename(), parameters.getFileType());
                String imageUrl = s3Service.uploadImage(file, objectKey);

                Picture picture = Picture.of(file, imageUrl);
                pictureList.add(pictureRepository.save(picture));
            }
        } catch (FileProcessingException | S3UploadFailedException e) {

            handleUploadFailure(pictureList, "S3에 이미지 파일 업로드시 에러 발생");
        } catch (Exception e) {

            handleUploadFailure(pictureList, "DB에 이미지 파일 저장시 에러 발생");
        }

        return pictureList;
    }

    /**
     * 주어진 PictureWithKeyRecord 리스트를 기반으로 이미지를 S3에 업로드하고, 업로드된 이미지 URL을 사용하여 Picture 엔티티를 생성한 뒤 데이터베이스에 저장합니다.
     * 업로드 또는 데이터베이스 저장 중 오류 발생 시 롤백 처리를 수행합니다.
     */
    @Transactional
    public List<Picture> uploadPictureListToS3AndDB(PictureUploadWithKeysParameters parameters) {
        List<Picture> pictureList = new ArrayList<>();

        try {
            for (PictureWithKeyRecord record : parameters.pictureWithKeyRecordList()) {
                String imageUrl = s3Service.uploadImage(record.multipartFile(), record.objectKey());
                Picture picture = Picture.of(record.multipartFile(), imageUrl);
                pictureList.add(pictureRepository.save(picture));
            }
        } catch (FileProcessingException | S3UploadFailedException e) {

            handleUploadFailure(pictureList, "S3에 이미지 파일 업로드시 에러 발생");

        } catch (Exception e) {

            handleUploadFailure(pictureList, "DB에 이미지 파일 저장시 에러 발생");
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
        try {
            // 업로드 된 S3 이미지 삭제
            deletePicturesFromS3(pictureList);

            // DB에 저장된 데이터 논리적 삭제
            softDeletePicturesInDB(pictureList);

        } catch (S3Exception | SdkException e) {

            // 클라이언트 레벨 에러 및 Sdk 에러 처리
            log.error("S3 Exception: {}", e.getMessage());
            rollBackS3Delete(pictureList);

        } catch (Exception e) {

            log.error("이미지 삭제 처리중 에러 발생 {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.PICTURE_DELETE_FAILED);

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
     * 주어진 Picture 리스트를 기반으로 S3에서 이미지를 삭제합니다.
     * 삭제 과정에서 예외가 발생할 경우, 전체 삭제 작업을 다시 시도합니다.
     */
    private void deletePicturesFromS3(List<Picture> pictureList) {
        for (Picture picture : pictureList) {
            String objectKey = s3Service.extractObjectKeyFromUrl(picture.getPictureUrl());
            s3Service.deleteImage(objectKey);
        }
    }


    /**
     * 주어진 Picture 리스트를 기반으로 S3에서 업로드된 이미지를 롤백(삭제)하는 메서드입니다.
     * 각 이미지의 URL에서 S3 객체 키를 추출한 후, 이를 이용하여 S3에서 이미지를 삭제합니다.
     * 삭제 도중 예외가 발생한 경우에도 작업이 중단되지 않고, 로그에 에러 메시지를 기록합니다.
     */
    private void rollBackS3Upload(List<Picture> pictureList) {
        if (pictureList == null || pictureList.isEmpty()) {
            return;
        }

        for (Picture picture : pictureList) {
            try {
                String objectKey = s3Service.extractObjectKeyFromUrl(picture.getPictureUrl());
                s3Service.deleteImage(objectKey);
            } catch (S3UploadFailedException e) {
                log.error("S3 롤백중 사진 삭제 실패", e.getMessage());
            }
        }
    }

    /**
     * S3에서 삭제된 이미지들에 대한 롤백(복원)을 처리하는 메서드입니다.
     * 주어진 Picture 리스트의 각 이미지 URL에서 S3 객체 키를 추출하고,
     * 해당 객체를 S3에서 삭제 후 엔티티를 soft delete 처리합니다.
     * 롤백 처리 중 에러가 발생하면 로그에 에러 메시지를 기록합니다.
     */
    private void rollBackS3Delete(List<Picture> pictureList) {
        if (pictureList == null || pictureList.isEmpty()) {
            return;
        }

        for (Picture picture : pictureList) {
            try {
                String objectKey = s3Service.extractObjectKeyFromUrl(picture.getPictureUrl());
                if (objectKey != null) {
                    s3Service.deleteImage(objectKey);
                    picture.softDelete();
                }
            } catch (Exception e) {
                log.error("이미지 삭제 처리중 롤백 실패");
            }
        }
    }

    /**
     * S3 파일 업로드 실패 시 호출되는 메서드입니다.
     * 업로드된 S3 객체를 롤백 처리하고, 사용자 정의 예외를 발생시킵니다.
     */
    private void handleUploadFailure(List<Picture> pictureList, String logMessage) {
        log.error(logMessage);
        rollBackS3Upload(pictureList);
        throw new CustomException(ErrorCode.PICTURE_UPLOAD_FAILED);
    }
}