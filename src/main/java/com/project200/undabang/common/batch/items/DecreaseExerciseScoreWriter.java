package com.project200.undabang.common.batch.items;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.item.database.JpaItemWriter;

/**
 * '운동 점수 감소' 배치 작업의 ItemWriter 구현체입니다.
 * JpaItemWriter를 상속받아 DecreaseExerciseScoreProcessor에서 처리된 Member 엔티티 리스트를 DB에 영속화(저장)하는 역할을 합니다.
 *
 * 이 클래스는 내부 로직이 단순하여 DecreaseExerciseScoreJobConfig에 직접 빈으로 등록할 수도 있지만,
 * DecreaseExerciseScoreReader, DecreaseExerciseScoreProcessor와 같이 각 단계를 별도의 클래스로 분리하여
 * 프로젝트 전체의 구조적 통일성을 유지하기 위해 독립된 클래스로 작성되었습니다.
 */
public class DecreaseExerciseScoreWriter extends JpaItemWriter<Member> {
    public DecreaseExerciseScoreWriter(EntityManagerFactory entityManagerFactory){
        super.setEntityManagerFactory(entityManagerFactory);
    }
}