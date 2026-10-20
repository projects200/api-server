package com.project200.undabang.timer.sync.entity;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.timer.sync.TimerEditOperation;
import com.project200.undabang.timer.sync.TimerType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "timer_edit_receipts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_timer_edit_receipt_request",
                columnNames = {"member_id", "timer_type", "timer_id", "edit_id"})
})
public class TimerEditReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timer_edit_receipt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "timer_type", nullable = false, updatable = false, length = 10)
    private TimerType timerType;

    @Column(name = "timer_id", nullable = false, updatable = false)
    private Long timerId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "edit_id", nullable = false, updatable = false, columnDefinition = "char(36)")
    private UUID editId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "operation", nullable = false, updatable = false, length = 10)
    private TimerEditOperation operation;

    @Column(name = "updated_at", nullable = false, updatable = false, columnDefinition = "datetime(3)")
    private LocalDateTime updatedAt;

    public static TimerEditReceipt of(Member member, TimerType timerType, Long timerId, UUID editId,
                                      TimerEditOperation operation, LocalDateTime updatedAt) {
        TimerEditReceipt receipt = new TimerEditReceipt();
        receipt.member = member;
        receipt.timerType = timerType;
        receipt.timerId = timerId;
        receipt.editId = editId;
        receipt.operation = operation;
        receipt.updatedAt = updatedAt;
        return receipt;
    }
}
