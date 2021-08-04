package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.repository.MemberJPARepository;
import study.querydsl.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberJPARepository memberJPARepository;
    private final MemberRepository memberRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition searchCondition) {
        return memberJPARepository.findBySearchCondition(searchCondition);
    }

    @GetMapping("/v2/members")
    public List<MemberTeamDto> searchMemberV2(MemberSearchCondition searchCondition) {
        return memberRepository.search(searchCondition);
    }

    @GetMapping("/v3/members")
    public Page<MemberTeamDto> searchMemberV3(MemberSearchCondition searchCondition, Pageable pageable) {
        return memberRepository.pagingSimple(searchCondition, pageable);
    }

    @GetMapping("/v4/members")
    public Page<MemberTeamDto> searchMemberV4(MemberSearchCondition searchCondition, Pageable pageable) {
        return memberRepository.pagingComplex(searchCondition, pageable);
    }

}
