package com.project200.undabang.comment.repository.impl;

import com.project200.undabang.comment.dto.record.TaggedMemberRecord;
import com.project200.undabang.comment.dto.response.CommentResponse;
import com.project200.undabang.comment.entity.Comment;
import com.project200.undabang.comment.entity.QComment;
import com.project200.undabang.comment.entity.QCommentTag;
import com.project200.undabang.comment.repository.CommentRepositoryCustom;
import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.member.entity.QMemberPicture;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CommentResponse> findCommentsWithChildrenByFeedId(Long feedId) {
        QComment comment = QComment.comment;
        QMember member = QMember.member;
        QMemberPicture memberPicture = QMemberPicture.memberPicture;
        QPicture picture = QPicture.picture;
        QCommentTag commentTag = QCommentTag.commentTag;
        QMember taggedMember = new QMember("taggedMember");

        // 1. 부모 댓글 및 태그 정보 조회
        List<Comment> parentComments = queryFactory
                .selectFrom(comment)
                .leftJoin(comment.member, member).fetchJoin()
                .leftJoin(member.memberPicture, memberPicture).fetchJoin()
                .leftJoin(memberPicture.picture, picture).fetchJoin()
                .where(
                        comment.feed.id.eq(feedId),
                        comment.parent.isNull(),
                        comment.deletedAt.isNull())
                .orderBy(comment.createdAt.asc())
                .fetch();

        if (parentComments.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 부모 댓글의 태그 정보 조회
        List<Long> parentIds = parentComments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        Map<Long, TaggedMemberRecord> parentTagsMap = queryFactory
                .selectFrom(commentTag)
                .leftJoin(commentTag.taggedMember, taggedMember).fetchJoin()
                .where(commentTag.comment.id.in(parentIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        ct -> ct.getComment().getId(),
                        ct -> new TaggedMemberRecord(
                                ct.getTaggedMember().getMemberId(),
                                ct.getTaggedMember().getMemberNickname())));

        // 3. 대댓글 조회
        List<Comment> childComments = queryFactory
                .selectFrom(comment)
                .leftJoin(comment.member, member).fetchJoin()
                .leftJoin(member.memberPicture, memberPicture).fetchJoin()
                .leftJoin(memberPicture.picture, picture).fetchJoin()
                .where(
                        comment.parent.id.in(parentIds),
                        comment.deletedAt.isNull())
                .orderBy(comment.createdAt.asc())
                .fetch();

        // 4. 대댓글의 태그 정보 조회
        List<Long> childIds = childComments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        Map<Long, TaggedMemberRecord> childTagsMap = childIds.isEmpty() ? Map.of()
                : queryFactory
                .selectFrom(commentTag)
                .leftJoin(commentTag.taggedMember, taggedMember).fetchJoin()
                .where(commentTag.comment.id.in(childIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        ct -> ct.getComment().getId(),
                        ct -> new TaggedMemberRecord(
                                ct.getTaggedMember().getMemberId(),
                                ct.getTaggedMember()
                                        .getMemberNickname())));

        // 5. 부모 ID별 대댓글 그룹핑
        Map<Long, List<CommentResponse>> childrenMap = childComments.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getParent().getId(),
                        Collectors.mapping(
                                child -> CommentResponse.of(
                                        child,
                                        new ArrayList<>(),
                                        childTagsMap.get(child.getId())),
                                Collectors.toList())));

        // 6. 부모 댓글에 대댓글 조립
        return parentComments.stream()
                .map(parent -> CommentResponse.of(
                        parent,
                        childrenMap.getOrDefault(parent.getId(), new ArrayList<>()),
                        parentTagsMap.get(parent.getId())))
                .collect(Collectors.toList());
    }

}
