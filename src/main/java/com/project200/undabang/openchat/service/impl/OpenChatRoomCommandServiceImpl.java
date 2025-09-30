package com.project200.undabang.openchat.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.openchat.dto.request.CreateOpenChatRoomRequest;
import com.project200.undabang.openchat.dto.request.UpdateOpenChatRoomRequest;
import com.project200.undabang.openchat.dto.response.CreateOpenChatRoomResponse;
import com.project200.undabang.openchat.dto.response.UpdateOpenChatRoomResponse;
import com.project200.undabang.openchat.entity.OpenChatRoom;
import com.project200.undabang.openchat.repository.OpenChatRoomRepository;
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
     * 요청된 URL의 정규화 및 유효성 검사를 수행하며, 생성 중 데이터 충돌이 발생할 경우 예외를 반환합니다.
     */
    @Override
    @Transactional
    public CreateOpenChatRoomResponse createOpenChatRoom(CreateOpenChatRoomRequest request) {
        Member member = getMember(UserContextHolder.getUserId());
        String openChatroomUrl = normalizeUrl(request.getOpenChatroomUrl());

        validateForOpenChatRoomCreation(member, openChatroomUrl);

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
     * 주어진 오픈채팅방 ID와 요청 데이터를 기반으로 오픈채팅방 정보를 업데이트합니다.
     */
    @Override
    @Transactional
    public UpdateOpenChatRoomResponse updateOpenChatRoom(Long openChatRoomId, UpdateOpenChatRoomRequest request) {
        Member member = getMember(UserContextHolder.getUserId());
        String openChatUrl = normalizeUrl(request.getOpenChatroomUrl());

        OpenChatRoom openChatRoom = getOpenChatRoom(member, openChatRoomId);

        if (openChatRoom.isSameUrl(openChatUrl)) {
            return UpdateOpenChatRoomResponse.of(openChatRoomId);
        }

        validateUrlIsUniqueForUpdate(openChatUrl, openChatRoomId);

        openChatRoom.updateOpenChatUrl(openChatUrl);

        return UpdateOpenChatRoomResponse.of(openChatRoom.getId());
    }

    /**
     * 주어진 오픈채팅방 URL이 중복되지 않았는지 검사합니다.
     */
    private void validateUrlIsUniqueForUpdate(String openChatUrl, Long currentChatRoomId) {
        if (openChatRoomRepository.existsByUrlAndIdNotAndDeletedAtNull(openChatUrl, currentChatRoomId)) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_URL_DUPLICATED);
        }
    }

    /**
     * 오픈채팅방 생성을 위한 유효성 검사를 수행합니다.
     * 검사 대상
     * 1. 회원이 이미 오픈 채팅방을 보유중인지 확인
     * 2. 생성하려는 URL이 다른 사용자가 사용중인 URL인지 확인
     */
    private void validateForOpenChatRoomCreation(Member member, String openChatUrl) {
        if (openChatRoomRepository.existsByMemberAndDeletedAtNull(member)) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_ALREADY_EXIST);
        }

        if (openChatRoomRepository.existsByUrlAndDeletedAtNull(openChatUrl)) {
            throw new CustomException(ErrorCode.OPEN_CHAT_ROOM_URL_DUPLICATED);
        }
    }

    /**
     * 주어진 URL을 정규화합니다. URL이 "http://"로 시작하는 경우 "https://"로 변경합니다.
     */
    private String normalizeUrl(String url) {
        if (url.startsWith("http://")) {
            // 혹시 쿼리 파라미터로 redirect?=http:// 가 있을 수 있으므로 프로토콜 부분만 검사
            return url.replaceFirst("http://", "https://");
        }

        return url;
    }

    /**
     * 지정된 회원 ID에 해당하는 회원 정보를 조회합니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 지정된 회원과 오픈채팅방 ID를 기반으로 오픈채팅방 정보를 조회합니다.
     * 회원이 소유하지 않은 오픈채팅방에 접근할 경우 예외를 발생시킵니다.
     */
    private OpenChatRoom getOpenChatRoom(Member member, Long openChatId) {
        OpenChatRoom openChatRoom = openChatRoomRepository.findByIdAndDeletedAtNull(openChatId).orElseThrow(
                () -> new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND)
        );

        // 자신이 소유한 오픈 채팅방이 아닐경우 403 에러
        if (!member.getMemberId().equals(openChatRoom.getMember().getMemberId())) {
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
        }

        return openChatRoom;
    }
}
