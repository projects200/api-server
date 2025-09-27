package com.project200.undabang.openchat.respository;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.openchat.entity.OpenChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenChatRoomRepository extends JpaRepository<OpenChatRoom, Long> {
    boolean existsByMemberAndDeletedAtNull(Member member);

    boolean existsByUrlAndDeletedAtNull(String url);
}
