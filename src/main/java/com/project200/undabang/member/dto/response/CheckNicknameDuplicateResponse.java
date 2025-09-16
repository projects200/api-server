package com.project200.undabang.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CheckNicknameDuplicateResponse {
    private boolean available;

    public static CheckNicknameDuplicateResponse of(boolean available) {
        return new CheckNicknameDuplicateResponse(available);
    }
}
