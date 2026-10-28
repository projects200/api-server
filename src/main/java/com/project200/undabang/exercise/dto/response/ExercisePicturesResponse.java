package com.project200.undabang.exercise.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExercisePicturesResponse(Long exerciseId, long version, List<UploadedPicture> pictures) {
    public record UploadedPicture(UUID clientImageId, Long pictureId, String pictureUrl) {
    }
}
