package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * 시험 화면용 문제 목록
 *
 * @param groupName      묶음 이름 (CEFR 원문: Basic / Independent / Proficient)
 * @param allowedMinutes 허용 시간(분). 안내 문구용
 * @param remainingSec   마감까지 남은 시간(초, 서버 기준). 기기 시계 차이를 피하려고 시각이 아닌 남은 시간으로 전달
 */
public record ExamQuestionsResponse(
        UUID examId,
        String level,
        String groupName,
        String direction,
        int questionCount,
        int timePerQuestionSec,
        long allowedMinutes,
        long remainingSec,
        List<ExamQuestionView> questions
) {
}
