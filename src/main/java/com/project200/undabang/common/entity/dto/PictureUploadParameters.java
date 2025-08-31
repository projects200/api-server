package com.project200.undabang.common.entity.dto;

import com.project200.undabang.common.service.FileType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class PictureUploadParameters {

    @NotNull
    private List<MultipartFile> pictureList;

    @NotNull
    private FileType fileType;
}
