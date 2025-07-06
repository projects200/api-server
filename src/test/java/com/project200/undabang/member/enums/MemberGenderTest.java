package com.project200.undabang.member.enums;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MemberGenderTest {

    @Test
    @DisplayName("유효한 코드로 MALE 반환")
    public void testFromCode_returnsMaleForValidCode() {
        // given
        String validCode = "M";

        // when
        MemberGender result = MemberGender.fromCode(validCode);

        // then
        assertThat(result)
                .as("유효한 코드 'M'은 MALE 반환")
                .isEqualTo(MemberGender.MALE);
    }

    @Test
    @DisplayName("유효한 코드로 FEMALE 반환")
    public void testFromCode_returnsFemaleForValidCode() {
        // given
        String validCode = "F";

        // when
        MemberGender result = MemberGender.fromCode(validCode);

        // then
        assertThat(result)
                .as("유효한 코드 'F'는 FEMALE 반환")
                .isEqualTo(MemberGender.FEMALE);
    }

    @Test
    @DisplayName("유효한 코드로 UNKNOWN 반환")
    public void testFromCode_returnsUnknownForValidCode() {
        // given
        String validCode = "U";

        // when
        MemberGender result = MemberGender.fromCode(validCode);

        // then
        assertThat(result)
                .as("유효한 코드 'U'는 UNKNOWN 반환")
                .isEqualTo(MemberGender.UNKNOWN);
    }

    @Test
    @DisplayName("소문자 코드로도 올바르게 반환")
    public void testFromCode_isCaseInsensitive() {
        // given
        String lowerCaseCodeMale = "m";
        String lowerCaseCodeFemale = "f";
        String lowerCaseCodeUnknown = "u";

        // when
        MemberGender resultMale = MemberGender.fromCode(lowerCaseCodeMale);
        MemberGender resultFemale = MemberGender.fromCode(lowerCaseCodeFemale);
        MemberGender resultUnknown = MemberGender.fromCode(lowerCaseCodeUnknown);

        // then
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(resultMale)
                    .as("소문자 'm'은 MALE 반환").isEqualTo(MemberGender.MALE);
            softAssertions.assertThat(resultFemale)
                    .as("소문자 'f'는 FEMALE 반환").isEqualTo(MemberGender.FEMALE);
            softAssertions.assertThat(resultUnknown)
                    .as("소문자 'u'는 UNKNOWN 반환").isEqualTo(MemberGender.UNKNOWN);
        });
    }

    @Test
    @DisplayName("null 코드로 null 반환")
    public void testFromCode_returnsNullForNullCode() {
        // given
        String nullCode = null;

        // when
        MemberGender result = MemberGender.fromCode(nullCode);

        // then
        assertThat(result)
                .as("null 값은 null 반환").isNull();
    }

    @Test
    @DisplayName("유효하지 않은 코드로 예외 발생")
    public void testFromCode_throwsExceptionForInvalidCode() {
        // given
        String invalidCode = "X";

        // when & then
        assertThatThrownBy(() -> MemberGender.fromCode(invalidCode))
                .as("유효하지 않은 코드 'X'는 예외 발생")
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_GENDER_ERROR)
                .hasMessageContaining("유효하지 않은 성별 코드입니다: X");
    }

    @Test
    @DisplayName("빈 문자열로 예외 발생")
    public void testFromCode_throwsExceptionForEmptyString() {
        // given
        String emptyCode = "";

        // when & then
        assertThatThrownBy(() -> MemberGender.fromCode(emptyCode))
                .as("빈 문자열은 예외 발생")
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_GENDER_ERROR)
                .hasMessageContaining("유효하지 않은 성별 코드입니다: ");
    }
}