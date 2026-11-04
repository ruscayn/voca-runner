package org.ruscayn.vocarunner.exam.application.service;

import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;

class ExamNotFoundException extends CustomException {

    ExamNotFoundException() {
        super(ErrorCode.ENTITY_NOT_FOUND, "시험을 찾을 수 없습니다. (종료되었거나 만료됨)");
    }

}
