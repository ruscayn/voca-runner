package org.ruscayn.vocarunner.word.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 단어 노출 규칙 : 단어 노출(word_exposure)을 적용하는 규칙.
 * 단어 출제 목록을 만들 때 레벨 단어마다 적용한다 (단어 정책 4부 6장).
 * <ul>
 *   <li>단어 제외: 그 단어의 모든 레벨 단어를 뺀다</li>
 *   <li>레벨 제외: 그 레벨 단어만 뺀다</li>
 *   <li>뜻 제외: 그 뜻을 보기에서 뺀다. 보일 뜻이 남지 않는 레벨 단어는 뺀다</li>
 *   <li>유지: 아무것도 하지 않는다 (검토 기록)</li>
 * </ul>
 * 뺀 레벨 단어는 정답으로도, 오답 보기로도 내지 않는다 ({@link CatalogWord#excluded()}).
 * 규칙이 앱 한 곳에만 있도록 판정 데이터는 표로만 두고 적용은 여기서 한다.
 */
public final class WordExposureRules {

    /** 판정이 하나도 없을 때 */
    public static final WordExposureRules NONE = new WordExposureRules(List.of());

    private final Set<Long> excludedWords = new HashSet<>();
    private final Set<LevelWordKey> excludedLevelWords = new HashSet<>();
    private final Set<MeaningKey> excludedMeanings = new HashSet<>();

    public WordExposureRules(Collection<Rule> rules) {
        for (Rule rule : rules) {
            switch (rule.action()) {
                case EXCLUDE_WORD -> excludedWords.add(rule.wordId());
                case EXCLUDE_LEVEL -> excludedLevelWords.add(new LevelWordKey(rule.wordId(), rule.level()));
                case EXCLUDE_MEANING -> excludedMeanings.add(new MeaningKey(rule.wordId(), rule.meaningSeq()));
                case KEEP -> {
                }
                default -> throw new IllegalStateException("처리하지 않은 노출 판정: " + rule.action());
            }
        }
    }

    /**
     * 레벨 단어 하나에 판정을 적용한다.
     *
     * @param wordId         word_id
     * @param level          세부 레벨
     * @param optionMeanings 레벨 단어의 보기 뜻 (level_word 의 meaning1, meaning2 순서)
     * @return 보여줄 뜻과 제외 여부. 보일 뜻이 하나도 남지 않으면 제외하고 원래 뜻을 그대로 둔다
     *         (제외된 레벨 단어는 시험에 쓰지 않으므로 뜻은 표시에 쓰이지 않는다)
     */
    public Applied apply(long wordId, Level level, List<Meaning> optionMeanings) {
        List<String> visible = optionMeanings.stream()
                .filter(m -> !excludedMeanings.contains(new MeaningKey(wordId, m.seq())))
                .map(Meaning::text)
                .toList();
        boolean excluded = excludedWords.contains(wordId)
                || excludedLevelWords.contains(new LevelWordKey(wordId, level))
                || visible.isEmpty();
        return new Applied(visible.isEmpty() ? optionMeanings.stream().map(Meaning::text).toList() : visible, excluded);
    }

    /**
     * 판정 한 줄. 처리마다 대상이 맞아야 한다 (DB 의 ck_word_exposure_target 과 같은 규칙).
     *
     * @param meaningSeq 뜻 제외·유지의 대상 뜻 순번. 그 밖에는 null
     * @param level      레벨 제외·유지의 대상 레벨. 그 밖에는 null
     */
    public record Rule(long wordId, Integer meaningSeq, Level level, ExposureAction action) {

        public Rule {
            Objects.requireNonNull(action, "action");
            if (level != null && level.isGroup()) {
                throw new IllegalArgumentException("세부 레벨만 가능합니다: " + level);
            }
            boolean ok = switch (action) {
                case EXCLUDE_WORD -> meaningSeq == null && level == null;
                case EXCLUDE_MEANING -> meaningSeq != null && level == null;
                case EXCLUDE_LEVEL -> meaningSeq == null && level != null;
                case KEEP -> meaningSeq == null || level == null;
            };
            if (!ok) {
                throw new IllegalArgumentException("처리 %s 에 맞지 않는 대상: wordId=%d, meaningSeq=%s, level=%s"
                        .formatted(action, wordId, meaningSeq, level));
            }
        }
    }

    /** 레벨 단어의 보기 뜻 하나 (단어 안 뜻 순번, 한국어 뜻) */
    public record Meaning(int seq, String text) {
    }

    /** 적용 결과 */
    public record Applied(List<String> optionMeanings, boolean excluded) {
    }

    private record LevelWordKey(long wordId, Level level) {
    }

    private record MeaningKey(long wordId, int seq) {
    }

}
