package com.project200.undabang.member.repository;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberPictureRepository extends JpaRepository<MemberPicture, Long> {

    @EntityGraph(attributePaths = "picture")
    List<MemberPicture> findByMemberAndPicture_PictureDeletedAtNull(Member member);

    Optional<MemberPicture> findByMemberAndPicture_IdAndPicture_PictureDeletedAtNull(Member member, Long id);

    Optional<MemberPicture> findFirstByMemberAndMemberPicturesDeletedAtNullAndPicture_PictureDeletedAtNullOrderByPicture_PictureCreatedAtDesc(Member member);
}