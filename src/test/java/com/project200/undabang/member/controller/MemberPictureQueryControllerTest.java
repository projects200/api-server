package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.member.controller.picture.MemberPictureQueryController;
import com.project200.undabang.member.dto.record.ProfileImageRecord;
import com.project200.undabang.member.dto.response.GetProfilePictureResponse;
import com.project200.undabang.member.service.MemberPictureQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberPictureQueryController.class)
class MemberPictureQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private MemberPictureQueryService memberPictureQueryService;

    @Nested
    @DisplayName("GET /api/v1/profile-pictures API는")
    class GetProfilePictures {
        @Test
        @DisplayName("회원의 프로필 사진 목록을 성공적으로 조회한다")
        void getProfilePictures_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            List<ProfileImageRecord> recordList = createProfileImageRecordList();

            // from() 정적 팩토리 메소드를 사용하여 응답 객체 생성
            GetProfilePictureResponse expectedResponse = GetProfilePictureResponse.from(recordList.get(0), recordList);

            given(memberPictureQueryService.getProfilePictures()).willReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/api/v1/profile-pictures")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))) // AbstractRestDocSupport의 헬퍼 메소드 사용
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.data.representativeProfileImage").exists(),
                            jsonPath("$.data.representativeProfileImage.profileImageId").value(recordList.get(0).profileImageId()),
                            jsonPath("$.data.profileImageCount").value(expectedResponse.getProfileImageCount()),
                            jsonPath("$.data.profileImages").isArray(),
                            jsonPath("$.data.profileImages[0].profileImageId").value(recordList.get(0).profileImageId())
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.representativeProfileImage").type(OBJECT).description("대표 프로필 사진 정보입니다. 없을 경우 null 입니다."),
                                    fieldWithPath("data.representativeProfileImage.profileImageId").type(NUMBER).description("대표 프로필 사진의 식별자를 나타냅니다."),
                                    fieldWithPath("data.representativeProfileImage.profileImageUrl").type(STRING).description("대표 프로필 사진의 S3 URL 주소를 나타냅니다."),
                                    fieldWithPath("data.representativeProfileImage.profileImageName").type(STRING).description("대표 프로필 사진의 원본 파일 이름을 의미합니다."),
                                    fieldWithPath("data.representativeProfileImage.profileImageExtension").type(STRING).description("대표 프로필 사진의 파일 확장자를 나타냅니다."),
                                    fieldWithPath("data.profileImageCount").type(NUMBER).description("전체 프로필 사진의 개수를 의미합니다."),
                                    fieldWithPath("data.profileImages").type(ARRAY).description("전체 프로필 사진 목록을 담는 리스트"),
                                    fieldWithPath("data.profileImages[].profileImageId").type(NUMBER).description("프로필 사진의 식별자 정보를 나타냅니다."),
                                    fieldWithPath("data.profileImages[].profileImageUrl").type(STRING).description("프로필 사진의 S3 URL 정보를 나타냅니다."),
                                    fieldWithPath("data.profileImages[].profileImageName").type(STRING).description("프로필 사진의 원본 파일 이름을 의미합니다."),
                                    fieldWithPath("data.profileImages[].profileImageExtension").type(STRING).description("프로필 사진의 파일 확장자를 나타냅니다.")
                            ))
                    ));

            // then
            then(memberPictureQueryService).should().getProfilePictures();
        }

        @Test
        @DisplayName("회원 정보를 찾을 수 없어 프로필 사진 조회에 실패한다")
        void getProfilePictures_Fail_MemberNotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            given(memberPictureQueryService.getProfilePictures())
                    .willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/profile-pictures")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isNotFound(), // MEMBER_NOT_FOUND는 보통 404 Not Found에 매핑됩니다.
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.getCode()),
                            jsonPath("$.message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage())
                    );

            // then
            then(memberPictureQueryService).should().getProfilePictures();
        }

        private List<ProfileImageRecord> createProfileImageRecordList() {
            return List.of(
                    new ProfileImageRecord(1L, "https://example.com/pic1.jpg", "photo1.jpg", ".jpg"),
                    new ProfileImageRecord(2L, "https://example.com/pic2.png", "photo2.png", ".png"),
                    new ProfileImageRecord(3L, "https://example.com/pic3.jpeg", "photo3.jpeg", ".jpeg")
            );
        }
    }
}