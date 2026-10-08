package org.ruscayn.vocarunner.domain.model.vocabulary;

import org.ruscayn.vocarunner.domain.model.level.Level;

import java.util.List;

/**
 * 메모리 캐시에 보관하는 출제 단어 (level_word 1행 = 1개). 읽기 전용 값 객체.
 *
 * <p>JPA Entity 를 캐시에 그대로 두지 않고 필요한 값만 복사해 둔다.
 * (지연 로딩 오류, 공유 객체 변경 위험 방지)
 *
 * @param levelWordId level_word_id
 * @param wordId      word_id (같은 단어 판별용)
 * @param headword    영어 표제어
 * @param level       세부 레벨 A1~C2
 * @param meanings    보기에 보여줄 한국어 뜻 1~2개
 */
public record CatalogWord(long levelWordId, long wordId, String headword, Level level, List<String> meanings) {

    public CatalogWord {
        if (level == null || level.isGroup()) {
            throw new IllegalArgumentException("세부 레벨만 가능합니다: " + level);
        }
        if (headword == null || headword.isBlank()) {
            throw new IllegalArgumentException("표제어가 비어 있습니다: levelWordId=" + levelWordId);
        }
        if (meanings == null || meanings.isEmpty()) {
            throw new IllegalArgumentException("표시 뜻이 없습니다: levelWordId=" + levelWordId);
        }
        meanings = List.copyOf(meanings);
    }

    /** 보기 표시용 뜻 텍스트 (예: "자유로운, 무료의") */
    public String meaningText() {
        return String.join(", ", meanings);
    }

}
