package org.ruscayn.vocarunner.word.domain;

public enum ExposureAction {

    /** 단어 제외: 단어 전체. 모든 레벨에서 정답으로도, 오답 보기로도 내지 않는다 */
    EXCLUDE_WORD,

    /** 뜻 제외: 단어 안 뜻 하나. 모든 레벨 보기에서 그 뜻을 뺀다. 보일 뜻이 남지 않는 레벨 단어는 뺀다 */
    EXCLUDE_MEANING,

    /** 레벨 제외: 한 레벨 단어. 그 레벨 단어만 정답으로도, 오답 보기로도 내지 않는다 */
    EXCLUDE_LEVEL,

    /** 유지: 검토했지만 그대로 둔다 (검토 기록) */
    KEEP;

    /** DB 제약 ck_word_exposure_target 과 같은 규칙 */
    public boolean isAllowed(Integer meaningSeq, Level level) {
        return switch (this) {
            case EXCLUDE_WORD -> meaningSeq == null && level == null;
            case EXCLUDE_MEANING -> meaningSeq != null && level == null;
            case EXCLUDE_LEVEL -> meaningSeq == null && level != null;
            case KEEP -> meaningSeq == null || level == null;
        };
    }

}
