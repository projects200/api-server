package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
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
     * 사용자 프로필 사진 정보를 가져옵니다.
     * 대표 프로필 사진과 모든 프로필 사진 목록을 반환합니다.
     * 대표 사진이 없을 경우 null이 반환됩니다.
     */
    @Override
    public GetProfilePictureResponse getProfilePictures() {
        Member member = getMember(UserContextHolder.getUserId());

        // 회원의 회원사진(프로필사진) 목록 조회
        List<MemberPicture> profileImageList = memberPictureRepository.findByMemberAndPicture_PictureDeletedAtNull(member);

        // 회원의 프로필 사진 DTO 목록 생성
        List<ProfileImageRecord> profileImageRecordList = profileImageList.stream()
                .map(memberPicture -> ProfileImageRecord.from(memberPicture.getPicture()))
                .toList();

        // 회원사진 목록 기반으로 대표사진 조회
        ProfileImageRecord representativeProfileImageRecord = getRepresentativeProfileImage(member.getMemberPicture(), profileImageRecordList);

        return GetProfilePictureResponse.from(representativeProfileImageRecord, profileImageRecordList);
    }

    /**
     * 전체 프로필 DTO 목록과 대표 MemberPicture 엔티티를 받아, 목록에서 일치하는 대표 프로필 DTO를 찾아 반환합니다.
     */
    private ProfileImageRecord getRepresentativeProfileImage(MemberPicture representativeMemberPicture, List<ProfileImageRecord> profileImageRecordList) {
        // 대표사진이 없는경우 null 반환
        if (representativeMemberPicture == null) {
            return null;
        }

        long representativePictureId = representativeMemberPicture.getPicture().getId();

        // 대표사진 반환
        for (ProfileImageRecord record : profileImageRecordList) {
            if (Long.valueOf(record.profileImageId()).equals(representativePictureId)) {
                return record;
            }
        }

        // 목록을 전부 순회했지만, 일치하는 DTO 가 없는경우 null 반환
        return null;
    }

    /**
     * 주어진 회원 ID를 사용하여 회원 정보를 조회합니다.
     * 해당 ID에 해당하는 회원이 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}