package com.project200.undabang.exercise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureDataResponse {
    Long pictureId;
    String pictureUrl;
    String pictureName;
    String pictureExtension;
}
