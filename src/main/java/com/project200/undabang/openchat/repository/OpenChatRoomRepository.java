package com.project200.undabang.openchat.repository;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.openchat.entity.OpenChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpenChatRoomRepository extends JpaRepository<OpenChatRoom, Long> {
    boolean existsByMemberAndDeletedAtNull(Member member);

    Optional<OpenChatRoom> findByIdAndDeletedAtNull(Long id);

    boolean existsByUrlAndIdNotAndDeletedAtNull(String url, Long id);
}
