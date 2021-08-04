package study.querydsl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.domain.Member;
import study.querydsl.domain.Team;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJPARepository;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJPARepositoryTest {

    @Autowired
    private MemberJPARepository memberJPARepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("JPA 테스트")
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberJPARepository.save(member);

        Member findMember = memberJPARepository.findById(member.getId()).orElseThrow(() -> new IllegalStateException("저장 되지 않음"));
        assertThat(findMember).isEqualTo(member);

        List<Member> findMembers = memberJPARepository.findAll();
        assertThat(findMembers).containsExactly(member);


        List<Member> findMembersWithName = memberJPARepository.findByUsername(member.getUsername());
        assertThat(findMembersWithName).containsExactly(member);
    }

    @Test
    @DisplayName("querydsl 테스트")
    public void querydslTest() {
        Member member = new Member("member1", 10);
        memberJPARepository.save(member);

        Member findMember = memberJPARepository.findById(member.getId()).orElseThrow(() -> new IllegalStateException("저장 되지 않음"));
        assertThat(findMember).isEqualTo(member);

        List<Member> findMembers = memberJPARepository.findAllQuerydsl();
        assertThat(findMembers).containsExactly(member);


        List<Member> findMembersWithName = memberJPARepository.findByUsernameQuerydsl(member.getUsername());
        assertThat(findMembersWithName).containsExactly(member);
    }

    @Test
    @DisplayName("동적 쿼리 테스트")
    public void searchTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);


        MemberSearchCondition condition1 = new MemberSearchCondition();
        condition1.setTeamName("teamA");

        List<MemberTeamDto> result1 = memberJPARepository.findBySearchCondition(condition1);
        assertThat(result1).extracting("username").containsExactly("member1", "member2");


        MemberSearchCondition condition2 = new MemberSearchCondition();
        condition2.setUsername("member1");

        List<MemberTeamDto> result2 = memberJPARepository.findBySearchCondition(condition2);
        assertThat(result2).extracting("username").containsExactly("member1");


        MemberSearchCondition condition3 = new MemberSearchCondition();
        condition3.setGoe(20);

        List<MemberTeamDto> result3 = memberJPARepository.findBySearchCondition(condition3);
        assertThat(result3).extracting("username").containsExactly("member2", "member3", "member4");

    }

}
