package org.ruscayn.vocarunner.word.domain;

import java.util.List;

/**
 * 출제 목록 단어 : 단어 출제 목록(메모리)에 보관하는 값 (level_word 1행 = 1개). 읽기 전용 값 객체.
 *
 * <p>JPA Entity 를 캐시에 그대로 두지 않고 필요한 값만 복사해 둔다.
 * (지연 로딩 오류, 공유 객체 변경 위험 방지)
 *
 * @param levelWordId    level_word_id
 * @param wordId         word_id (같은 단어 판별용)
 * @param headword       영어 표제어
 * @param headwordSub    표제어 보조 = 표제어 아래 줄에 함께 보여 줄 형태 (예: for one's sake). 없으면 null
 * @param level          세부 레벨 A1~C2
 * @param optionMeanings 보기 뜻 = 보기에 보여 줄 한국어 뜻 1~2개 (뜻 제외된 뜻은 빠짐)
 * @param excluded       이 레벨에서 시험 제외 (단어 노출 적용 결과, {@link WordExposureRules}).
 *                       정답 문제로도, 오답 보기로도 내지 않음
 */
public record CatalogWord(long levelWordId, long wordId, String headword, String headwordSub,
                          Level level, List<String> optionMeanings, boolean excluded) {

    public CatalogWord {
        if (level == null || level.isGroup()) {
            throw new IllegalArgumentException("세부 레벨만 가능합니다: " + level);
        }
        if (headword == null || headword.isBlank()) {
            throw new IllegalArgumentException("표제어가 비어 있습니다: levelWordId=" + levelWordId);
        }
        if (optionMeanings == null || optionMeanings.isEmpty()) {
            throw new IllegalArgumentException("보기 뜻이 없습니다: levelWordId=" + levelWordId);
        }
        if (headwordSub != null && headwordSub.isBlank()) {
            headwordSub = null;
        }
        optionMeanings = List.copyOf(optionMeanings);
    }

    /** 보기 표시용 뜻 텍스트 (예: "자유로운, 무료의") */
    public String optionText() {
        return String.join(", ", optionMeanings);
    }

    /** 정답 문제로 낼 수 있는지 */
    public boolean isAnswerable() {
        return !excluded;
    }

}
