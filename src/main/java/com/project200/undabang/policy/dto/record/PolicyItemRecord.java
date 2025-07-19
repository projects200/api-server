package com.project200.undabang.policy.dto.record;

import lombok.Builder;

@Builder
public record PolicyItemRecord(String policyKey, String policyValue, String policyUnit, String policyDescription) {
}
