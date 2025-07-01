package com.project200.undabang.member.entity;

import com.project200.undabang.member.entity.converter.MemberGenderConverter;
import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "member_id", nullable = false, updatable = false, columnDefinition = "char(36)")
    private UUID memberId;

    @Size(max = 320)
    @NotNull
    @Column(name = "member_email", length = 320, unique = true)
    private String memberEmail;

    @Comment("M: 남 / F: 여 / U: 비공개")
    @ColumnDefault("'U'")
    @Convert(converter = MemberGenderConverter.class)
    @Column(name = "member_gender", columnDefinition = "char(1)")
    @Builder.Default
    private MemberGender memberGender = MemberGender.U;

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
    @Builder.Default
    private Byte memberScore = 35;

    @NotNull
    @Comment("관리자 처리 신고 누적")
    @ColumnDefault("0")
    @Column(name = "member_warned_count", nullable = false)
    @Builder.Default
    private Byte memberWarnedCount = 0;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "member_created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime memberCreatedAt = LocalDateTime.now();

    @Comment("탈퇴 시 삭제 일시 기록")
    @Column(name = "member_deleted_at")
    private LocalDateTime memberDeletedAt;

    public static Member createFromSignUp(UUID memberId, String memberEmail, String memberNickname, MemberGender memberGender, LocalDate memberBday){
        return Member.builder()
                .memberId(memberId)
                .memberEmail(memberEmail)
                .memberNickname(memberNickname)
                .memberGender(memberGender)
                .memberScore((byte) 0)
                .memberBday(memberBday)
                .build();
    }
}