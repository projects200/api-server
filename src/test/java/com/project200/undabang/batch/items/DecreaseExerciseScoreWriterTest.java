package com.project200.undabang.batch.items;

import com.project200.undabang.common.batch.items.DecreaseExerciseScoreWriter;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class DecreaseExerciseScoreWriterTest {
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;

    private DecreaseExerciseScoreWriter decreaseExerciseWriter;


    @BeforeEach
    void setUp() throws Exception {
        decreaseExerciseWriter = new DecreaseExerciseScoreWriter(entityManagerFactory);
        decreaseExerciseWriter.afterPropertiesSet();
    }


    @Test
    @DisplayName("writer는 DB에 정보를 정상적으로 반영해야 함")
    void ItemWriter_updatesMemberScoreToDB(){
        // given
        Member savedMember = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail("e@mail.com")
                .memberNickname("testNickName")
                .memberScore((byte) 47)
                .build();
        memberRepository.save(savedMember);

        em.flush();
        em.clear();

        Member detachedMember = memberRepository.findById(savedMember.getMemberId()).orElseThrow();
        detachedMember.decreaseMemberScore((byte) 1);

        Chunk<Member> chunk = new Chunk<>(List.of(detachedMember));

        // when
        // JpaItemWriter는 내부적으로 merge를 수행해서 DB에 반영
        decreaseExerciseWriter.write(chunk);

        em.flush();
        em.clear();

        // then
        Member updatedMember = memberRepository.findById(savedMember.getMemberId()).orElseThrow();
        Assertions.assertThat(updatedMember.getMemberScore()).isEqualTo((byte) 46);
    }

    @Test
    @DisplayName("Writer는 여러 개의 Member가 포함된 Chunk도 정상적으로 처리해야 한다")
    void ItemWriter_WriteMultipleMembersToDB() {
        // given
        Member member1 = Member.builder()
                .memberId(UUID.randomUUID())
                .memberNickname("testMember1")
                .memberEmail("e1@mail.com")
                .memberScore((byte) 100)
                .build();

        Member member2 = Member.builder()
                .memberId(UUID.randomUUID())
                .memberNickname("testMember2")
                .memberEmail("e2@mail.com")
                .memberScore((byte) 4)
                .build();

        memberRepository.saveAll(List.of(member1, member2));

        em.flush();
        em.clear();

        Member detachedMember1 = memberRepository.findById(member1.getMemberId()).orElseThrow();
        Member detachedMember2 = memberRepository.findById(member2.getMemberId()).orElseThrow();

        detachedMember1.decreaseMemberScore(1);
        detachedMember2.decreaseMemberScore(1);

        Chunk<Member> chunk = new Chunk<>(List.of(detachedMember1, detachedMember2));

        // when
        decreaseExerciseWriter.write(chunk);

        em.flush();
        em.clear();

        // then
        Member updatedMember1 = memberRepository.findById(member1.getMemberId()).orElseThrow();
        Member updatedMember2 = memberRepository.findById(member2.getMemberId()).orElseThrow();

        Assertions.assertThat(updatedMember1.getMemberScore()).isEqualTo((byte) 99);
        Assertions.assertThat(updatedMember2.getMemberScore()).isEqualTo((byte) 3);
    }
}
