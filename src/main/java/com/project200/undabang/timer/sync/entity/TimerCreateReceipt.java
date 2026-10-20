package com.project200.undabang.timer.sync.entity;

import com.project200.undabang.member.entity.Member;
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
@Table(name = "timer_create_receipts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_timer_create_receipt_member_request", columnNames = {"member_id", "create_request_id"})
})
public class TimerCreateReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timer_create_receipt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "create_request_id", nullable = false, updatable = false, columnDefinition = "char(36)")
    private UUID createRequestId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "timer_type", nullable = false, updatable = false, length = 10)
    private TimerType timerType;

    // 타이머가 삭제되거나 정리되어도 원래 응답을 재생하므로 타이머 FK를 두지 않습니다.
    @Column(name = "timer_id", nullable = false, updatable = false)
    private Long timerId;

    @Column(name = "updated_at", nullable = false, updatable = false, columnDefinition = "datetime(3)")
    private LocalDateTime updatedAt;

    public static TimerCreateReceipt of(Member member, UUID createRequestId, TimerType timerType,
                                        Long timerId, LocalDateTime updatedAt) {
        TimerCreateReceipt receipt = new TimerCreateReceipt();
        receipt.member = member;
        receipt.createRequestId = createRequestId;
        receipt.timerType = timerType;
        receipt.timerId = timerId;
        receipt.updatedAt = updatedAt;
        return receipt;
    }
}
