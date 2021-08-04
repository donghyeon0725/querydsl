package study.querydsl.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.domain.Member;
import study.querydsl.domain.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

// local 에서 동작한다.
@Profile("local")
@Component
@RequiredArgsConstructor
public class InitData {

    // static class 로 작성했던 service 가 주입됩니다.
    private final InitService initService;

    // WAS 가 시작될 때 호출된다.
    @PostConstruct
    public void init() {
        initService.init();
    }

    /**
     * PostConstruct 와 라이프사이클이 다르기 때문에
     * 아래와 같이 별도로 나누어 주어야 합니다.
     * */
    @Component
    static class InitService {
        @Autowired
        private EntityManager entityManager;

        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            entityManager.persist(teamA);
            entityManager.persist(teamB);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;

                entityManager.persist(new Member("member"+i, i, selectedTeam));
            }
        }
    }
}
