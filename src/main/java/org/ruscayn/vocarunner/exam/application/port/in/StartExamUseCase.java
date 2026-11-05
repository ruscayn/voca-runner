package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.UUID;

public interface StartExamUseCase {

    UUID start(StartExamCommand command);

}
