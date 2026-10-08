package org.ruscayn.myvoca.application.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 시험 화면용 문제 목록
 *
 * @param remainingSec 마감까지 남은 시간(초, 서버 기준). 기기 시계 차이를 피하려고 시각이 아닌 남은 시간으로 전달
 */
public record TestQuestionsResponse(
        UUID sessionId,
        String level,
        String direction,
        int questionCount,
        int timeLimitSec,
        long remainingSec,
        List<TestQuestionView> questions
) {
}
