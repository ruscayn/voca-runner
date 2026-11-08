package org.ruscayn.vocarunner.word.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.ruscayn.vocarunner.common.adapter.out.persistence.JpaConfig;
import org.ruscayn.vocarunner.support.TestPostgres;
import org.ruscayn.vocarunner.word.domain.ExposureAction;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.LevelWord;
import org.ruscayn.vocarunner.word.domain.WordExposureRules.Rule;
import org.ruscayn.vocarunner.word.domain.WordMeaning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 레벨 단어 · 단어 노출(word_exposure)을 실제 PostgreSQL 에서 그대로 읽는지 확인한다 (단어 노출 적용은 하지 않음).
 * 테스트 동안만 Docker 로 임시 PostgreSQL 을 띄워 표를 만든다 (db/schema.sql. 개발 DB 는 건드리지 않음).
 * 테스트마다 트랜잭션을 되돌리므로 넣은 단어는 남지 않는다.
 */
@DataJpaTest
@ImportTestcontainers(TestPostgres.class)
@Import({WordPersistenceAdapter.class, LevelWordMapper.class, JpaConfig.class})
@DisplayName("WordPersistenceAdapter : 레벨 단어 · 단어 노출 읽기 (PostgreSQL)")
class WordPersistenceAdapterTest {

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

    @Test
    @DisplayName("레벨 단어를 표제어 · 보기 뜻(순번)과 함께 읽는다")
    void loadLevelWords() {
        Map<Long, LevelWord> levelWords = adapter.loadLevelWords().stream()
                .collect(Collectors.toMap(LevelWord::levelWordId, Function.identity()));

        assertThat(levelWords).hasSize(6);
        assertThat(levelWords.get(201L)).isEqualTo(new LevelWord(201, 2, "sex", null, Level.B1,
                List.of(new WordMeaning(1, "성별"), new WordMeaning(2, "성관계"))));
        assertThat(levelWords.get(301L).optionMeanings()).containsExactly(new WordMeaning(1, "끈적한 것"));
    }

    @Test
    @DisplayName("단어 노출 판정을 그대로 읽는다 (유지 포함)")
    void loadExposureRules() {
        assertThat(adapter.loadExposureRules()).containsExactlyInAnyOrder(
                new Rule(1, null, Level.C2, ExposureAction.EXCLUDE_LEVEL),
                new Rule(2, 2, null, ExposureAction.EXCLUDE_MEANING),
                new Rule(3, null, null, ExposureAction.EXCLUDE_WORD),
                new Rule(4, null, Level.A1, ExposureAction.KEEP));
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
