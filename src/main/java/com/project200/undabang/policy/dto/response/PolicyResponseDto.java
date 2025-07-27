package com.project200.undabang.policy.dto.response;

import com.project200.undabang.policy.dto.record.PolicyItemRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponseDto {
    private String groupName;
    private int size;
    @Builder.Default
    private List<PolicyItemRecord> policies = new ArrayList<>();

    public void addPolicyItem(PolicyItemRecord item){
        this.policies.add(item);
        this.size = this.policies.size();
    }
}
