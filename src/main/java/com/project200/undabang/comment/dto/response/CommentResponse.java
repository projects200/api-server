package com.project200.undabang.comment.dto.response;

import com.project200.undabang.comment.dto.record.TaggedMemberRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
        Long commentId,
        UUID memberId,
        String memberNickname,
        String memberProfileImageUrl,
        String memberThumbnailUrl,
        String content,
        Integer likesCount,
        Boolean commentIsLiked,
        LocalDateTime createdAt,
        TaggedMemberRecord taggedMember,
        List<CommentResponse> children) {
}
