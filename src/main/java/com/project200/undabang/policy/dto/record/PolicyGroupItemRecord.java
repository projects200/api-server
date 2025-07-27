package com.project200.undabang.policy.dto.record;

import lombok.Builder;

/**
 * 정책 그룹 아이템 정보를 담는 레코드 클래스입니다.
 */
@Builder
public record PolicyGroupItemRecord(String groupName, String policyKey, String policyValue, String policyUnit, String policyDescription) {
}
