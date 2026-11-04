package org.ruscayn.vocarunner.exam.adapter.in.web;

import java.util.List;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamCommand;

/**
 * 제출 본문(JSON). 시험 아이디(주소) · 기기 아이디(쿠키)와 합쳐 SubmitExamCommand 로 옮기고, 검증은 Command 가 한다.
 *
 * @param answers 문항별 고른 보기 번호 (JSON 칸 answers[].questionNo · selectedOptionNo)
 */
public record SubmitExamRequest(List<SubmitExamCommand.Answer> answers) {
}
