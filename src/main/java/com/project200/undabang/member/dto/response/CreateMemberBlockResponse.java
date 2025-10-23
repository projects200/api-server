package com.project200.undabang.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemberBlockResponse {
    public long memberBlockId;

    public static CreateMemberBlockResponse of(long memberBlockId) {
        return new CreateMemberBlockResponse(memberBlockId);
    }
}
