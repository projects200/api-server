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
import com.project200.undabang.member.service.MemberPictureCommandService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberPictureCommandServiceImpl implements MemberPictureCommandService {

    private final MemberRepository memberRepository;
    private final MemberPictureRepository memberPictureRepository;
    private final PictureService pictureService;
    private final PictureRepository pictureRepository;

    /**
     * 사용자가 업로드한 프로필 사진을 S3에 저장하고, 이를 데이터베이스에 기록한 뒤
     * 사용자의 대표 프로필 사진을 업데이트합니다.
     *
     * @param profilePicture 업로드할 프로필 사진을 포함한 MultipartFile 객체
     * @return 저장된 프로필 사진과 관련 썸네일 URL 정보를 포함한 CreateProfilePictureResponse 객체
     * @throws CustomException 파일 업로드 실패 시 또는 회원 정보를 찾을 수 없는 경우 발생
     */
    @Override
    public CreateProfilePictureResponse createProfilePicture(@NotNull MultipartFile profilePicture) {
        Member member = getMember(UserContextHolder.getUserId());

        // 프로필 사진을 S3에 업로드 및 디비에 저장
        Picture savedPicture = pictureService.uploadPictureToS3AndDB(profilePicture, FileType.PROFILE);

        // Todo : 썸네일 사진 생성 과정 개발 필요
        // 그 후 DB에 저장해야 함. 현재는 null 값만 저장
        MemberPicture savedMemberPicture = memberPictureRepository.save(MemberPicture.from(member, savedPicture));

        // 대표 프로필 사진 변경
        member.updateProfilePicture(savedMemberPicture);

        return CreateProfilePictureResponse.from(savedPicture);
    }

    /**
     * 사용자의 대표 프로필 사진을 업데이트합니다.
     * 주어진 사진 ID에 해당하는 사진이 존재하고, 사용자의 사진인 경우 사용자의 대표 프로필 사진을 변경합니다.
     *
     * @param pictureId 업데이트할 프로필 사진의 ID
     * @return 변경된 대표 프로필 사진 정보를 담고 있는 UpdateProfilePictureResponse 객체
     * @throws CustomException 사진이 존재하지 않거나 사용 권한이 없는 경우 발생
     */
    @Override
    public UpdateProfilePictureResponse updateRepresentativeProfileImage(Long pictureId) {
        Member member = getMember(UserContextHolder.getUserId());

        if (!pictureRepository.existsByIdAndPictureDeletedAtNull(pictureId)) {
            throw new CustomException(ErrorCode.PICTURE_NOT_FOUND);
        }

        MemberPicture memberPicture = memberPictureRepository.findByMemberAndPicture_IdAndPicture_PictureDeletedAtNull(member, pictureId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTHORIZATION_DENIED));

        member.updateProfilePicture(memberPicture);

        return UpdateProfilePictureResponse.from(member);
    }

    /**
     * 주어진 사진 ID에 해당하는 프로필 사진을 삭제합니다.
     * 사진이 존재하지 않거나 사용 권한이 없는 경우 예외가 발생합니다.
     * 기존 대표 프로필 사진을 삭제한 경우, 삭제 후 가장 최신 사진으로 대표 프로필 사진을 갱신하거나 null로 업데이트합니다.
     *
     * @param pictureId 삭제할 프로필 사진의 ID
     * @throws CustomException 사진이 존재하지 않거나 사용 권한이 없는 경우 발생
     */
    @Override
    public void deleteProfilePicture(Long pictureId) {
        Member member = getMember(UserContextHolder.getUserId());
        Picture picture = getPicture(pictureId);

        MemberPicture memberPicture = validateAndGetMemberPicture(member, picture);

        // 썸네일 사진, 프로필 사진 논리적 삭제 및 S3에 저장된 프로필 사진 삭제
        deleteProfileAndThumbnailImageFromS3AndDB(picture, memberPicture);
        // 회원 테이블의 대표사진을 제거합니다. 그 후, 가장 최근에 생성된 사진 데이터를 넣어주고 그렇지 않으면 null 을 넣어줘야 합니다.
        updateRepresentativeProfileImageAfterDelete(member, memberPicture);

        // todo 썸네일 생성 이후 삭제를 진행해야 함 S3에 업로드 및 삭제 과정에 대한 고려 후 개발 진행
    }

    /**
     * 특정 프로필 사진을 삭제한 후, 회원의 대표 프로필 사진을 갱신합니다.
     * 대표 프로필 사진이 삭제된 경우, 남아 있는 사진 중 가장 최신의 사진으로 갱신하거나, 사진이 없는 경우 null로 설정합니다.
     */
    private void updateRepresentativeProfileImageAfterDelete(Member member, MemberPicture memberPicture) {
        if (member.getMemberPicture() != null && Objects.equals(member.getMemberPicture().getId(), memberPicture.getId())) {
            memberPictureRepository.findFirstByMemberAndMemberPicturesDeletedAtNullAndPicture_PictureDeletedAtNullOrderByPicture_PictureCreatedAtDesc(member)
                    .ifPresentOrElse(
                            member::updateProfilePicture,
                            () -> member.updateProfilePicture(null)
                    );
        }
    }

    /**
     * 주어진 Picture와 MemberPicture 객체를 사용하여 해당 사진 및 관련 정보를 삭제합니다.
     */
    private void deleteProfileAndThumbnailImageFromS3AndDB(Picture picture, MemberPicture memberPicture) {
        pictureService.deletePictureFromS3AndDB(picture);
        memberPicture.deleteMemberPicture();
    }

    /**
     * 주어진 회원과 사진 정보를 검증하고, 해당 회원과 연관된 MemberPicture 정보를 반환합니다.
     */
    private MemberPicture validateAndGetMemberPicture(Member member, Picture picture) {
        return memberPictureRepository.findByMemberAndPicture_IdAndPicture_PictureDeletedAtNull(member, picture.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTHORIZATION_DENIED));
    }

    /**
     * 주어진 사진 ID를 기반으로 사진 정보를 조회합니다.
     * 사진 정보를 찾을 수 없는 경우 예외를 발생시킵니다.
     */
    private Picture getPicture(Long pictureId) {
        return pictureRepository.findById(pictureId).orElseThrow(() -> new CustomException(ErrorCode.PICTURE_NOT_FOUND));
    }

    /**
     * 주어진 회원 ID를 기반으로 회원 정보를 조회합니다.
     * 회원 정보가 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
