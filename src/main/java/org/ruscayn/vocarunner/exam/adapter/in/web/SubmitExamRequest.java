package org.ruscayn.vocarunner.exam.adapter.in.web;

import java.util.List;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamCommand;

public record SubmitExamRequest(List<SubmitExamCommand.SelectedOptionItem> selectedOptions) {

}
