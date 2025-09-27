package com.project200.undabang.openchat.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.openchat.dto.request.CreateOpenChatRoomRequest;
import com.project200.undabang.openchat.dto.response.CreateOpenChatRoomResponse;
import com.project200.undabang.openchat.entity.OpenChatRoom;
import com.project200.undabang.openchat.respository.OpenChatRoomRepository;
import com.project200.undabang.openchat.service.OpenChatRoomCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenChatRoomCommandServiceImpl implements OpenChatRoomCommandService {
    private final MemberRepository memberRepository;
    private final OpenChatRoomRepository openChatRoomRepository;

    /**
     * 새로운 오픈채팅방을 생성합니다.
     *
     * @param request 오픈채팅방 생성 요청 정보를 담고 있는 객체
     * @return 생성된 오픈채팅방의 ID를 포함한 응답 객체
     * @throws CustomException 오픈채팅방 URL 중복, 회원 정보 미존재, 또는 이미 존재하는 오픈채팅방이 있는 경우 발생
     */
    @Override
    @Transactional
    public CreateOpenChatRoomResponse createOpenChatRoom(CreateOpenChatRoomRequest request) {
        Member member = getMember(UserContextHolder.getUserId());

        String openChatroomUrl = request.getOpenChatroomUrl();

        // 이미 등록한 오픈카톡 ID가 있는지 우선 검사
        if (openChatRoomRepository.existsByMemberAndDeletedAtNull(member)) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_ALREADY_EXIST);
        }

        OpenChatRoom openChatRoom = OpenChatRoom.of(member, openChatroomUrl);

        try {
            OpenChatRoom savedOpenChatRoom = openChatRoomRepository.save(openChatRoom);
            return CreateOpenChatRoomResponse.of(savedOpenChatRoom.getId());

        } catch (DataIntegrityViolationException e) {

            log.warn("생성시 DB Race Condition 발생으로 인한 오류 발생");
            // 다른 사람이 사용중인 오픈카톡 URL로 생성 시도시 오류 반환
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_URL_DUPLICATED);
        }
    }


    /**
     * 지정된 회원 ID에 해당하는 회원 정보를 조회합니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
