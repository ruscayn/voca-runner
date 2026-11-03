package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * 시험 화면용 문항 목록. 정답 보기 번호는 넣지 않는다.
 *
 * @param groupName      묶음 이름 (CEFR 원문: Basic / Independent / Proficient)
 * @param allowedMinutes 허용 시간(분). 안내 문구용
 * @param remainingSec   마감까지 남은 시간(초, 서버 기준). 기기 시계 차이를 피하려고 시각이 아닌 남은 시간으로 전달
 */
public record ExamQuestionsResult(
        UUID examId,
        String level,
        String groupName,
        String direction,
        int questionCount,
        int timePerQuestionSec,
        long allowedMinutes,
        long remainingSec,
        List<QuestionItem> questions
) {

    /**
     * 문항 하나.
     *
     * @param no        문항 번호 1~N
     * @param prompt    제시어 (EN_KO: 영어 단어, KO_EN: 한국어 뜻). 발음(TTS)은 이 값만 읽는다
     * @param promptSub 제시어 보조 = 제시어 아래 줄에 함께 보여 줄 형태 (EN_KO 의 headword_sub). 없으면 null
     * @param options   보기 1~4
     */
    public record QuestionItem(int no, String prompt, String promptSub, List<Option> options) {
    }

    /**
     * 보기 하나.
     *
     * @param no   보기 번호 1~4
     * @param text 보기 글 (보기 뜻을 이어 붙인 것)
     */
    public record Option(int no, String text) {
    }
}
