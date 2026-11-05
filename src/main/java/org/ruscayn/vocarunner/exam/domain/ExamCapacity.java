package org.ruscayn.vocarunner.exam.domain;

import java.util.List;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;

public record ExamCapacity(int maxInProgress, int maxInProgressPerIp) {

    /** 같은 기기의 진행 중 시험은 새 시험으로 바뀌므로 세지 않는다 */
    public void check(Examinee examinee, int inProgressCount, int inProgressCountByIp, List<Exam> examsOnDevice) {
        requireBelowMaxInProgress(inProgressCount - examsOnDevice.size());
        requireBelowMaxInProgressPerIp(inProgressCountByIp - countSameIp(examinee, examsOnDevice));
    }

    private void requireBelowMaxInProgress(int inProgressCount) {
        if (inProgressCount >= maxInProgress) {
            throw new CustomException(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS);
        }
    }

    private static long countSameIp(Examinee examinee, List<Exam> examsOnDevice) {
        return examsOnDevice.stream().filter(exam -> exam.getExaminee().isSameIp(examinee)).count();
    }

    private void requireBelowMaxInProgressPerIp(long inProgressCountByIp) {
        if (inProgressCountByIp >= maxInProgressPerIp) {
            throw new CustomException(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS_PER_IP);
        }
    }

}
