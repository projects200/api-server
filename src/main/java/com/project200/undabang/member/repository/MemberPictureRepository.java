package com.project200.undabang.member.repository;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberPictureRepository extends JpaRepository<MemberPicture, Long> {

    @EntityGraph(attributePaths = "picture")
    List<MemberPicture> findByMemberAndPicture_PictureDeletedAtNull(Member member);
}