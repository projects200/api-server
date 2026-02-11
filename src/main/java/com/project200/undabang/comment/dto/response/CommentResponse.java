package com.project200.undabang.comment.dto.response;

import com.project200.undabang.comment.entity.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
        LocalDateTime createdAt,
        List<CommentResponse> children) {

    public static CommentResponse from(Comment comment) {
        return of(comment, new ArrayList<>());
    }

    public static CommentResponse of(Comment comment, List<CommentResponse> children) {
        String profileImageUrl = null;
        String thumbnailUrl = null;

        if (comment.getMember().getMemberPicture() != null) {
            profileImageUrl = comment.getMember().getMemberPicture().getPicture().getPictureUrl();
            if (comment.getMember().getMemberPicture().getPicture() != null) {
                thumbnailUrl = null; // 썸네일은 추후 개발 예정
            }
        }

        return new CommentResponse(
                comment.getId(),
                comment.getMember().getMemberId(),
                comment.getMember().getMemberNickname(),
                profileImageUrl,
                thumbnailUrl,
                comment.getContent(),
                comment.getLikesCount(),
                comment.getCreatedAt(),
                children);
    }
}
