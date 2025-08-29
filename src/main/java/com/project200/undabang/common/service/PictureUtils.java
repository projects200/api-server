package com.project200.undabang.common.service;

import com.project200.undabang.common.entity.Picture;
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
 * S3와 DB에 사진 파일을 업로드 및 삭제하는 유틸리티 클래스입니다.
 * S3 업로드/삭제와 DB soft delete를 일관되게 처리하며,
 * 예외 발생 시 롤백 처리를 지원합니다.
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class PictureUtils {
    private final S3Service s3Service;
    private final PictureRepository pictureRepository;

    /**
     * 여러 개의 파일을 S3에 업로드하고, DB에 Picture 엔티티로 저장합니다.
     * 업로드 도중 예외 발생 시, 이미 업로드된 S3 객체는 롤백(삭제)됩니다.
     */
    public List<Picture> uploadPicturesToS3AndDB(@NotNull List<MultipartFile> fileList, @NotNull FileType fileType) {
        List<Picture> pictureList = new ArrayList<>();

        try {
            for (MultipartFile file : fileList) {
                String imageUrl = uploadImageToS3(file, fileType);

                Picture picture = Picture.of(file, imageUrl);
                Picture savedPicture = pictureRepository.save(picture);

                pictureList.add(savedPicture);
            }
        } catch (FileProcessingException | S3UploadFailedException e) {
            rollBackS3Upload(pictureList);
            throw new CustomException(ErrorCode.PICTURE_UPLOAD_FAILED);
        }

        return pictureList;
    }

    /**
     * Picture 엔티티 리스트에 해당하는 S3 객체를 삭제하고,
     * 각 Picture 엔티티를 soft delete 처리합니다.
     * 삭제 도중 예외 발생 시, 이미 삭제된 S3 객체는 롤백(재삭제) 시도됩니다.
     */
    public void deletePictureFromS3AndDB(@NotNull List<Picture> pictureList) throws S3UploadFailedException {
        try {
            for (Picture picture : pictureList) {
                deletePictureFromS3(picture);
                picture.softDelete();
            }
        } catch (Exception e) {
            rollBackS3Upload(pictureList);
            throw new CustomException(ErrorCode.PICTURE_DELETE_FAILED);
        }
    }

    /**
     * 단일 파일을 S3에 업로드하고, 업로드된 파일의 URL을 반환합니다.
     */
    private String uploadImageToS3(MultipartFile file, FileType fileType) {
        String objectKey = s3Service.generateObjectKey(file.getOriginalFilename(), fileType);
        return s3Service.uploadImage(file, objectKey);
    }

    /**
     * Picture 엔티티의 S3 URL에서 object key를 추출하여 S3에서 해당 이미지를 삭제합니다.
     */
    private void deletePictureFromS3(Picture picture) throws S3UploadFailedException {
        String s3Url = s3Service.extractObjectKeyFromUrl(picture.getPictureUrl());
        s3Service.deleteImage(s3Url);
    }

    /**
     * 업로드/삭제 도중 예외 발생 시, 이미 S3에 업로드된 객체들을 롤백(삭제)합니다.
     */
    private void rollBackS3Upload(List<Picture> pictureList) {
        for (Picture picture : pictureList) {
            try {
                String objectKey = s3Service.extractObjectKeyFromUrl(picture.getPictureUrl());
                s3Service.deleteImage(objectKey);
            } catch (S3UploadFailedException ignored) {

            }
        }
    }
}
