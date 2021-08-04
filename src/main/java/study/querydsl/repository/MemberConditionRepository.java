package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberConditionRepository {
    List<MemberTeamDto> search(MemberSearchCondition condition);

    Page<MemberTeamDto> pagingSimple(MemberSearchCondition condition, Pageable pageable);
    Page<MemberTeamDto> pagingComplex(MemberSearchCondition condition, Pageable pageable);
}
