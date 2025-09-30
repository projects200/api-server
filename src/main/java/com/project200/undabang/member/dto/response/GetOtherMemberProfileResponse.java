package com.project200.undabang.member.dto.response;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.enums.MemberGender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetOtherMemberProfileResponse {
    private String profileThumbnailUrl;
    private String profileImageUrl;
    private String nickname;
    private MemberGender gender;
    private String birthDate;
    private String bio;
    private int yearlyExerciseDays;
    private int exerciseCountInLast30Days;
    private int exerciseScore;
    private List<PreferredExercisesOfMemberResponse> preferredExercises;

    public static GetOtherMemberProfileResponse of(Member member, int yearlyExerciseDays, int exerciseCountInLast30Days) {
        Optional<MemberPicture> memberPictureOptional = Optional.ofNullable(member.getMemberPicture());

        String thumbnailUrl = memberPictureOptional
                .map(MemberPicture::getMemberPicturesUrl)
                .orElse(null);

        String imageUrl = memberPictureOptional
                .map(MemberPicture::getPicture)
                .map(Picture::getPictureUrl)
                .orElse(null);

        List<PreferredExercisesOfMemberResponse> preferredExercises = Optional.ofNullable(member.getPreferredExercises())
                .map(exercises -> exercises.stream()
                        .map(PreferredExercisesOfMemberResponse::new)
                        .toList())
                .orElse(Collections.emptyList());

        return new GetOtherMemberProfileResponse(
                thumbnailUrl,
                imageUrl,
                member.getMemberNickname(),
                member.getMemberGender(),
                member.getMemberBday().toString(),
                member.getMemberDesc(),
                yearlyExerciseDays,
                exerciseCountInLast30Days,
                member.getMemberScore(),
                preferredExercises
        );
    }
}
