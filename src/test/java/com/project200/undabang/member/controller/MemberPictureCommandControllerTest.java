package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.member.controller.picture.MemberPictureCommandController;
import com.project200.undabang.member.dto.response.CreateProfilePictureResponse;
import com.project200.undabang.member.dto.response.UpdateProfilePictureResponse;
import com.project200.undabang.member.service.MemberPictureCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberPictureCommandController.class)
class MemberPictureCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private MemberPictureCommandService memberPictureCommandService;

    @Nested
    class CreateProfilePicture {

        @Test
        @DisplayName("성공: 유효한 파일을 업로드하면 201 상태 코드와 응답 데이터를 반환한다")
        void createProfilePicture_Success() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            MockMultipartFile profilePictureFile = new MockMultipartFile(
                    "profilePicture",
                    "my-profile.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "dummy-image-content".getBytes()
            );

            CreateProfilePictureResponse responseDto = new CreateProfilePictureResponse(
                    1L, "https://s3.com/images/my-profile.jpg"
            );

            BDDMockito.given(memberPictureCommandService.createProfilePicture(any(MockMultipartFile.class)))
                    .willReturn(responseDto);

            // when
            String response = mockMvc.perform(MockMvcRequestBuilders
                            .multipart("/api/v1/profile-pictures")
                            .file(profilePictureFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpectAll(status().isCreated())
                    .andDo(document.document(
                            requestHeaders(RestDocsUtils.HEADER_ACCESS_TOKEN),
                            requestParts(
                                    partWithName("profilePicture").attributes(getTypeFormat("FILE"))
                                            .description("업로드할 프로필 사진 파일입니다. 파일 크기는 10MB 이하여야 하며, 확장자는 .jpg, .jpeg, .png 만 허용됩니다.")
                            ),
                            responseFields(RestDocsUtils.commonResponseFields(
                                    fieldWithPath("data.pictureId").type(JsonFieldType.NUMBER).description("생성된 사진의 식별자 입니다."),
                                    fieldWithPath("data.profileThumbnailUrl").type(JsonFieldType.STRING).optional().description("생성된 프로필 썸네일 이미지의 URL입니다."),
                                    fieldWithPath("data.profileImageUrl").type(JsonFieldType.STRING).description("생성된 프로필 원본 이미지의 URL입니다.")
                            ))
                    ))
                    .andReturn().getResponse().getContentAsString();

            // then
            assertThat(response).isEqualTo(objectMapper.writeValueAsString(CommonResponse.create(responseDto)));
            BDDMockito.then(memberPictureCommandService).should(times(1)).createProfilePicture(any(MockMultipartFile.class));
            BDDMockito.then(memberPictureCommandService).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("실패: 지원하지 않는 파일 확장자 업로드 시 400 Bad Request를 반환한다")
        void createProfilePicture_Fail_UnsupportedFileExtension() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            MockMultipartFile invalidFile = new MockMultipartFile("profilePicture", "document.pdf", MediaType.APPLICATION_PDF_VALUE, "invalid-content".getBytes());

            // when & then
            mockMvc.perform(MockMvcRequestBuilders
                            .multipart("/api/v1/profile-pictures")
                            .file(invalidFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpectAll(status().isBadRequest())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.message").value("요청 파라미터 유효성 검증에 실패했습니다."));

            BDDMockito.then(memberPictureCommandService).should(BDDMockito.never()).createProfilePicture(any(MockMultipartFile.class));
        }

        @Test
        @DisplayName("실패: 파일 파트의 이름이 일치하지 않을 때 500 Internal Server Error를 반환한다")
        void createProfilePicture_Fail_MismatchedPartName() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            MockMultipartFile wrongNameFile = new MockMultipartFile("wrongPictureName", "my-profile.jpg", MediaType.IMAGE_JPEG_VALUE, "dummy-image-content".getBytes());

            // when & then
            mockMvc.perform(MockMvcRequestBuilders
                            .multipart("/api/v1/profile-pictures")
                            .file(wrongNameFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpectAll(status().isInternalServerError())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.message").value("Required part 'profilePicture' is not present."));

            BDDMockito.then(memberPictureCommandService).should(BDDMockito.never()).createProfilePicture(any(MockMultipartFile.class));
        }

        @Test
        @DisplayName("실패: 서비스에서 MEMBER_NOT_FOUND 예외 발생 시 404 Not Found를 반환한다")
        void createProfilePicture_Fail_MemberNotFound() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            MockMultipartFile profilePictureFile = new MockMultipartFile("profilePicture", "my-profile.jpg", MediaType.IMAGE_JPEG_VALUE, "dummy-image-content".getBytes());

            BDDMockito.given(memberPictureCommandService.createProfilePicture(any(MockMultipartFile.class)))
                    .willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(MockMvcRequestBuilders
                            .multipart("/api/v1/profile-pictures")
                            .file(profilePictureFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpectAll(status().isNotFound())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

            BDDMockito.then(memberPictureCommandService).should(times(1)).createProfilePicture(any(MockMultipartFile.class));
        }
    }

    @Nested
    @DisplayName("대표 프로필 사진 변경")
    class updateRepresentativeProfileImage {

        @Test
        @DisplayName("성공: 유효한 사진 ID로 요청 시 200 OK와 응답 데이터를 반환한다")
        void updateRepresentativeProfileImage_Success() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long pictureId = 1L;

            UpdateProfilePictureResponse responseDto = new UpdateProfilePictureResponse(pictureId);

            BDDMockito.given(memberPictureCommandService.updateRepresentativeProfileImage(pictureId))
                    .willReturn(responseDto);

            // when
            String response = mockMvc.perform(put("/api/v1/profile-pictures/{pictureId}/represent", pictureId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpectAll(status().isOk())
                    .andDo(document.document(
                            requestHeaders(RestDocsUtils.HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("pictureId").attributes(getTypeFormat("NUMBER")).description("대표 사진으로 지정할 사진의 식별자를 나타냅니다.")
                            ),
                            responseFields(RestDocsUtils.commonResponseFields(
                                    fieldWithPath("data.profileImageId").type(JsonFieldType.NUMBER).description("새롭게 대표로 지정된 프로필 사진의 식별자를 나타냅니다.")
                            ))
                    ))
                    .andReturn().getResponse().getContentAsString();

            // then
            assertThat(response).isEqualTo(objectMapper.writeValueAsString(CommonResponse.update(responseDto)));
            BDDMockito.then(memberPictureCommandService).should().updateRepresentativeProfileImage(pictureId);
            BDDMockito.then(memberPictureCommandService).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("실패: 사진이 존재하지 않을 때 서비스에서 예외 발생 시 404 Not Found를 반환한다")
        void updateRepresentativeProfileImage_Fail_PictureNotFound() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long pictureId = 999L; // 존재하지 않는 사진 ID

            BDDMockito.given(memberPictureCommandService.updateRepresentativeProfileImage(pictureId))
                    .willThrow(new CustomException(ErrorCode.PICTURE_NOT_FOUND));

            // when & then
            mockMvc.perform(put("/api/v1/profile-pictures/{pictureId}/represent", pictureId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpectAll(status().isNotFound())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.PICTURE_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.PICTURE_NOT_FOUND.getMessage()));

            BDDMockito.then(memberPictureCommandService).should().updateRepresentativeProfileImage(pictureId);
        }

        @Test
        @DisplayName("실패: 사진이 사용자의 소유가 아닐 때 서비스에서 예외 발생 시 403 Forbidden을 반환한다")
        void updateRepresentativeProfileImage_Fail_AuthorizationDenied() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long pictureId = 2L; // 다른 사람의 사진 ID

            BDDMockito.given(memberPictureCommandService.updateRepresentativeProfileImage(pictureId))
                    .willThrow(new CustomException(ErrorCode.AUTHORIZATION_DENIED));

            // when & then
            mockMvc.perform(put("/api/v1/profile-pictures/{pictureId}/represent", pictureId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpectAll(status().isForbidden())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.AUTHORIZATION_DENIED.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.AUTHORIZATION_DENIED.getMessage()));

            BDDMockito.then(memberPictureCommandService).should().updateRepresentativeProfileImage(pictureId);
        }
    }

    @Nested
    @DisplayName("프로필 사진 삭제")
    class DeleteProfilePicture {

        @Test
        @DisplayName("성공: 유효한 사진 ID로 요청 시 200 OK와 성공 응답을 반환한다")
        void deleteProfilePicture_Success() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long pictureId = 1L;

            // deleteProfilePicture는 void를 반환하므로, doNothing()으로 설정
            BDDMockito.doNothing().when(memberPictureCommandService).deleteProfilePicture(pictureId);

            // when
            String response = mockMvc.perform(delete("/api/v1/profile-pictures/{pictureId}", pictureId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpectAll(status().isOk())
                    .andDo(document.document(
                            requestHeaders(RestDocsUtils.HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("pictureId").attributes(getTypeFormat("NUMBER")).description("삭제할 프로필 사진의 식별자입니다.")
                            ),
                            responseFields(RestDocsUtils.commonResponseFields(
                                    fieldWithPath("data").type(JsonFieldType.NULL).description("삭제 성공 시 데이터는 null 을 반환합니다")
                            ))
                    ))
                    .andReturn().getResponse().getContentAsString();

            // then
            assertThat(response).isEqualTo(objectMapper.writeValueAsString(CommonResponse.delete(null)));
            BDDMockito.then(memberPictureCommandService).should(times(1)).deleteProfilePicture(pictureId);
            BDDMockito.then(memberPictureCommandService).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("실패: 사진이 존재하지 않을 때 서비스에서 예외 발생 시 404 Not Found를 반환한다")
        void deleteProfilePicture_Fail_PictureNotFound() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long nonExistentPictureId = 999L;

            // 서비스 레이어에서 PICTURE_NOT_FOUND 예외를 던지도록 Mocking
            BDDMockito.willThrow(new CustomException(ErrorCode.PICTURE_NOT_FOUND))
                    .given(memberPictureCommandService).deleteProfilePicture(nonExistentPictureId);

            // when & then
            mockMvc.perform(delete("/api/v1/profile-pictures/{pictureId}", nonExistentPictureId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpectAll(status().isNotFound())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.PICTURE_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.PICTURE_NOT_FOUND.getMessage()));

            BDDMockito.then(memberPictureCommandService).should(times(1)).deleteProfilePicture(nonExistentPictureId);
        }

        @Test
        @DisplayName("실패: 사진이 사용자의 소유가 아닐 때 서비스에서 예외 발생 시 403 Forbidden을 반환한다")
        void deleteProfilePicture_Fail_AuthorizationDenied() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long othersPictureId = 2L;

            // 서비스 레이어에서 AUTHORIZATION_DENIED 예외를 던지도록 Mocking
            BDDMockito.willThrow(new CustomException(ErrorCode.AUTHORIZATION_DENIED))
                    .given(memberPictureCommandService).deleteProfilePicture(othersPictureId);

            // when & then
            mockMvc.perform(delete("/api/v1/profile-pictures/{pictureId}", othersPictureId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpectAll(status().isForbidden())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.AUTHORIZATION_DENIED.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.AUTHORIZATION_DENIED.getMessage()));

            BDDMockito.then(memberPictureCommandService).should(times(1)).deleteProfilePicture(othersPictureId);
        }

        @Test
        @DisplayName("실패: 사진 업로드 중 서버 내부 오류 발생 시 500 Internal Server Error를 반환한다")
        void createProfilePicture_Fail_InternalServerError() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            MockMultipartFile profilePictureFile = new MockMultipartFile(
                    "profilePicture",
                    "my-profile.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "dummy-image-content".getBytes()
            );

            // 서비스 레이어에서 PICTURE_UPLOAD_FAILED 예외를 던지도록 Mocking
            BDDMockito.given(memberPictureCommandService.createProfilePicture(any(MockMultipartFile.class)))
                    .willThrow(new CustomException(ErrorCode.PICTURE_UPLOAD_FAILED));

            // when & then
            mockMvc.perform(MockMvcRequestBuilders
                            .multipart("/api/v1/profile-pictures")
                            .file(profilePictureFile)
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpectAll(status().isInternalServerError()) // 500 상태 코드 검증
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.PICTURE_UPLOAD_FAILED.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.PICTURE_UPLOAD_FAILED.getMessage()));

            BDDMockito.then(memberPictureCommandService).should(times(1)).createProfilePicture(any(MockMultipartFile.class));
        }
    }
}