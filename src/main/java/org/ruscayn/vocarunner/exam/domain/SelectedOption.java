package org.ruscayn.vocarunner.exam.domain;

/** 문항 하나에서 선택한 보기. 보기 번호가 없으면 선택하지 못함 (시간 초과) */
public record SelectedOption(int questionNo, Integer optionNo) {

}
