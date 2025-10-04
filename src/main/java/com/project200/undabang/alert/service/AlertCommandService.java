package com.project200.undabang.alert.service;

import com.project200.undabang.alert.dto.response.UpdateExerciseEncouragementResponse;

public interface AlertCommandService {
    UpdateExerciseEncouragementResponse activateAllExerciseEncouragementToken();

    UpdateExerciseEncouragementResponse deactivateAllExerciseEncouragementToken();
}
