package com.project200.undabang.member.dto.record;

public record Viewport(
        Double leftTopLatitude,
        Double leftTopLongitude,
        Double rightBottomLatitude,
        Double rightBottomLongitude) {
    public static Viewport of(Double leftTopLatitude, Double leftTopLongitude, Double rightBottomLatitude, Double rightBottomLongitude) {
        return new Viewport(leftTopLatitude, leftTopLongitude, rightBottomLatitude, rightBottomLongitude);
    }
}
