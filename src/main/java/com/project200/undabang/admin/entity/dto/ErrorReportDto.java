package com.project200.undabang.admin.entity.dto;

import com.project200.undabang.admin.entity.dto.error.CommonErrorData;

public interface ErrorReportDto<T> {
    CommonErrorData getCommonErrorData();
    T getSpecificData(); // 각 DTO 별 구체화 된 내용을 담는 메소드
    String formattingMessage(); // 각 구현 클래스에서 리포트의 내용을 알림에 적합한 문자열로 바꿔야 함
}
