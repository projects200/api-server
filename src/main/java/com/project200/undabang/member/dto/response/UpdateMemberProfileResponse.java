package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberProfileResponse {
    private String nickname;
    private String gender;
    private String bio;

    /**
     * 주어진 Member 객체를 기반으로 UpdateMemberProfileResponse 객체를 생성합니다.
     *
     * @param member Member 객체로, 회원의 닉네임, 성별, 자기소개 등 프로필 정보를 포함합니다.
     * @return Member 객체의 정보를 기반으로 생성된 UpdateMemberProfileResponse 객체
     */
    public static UpdateMemberProfileResponse from(Member member) {
        return UpdateMemberProfileResponse.builder()
                .nickname(member.getMemberNickname())
                .gender(member.getMemberGender().toString())
                .bio(member.getMemberDesc())
                .build();
    }
}
