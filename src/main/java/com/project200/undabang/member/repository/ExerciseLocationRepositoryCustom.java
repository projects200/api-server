package com.project200.undabang.member.repository;

import com.project200.undabang.member.dto.record.MemberProfileAndLocationRecord;

import java.util.List;

public interface ExerciseLocationRepositoryCustom {
    List<MemberProfileAndLocationRecord> getMembersExerciseLocations();
}
