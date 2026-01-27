package com.project200.undabang.feed.repository.impl;

import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.feed.dto.record.FeedDetailRecord;
import com.project200.undabang.feed.dto.record.FeedPictureRecord;
import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import com.project200.undabang.feed.entity.QFeed;
import com.project200.undabang.feed.entity.QFeedPicture;
import com.project200.undabang.feed.entity.QFeedType;
import com.project200.undabang.feed.repository.FeedRepositoryCustom;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.member.entity.QMemberPicture;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    private static final int EXTRA_FETCH_SIZE = 1; // hasNext 확인용 상수

    @Override
    public Slice<FeedDetailResponse> getAllFeedList(Long prevFeedId, Pageable pageable) {

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
                        member.memberId,
                        member.memberNickname,
                        memberPicture.memberPicturesUrl,
                        picture.pictureUrl))
                .from(feed)
                .join(feed.member, member)
                .leftJoin(member.memberPicture, memberPicture)
                .leftJoin(memberPicture.picture, picture)
                .join(feed.feedType, feedType)
                .where(
                        olderThanPrevFeedId(prevFeedId)
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
     * 주어진 페이지 요청 정보와 결과 목록을 기반으로 다음 페이지의 존재 여부를 확인하고,
     * 결과를 뒤집은 후 Slice 객체를 생성하여 반환합니다.
     */
    private boolean checkAndTrimForNextPage(List<FeedDetailRecord> recordList, int pageSize) {
        if (recordList.size() > pageSize) {
            recordList.remove(pageSize); // 다음 피드는 삭제 (다음번 피드가 존재함을 확인)
            return true;
        }

        return false;
    }
}
