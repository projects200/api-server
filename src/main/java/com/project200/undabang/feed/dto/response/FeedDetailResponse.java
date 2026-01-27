package com.project200.undabang.feed.dto.response;

import com.project200.undabang.feed.dto.record.FeedPictureRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FeedDetailResponse {
    private long feedId;
    private String feedContent;
    private int feedLikesCount;
    private int feedCommentsCount;
    private long feedTypeId;
    private String feedTypeName;
    private String feedTypeDesc;
    private UUID memberId;
    private String nickname;
    private String profileUrl;
    private String thumbnailUrl;
    List<FeedPictureRecord> feedPictures;
}
