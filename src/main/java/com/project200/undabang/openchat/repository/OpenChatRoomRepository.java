package com.project200.undabang.openchat.repository;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.openchat.entity.OpenChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OpenChatRoomRepository extends JpaRepository<OpenChatRoom, Long> {
    boolean existsByMemberAndDeletedAtNull(Member member);
    boolean existsByUrlAndIdNotAndDeletedAtNull(String url, Long id);
    boolean existsByUrlAndDeletedAtNull(String url);
    Optional<OpenChatRoom> findByIdAndDeletedAtNull(Long id);
    Optional<OpenChatRoom> findByMember_MemberIdAndDeletedAtNull(UUID memberId);
}
