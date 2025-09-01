package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.record.ProfileImageRecord;
import com.project200.undabang.member.dto.response.GetProfilePictureResponse;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.repository.MemberPictureRepository;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.MemberPictureQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberPictureQueryServiceImpl implements MemberPictureQueryService {
    private final MemberPictureRepository memberPictureRepository;
    private final MemberRepository memberRepository;

    /**
     * 유저의 프로필 사진 정보를 조회합니다.
     * 대표 프로필 사진과 추가 프로필 사진들의 리스트를 반환합니다.
     * 만약 대표 프로필 사진이 없을 경우 null을 반환합니다.
     */
    @Override
    public GetProfilePictureResponse getProfilePicture() {
        Member member = getMember(UserContextHolder.getUserId());
        List<ProfileImageRecord> profileImageRecordList = new ArrayList<>();

        // 대표사진 우선 검색
        // 대표 사진이 없으면 (사진이 없는 경우) null 반환
        if (member.getMemberPicture() == null || member.getMemberPicture().getPicture() == null) {
            return null;
        }
        Picture picture = member.getMemberPicture().getPicture();
        ProfileImageRecord representativeProfileImageRecord = ProfileImageRecord.from(picture);

        // 프로필 사진 리스트에 추가
        List<MemberPicture> profileImageList = memberPictureRepository.findByMemberAndPicture_PictureDeletedAtNull(member);
        for (MemberPicture profileImage : profileImageList) {
            profileImageRecordList.add(ProfileImageRecord.from(profileImage.getPicture()));
        }

        return GetProfilePictureResponse.from(representativeProfileImageRecord, profileImageRecordList);
    }

    /**
     * 주어진 회원 ID를 사용하여 회원 정보를 조회합니다.
     * 해당 ID에 해당하는 회원이 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}