package com.project200.undabang.member.repository.impl;

import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.member.dto.record.MemberBlockRecord;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.member.entity.QMemberBlock;
import com.project200.undabang.member.entity.QMemberPicture;
import com.project200.undabang.member.repository.MemberBlockRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MemberBlockRepositoryImpl implements MemberBlockRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 주어진 두 회원 간의 차단 기록이 존재하는지 확인합니다.
     */
    @Override
    public boolean checkMemberBlockExists(Member currentMember, Member blockedMember) {
        QMemberBlock memberBlock = QMemberBlock.memberBlock;

        return queryFactory.selectOne()
                .from(memberBlock)
                .where(
                        (memberBlock.blocker.eq(currentMember) // 내가 상대방을 차단했거나
                                .and(memberBlock.blocked.eq(blockedMember))
                                .or(memberBlock.blocker.eq(blockedMember) // 상대방이 나를 차단한 경우
                                        .and(memberBlock.blocked.eq(currentMember))))
                                .and(memberBlock.memberBlockDeletedAt.isNull()) // 차단 해제를 안한 경우
                )
                .fetchFirst() != null;
    }

    /**
     * 주어진 회원에 대한 모든 차단 기록(MemberBlockRecord)을 조회합니다.
     */
    @Override
    public List<MemberBlockRecord> findAllMemberBlockRecordsByMember(Member currentMember) {
        QMemberBlock memberBlock = QMemberBlock.memberBlock;
        QMember blockedMember = QMember.member;
        QMemberPicture memberPicture = QMemberPicture.memberPicture;
        QPicture picture = QPicture.picture;

        return queryFactory.select(Projections.constructor(MemberBlockRecord.class,
                        memberBlock.id,
                        blockedMember.memberId,
                        blockedMember.memberNickname,
                        memberPicture.memberPicturesUrl,
                        picture.pictureUrl,
                        memberBlock.memberBlockCreatedAt))
                .from(memberBlock)
                .join(memberBlock.blocked, blockedMember)
                .leftJoin(blockedMember.memberPicture, memberPicture)
                .leftJoin(memberPicture.picture, picture)
                .where(
                        memberBlock.blocker.eq(currentMember),
                        memberBlock.memberBlockDeletedAt.isNull()
                )
                .orderBy(memberBlock.memberBlockCreatedAt.desc())
                .fetch();
    }

    /**
     * 주어진 회원과 관련된 모든 차단 회원 ID를 조회합니다.
     */
    @Override
    public Set<UUID> findAllBlockedMemberIdsByMember(Member currentMember) {
        QMemberBlock memberBlock = QMemberBlock.memberBlock;

        // 내가 차단한 사람들의 식별자 모음
        List<UUID> membersThatIBanned = queryFactory
                .select(memberBlock.blocked.memberId)
                .from(memberBlock)
                .where(
                        memberBlock.blocker.eq(currentMember),
                        memberBlock.memberBlockDeletedAt.isNull()
                )
                .fetch();

        // 나를 차단한 사람들의 식별자 모음
        List<UUID> membersThatBannedMe = queryFactory
                .select(memberBlock.blocker.memberId)
                .from(memberBlock)
                .where(
                        memberBlock.blocked.eq(currentMember),
                        memberBlock.memberBlockDeletedAt.isNull()
                )
                .fetch();

        Set<UUID> result = new HashSet<>();
        result.addAll(membersThatIBanned);
        result.addAll(membersThatBannedMe);
        result.add(currentMember.getMemberId()); // 지도에서 나도 포함되면 안됨

        return result;
    }
}
