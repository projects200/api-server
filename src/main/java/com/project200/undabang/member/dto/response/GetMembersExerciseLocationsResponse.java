package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.dto.record.ExerciseLocationRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetMembersExerciseLocationsResponse {
    private List<ExerciseLocationRecord> locations;
}
