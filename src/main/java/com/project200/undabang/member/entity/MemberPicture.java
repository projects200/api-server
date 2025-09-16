package com.project200.undabang.member.entity;

import com.project200.undabang.common.entity.Picture;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "member_pictures")
public class MemberPicture {
    @Id
    @Column(name = "picture_id", nullable = false)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "picture_id", nullable = false)
    private Picture picture;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Size(max = 255)
    @Column(name = "member_pictures_name")
    private String memberPicturesName;

    @org.hibernate.annotations.Comment("바이트 단위")
    @Column(name = "member_pictures_size")
    private Integer memberPicturesSize;

    @Size(max = 255)
    @Column(name = "member_pictures_url")
    private String memberPicturesUrl;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "member_pictures_created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime memberPicturesCreatedAt = LocalDateTime.now();

    @Column(name = "member_pictures_deleted_at")
    private LocalDateTime memberPicturesDeletedAt;

    /**
     * 주어진 Member와 Picture 객체를 기반으로 MemberPicture 객체를 생성합니다.
     */
    public static MemberPicture from(Member member, Picture picture) {
        return MemberPicture.builder()
                .member(member)
                .picture(picture)
                .memberPicturesCreatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 회원 사진이 삭제된 시간(memberPicturesDeletedAt)을 현재 시간으로 설정합니다.
     * 이를 통해 회원 사진이 삭제되었음을 나타냅니다.
     */
    public void deleteMemberPicture() {
        this.memberPicturesDeletedAt = LocalDateTime.now();
    }
}