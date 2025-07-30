package com.project200.undabang.admin.component.dto;

public interface ErrorReport<T> {
    CommonErrorData getCommonErrorData();
    T getContextData();
    String formattingMessage(); // 각 구현 클래스에서 리포트의 내용을 알림에 적합한 문자열로 바꿔야 함
}
