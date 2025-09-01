package com.project200.undabang.common.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PictureUploadWithKeyParameters {

    @NotNull
    private List<MultipartFile> multipartFile;

    @NotNull
    private List<String> objectKey;
}
