package com.project200.undabang.feed.repository.impl;

import com.project200.undabang.common.entity.QPicture;
import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import com.project200.undabang.feed.entity.QFeed;
import com.project200.undabang.feed.entity.QFeedPicture;
import com.project200.undabang.feed.entity.QFeedType;
import com.project200.undabang.feed.repository.FeedRepositoryCustom;
import com.project200.undabang.member.entity.QMember;
import com.project200.undabang.member.entity.QMemberPicture;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public FeedDetailResponse getAllFeedList() {
        QMember member = QMember.member;
        QMemberPicture memberPicture = QMemberPicture.memberPicture;
        QPicture picture = QPicture.picture;
        QFeed feed = QFeed.feed;
        QFeedPicture feedPicture = QFeedPicture.feedPicture;
        QFeedType feedType = QFeedType.feedType;


    }
}
