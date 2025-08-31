package com.project200.undabang.common.entity.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record PictureWithKeyRecord(@NotNull MultipartFile multipartFile, @NotNull String objectKey) {
}
