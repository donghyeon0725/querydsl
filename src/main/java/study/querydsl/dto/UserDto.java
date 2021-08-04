package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDto {
    private String title;
    private int order;

    @QueryProjection
    public UserDto(String title, int order) {
        this.title = title;
        this.order = order;
    }
}
