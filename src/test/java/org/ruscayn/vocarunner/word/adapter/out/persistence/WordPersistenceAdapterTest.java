package org.ruscayn.vocarunner.word.adapter.out.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.ruscayn.vocarunner.common.adapter.out.persistence.JpaConfig;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 단어 출제 목록 로딩에 단어 노출(word_exposure)이 적용되는지 실제 PostgreSQL 로 확인한다.
 * 테스트 동안만 Docker 로 임시 PostgreSQL 을 띄워 표를 만든다 (db/schema.sql. 개발 DB 는 건드리지 않음).
 * 테스트마다 트랜잭션을 되돌리므로 넣은 단어는 남지 않는다.
 */
@DataJpaTest
@Testcontainers
@Import({WordPersistenceAdapter.class, CatalogWordMapper.class, JpaConfig.class})
@DisplayName("WordPersistenceAdapter : 단어 출제 목록 로딩과 단어 노출 (PostgreSQL)")
class WordPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
            .withInitScript("db/schema.sql");

    @Autowired
    private WordPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 단어 4개, 레벨 단어 6개
     * <pre>
     * 1 lark : B2 종달새·장난 / C2 종달새·장난 (C2 레벨 제외 — 같은 뜻 다른 레벨)
     * 2 sex  : B1 성별·성관계 (뜻 2 뜻 제외)
     * 3 gook : B2 끈적한 것 / C1 끈적한 것 (단어 제외)
     * 4 bus  : A1 버스 (유지)
     * </pre>
     */
    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                INSERT INTO word (word_id, headword) VALUES (1, 'lark'), (2, 'sex'), (3, 'gook'), (4, 'bus')""");
        jdbcTemplate.update("""
                INSERT INTO word_meaning (meaning_id, word_id, seq, pos, meaning_ko) VALUES
                  (11, 1, 1, 'noun', '종달새'), (12, 1, 2, 'noun', '장난'),
                  (21, 2, 1, 'noun', '성별'), (22, 2, 2, 'noun', '성관계'),
                  (31, 3, 1, 'noun', '끈적한 것'),
                  (41, 4, 1, 'noun', '버스')""");
        jdbcTemplate.update("""
                INSERT INTO level_word (level_word_id, level_code, word_id, meaning1_id, meaning2_id) VALUES
                  (101, 'B2', 1, 11, 12), (102, 'C2', 1, 11, 12),
                  (201, 'B1', 2, 21, 22),
                  (301, 'B2', 3, 31, NULL), (302, 'C1', 3, 31, NULL),
                  (401, 'A1', 4, 41, NULL)""");
        jdbcTemplate.update("""
                INSERT INTO word_exposure_type (issue_type_code, name_ko, description, handling_rule, sort_order) VALUES
                  ('SAME_MEANING_OTHER_LEVEL', '같은 뜻 다른 레벨', '설명', '레벨 제외', 9),
                  ('SEXUAL_ACT', '성행위', '설명', '일부 제외', 5),
                  ('SLUR', '비하어', '설명', '완전 제외', 1),
                  ('LOANWORD', '외래어', '설명', '레벨 제외', 8)""");
        jdbcTemplate.update("""
                INSERT INTO word_exposure (word_id, meaning_seq, level_code, action, issue_type, reason, decided_at, decided_by) VALUES
                  (1, NULL, 'C2', 'EXCLUDE_LEVEL', 'SAME_MEANING_OTHER_LEVEL', 'B2 칸과 같은 뜻', '2026-10-27', '사용자'),
                  (2, 2, NULL, 'EXCLUDE_MEANING', 'SEXUAL_ACT', '성행위를 직접 가리킴', '2026-10-24', '사용자'),
                  (3, NULL, NULL, 'EXCLUDE_WORD', 'SLUR', '멸칭', '2026-10-24', '사용자'),
                  (4, NULL, 'A1', 'KEEP', 'LOANWORD', '검토 후 유지', '2026-10-10', '사용자')""");
    }

    private Map<Long, CatalogWord> loadById() {
        return adapter.loadCatalogWords().stream()
                .collect(Collectors.toMap(CatalogWord::levelWordId, Function.identity()));
    }

    @Test
    @DisplayName("판정대로 레벨 단어를 빼고 뜻을 뺀다")
    void appliesExposure() {
        Map<Long, CatalogWord> words = loadById();

        assertThat(words).hasSize(6);
        // 레벨 제외: lark C2 만 제외
        assertThat(words.get(101L).excluded()).isFalse();
        assertThat(words.get(102L).excluded()).isTrue();
        assertThat(words.get(102L).level()).isEqualTo(Level.C2);
        // 뜻 제외: sex 뜻 2 를 보기에서 빼고 출제
        assertThat(words.get(201L).optionMeanings()).containsExactly("성별");
        assertThat(words.get(201L).excluded()).isFalse();
        // 단어 제외: gook 모든 레벨 단어
        assertThat(List.of(words.get(301L), words.get(302L))).allMatch(CatalogWord::excluded);
        // 유지: 그대로
        assertThat(words.get(401L).excluded()).isFalse();
        assertThat(words.get(401L).optionMeanings()).containsExactly("버스");
    }

    @Test
    @DisplayName("판정이 없으면 모든 레벨 단어를 그대로 낸다")
    void noExposure() {
        jdbcTemplate.update("DELETE FROM word_exposure");

        assertThat(adapter.loadCatalogWords()).hasSize(6).noneMatch(CatalogWord::excluded);
    }

    /** 오류가 나면 PostgreSQL 트랜잭션이 깨지므로 한 테스트에 잘못된 줄 하나씩 */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "(4, NULL, 'C2', 'EXCLUDE_LEVEL', 'LOANWORD')",    // 없는 레벨 단어
            "(4, 3, NULL, 'EXCLUDE_MEANING', 'LOANWORD')",     // 없는 뜻
            "(4, 1, NULL, 'EXCLUDE_WORD', 'LOANWORD')",        // 단어 제외인데 뜻 순번
            "(4, NULL, NULL, 'EXCLUDE_LEVEL', 'LOANWORD')",    // 레벨 제외인데 레벨 없음
            "(3, NULL, NULL, 'KEEP', 'SLUR')",                 // 같은 대상 두 줄 (gook 단어 제외)
            "(4, NULL, NULL, 'HIDE', 'LOANWORD')",             // 없는 처리
            "(4, NULL, NULL, 'EXCLUDE_WORD', 'NO_SUCH_TYPE')"  // 없는 유형
    })
    @DisplayName("잘못된 판정은 DB 가 받지 않는다")
    void rejectsInvalidRow(String values) {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO word_exposure (word_id, meaning_seq, level_code, action, issue_type, reason, decided_at, decided_by)
                SELECT v.word_id, v.meaning_seq::smallint, v.level_code, v.action, v.issue_type, '사유', DATE '2026-10-27', '사용자'
                  FROM (VALUES %s) AS v (word_id, meaning_seq, level_code, action, issue_type)""".formatted(values)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}
