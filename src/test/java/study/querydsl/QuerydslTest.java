package study.querydsl;

import com.mysema.commons.lang.Assert;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.swing.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static study.querydsl.QMember.*;
import static study.querydsl.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslTest {

    @Autowired
    private EntityManager em;

    private JPAQueryFactory queryFactory;

    @BeforeEach
    public void beforeEach() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("team A");
        Team teamB = new Team("team B");

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

        em.flush();
        em.clear();
    }

    @DisplayName("기존 jpql 에서 짜던 스타일")
    @Test
    public void jpql() {
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertEquals("member1", findMember.getUsername());
    }

    @DisplayName("querydsl 로 만드는 스타일")
    @Test
    public void querydsl() {
        QMember m = member;

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertEquals("member1", findMember.getUsername());
    }

    @DisplayName("and 조건을 주는 방법")
    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @DisplayName("and 조건을 주는 방법2")
    @Test
    public void search2() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10),
                        null // => null 은 무시합니다.
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @DisplayName("조회")
    @Test
    public void resultFetch() {
        // 리스트 조회 / 비었으면 null
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        // 1개만 조회 => 결과가 2이상이면 Exception
        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        // 첫번째 하나만 조회
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // paging 용도의 쿼리 생성
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        // total 카운트를 제공합니다.
        results.getTotal();
        // content 를 가져옵니다.
        List<Member> resultList = results.getResults();

        // 위에서 results.getTotal() 과 동일하다고 보면 됨
        long total = queryFactory
                .selectFrom(member)
                .fetchCount();

    }

    /**
     * 회원 정렬 순서
     *
     * 1. 회원 나이 내림차순
     * 2. 회원 이름 올림차순
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     * */
    @DisplayName("정렬 기능 사용하기")
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @DisplayName("페이징 기능 적용하기")
    @Test
    public void paging() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @DisplayName("페이징 기능 적용하기2")
    @Test
    public void paging2() {
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(results.getTotal()).isEqualTo(4);
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(results.getLimit()).isEqualTo(2);
        assertThat(results.getResults().size()).isEqualTo(2);
    }

    @DisplayName("각각 조회하는 경우 : Tuple")
    @Test
    public void aggregation() {
        List<Tuple> fetch = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min(),
                        member.team.members.size().sum()
                )
                .from(member)
                .fetch();

        Tuple tuple = fetch.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령
     * */
    @DisplayName("groupby와 having")
    @Test
    public void groupby() {
        List<Tuple> fetch = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(member.team.name)
                .having(member.age.avg().gt(20))
                .fetch();


//        Tuple teamA = fetch.get(0);
        Tuple teamB = fetch.get(0);


        /*assertThat(teamA.get(team.name)).isEqualTo("team A");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);*/

        assertThat(teamB.get(team.name)).isEqualTo("team B");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }


    @Test
    @DisplayName("join : team A 소속인 사람 구하기")
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(member.team.name.eq("team A"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 회원의 이름이 팀 이름과 같은 사람 조회
     * */
    @Test
    @DisplayName("연관관계 없이 조인하기")
    public void join_theta() {
        em.persist(new Member("team A"));
        em.persist(new Member("team B"));
        em.persist(new Member("team C"));

        List<Member> members = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(members)
                .extracting("username")
                .containsExactly("team A", "team B");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 team A 인 팀만 조회, 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'team A'
     *
     * 회원과 팀을 조인하면서, 팀 이름이 A 인 팀만 조회
     * JPQL : select m, t from Member m left join m.team t where t.name = 'team A'
     * JPQL : select m, t from Member m inner join m.team t on t.name = 'team A'
     * */
    @Test
    @DisplayName("join on 심화 : 조인 대상 필터링 할 때 on 절 사용")
    public void join_on_filtering() {
        List<Tuple> result1 = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("team A"))
                .fetch();

        List<Tuple> result2 = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("team A"))
                .fetch();

        List<Tuple> result3 = queryFactory
                .select(member, team)
                .from(member)
                .innerJoin(member.team, team).on(team.name.eq("team A"))
                .fetch();

        // result2와 result3 의 결과는 동일하고 result 1 은 회원이 모두 조회 되어야 함

        for (Tuple t : result1)
            System.out.println("result1 = " + t);

        for (Tuple t : result2)
            System.out.println("result2 = " + t);

        for (Tuple t : result3)
            System.out.println("result3 = " + t);

    }


    /**
     * 연관관계가 없는 엔티티의 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     * JPQL : select m, t from Member m left join m.team t on t.name = 'team A'
     * */
    @Test
    @DisplayName("join on 심화 : 연관관계가 없는 엔티티를 외부 조인할 때")
    public void join_on_no_relation() {
        em.persist(new Member("team A"));
        em.persist(new Member("team B"));
        em.persist(new Member("team C"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
//                .leftJoin(member.team, team).on(member.username.eq(team.name))

        for (Tuple t : result)
            System.out.println("result = " + t);

    }

    @PersistenceUnit
    private EntityManagerFactory emf;

    @Test
    @DisplayName("fetch join 을 사용하는 방법")
    public void join_fetch() {
        em.flush();
        em.clear();


        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 미적용").isFalse();



        em.flush();
        em.clear();

        Member findFetchMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loadedTeam = emf.getPersistenceUnitUtil().isLoaded(findFetchMember.getTeam());

        assertThat(loadedTeam).as("페치 조인 적용").isTrue();
    }


    /**
     * 나이가 평균 이상인 사람 조회
     */
    @Test
    @DisplayName("서브쿼리 사용하기")
    public void subQuery() {

        // 서브 쿼리를 위해서 alias 충돌나지 않도록
        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.gt(
                        JPAExpressions.select(memberSub.age.avg()).from(memberSub)
                ))
                .fetch();

        for (Member member : fetch) {
            System.out.println("member = " + member);
        }

        assertThat(fetch).extracting("age")
                .containsExactly(30, 40);
    }


    /**
     * 서브쿼리와 in 사용 예제
     * */
    @Test
    @DisplayName("서브쿼리 in")
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        for (Member member : fetch) {
            System.out.println("member = " + member);
        }

        assertThat(fetch).extracting("age")
                .containsExactly(20, 30, 40);
    }


    /**
     * select 절 서브쿼리
     * */
    @Test
    @DisplayName("select 절 서브쿼리")
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> fetch = queryFactory
                .select(
                        member.username,
                        JPAExpressions.select(memberSub.age.avg()).from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple t : fetch) {
            System.out.println("Tuple = " + t);
        }
    }

    @Test
    @DisplayName("case 문 : 정확하게 맞아 떨어지는 상황")
    public void simpleCase() {
        List<String> result = queryFactory
                .select(
                        member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String r : result)
            System.out.println("r = " + r);
    }

    @Test
    @DisplayName("case 문 : 복잡하게 조건을 거는 경우")
    public void complexCase() {
        List<String> result = queryFactory
                .select(
                        new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0 ~ 20 살")
                        .when(member.age.between(21, 30)).then("21 ~ 30 살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();


        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("상수 처리")
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple t : result) {
            System.out.println("t = " + t);
        }
    }

    @Test
    @DisplayName("문자열 더하기")
    public void concat() {
        List<String> fetch = queryFactory
                .select(member.username.concat(member.age.stringValue()))
                .from(member)
                .fetch();


        for (String s : fetch)
            System.out.println("s = " + s);

    }
}
