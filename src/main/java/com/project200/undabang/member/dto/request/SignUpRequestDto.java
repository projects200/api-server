package com.project200.undabang.member.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.project200.undabang.member.deserializer.MemberGenderDeserializer;
import com.project200.undabang.member.enums.MemberGender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDto {
    @NotNull(message = "성별을 입력해주세요")
    @JsonDeserialize(using = MemberGenderDeserializer.class)
    private MemberGender memberGender;

    @NotNull (message = "생일을 입력하세요")
    private LocalDate memberBday;

    @NotBlank(message = "닉네임을 설정해주세요")
    @Size(min=1, max = 30, message = "닉네임은 30자 이내로 설정해주세요")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]{1,30}$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다")
    private String memberNickname;
}
