package com.project200.undabang.test;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.entity.dto.PictureUploadParameters;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.service.FileType;
import com.project200.undabang.common.service.PictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TestService {
    private final PictureService pictureService;
    private final PictureRepository pictureRepository;

    /**
     * 파일 리스트를 받아 PictureService를 통해 업로드하고, 생성된 ID 리스트를 반환합니다.
     *
     * @param files 업로드할 MultipartFile 리스트
     * @return 생성된 Picture 엔티티의 ID 리스트
     */
    public List<Long> uploadPictures(List<MultipartFile> files) {
        // PictureUploadParameters는 PictureService가 필요로 하는 DTO입니다.
        // 프로젝트에 맞게 정의하여 사용해야 합니다.
        PictureUploadParameters params = new PictureUploadParameters(files, FileType.PROFILE); // FileType.POST는 예시

        List<Picture> uploadedPictures = pictureService.uploadPictureListToS3AndDB(params);

        return uploadedPictures.stream()
                .map(Picture::getId)
                .collect(Collectors.toList());
    }

    /**
     * Picture ID 리스트를 받아 PictureService를 통해 삭제를 요청합니다.
     *
     * @param pictureIds 삭제할 Picture 엔티티의 ID 리스트
     */
    public void deletePictures(List<Long> pictureIds) {
        List<Picture> pictures = pictureRepository.findAllById(pictureIds);
        pictureService.deletePictureFromS3AndDB(pictures);
    }

}
