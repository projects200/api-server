package com.project200.undabang.feed.service;

import com.project200.undabang.feed.dto.response.CreateFeedPictureResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FeedPictureService {
    List<CreateFeedPictureResponse> createFeedPictures(Long feedId, List<MultipartFile> imageFileList);

    void deleteFeedPicture(long feedId, long pictureId);
}
