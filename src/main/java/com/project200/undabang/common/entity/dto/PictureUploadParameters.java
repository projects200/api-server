package com.project200.undabang.common.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PictureUploadParameters {

    @NotNull
    private MultipartFile multipartFile;

    @NotNull
    private String objectKey;
}
