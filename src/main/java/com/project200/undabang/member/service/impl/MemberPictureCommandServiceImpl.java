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
     * 주어진 회원 ID를 기반으로 회원 정보를 조회합니다.
     * 회원 정보가 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
