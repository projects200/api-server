package com.project200.undabang.batch.itemWriter;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class DecreaseExerciseWriterTest {
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private MemberRepository memberRepository;

    private JpaItemWriter<Member> decreaseExerciseWriter;

    @BeforeEach
    void setUp() {
        decreaseExerciseWriter = new JpaItemWriterBuilder<Member>()
                .entityManagerFactory(entityManagerFactory)
                .build();
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

        savedMember.decreaseMemberScore((byte) 1);

        List<Member> memberUpdateList = List.of(savedMember);

        Chunk<Member> chunk = new Chunk<>(memberUpdateList);

        // when
        decreaseExerciseWriter.write(chunk);

        // JpaItemWriter는 내부적으로 merge를 수행한다.
        // DB에 반영되었는지 확인하려면 영속성 컨텍스트를 비우고 다시 조회해야 함
        entityManagerFactory.createEntityManager().clear();

        // then
        Member updatedMember = memberRepository.findById(savedMember.getMemberId()).orElseThrow();
        Assertions.assertThat(updatedMember.getMemberScore()).isEqualTo((byte) 46);
    }
}
