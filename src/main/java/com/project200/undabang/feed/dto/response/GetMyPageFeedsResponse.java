package com.project200.undabang.feed.dto.response;

import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.feed.dto.record.GetMyPageFeedsRecord;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetMyPageFeedsResponse {
    private UUID memberId;
    private String nickname;
    private String thumbnailUrl;
    private String profileUrl;
    private boolean hasNext;
    private List<GetMyPageFeedsRecord> feeds;

    public static GetMyPageFeedsResponse from(Member member, Slice<FeedDetailResponse> responseSliceList) {
        List<GetMyPageFeedsRecord> feeds = responseSliceList.getContent().stream().map(GetMyPageFeedsRecord::from).toList();

        Optional<MemberPicture> memberPictureOptional = Optional.ofNullable(member.getMemberPicture());

        String thumbnailUrl = memberPictureOptional
                .map(MemberPicture::getMemberPicturesUrl)
                .orElse(null);

        String profileUrl = memberPictureOptional
                .map(MemberPicture::getPicture)
                .map(Picture::getPictureUrl)
                .orElse(null);

        return GetMyPageFeedsResponse.builder()
                .memberId(member.getMemberId())
                .nickname(member.getMemberNickname())
                .thumbnailUrl(thumbnailUrl)
                .profileUrl(profileUrl)
                .hasNext(responseSliceList.hasNext())
                .feeds(feeds)
                .build();
    }
}
