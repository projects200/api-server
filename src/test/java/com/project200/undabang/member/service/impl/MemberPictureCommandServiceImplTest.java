package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.service.FileType;
import com.project200.undabang.common.service.PictureService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.response.CreateProfilePictureResponse;
import com.project200.undabang.member.dto.response.UpdateProfilePictureResponse;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.repository.MemberPictureRepository;
import com.project200.undabang.member.repository.MemberRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MemberPictureCommandServiceImplTest {

    @InjectMocks
    private MemberPictureCommandServiceImpl memberPictureCommandService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberPictureRepository memberPictureRepository;

    @Mock
    private PictureService pictureService;

    @Mock
    private PictureRepository pictureRepository;

    @Nested
    @DisplayName("프로필 사진 생성 성공 케이스")
    class CreateProfilePictureSuccessTests {

        @Test
        @DisplayName("모든 조건이 유효할 때 프로필 사진이 성공적으로 생성되고 업데이트된다")
        void createProfilePicture_Success() {
            // given
            UUID testUserId = UUID.randomUUID();
            Member testMember = createTestMember(testUserId);
            MultipartFile mockFile = createTestMultipartFile();

            Picture savedPicture = Picture.builder()
                    .id(1L)
                    .pictureName("profile.jpg")
                    .pictureUrl("https://s3.com/some-url/profile.jpg")
                    .build();

            MemberPicture savedMemberPicture = MemberPicture.from(testMember, savedPicture);

            try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // Mocking static method
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);

                // Mocking repository and service calls
                BDDMockito.given(memberRepository.findById(testUserId)).willReturn(Optional.of(testMember));
                BDDMockito.given(pictureService.uploadPictureToS3AndDB(mockFile, FileType.PROFILE)).willReturn(savedPicture);
                BDDMockito.given(memberPictureRepository.save(any(MemberPicture.class))).willReturn(savedMemberPicture);

                // when
                CreateProfilePictureResponse result = memberPictureCommandService.createProfilePicture(mockFile);

                // then
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(result).as("응답 객체는 null이 아니어야 함").isNotNull();
                    softly.assertThat(result.getPictureId()).as("올바른 Picture ID가 반환되어야 함").isEqualTo(savedPicture.getId());
                    softly.assertThat(result.getProfileImageUrl()).as("올바른 원본 이미지 URL이 반환되어야 함").isEqualTo(savedPicture.getPictureUrl());
                });

                // Member 엔티티의 상태가 올바르게 변경되었는지 검증
                assertThat(testMember.getMemberPicture()).as("Member 객체의 프로필 사진이 업데이트되어야 함").isEqualTo(savedMemberPicture);

                // Mock 객체들의 상호작용 검증
                BDDMockito.then(memberRepository).should(BDDMockito.times(1)).findById(testUserId);
                BDDMockito.then(pictureService).should(BDDMockito.times(1)).uploadPictureToS3AndDB(mockFile, FileType.PROFILE);
                BDDMockito.then(memberPictureRepository).should(BDDMockito.times(1)).save(any(MemberPicture.class));
            }
        }
    }

    @Nested
    @DisplayName("프로필 사진 생성 실패 케이스")
    class CreateProfilePictureFailureTests {

        @Test
        @DisplayName("회원을 찾을 수 없을 때 CustomException(MEMBER_NOT_FOUND)을 던진다")
        void createProfilePicture_Fails_WhenMemberNotFound() {
            // given
            UUID testUserId = UUID.randomUUID();
            MultipartFile mockFile = createTestMultipartFile();

            try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findById(testUserId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberPictureCommandService.createProfilePicture(mockFile))
                        .as("회원을 찾을 수 없을 때 예외가 발생해야 함")
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

                // 실패했으므로 PictureService나 MemberPictureRepository는 호출되지 않아야 함
                BDDMockito.then(pictureService).should(BDDMockito.never()).uploadPictureToS3AndDB(any(MultipartFile.class), any(FileType.class));
                BDDMockito.then(pictureService).should(BDDMockito.never()).uploadPictureToS3AndDB(any(MultipartFile.class), any(String.class));
                BDDMockito.then(memberPictureRepository).should(BDDMockito.never()).save(any());
            }
        }

        @Test
        @DisplayName("S3 또는 DB에 Picture 저장 실패 시 CustomException을 던진다")
        void createProfilePicture_Fails_WhenPictureUploadFails() {
            // given
            UUID testUserId = UUID.randomUUID();
            Member testMember = createTestMember(testUserId);
            MultipartFile mockFile = createTestMultipartFile();

            try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findById(testUserId)).willReturn(Optional.of(testMember));
                // PictureService에서 예외 발생 시나리오 Mocking
                BDDMockito.given(pictureService.uploadPictureToS3AndDB(mockFile, FileType.PROFILE))
                        .willThrow(new CustomException(ErrorCode.PICTURE_UPLOAD_FAILED));

                // when & then
                assertThatThrownBy(() -> memberPictureCommandService.createProfilePicture(mockFile))
                        .as("사진 업로드 실패 시 예외가 발생해야 함")
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICTURE_UPLOAD_FAILED);

                // 실패했으므로 MemberPictureRepository는 호출되지 않아야 함
                BDDMockito.then(memberPictureRepository).should(BDDMockito.never()).save(any());
            }
        }

        @Test
        @DisplayName("MemberPicture 저장 중 DB 에러 발생 시 RuntimeException을 던진다")
        void createProfilePicture_Fails_WhenMemberPictureSaveFails() {
            // given
            UUID testUserId = UUID.randomUUID();
            Member testMember = createTestMember(testUserId);
            MultipartFile mockFile = createTestMultipartFile();
            Picture savedPicture = Picture.builder().id(1L).build();

            try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findById(testUserId)).willReturn(Optional.of(testMember));
                BDDMockito.given(pictureService.uploadPictureToS3AndDB(mockFile, FileType.PROFILE)).willReturn(savedPicture);
                // MemberPictureRepository 저장 시 예외 발생 시나리오 Mocking
                BDDMockito.given(memberPictureRepository.save(any(MemberPicture.class)))
                        .willThrow(new RuntimeException("Database save error"));

                // when & then
                assertThatThrownBy(() -> memberPictureCommandService.createProfilePicture(mockFile))
                        .as("MemberPicture 저장 실패 시 예외가 발생해야 함")
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Database save error");

                // 상위 로직들이 정상적으로 1번씩 호출되었는지 검증
                BDDMockito.then(memberRepository).should(BDDMockito.times(1)).findById(testUserId);
                BDDMockito.then(pictureService).should(BDDMockito.times(1)).uploadPictureToS3AndDB(mockFile, FileType.PROFILE);
            }
        }
    }

    private Member createTestMember(UUID memberId) {
        return Member.builder().memberId(memberId).build();
    }

    private Picture createTestPicture(Long pictureId) {
        return Picture.builder().id(pictureId).build();
    }

    private MemberPicture createTestMemberPicture(Member member, Picture picture) {
        return MemberPicture.builder()
                .id(picture.getId())
                .member(member)
                .picture(picture)
                .build();
    }

    private MultipartFile createTestMultipartFile() {
        return new MockMultipartFile(
                "profilePicture",
                "profile.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test-image-content".getBytes()
        );
    }

    @Nested
    @DisplayName("대표 프로필 사진 변경 테스트")
    class UpdateRepresentativeProfileImageTests {

        @Test
        @DisplayName("성공: 모든 조건이 유효할 때 대표 프로필 사진이 성공적으로 변경된다")
        void updateRepresentativeProfileImage_Success() {
            // given
            UUID testUserId = UUID.randomUUID();
            Long pictureId = 100L;
            Member testMember = createTestMember(testUserId);
            Picture testPicture = createTestPicture(pictureId);
            MemberPicture testMemberPicture = createTestMemberPicture(testMember, testPicture);

            try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // Mocking static method and repository calls
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findById(testUserId)).willReturn(Optional.of(testMember));
                BDDMockito.given(pictureRepository.existsByIdAndPictureDeletedAtNull(pictureId)).willReturn(true);
                BDDMockito.given(memberPictureRepository.findByMemberAndPicture_IdAndPicture_PictureDeletedAtNull(testMember, pictureId))
                        .willReturn(Optional.of(testMemberPicture));

                // when
                UpdateProfilePictureResponse result = memberPictureCommandService.updateRepresentativeProfileImage(pictureId);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getProfileImageId()).isEqualTo(testMemberPicture.getId());

                // Member 객체의 상태가 올바르게 변경되었는지 검증
                assertThat(testMember.getMemberPicture()).isEqualTo(testMemberPicture);

                // Mock 객체 상호작용 검증
                BDDMockito.then(memberRepository).should().findById(testUserId);
                BDDMockito.then(pictureRepository).should().existsByIdAndPictureDeletedAtNull(pictureId);
                BDDMockito.then(memberPictureRepository).should().findByMemberAndPicture_IdAndPicture_PictureDeletedAtNull(testMember, pictureId);
            }
        }

        @Test
        @DisplayName("실패: 회원을 찾을 수 없을 때 CustomException(MEMBER_NOT_FOUND)을 던진다")
        void updateRepresentativeProfileImage_Fails_WhenMemberNotFound() {
            // given
            UUID testUserId = UUID.randomUUID();
            Long pictureId = 100L;

            try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findById(testUserId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberPictureCommandService.updateRepresentativeProfileImage(pictureId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

                // 이후 로직이 호출되지 않았는지 검증
                BDDMockito.then(pictureRepository).should(BDDMockito.never()).existsByIdAndPictureDeletedAtNull(any());
                BDDMockito.then(memberPictureRepository).should(BDDMockito.never()).findByMemberAndPicture_IdAndPicture_PictureDeletedAtNull(any(), any());
            }
        }

        @Test
        @DisplayName("실패: 사진이 존재하지 않거나 삭제되었을 때 CustomException(PICTURE_NOT_FOUND)을 던진다")
        void updateRepresentativeProfileImage_Fails_WhenPictureNotFound() {
            // given
            UUID testUserId = UUID.randomUUID();
            Long pictureId = 100L;
            Member testMember = createTestMember(testUserId);

            try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findById(testUserId)).willReturn(Optional.of(testMember));
                BDDMockito.given(pictureRepository.existsByIdAndPictureDeletedAtNull(pictureId)).willReturn(false);

                // when & then
                assertThatThrownBy(() -> memberPictureCommandService.updateRepresentativeProfileImage(pictureId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICTURE_NOT_FOUND);

                BDDMockito.then(memberPictureRepository).should(BDDMockito.never()).findByMemberAndPicture_IdAndPicture_PictureDeletedAtNull(any(), any());
            }
        }

        @Test
        @DisplayName("실패: 사진이 현재 사용자의 소유가 아닐 때 CustomException(AUTHORIZATION_DENIED)을 던진다")
        void updateRepresentativeProfileImage_Fails_WhenPictureNotOwnedByUser() {
            // given
            UUID testUserId = UUID.randomUUID();
            Long pictureId = 100L;
            Member testMember = createTestMember(testUserId);

            try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findById(testUserId)).willReturn(Optional.of(testMember));
                BDDMockito.given(pictureRepository.existsByIdAndPictureDeletedAtNull(pictureId)).willReturn(true);
                // 사용자의 사진이 아니므로 empty Optional 반환
                BDDMockito.given(memberPictureRepository.findByMemberAndPicture_IdAndPicture_PictureDeletedAtNull(testMember, pictureId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> memberPictureCommandService.updateRepresentativeProfileImage(pictureId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHORIZATION_DENIED);
            }
        }
    }

}