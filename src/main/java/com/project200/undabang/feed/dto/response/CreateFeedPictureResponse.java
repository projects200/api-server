package com.project200.undabang.feed.dto.response;

import com.project200.undabang.feed.entity.FeedPicture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeedPictureResponse {
    private Long pictureId;
    private String pictureUrl;

    public static CreateFeedPictureResponse of(FeedPicture feedPicture) {
        return new CreateFeedPictureResponse(feedPicture.getId(), feedPicture.getPicture().getPictureUrl());
    }
}
