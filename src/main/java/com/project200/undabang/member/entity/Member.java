package com.project200.undabang.member.entity;

import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "member_id", nullable = false, updatable = false, columnDefinition = "char(36)")
    private UUID memberId;

    @Size(max = 320)
    @Column(name = "member_email", length = 320, unique = true)
    private String memberEmail;

    @Enumerated(EnumType.STRING)
    @Comment("M: 남 / F: 여 / U: 비공개")
    @Column(name = "member_gender")
    private MemberGender memberGender;

    @Column(name = "member_bday")
    private LocalDate memberBday;

    @Size(max = 50)
    @NotNull
    @Column(name = "member_nickname", nullable = false, length = 50, unique = true)
    private String memberNickname;

    @Size(max = 500)
    @Column(name = "member_desc", length = 500)
    private String memberDesc;

    @Comment("0~100, 초기값 35")
    @ColumnDefault("35")
    @Column(name = "member_score")
    private Byte memberScore = 35;

    @NotNull
    @Comment("관리자 처리 신고 누적")
    @ColumnDefault("0")
    @Column(name = "member_warned_count", nullable = false)
    private Byte memberWarnedCount = 0;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "member_created_at", nullable = false, updatable = false)
    private LocalDateTime memberCreatedAt = LocalDateTime.now();

    @Comment("탈퇴 시 삭제 일시 기록")
    @Column(name = "member_deleted_at")
    private LocalDateTime memberDeletedAt;
}