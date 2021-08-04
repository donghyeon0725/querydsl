package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.MemberSearchCondition;
import study.querydsl.QMemberTeamDto;
import study.querydsl.domain.Member;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

import static study.querydsl.QMember.member;
import static study.querydsl.QTeam.team;


@RequiredArgsConstructor
public class MemberConditionRepositoryImpl implements MemberConditionRepository{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {

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

    @Override
    public Page<MemberTeamDto> pagingSimple(MemberSearchCondition condition, Pageable pageable) {

        QueryResults<MemberTeamDto> results = jpaQueryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"), member.username, member.age, member.team.id.as("teamId"), member.team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        teamNameEq(condition.getTeamName()),
                        usernameEq(condition.getUsername()),
                        ageGoe(condition.getGoe()),
                        ageLoe(condition.getLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> pagingComplex(MemberSearchCondition condition, Pageable pageable) {

        List<MemberTeamDto> content = jpaQueryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"), member.username, member.age, member.team.id.as("teamId"), member.team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        teamNameEq(condition.getTeamName()),
                        usernameEq(condition.getUsername()),
                        ageGoe(condition.getGoe()),
                        ageLoe(condition.getLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Member> countQuery = jpaQueryFactory
                //.select(new QMemberTeamDto(member.id.as("memberId"), member.username, member.age, member.team.id.as("teamId"), member.team.name.as("teamName")))
                .select(member)
                .from(member)
                // .leftJoin(member.team, team)
                .where(
                        teamNameEq(condition.getTeamName()),
                        usernameEq(condition.getUsername()),
                        ageGoe(condition.getGoe()),
                        ageLoe(condition.getLoe())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
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
