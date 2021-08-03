package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

    @Autowired
    EntityManager em;

    @Test
    void contextLoads() {
        Hello hello = new Hello();

        em.persist(hello);

        JPAQueryFactory query = new JPAQueryFactory(em);
        // querydsl 이 빌드한 qtype 클래스를 가지고 조회
        QHello qHello = new QHello("h");
        // 아래 코드를 대신 사용해도 됩니다.
        // QHello qHello = QHello.hello

        Hello result = query
                .selectFrom(qHello)
                .fetchOne();

        assertThat(result).isEqualTo(hello);
    }

}
