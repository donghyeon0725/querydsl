package study.querydsl;

import lombok.Data;

@Data
public class MemberSearchCondition {
    private String username;
    private String teamName;

    // 그거나 같거나
    private Integer Goe;
    // 작거나 같거나
    private Integer Loe;

    private Integer size;
}
