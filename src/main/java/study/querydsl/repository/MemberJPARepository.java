package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.domain.Member;
import study.querydsl.MemberSearchCondition;
import study.querydsl.QMemberTeamDto;
import study.querydsl.dto.MemberTeamDto;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static study.querydsl.QMember.member;
import static study.querydsl.QTeam.team;

@Repository
@RequiredArgsConstructor
public class MemberJPARepository {

    private final EntityManager entityManager;
    private final JPAQueryFactory jpaQueryFactory;

    public void save(Member member) {
        entityManager.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member member = entityManager.find(Member.class, id);
        return Optional.of(member);
    }

    public Optional<Member> findByIdQuerydsl(Long id) {
        Member findMember = jpaQueryFactory.selectFrom(member).where(member.id.eq(id)).fetchOne();
        return Optional.of(findMember);
    }

    public List<Member> findAll() {
        return entityManager.createQuery("select m from Member m", Member.class).getResultList();
    }

    public List<Member> findAllQuerydsl() {
        return jpaQueryFactory.selectFrom(member).fetch();
    }

    public List<Member> findByUsername(String username) {
        return entityManager.createQuery("select m from Member m where m.username = :username")
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsernameQuerydsl(String username) {
        return jpaQueryFactory.selectFrom(member).where(member.username.eq(username)).fetch();
    }

    public List<MemberTeamDto> findBySearchCondition(MemberSearchCondition condition) {

        return jpaQueryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"), member.username, member.age, member.team.id.as("teamId"), member.team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        teamNameEq(condition.getTeamName()),
                        usernameEq(condition.getUsername()),
                        ageGoe(condition.getGoe()),
                        ageLoe(condition.getLoe())
                )
                .limit(limit(condition.getSize()))
                .fetch();
    }

    private long limit(Integer size) {
        return size != null ? size : 10;
    }

    private BooleanExpression ageLoe(Integer loe) {
        return loe != null ? member.age.loe(loe) : null;
    }

    private BooleanExpression ageGoe(Integer goe) {
        return goe != null ? member.age.goe(goe) : null;
    }

    private BooleanExpression usernameEq(String username) {
        return StringUtils.hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? member.team.name.eq(teamName) : null;
    }


}
