package org.ruscayn.vocarunner.exam.domain;

/** 문항 하나에서 고른 보기. 보기 번호가 없으면 고르지 못함 (시간 초과) */
public record SelectedOption(int questionNo, Integer optionNo) {

}
