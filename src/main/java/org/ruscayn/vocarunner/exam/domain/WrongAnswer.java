package org.ruscayn.vocarunner.exam.domain;

public record WrongAnswer(int no, long answerOptionId, Long selectedOptionId) {

    public boolean isTimeout() {
        return selectedOptionId == null;
    }

}
