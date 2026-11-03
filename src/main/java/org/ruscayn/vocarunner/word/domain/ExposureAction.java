package org.ruscayn.vocarunner.word.domain;

/**
 * 시험 노출 판정의 처리 (word_exposure.action). 처리마다 가리키는 대상이 다르다.
 */
public enum ExposureAction {

    /** 완전 제외: 단어 전체. 모든 레벨에서 정답으로도, 오답 보기로도 내지 않는다 */
    EXCLUDE_WORD,

    /** 일부 제외: 단어 안 뜻 하나. 모든 레벨 보기에서 그 뜻을 뺀다. 칸에 보일 뜻이 없으면 그 칸을 뺀다 */
    EXCLUDE_MEANING,

    /** 레벨 제외: 단어의 한 레벨 칸. 그 칸만 정답으로도, 오답 보기로도 내지 않는다 */
    EXCLUDE_LEVEL,

    /** 유지: 검토했지만 그대로 둔다 (검토 기록) */
    KEEP

}
