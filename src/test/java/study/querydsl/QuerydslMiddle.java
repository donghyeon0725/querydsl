package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.domain.Member;
import study.querydsl.domain.Team;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.QMember.*;

// 중급 문법 정리
@SpringBootTest
@Transactional
public class QuerydslMiddle {
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


    @Test
    @DisplayName("기본 프로젝션")
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    @DisplayName("튜플 프로젝션")
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("username = " + tuple.get(member.username) + ", age = " + tuple.get(member.age));
        }
    }

    /**
     * 이름 기반으로 적절한 setter를 찾아 주입
     * */
    @Test
    @DisplayName("dto 프로젝션 setter 기반")
    public void dtoProjectionWithSetter() {
        List<MemberDto> result = queryFactory
                .select(
                        Projections.bean(MemberDto.class, member.username, member.age)
                )
                .from(member)
                .fetch();

        for (MemberDto member : result) {
            System.out.println("member = " + member);
        }
    }


    /**
     * 이름 기반으로 적절한 필드를 찾아 주입
     * */
    @Test
    @DisplayName("dto 프로젝션 필드 주입 기반")
    public void dtoProjectionWithFieldInject() {
        List<MemberDto> result = queryFactory
                .select(
                        Projections.fields(MemberDto.class, member.username, member.age)
                )
                .from(member)
                .fetch();

        for (MemberDto member : result) {
            System.out.println("member = " + member);
        }
    }

    /**
     * 필드 주입 이름이 다를때
     */
    @Test
    @DisplayName("dto 프로젝션 생성자 주입 기반")
    public void dtoProjectionWithFieldInject2() {
        List<UserDto> result = queryFactory
                .select(
                        Projections.fields(UserDto.class, member.username.as("title"), member.age.as("order"))
                )
                .from(member)
                .fetch();

        for (UserDto user : result) {
            System.out.println("user = " + user);
        }
    }

    /**
     * 서브쿼리가 있는 프로젝션
     */
    @Test
    @DisplayName("dto 프로젝션 with 서브쿼리 (필드, 프로퍼티 기반에서 사용)")
    public void dtoProjectionWithSubquery() {
        QMember sub = new QMember("subQuery");

        List<UserDto> result = queryFactory
                .select(
                        Projections.fields(
                                UserDto.class,
                                member.username.as("title"),
                                ExpressionUtils.as(
                                        JPAExpressions.select(sub.age.avg().intValue()).from(sub),
                                        "order"
                                )
                        )

                )
                .from(member)
                .fetch();

        for (UserDto user : result) {
            System.out.println("user = " + user);
        }
    }

    /**
     * 생성자 기반 프로젝션 => 타입을 보고 setting
     */
    @Test
    @DisplayName("dto 프로젝션 with 서브쿼리 (필드, 프로퍼티 기반에서 사용)")
    public void dtoProjectionWithConstructor() {
        List<UserDto> result = queryFactory
                .select(
                        Projections.constructor(
                                UserDto.class,
                                member.username,
                                member.age
                        )

                )
                .from(member)
                .fetch();

        for (UserDto user : result) {
            System.out.println("user = " + user);
        }
    }

    /**
     * queryProjection 으로 받는 방법
     */
    @Test
    @DisplayName("dto 프로젝션 with 쿼리프로젝션")
    public void dtoProjectionWithQueryProjection() {
        List<UserDto> result = queryFactory
                .select(new QUserDto(member.username,member.age))
                .from(member)
                .fetch();

        for (UserDto user : result) {
            System.out.println("user = " + user);
        }
    }


    @Test
    @DisplayName("동적 쿼리 : BooleanBuilder")
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();

        if (usernameCond != null)
            builder.and(member.username.eq(usernameCond));

        if (ageCond != null)
            builder.and(member.age.eq(ageCond));

        return queryFactory.selectFrom(member).where(builder).fetch();
    }

    @Test
    @DisplayName("동적 쿼리 : Where 다중 파라미터 이용")
    public void dynamicQuery_where() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = queryFactory.selectFrom(member).where(
                usernameEq(usernameParam), ageEq(ageParam)
        ).fetch();
        assertThat(result.size()).isEqualTo(1);
    }

    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam != null ? member.age.eq(ageParam) : null;
    }

    private BooleanExpression usernameEq(String usernameParam) {
        return usernameParam != null ? member.username.eq(usernameParam) : null;
    }

    @Test
    @DisplayName("동적 쿼리 : Where 다중 파라미터 이용 2")
    public void dynamicQuery_where2() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = queryFactory.selectFrom(member).where(
                usernameAndAge(usernameParam, ageParam)
        ).fetch();
        assertThat(result.size()).isEqualTo(1);
    }

    private BooleanExpression usernameAndAge(String username, Integer age) {
        return usernameEq(username).and(ageEq(age));
    }




    @Test
    @DisplayName("벌크 연산 update")
    @Rollback(false)
    public void bulkUpdate() {
        em.persist(new Member("member5", 10));
        em.persist(new Member("member6", 20));

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // 꼭 플러시 필요
        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member m : result)
            System.out.println("m = " + m);

    }

    @Test
    @DisplayName("벌크 연산 update2")
    @Rollback(false)
    public void bulkUpdate2() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(100))
                .where(member.age.lt(28))
                .execute();

        // 꼭 플러시 필요
        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member m : result)
            System.out.println("m = " + m);
    }

    @Test
    @DisplayName("벌크 연산 delete")
    @Rollback(false)
    public void bulkDelete() {

        em.persist(new Member("member5", 10));
        em.persist(new Member("member6", 20));


        long count = queryFactory
                .delete(member)
                .where(member.age.lt(28))
                .execute();

        // 꼭 플러시 필요
        em.flush();
        em.clear();


        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member m : result)
            System.out.println("m = " + m);
    }


    @Test
    @DisplayName("SQL Function 사용하기")
    public void sqlFunction() {
        List<String> fetch = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",
                                member.username,
                                "member",
                                "M"
                        )
                )
                .from(member)
                .fetch();

        for(String s : fetch)
            System.out.println("s = " + s);
    }


    @Test
    @DisplayName("SQL Function 사용하기")
    public void sqlFunction2() {
        List<String> fetch1 = queryFactory
                .select(member.username)
                .from(member)
                .where(
                        member.username.eq(
                                Expressions.stringTemplate("function('lower', {0})", member.username)
                        )
                )
                .fetch();

        List<String> fetch2 = queryFactory
                .select(member.username)
                .from(member)
                .where(
                        member.username.eq(
                                member.username.lower()
                        )
                )
                .fetch();

        for(String s : fetch1)
            System.out.println("s = " + s);

        for(String s : fetch2)
            System.out.println("s = " + s);

    }


}
