package com.project200.undabang.feed.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeedRequest {
    @NotBlank(message = "피드 내용을 입력해주세요.")
    private String feedContent;
    private Long feedTypeId;
}
