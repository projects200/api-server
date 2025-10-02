package com.project200.undabang.match.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Long matchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private Member requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private Member receiver;

    @Builder.Default
    @Column(name = "match_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private MatchStatus matchStatus = MatchStatus.PENDING;

    @Builder.Default
    @Column(name = "match_created_at", nullable = false)
    private LocalDateTime matchCreatedAt = LocalDateTime.now();

    @Column(name = "match_canceled_at")
    private LocalDateTime matchCanceledAt;

    @Column(name = "match_handled_at")
    private LocalDateTime matchHandledAt;

    public static Match from(Member requester, Member receiver) {
        return Match.builder()
                .requester(requester)
                .receiver(receiver)
                .matchStatus(MatchStatus.PENDING)
                .matchCreatedAt(LocalDateTime.now())
                .build();
    }
}
