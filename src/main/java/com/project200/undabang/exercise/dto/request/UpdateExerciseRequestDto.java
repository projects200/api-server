package com.project200.undabang.exercise.dto.request;

import com.project200.undabang.common.validation.AllowedExtensions;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.member.entity.Member;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Builder@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UpdateExerciseRequestDto {
    @NotNull(message = "운동기록 식별자를 입력해주세요.")
    private Long exerciseId;

    @NotNull(message = "제목은 필수 입력값 입니다.")
    private String exerciseTitle;
    private String exerciseDetail;
    private String exercisePersonalType;
    private String exerciseLocation;

    @NotNull(message = "운동 시작 시간은 필수 입력값 입니다.")
    private LocalDateTime exerciseStartedAt;

    @NotNull(message = "운동 종료 시간은 필수 입력값 입니다.")
    private LocalDateTime exerciseEndedAt;

    private List<Long> currentPictureIdList; // 회원의 등록한 운동사진의 번호를 담고있는 리스트
    private List<Long> deletePictureIdList; // 회원이 등록한 운동사진중 수정할 사진의 사진번호를 담고있는 리스트

    @Size(max = 5, message = "최대 5개의 파일만 업로드 할 수 있습니다.")
    @AllowedExtensions(extensions = {".jpg", ".png.", ".jpeg"})
    private List<MultipartFile> exercisePictureList;

    public Exercise toExerciseEntity(Member member) {
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
