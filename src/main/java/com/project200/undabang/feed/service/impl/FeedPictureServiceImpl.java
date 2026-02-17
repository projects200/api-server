package com.project200.undabang.feed.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.service.FileType;
import com.project200.undabang.common.service.PictureService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.dto.response.CreateFeedPictureResponse;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedPicture;
import com.project200.undabang.feed.repository.FeedPictureRepository;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.feed.service.FeedPictureService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedPictureServiceImpl implements FeedPictureService {

    private final FeedRepository feedRepository;
    private final MemberRepository memberRepository;
    private final PictureService pictureService;
    private final FeedPictureRepository feedPictureRepository;
    private final PolicyService policyService;

    /**
     * 주어진 피드 ID와 사진 ID에 해당하는 피드 이미지를 삭제합니다.
     * 이미지가 존재하지 않거나 이미 삭제된 경우 예외를 발생시킵니다.
     */
    @Override
    public void deleteFeedPictures(long feedId, long pictureId) {
        Member member = getMember(UserContextHolder.getUserId());
        Feed feed = getFeed(feedId, member);

        FeedPicture feedPicture = feedPictureRepository.findByFeedAndIdAndPicture_PictureDeletedAtNull(feed, pictureId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEED_PICTURE_NOT_FOUND));

        pictureService.deletePictureFromS3AndDB(feedPicture.getPicture());
    }

    /**
     * 주어진 피드 ID에 해당하는 피드에 이미지 파일 리스트를 업로드한 후, 이를 데이터베이스에 저장하고 결과를 반환합니다.
     */
    @Override
    public List<CreateFeedPictureResponse> createFeedPictures(Long feedId, List<MultipartFile> imageFileList) {
        Member member = getMember(UserContextHolder.getUserId());
        Feed feed = getFeed(feedId, member);

        // 사진 업로드시 사진 갯수 검증 메소드 (기존사진 + 최대사진) < MAX_COUNT
        validateFeedPictureCount(feed, imageFileList.size());

        // Picture Service를 이용하여 파일 업로드
        List<Picture> savedPictureList = pictureService.uploadPictureListToS3AndDB(imageFileList, FileType.FEED);

        List<FeedPicture> feedPictureList = savedPictureList.stream()
                .map(picture -> FeedPicture.of(picture, feed))
                .toList();

        return feedPictureRepository.saveAll(feedPictureList).stream().map(CreateFeedPictureResponse::of).toList();
    }

    /**
     * 주어진 피드와 입력된 사진의 수를 기준으로 피드에 업로드 가능한 사진의 최대 갯수를 검증합니다.
     */
    private void validateFeedPictureCount(Feed feed, int inputSize) {
        long prevPictureSize = feedPictureRepository.countByFeedAndPicture_PictureDeletedAtNull(feed);

        if (prevPictureSize + inputSize > policyService.getPolicyValueAsInt(PolicyKey.FEED_PICTURE_MAX_COUNT)) {
            throw new CustomException(ErrorCode.FEED_PICTURE_MAX_COUNT_EXCEED);
        }
    }

    /**
     * 주어진 피드 ID와 멤버 정보를 기반으로 특정 피드를 조회합니다.
     * 조회된 피드는 삭제되지 않은 상태여야 합니다.
     */
    private Feed getFeed(long feedId, Member member) {
        return feedRepository.findByIdAndMemberAndDeletedAtNull(feedId, member).orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));
    }

    /**
     * UUID를 사용하여 삭제되지 않은 멤버 정보를 조회합니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findByMemberIdAndMemberDeletedAtNull(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
