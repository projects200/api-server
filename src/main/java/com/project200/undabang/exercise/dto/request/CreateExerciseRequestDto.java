package com.project200.undabang.exercise.dto.request;

import com.project200.undabang.common.validation.AllowedExtensions;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class CreateExerciseRequestDto {
    @NotNull(message = "제목은 필수 입력값입니다.")
    private String title;
    private String type;
    private String location;
    private String detail;

    @NotNull(message = "시작 시간은 필수 입력값입니다.")
    private LocalDateTime startedAt = LocalDateTime.now();

    @NotNull(message = "종료 시간은 필수 입력값입니다.")
    private LocalDateTime endedAt = LocalDateTime.now().plusHours(1);

    @Size(max = 5, message = "최대 5개의 파일만 업로드할 수 있습니다.")
    @AllowedExtensions(extensions = {".jpg", ".jpeg", ".png"})
    private List<MultipartFile> images;
}
