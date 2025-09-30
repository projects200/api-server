package com.project200.undabang.member.entity;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.command.SignUpMemberCommand;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.openchat.entity.OpenChatRoom;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    @Column(name = "member_gender", columnDefinition = "char(1)")
    @Builder.Default
    private MemberGender memberGender = MemberGender.UNKNOWN;

    @Column(name = "member_bday")
    private LocalDate memberBday;

    @Size(max = 50)
    @NotNull
    @Column(name = "member_nickname", nullable = false, length = 50, unique = true)
    private String memberNickname;

    @Size(max = 500)
    @Column(name = "member_desc", length = 500)
    private String memberDesc;

    @Comment("0~100")
    @NotNull
    @Column(name = "member_score")
    @Builder.Default
    private Byte memberScore = (byte) 0;

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

    // 프로필 사진 식별자 필드 추가
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_picture_id")
    private MemberPicture memberPicture;

    @OneToMany(mappedBy = "member")
    private List<PreferredExercise> preferredExercises = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<ExerciseLocation> exerciseLocations = new ArrayList<>();

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    @Where(clause = "open_chatroom_deleted_at IS NULL")
    private List<OpenChatRoom> activeOpenChatRoomList;


    /**
     * 회원의 점수를 증가시킵니다. 점수는 정책에 정의된 최소/최대 값을 벗어나지 않습니다.
     *
     * @param pointsToAdd 추가할 점수
     * @param minScore    허용되는 최소 점수
     * @param maxScore    허용되는 최대 점수
     * @return 실제로 증가한 점수
     */
    public byte addScore(byte pointsToAdd, byte minScore, byte maxScore) {
        byte oldScore = this.memberScore;
        int newScore = oldScore + pointsToAdd;

        if (newScore > maxScore) {
            this.memberScore = maxScore;
        } else if (newScore < minScore) {
            this.memberScore = minScore;
        } else {
            this.memberScore = (byte) newScore;
        }

        // 실제 변경된 점수 = 변경 후 점수 - 변경 전 점수
        return (byte) (this.memberScore - oldScore);
    }

    /**
     * 새로운 회원을 생성하는 메서드입니다. 입력받은 회원 정보에 따라 유효성을 검증한 후
     * 회원 엔티티 객체를 생성합니다.
     *
     * @param command 회원 가입에 필요한 정보를 담고 있는 커맨드 객체
     *                - memberId: 회원 ID
     *                - memberEmail: 회원 이메일
     *                - memberNickname: 회원 닉네임
     *                - memberGender: 회원 성별
     *                - initialSignupPoints: 초기 가입 점수
     *                - memberBday: 회원 생년월일
     * @return 생성된 회원 엔티티 객체
     * @throws NullPointerException 입력된 커맨드 객체의 필수 필드가 null인 경우
     * @throws CustomException      회원 생년월일이 현재 날짜 이후인 경우
     */
    public static Member signUp(SignUpMemberCommand command) {
        // 필수 필드 검증
        Objects.requireNonNull(command.memberId(), "Member ID는 null일 수 없습니다.");
        Objects.requireNonNull(command.memberEmail(), "Member Email은 null일 수 없습니다.");
        Objects.requireNonNull(command.memberNickname(), "Member Nickname은 null일 수 없습니다.");
        Objects.requireNonNull(command.memberGender(), "Member Gender는 null일 수 없습니다.");
        Objects.requireNonNull(command.memberBday(), "Member Birthday는 null일 수 없습니다.");

        // 생년월일 유효성 검증
        // 생년월일이 현재 날짜 이전인지 확인
        if (!command.memberBday().isBefore(LocalDate.now())) {
            throw new CustomException((ErrorCode.MEMBER_BDAY_ERROR));
        }

        return Member.builder()
                .memberId(command.memberId())
                .memberEmail(command.memberEmail())
                .memberNickname(command.memberNickname())
                .memberGender(command.memberGender())
                .memberScore(command.initialSignupPoints())
                .memberBday(command.memberBday())
                .build();
    }

    public void decreaseMemberScore(int decreaseScore){
        if(this.memberScore > 0){
            this.memberScore = (byte) Math.max(0, this.memberScore - decreaseScore);
        }
    }

    // 대표 사진 변경
    public void updateProfilePicture(MemberPicture memberPicture) {
        this.memberPicture = memberPicture;
    }

    /**
     * 회원의 정보를 업데이트합니다.
     * 제공된 닉네임, 성별, 자기소개를 기반으로 회원 정보를 수정합니다.
     *
     * @param nickname 새로운 닉네임
     * @param gender   새로운 성별 정보
     * @param bio      새로운 자기소개 내용
     */
    public void updateMemberInfo(String nickname, MemberGender gender, String bio) {
        this.memberNickname = nickname;
        this.memberGender = gender;
        this.memberDesc = bio;
    }
}