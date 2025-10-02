package com.project200.undabang.openchat.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.match.service.MatchService;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.openchat.dto.response.GetOpenChatUrlResponse;
import com.project200.undabang.openchat.dto.response.GetOtherMemberOpenChatUrlResponse;
import com.project200.undabang.openchat.entity.OpenChatRoom;
import com.project200.undabang.openchat.repository.OpenChatRoomRepository;
import com.project200.undabang.openchat.service.OpenChatRoomQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenChatRoomQueryServiceImpl implements OpenChatRoomQueryService {

    private final OpenChatRoomRepository openChatRoomRepository;
    private final MemberRepository memberRepository;
    private final MatchService matchService;

    /**
     * 현재 사용자와 연관된 오픈 채팅방의 URL을 조회하여 반환합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public GetOpenChatUrlResponse getOpenChatroomUrl() {
        OpenChatRoom openChatRoom = getMemberOpenChatRoom(UserContextHolder.getUserId());

        return GetOpenChatUrlResponse.of(openChatRoom.getId(), openChatRoom.getUrl());
    }

    /**
     * 주어진 회원 ID를 기반으로 해당 회원의 오픈 채팅방 URL을 조회하고 반환합니다.
     * 매칭 서비스 호출을 통해 회원 간 매칭 정보를 생성하며, 추후 성공/실패/취소 상태는 업데이트 됩니다.
     */
    @Override
    @Transactional
    public GetOtherMemberOpenChatUrlResponse getOtherMemberOpenChatroomUrl(UUID memberId) {
        OpenChatRoom openChatRoom = getMemberOpenChatRoom(memberId);
        UUID requesterMemberId = UserContextHolder.getUserId();

        if (requesterMemberId.equals(memberId)) {
            // 자신의 정보로 조회하려는 경우는 400 에러 반환
            throw new CustomException(ErrorCode.MEMBER_SELF_REQUEST_NOT_ALLOWED);
        }

        // 회원간의 매칭 정보를 연관시킴
        // todo : 현재는 매칭이 성공하지 않아도 비동기적으로 DB에 저장하도록 설정함. 차후에는 매칭 성공/실패/취소 여부와 매칭 처리 시간을 저장해야 함 (일단은 보류상태로 저장).
        matchService.createMatchRecordBetweenMembers(requesterMemberId, memberId);

        return GetOtherMemberOpenChatUrlResponse.of(openChatRoom.getUrl());
    }

    /**
     * 주어진 멤버 ID를 기반으로 삭제되지 않은 오픈 채팅방 정보를 조회합니다.
     * 멤버 ID에 해당하는 회원 정보가 존재하지 않을 경우 예외를 발생시킵니다.
     * 해당 멤버 ID로 조회된 오픈 채팅방 정보가 없거나 이미 삭제된 경우 예외를 발생시킵니다.
     */
    private OpenChatRoom getMemberOpenChatRoom(UUID memberId) {
        OpenChatRoom openChatRoom = openChatRoomRepository.findByMember_MemberIdAndDeletedAtNull(memberId).orElse(null);

        if (openChatRoom == null) {
            if (!memberRepository.existsById(memberId)) {
                throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
            }
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
        }

        return openChatRoom;
    }
}
