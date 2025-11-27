package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.exercise.repository.ExerciseTypeRepository;
import com.project200.undabang.member.dto.response.AvailableExerciseTypeResponse;
import com.project200.undabang.member.dto.response.MyPreferredExerciseResponse;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.repository.PreferredExerciseRepository;
import com.project200.undabang.member.service.PreferredExerciseQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 선호 운동 조회 관련 비즈니스 로직을 처리하는 서비스 구현체입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreferredExerciseQueryServiceImpl implements PreferredExerciseQueryService {
    
    private final ExerciseTypeRepository exerciseTypeRepository;
    private final PreferredExerciseRepository preferredExerciseRepository;
    private final MemberRepository memberRepository;
    
    @Override
    public List<AvailableExerciseTypeResponse> getAvailableExerciseTypes() {
        List<ExerciseType> exerciseTypes = exerciseTypeRepository.findAllByExerciseTypeDeletedAtNull();
        
        return exerciseTypes.stream()
                .map(AvailableExerciseTypeResponse::from)
                .toList();
    }
    
    @Override
    public List<MyPreferredExerciseResponse> getMyPreferredExercises() {
        Member member = getMember(UserContextHolder.getUserId());
        
        List<PreferredExercise> preferredExercises = 
                preferredExerciseRepository.findAllByMemberAndPreferredExerciseDeletedAtNull(member);
        
        return preferredExercises.stream()
                .map(MyPreferredExerciseResponse::from)
                .toList();
    }
    
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}


