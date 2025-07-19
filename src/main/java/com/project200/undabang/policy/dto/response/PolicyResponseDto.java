package com.project200.undabang.policy.dto.response;

import com.project200.undabang.policy.dto.record.PolicyItemRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponseDto {
    private String groupName;
    private int size;
    private List<PolicyItemRecord> policies;
}
