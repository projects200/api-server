package com.project200.undabang.feed.repository.impl;

import com.project200.undabang.comment.entity.QComment;
import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.feed.dto.record.FeedDetailRecord;
import com.project200.undabang.feed.dto.record.FeedPictureRecord;
import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import com.project200.undabang.feed.dto.response.GetSpecificFeedResponse;
import com.project200.undabang.feed.entity.QFeed;
import com.project200.undabang.feed.entity.QFeedPicture;
import com.project200.undabang.feed.entity.QFeedType;
import com.project200.undabang.feed.repository.FeedRepositoryCustom;
import com.project200.undabang.like.entity.QFeedLike;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.member.entity.QMemberPicture;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QMember member = QMember.member;
    private final QMemberPicture memberPicture = QMemberPicture.memberPicture;
    private final QPicture picture = QPicture.picture;
    private final QFeed feed = QFeed.feed;
    private final QFeedType feedType = QFeedType.feedType;
    private final QFeedPicture feedPicture = QFeedPicture.feedPicture;
    private final QFeedLike feedLike = QFeedLike.feedLike;
    private final QComment comment = QComment.comment;

    private static final int EXTRA_FETCH_SIZE = 1; // hasNext 확인용 상수

    /**
     * 현재 사용자의 "마이 페이지"에 표시될 피드 리스트를 조회하여 반환합니다.
     */
    @Override
    public Slice<FeedDetailResponse> getMyPageFeedList(Member currentMember, Long prevFeedId, Pageable pageable) {
        BooleanExpression condition = feed.member.memberId.eq(currentMember.getMemberId());

        return getFeedDetailSlice(currentMember, prevFeedId, pageable, condition);
    }

    /**
     * 모든 피드의 리스트를 조회하여 페이징된 결과를 반환합니다.
     */
    @Override
    public Slice<FeedDetailResponse> getAllFeedList(Member currentMember, Long prevFeedId, Pageable pageable) {
        return getFeedDetailSlice(currentMember, prevFeedId, pageable, null);
    }

    /**
     * 특정 피드 ID에 해당하는 피드 정보를 조회합니다.
     */
    @Override
    public Optional<GetSpecificFeedResponse> getSpecificFeed(Member currentMember, Long feedId) {
        FeedDetailRecord contentRecord = queryFactory
                .select(Projections.constructor(FeedDetailRecord.class,
                        feed.id,
                        feed.feedContent,
                        feed.likesCount,
                        feed.commentsCount,
                        feedType.feedTypeId,
                        feedType.feedTypeName,
                        feedType.feedTypeDesc,
                        feed.createdAt,
                        isLikedExpression(currentMember),
                        hasCommentedExpression(currentMember),
                        member.memberId,
                        member.memberNickname,
                        memberPicture.memberPicturesUrl,
                        picture.pictureUrl))
                .from(feed)
                .join(feed.member, member)
                .leftJoin(member.memberPicture, memberPicture)
                .leftJoin(memberPicture.picture, picture)
                .leftJoin(feed.feedType, feedType)
                .where(feed.id.eq(feedId)) // 특정 피드만 조회
                .fetchOne();

        // 해당 피드 데이터가 없는 경우 Optional.empty()를 반환
        if (contentRecord == null) {
            return Optional.empty();
        }

        List<FeedPictureRecord> feedPictureList = queryFactory
                .select(Projections.constructor(FeedPictureRecord.class,
                        feedPicture.id,
                        feedPicture.picture.pictureUrl))
                .from(feedPicture)
                .join(feedPicture.picture, picture)
                .where(
                        feedPicture.feed.id.eq(feedId)
                )
                .fetch();

        return Optional.of(GetSpecificFeedResponse.from(contentRecord, feedPictureList));
    }

    /**
     * 주어진 조건을 기반으로 피드 상세 정보를 페이징 처리하여 반환합니다.
     *
     * @param currentMember 현재 요청을 보낸 회원의 정보
     * @param prevFeedId 이전에 조회한 마지막 피드의 ID
     * @param pageable 페이징 정보를 담고 있는 객체
     * @param condition 추가로 적용할 조건식
     * @return 특정 조건과 페이징 정보를 기반으로 필터링된 피드 상세 응답의 슬라이스
     */
    private Slice<FeedDetailResponse> getFeedDetailSlice(Member currentMember, Long prevFeedId, Pageable pageable, BooleanExpression condition) {

        List<FeedDetailRecord> contentRecords = queryFactory
                .select(Projections.constructor(FeedDetailRecord.class,
                        feed.id,
                        feed.feedContent,
                        feed.likesCount,
                        feed.commentsCount,
                        feedType.feedTypeId,
                        feedType.feedTypeName,
                        feedType.feedTypeDesc,
                        feed.createdAt,
                        isLikedExpression(currentMember),
                        hasCommentedExpression(currentMember),
                        member.memberId,
                        member.memberNickname,
                        memberPicture.memberPicturesUrl,
                        picture.pictureUrl))
                .from(feed)
                .join(feed.member, member)
                .leftJoin(member.memberPicture, memberPicture)
                .leftJoin(memberPicture.picture, picture)
                .leftJoin(feed.feedType, feedType)
                .where(
                        olderThanPrevFeedId(prevFeedId),
                        condition
                )
                .orderBy(feed.id.desc()) // 최근 피드부터 읽기 (정확히는 최근에 작성된 피드가 생성도 늦게 되었다고 가정)
                .limit(pageable.getPageSize() + EXTRA_FETCH_SIZE) // 다음 피드가 존재하는지 구별하도록 10 + 1 로 설정
                .fetch();

        // 다음 피드가 존재하는지 확인
        boolean hasNext = checkAndTrimForNextPage(contentRecords, pageable.getPageSize());

        // 피드 식별자만 List로 모은 후, 해당 피드 식별자에 속한 피드 사진을 Map형태로 유지 (없으면 EmptyMap)
        List<Long> feedIdList = contentRecords.stream().map(FeedDetailRecord::feedId).toList();
        Map<Long, List<FeedPictureRecord>> feedPictureMap = findFeedPicturesInFeeds(feedIdList);

        // 응답양식에 맞춰서 피드 식별자 기반으로 피드 문자 데이터 + 작성한 회원 데이터 + 피드 사진 조립
        List<FeedDetailResponse> content = contentRecords.stream()
                .map(record -> {
                    List<FeedPictureRecord> feedPictures = feedPictureMap.getOrDefault(record.feedId(), Collections.emptyList());
                    return FeedDetailResponse.from(record, feedPictures);
                })
                .toList();

        return new SliceImpl<>(content, pageable, hasNext);
    }

    /**
     * 주어진 피드 ID 목록을 기반으로 각 피드 ID에 매핑된 피드 사진 정보를 조회합니다.
     */
    private Map<Long, List<FeedPictureRecord>> findFeedPicturesInFeeds(List<Long> feedIdList) {
        if (feedIdList.isEmpty()) {
            return Collections.emptyMap();
        }

        return queryFactory
                .from(feedPicture)
                .join(feedPicture.picture, picture)
                .where(feedPicture.feed.id.in(feedIdList))
                .transform(GroupBy.groupBy(feedPicture.feed.id)
                        .as(GroupBy.list(Projections.constructor(FeedPictureRecord.class,
                                feedPicture.id,
                                feedPicture.picture.pictureUrl))));
    }

    /**
     * 주어진 이전 피드 ID(prevFeedId)보다 오래된 피드인지 판단하는 조건을 생성합니다.
     */
    private BooleanExpression olderThanPrevFeedId(Long prevFeedId) {
        if (prevFeedId == null) {
            return null;
        }

        return feed.id.lt(prevFeedId);
    }

    /**
     * 주어진 리스트의 크기를 전달된 페이지 크기 기준으로 확인하여,
     * 페이지 크기를 초과할 경우 초과된 요소를 삭제하고 다음 페이지의 존재 여부를 반환합니다.
     */
    private boolean checkAndTrimForNextPage(List<FeedDetailRecord> recordList, int pageSize) {
        if (recordList.size() > pageSize) {
            recordList.remove(pageSize); // 다음 피드는 삭제 (다음번 피드가 존재함을 확인)
            return true;
        }

        return false;
    }

    /**
     * 현재 사용자가 특정 피드에 댓글을 작성했는지 확인하는 조건식을 생성합니다.
     */
    private BooleanExpression hasCommentedExpression(Member currentMember) {
        if (currentMember == null) {
            return Expressions.FALSE;
        }

        return JPAExpressions
                .selectOne()
                .from(comment)
                .where(comment.feed.id.eq(feed.id).and(comment.member.memberId.eq(currentMember.getMemberId())))
                .exists();
    }

    /**
     * 현재 사용자가 특정 피드를 좋아요 했는지 확인하는 조건식을 생성합니다.
     */
    private BooleanExpression isLikedExpression(Member currentMember) {
        if (currentMember == null) {
            return Expressions.FALSE;
        }

        return JPAExpressions
                .selectOne()
                .from(feedLike)
                .where(feedLike.feed.id.eq(feed.id).and(feedLike.member.memberId.eq(currentMember.getMemberId())))
                .exists();
    }
}