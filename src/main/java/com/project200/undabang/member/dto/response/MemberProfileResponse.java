package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.dto.data.MemberProfileData;
import com.project200.undabang.member.enums.MemberGender;
import lombok.Data;

import java.util.List;

@Data
public class MemberProfileResponse {
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

    private void from(MemberProfileData member, List<PreferredExercisesOfMemberResponse> preferredExercises, int yearlyExerciseDays, int exerciseCountInLast30Days) {
        this.profileThumbnailUrl = member.profileThumbnailUrl();
        this.profileImageUrl = member.profileImageUrl();
        this.nickname = member.nickname();
        this.gender = member.gender();
        this.birthDate = member.birthDate();
        this.bio = member.bio();
        this.yearlyExerciseDays = yearlyExerciseDays;
        this.exerciseCountInLast30Days = exerciseCountInLast30Days;
        this.exerciseScore = member.exerciseScore();
        this.preferredExercises = preferredExercises;
    }
}
