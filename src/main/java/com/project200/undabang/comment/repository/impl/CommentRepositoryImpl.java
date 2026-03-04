package com.project200.undabang.comment.repository.impl;

import com.project200.undabang.comment.dto.response.CommentResponse;
import com.project200.undabang.comment.entity.QComment;
import com.project200.undabang.comment.repository.CommentRepositoryCustom;
import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.like.entity.QCommentLike;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.member.entity.QMemberBlock;
import com.project200.undabang.member.entity.QMemberPicture;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QComment comment = QComment.comment;
    private final QMember member = QMember.member;
    private final QMemberPicture memberPicture = QMemberPicture.memberPicture;
    private final QPicture picture = QPicture.picture;
    private final QCommentLike commentLike = QCommentLike.commentLike;
    private final QMemberBlock memberBlock = QMemberBlock.memberBlock;

    @Override
    public List<CommentResponse> findCommentsWithChildrenByFeedId(Long feedId, Member currentMember) {
        // 1. 부모 댓글 조회 (parent가 null인 것만)
        List<CommentResponse> parentComments = queryFactory
                .select(Projections.constructor(CommentResponse.class,
                        comment.id,
                        member.memberId,
                        member.memberNickname,
                        memberPicture.memberPicturesUrl,
                        Expressions.nullExpression(String.class), // 썸네일은 추후 개발 예정
                        comment.content,
                        comment.likesCount,
                        isCommentLikedExpression(currentMember),
                        comment.createdAt,
                        Expressions.constant(Collections.<CommentResponse>emptyList())))
                .from(comment)
                .leftJoin(comment.member, member)
                .leftJoin(member.memberPicture, memberPicture)
                .leftJoin(memberPicture.picture, picture)
                .where(
                        comment.feed.id.eq(feedId),
                        comment.parent.isNull(),
                        comment.deletedAt.isNull(),
                        isNotBlockedCommentOwner(currentMember))
                .orderBy(comment.createdAt.asc())
                .fetch();

        if (parentComments.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 부모 댓글 ID 목록 추출
        List<Long> parentIds = parentComments.stream()
                .map(CommentResponse::commentId)
                .collect(Collectors.toList());

        // 3. 대댓글 조회
        List<CommentResponse> childComments = queryFactory
                .select(Projections.constructor(CommentResponse.class,
                        comment.id,
                        member.memberId,
                        member.memberNickname,
                        memberPicture.memberPicturesUrl,
                        Expressions.nullExpression(String.class),
                        comment.content,
                        comment.likesCount,
                        isCommentLikedExpression(currentMember),
                        comment.createdAt,
                        Expressions.constant(Collections.<CommentResponse>emptyList())))
                .from(comment)
                .leftJoin(comment.member, member)
                .leftJoin(member.memberPicture, memberPicture)
                .leftJoin(memberPicture.picture, picture)
                .where(
                        comment.parent.id.in(parentIds),
                        comment.deletedAt.isNull(),
                        isNotBlockedCommentOwner(currentMember))
                .orderBy(comment.createdAt.asc())
                .fetch();

        // 4. 대댓글의 부모 ID를 가져오기 위한 매핑 쿼리
        Map<Long, List<CommentResponse>> childrenMap = buildChildrenMap(childComments, parentIds);

        // 5. 부모 댓글에 대댓글 조립
        return parentComments.stream()
                .map(parent -> new CommentResponse(
                        parent.commentId(),
                        parent.memberId(),
                        parent.memberNickname(),
                        parent.memberProfileImageUrl(),
                        parent.memberThumbnailUrl(),
                        parent.content(),
                        parent.likesCount(),
                        parent.commentIsLiked(),
                        parent.createdAt(),
                        childrenMap.getOrDefault(parent.commentId(), new ArrayList<>())))
                .collect(Collectors.toList());
    }

    /**
     * 대댓글을 부모 댓글 ID별로 그룹핑합니다.
     */
    private Map<Long, List<CommentResponse>> buildChildrenMap(List<CommentResponse> childComments,
                                                              List<Long> parentIds) {
        if (childComments.isEmpty()) {
            return Collections.emptyMap();
        }

        // 대댓글의 부모 ID 매핑을 위해 별도 조회
        QComment c = QComment.comment;
        Map<Long, Long> childToParentMap = queryFactory
                .select(c.id, c.parent.id)
                .from(c)
                .where(c.parent.id.in(parentIds), c.deletedAt.isNull())
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(c.id),
                        tuple -> tuple.get(c.parent.id)));

        return childComments.stream()
                .collect(Collectors.groupingBy(
                        child -> childToParentMap.getOrDefault(child.commentId(), -1L)));
    }

    /**
     * 현재 사용자가 특정 댓글을 좋아요 했는지 확인하는 조건식을 생성합니다.
     */
    private BooleanExpression isCommentLikedExpression(Member currentMember) {
        if (currentMember == null) {
            return Expressions.FALSE;
        }

        return JPAExpressions
                .selectOne()
                .from(commentLike)
                .where(commentLike.comment.id.eq(comment.id)
                        .and(commentLike.member.memberId.eq(currentMember.getMemberId())))
                .exists();
    }

    /**
     * 차단 관계(양방향)인 회원의 댓글을 제외하는 조건식을 생성합니다.
     */
    private BooleanExpression isNotBlockedCommentOwner(Member currentMember) {
        if (currentMember == null) {
            return null;
        }

        return JPAExpressions
                .selectOne()
                .from(memberBlock)
                .where(
                        memberBlock.memberBlockDeletedAt.isNull(),
                        memberBlock.blocker.memberId.eq(currentMember.getMemberId())
                                .and(memberBlock.blocked.memberId
                                        .eq(comment.member.memberId))
                                .or(
                                        memberBlock.blocker.memberId.eq(
                                                        comment.member.memberId)
                                                .and(memberBlock.blocked.memberId
                                                        .eq(currentMember
                                                                .getMemberId()))))
                .notExists();
    }
}
