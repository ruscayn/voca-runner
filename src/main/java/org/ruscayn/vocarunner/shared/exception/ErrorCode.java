package org.ruscayn.vocarunner.shared.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR("COMMON_001", "내부 서버 오류가 발생했습니다."),
    INVALID_INPUT_VALUE("COMMON_002", "올바르지 않은 입력값입니다."),
    METHOD_NOT_ALLOWED("COMMON_003", "허용되지 않은 HTTP 메서드입니다."),
    RESOURCE_NOT_FOUND("COMMON_004", "요청한 리소스를 찾을 수 없습니다."),
    UNSUPPORTED_MEDIA_TYPE("COMMON_005", "지원하지 않는 요청 형식입니다."),

    // Business Logic
    ENTITY_NOT_FOUND("BUSINESS_001", "요청한 엔티티를 찾을 수 없습니다."),

    // Test
    TEST_NOT_ENOUGH_WORDS("TEST_001", "문제를 만들 단어가 부족합니다."),
    TEST_NOT_IN_PROGRESS("TEST_002", "진행 중인 시험이 아닙니다."),
    TEST_INVALID_OPTION("TEST_003", "올바르지 않은 보기 번호입니다."),
    TEST_NOT_COMPLETED("TEST_004", "완료되지 않은 시험입니다."),
    TEST_TIME_OVER("TEST_005", "제한 시간이 지나 시험이 무효가 되었습니다."),
    TEST_SERVER_BUSY("TEST_006", "지금은 응시자가 많아 시험을 시작할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    TEST_TOO_MANY_IN_PROGRESS_SAME_NETWORK("TEST_008", "같은 인터넷(와이파이)에서 진행 중인 시험이 너무 많습니다. 잠시 후 다시 시도해 주세요.");

    private final String code;
    private final String message;

}
