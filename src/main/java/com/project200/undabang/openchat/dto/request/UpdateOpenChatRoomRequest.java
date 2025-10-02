package com.project200.undabang.openchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOpenChatRoomRequest {

    @NotBlank(message = "오픈채팅 URL을 입력해주세요.")
    @Pattern(regexp = "^https?://open\\.kakao\\.com/o/[a-zA-Z0-9]+$",
            message = "유효하지 않은 카카오 오픈채팅 URL 형식입니다.")
    private String openChatroomUrl;
}
