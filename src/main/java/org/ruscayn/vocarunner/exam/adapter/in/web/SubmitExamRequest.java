package org.ruscayn.vocarunner.exam.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamCommand;

/** 옛 JSON 이름 answers 는 배포할 때 시험을 풀던 화면(옛 exam.js)의 제출을 받으려고 한 번 더 받는다 (11/05 이름 바꿈, 다음 배포 뒤 지움) */
public record SubmitExamRequest(@JsonAlias("answers") List<SubmitExamCommand.SelectedOptionItem> selectedOptions) {

}
