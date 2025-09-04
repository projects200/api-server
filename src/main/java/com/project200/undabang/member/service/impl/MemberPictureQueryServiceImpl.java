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
import java.util.Optional;
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

        // 대표사진 검색 후 없으면 null 반환
        ProfileImageRecord representativeProfileImageRecord = Optional.ofNullable(member.getMemberPicture())
                .map(MemberPicture::getPicture)
                .map(ProfileImageRecord::from)
                .orElse(null);


        List<MemberPicture> profileImageList = memberPictureRepository.findByMemberAndPicture_PictureDeletedAtNull(member);

        // 프로필 사진 리스트에 추가
        List<ProfileImageRecord> profileImageRecordList = profileImageList.stream()
                .map(MemberPicture::getPicture)
                .map(ProfileImageRecord::from)
                .toList();

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