package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.List;

/**
 * 시험 화면용 문제. 정답 번호는 포함하지 않는다.
 *
 * @param no      문제 번호 1~N
 * @param prompt    문제 (EN_KO: 영어 단어, KO_EN: 한국어 뜻). 발음(TTS)은 이 값만 읽는다
 * @param promptSub 제시어 보조 = 제시어 아래 줄에 함께 보여 줄 형태 (EN_KO 의 headword_sub). 없으면 null
 * @param options   보기 1~4
 */
public record ExamQuestionView(int no, String prompt, String promptSub, List<Option> options) {

    /** @param no 보기 번호 1~4, @param text 보기 내용 */
    public record Option(int no, String text) {
    }

}
