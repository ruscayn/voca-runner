package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 시험 기능으로 들어오는 포트. 화면 · API · 정리 작업이 이 포트를 부른다.
 * 시험 시작 · 문제 받기 · 제출 · 결과 · 중단 · 정리와 메인 화면의 레벨 요약을 맡는다.
 */
public interface ExamUseCase {

    /** 시험을 시작하고 새 시험 아이디를 돌려준다. 최대 진행 중에 걸리면 거절 */
    UUID start(StartExamCommand command);

    /** 시험 화면용 문제 (진행 중인 시험만, 정답 번호 제외). 시작한 기기만 */
    ExamQuestionsResult getQuestions(UUID examId, String deviceId);

    /** 제출 및 채점. 시작한 기기만 */
    SubmitExamResult submit(SubmitExamCommand command);

    /** 결과 (완료된 시험만). 없거나(보관 시간 경과 포함) 완료 전이면 empty */
    Optional<ExamResult> findResult(UUID examId);

    /** 제출을 마쳐 결과를 보관 중인지 */
    boolean hasResult(UUID examId);

    /** 결과 (API). 없거나 완료 전이면 거절 */
    ExamResult getResult(UUID examId);

    /** 진행 중이면 중단(바로 지움). 없거나 이미 끝났거나 다른 기기에서 시작한 시험은 무시 */
    void abortExam(UUID examId, String deviceId);

    /** 진행 중(마감 전)인 시험인지 */
    boolean isInProgress(UUID examId);

    /** 폐기 대상 시험을 지우고 지운 수를 돌려준다 (정리 주기마다) */
    int cleanup();

    /** 세부 레벨을 CEFR 묶음별로 나눈 목록과 레벨별 단어 수 · 문항 수 · 허용 시간 (메인 화면) */
    List<LevelGroupSummary> getDetailLevelGroups();
}
