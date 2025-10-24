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

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberBlockRepositoryImpl implements MemberBlockRepositoryCustom {

    private final JPAQueryFactory queryFactory;

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
}
