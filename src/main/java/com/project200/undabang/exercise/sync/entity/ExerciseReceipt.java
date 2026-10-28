package com.project200.undabang.exercise.sync.entity;

import com.project200.undabang.exercise.sync.ExerciseOperation;
import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "exercise_receipts", uniqueConstraints = @UniqueConstraint(
        name = "uk_exercise_receipt_member_request", columnNames = {"member_id", "request_id"}))
public class ExerciseReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_receipt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_id", nullable = false, updatable = false, columnDefinition = "char(36)")
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, updatable = false, length = 20)
    private ExerciseOperation operation;

    // 삭제된 기록의 원본 응답도 유지하므로 운동/사진 FK를 두지 않습니다.
    @Column(name = "exercise_id", updatable = false)
    private Long exerciseId;

    @Column(nullable = false, updatable = false, length = 64)
    private String fingerprint;

    @Lob
    @Column(name = "response_json", nullable = false, updatable = false, columnDefinition = "text")
    private String responseJson;

    public static ExerciseReceipt of(Member member, UUID requestId, ExerciseOperation operation,
                                     Long exerciseId, String fingerprint, String responseJson) {
        ExerciseReceipt receipt = new ExerciseReceipt();
        receipt.member = member;
        receipt.requestId = requestId;
        receipt.operation = operation;
        receipt.exerciseId = exerciseId;
        receipt.fingerprint = fingerprint;
        receipt.responseJson = responseJson;
        return receipt;
    }
}
