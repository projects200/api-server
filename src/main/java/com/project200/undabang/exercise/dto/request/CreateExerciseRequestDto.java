package com.project200.undabang.exercise.dto.request;

import com.project200.undabang.common.validation.AllowedExtensions;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.member.entity.Member;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateExerciseRequestDto {
    @NotNull(message = "제목은 필수 입력값입니다.")
    private String exerciseTitle;
    private String exercisePersonalType;
    private String exerciseLocation;
    private String exerciseDetail;

    @NotNull(message = "시작 시간은 필수 입력값입니다.")
    private LocalDateTime exerciseStartedAt;

    @NotNull(message = "종료 시간은 필수 입력값입니다.")
    private LocalDateTime exerciseEndedAt;

    @Size(max = 5, message = "최대 5개의 파일만 업로드할 수 있습니다.")
    @AllowedExtensions(extensions = {".jpg", ".jpeg", ".png"})
    private List<MultipartFile> exercisePictureList;

    public Exercise toEntity(Member member) {
        return Exercise.builder()
                .member(member)
                .exerciseTitle(exerciseTitle)
                .exercisePersonalType(exercisePersonalType)
                .exerciseLocation(exerciseLocation)
                .exerciseDetail(exerciseDetail)
                .exerciseStartedAt(exerciseStartedAt)
                .exerciseEndedAt(exerciseEndedAt)
                .build();
    }
}
