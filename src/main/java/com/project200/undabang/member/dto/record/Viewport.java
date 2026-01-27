package com.project200.undabang.member.dto.record;

import com.project200.undabang.member.dto.request.ViewportRequest;

public record Viewport(
        Double leftTopLatitude,
        Double leftTopLongitude,
        Double rightBottomLatitude,
        Double rightBottomLongitude) {
    public static Viewport from(ViewportRequest request) {
        return new Viewport(request.getLeftTopLatitude(), request.getLeftTopLongitude(), request.getRightBottomLatitude(), request.getRightBottomLongitude());
    }
}
