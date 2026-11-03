package org.ruscayn.vocarunner.exam.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ruscayn.vocarunner.exam.application.port.in.TestSubmitRequest;
import org.ruscayn.vocarunner.exam.application.port.out.TestSessionRepository;
import org.ruscayn.vocarunner.exam.domain.Direction;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.word.domain.Level;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.ruscayn.vocarunner.support.TestWords.correctOptionNo;
import static org.ruscayn.vocarunner.support.TestWords.questions;

/**
 * 시험 제출 로그.
 * 저장소는 가짜, 트랜잭션은 커밋만 흉내 낸다 (DB 없음). 서비스의 @Transactional 대신 TransactionTemplate 으로 감싸 부른다
 */
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("TestApplicationService : 제출 로그")
class TestApplicationServiceTest {

    private static final String DEVICE_ID = "22222222-2222-2222-2222-222222222222";

    private final TestSessionRepository repository = mock(TestSessionRepository.class);
    // 제출에는 저장소만 쓴다
    private final TestApplicationService service = new TestApplicationService(repository, null, null, null, null);

    private final Exam session = Exam.start(Level.A1, Direction.EN_KO, 5, questions(2),
            new Examinee(DEVICE_ID, "10.0.0.1"), Instant.now(), Duration.ofMinutes(3));

    @Test
    @DisplayName("저장(커밋)이 끝나면 제출 로그를 남긴다")
    void logAfterCommit(CapturedOutput output) {
        submitIn(transactionManager(false));

        assertThat(output.getOut()).contains("시험 제출 session=" + session.getId());
    }

    @Test
    @DisplayName("동시 제출의 늦은 쪽처럼 커밋할 때 저장 충돌로 되돌려지면 제출 로그를 남기지 않는다 (U39)")
    void noLogWhenCommitConflicts(CapturedOutput output) {
        assertThatThrownBy(() -> submitIn(transactionManager(true)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(output.getOut()).doesNotContain("시험 제출 session=");
    }

    private void submitIn(PlatformTransactionManager transactionManager) {
        given(repository.findById(session.getId())).willReturn(Optional.of(session));
        TestSubmitRequest request = new TestSubmitRequest(List.of(new TestSubmitRequest.Answer(1, correctOptionNo(1))));
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(tx -> service.submit(session.getId(), DEVICE_ID, request));
    }

    /** DB 없이 커밋만 흉내 내는 트랜잭션 관리자. conflict 이면 커밋할 때 저장 충돌(@Version)을 낸다 */
    private PlatformTransactionManager transactionManager(boolean conflict) {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
                if (conflict) {
                    throw new ObjectOptimisticLockingFailureException(Exam.class, session.getId());
                }
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
    }

}
