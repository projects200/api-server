package com.project200.undabang.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter@Setter
@NoArgsConstructor
public class SignUpRequestDto {
    private String userId;
    @Email
    private String userEmail;
    @NotBlank
    @Size(min = 1, max = 1)
    private String memberGender;
    private LocalDate memberBday;
    @NotBlank
    private String memberNickname;
}
