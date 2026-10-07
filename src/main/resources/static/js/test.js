/*
 * 시험 화면
 * - 문제는 시작 시 한 번에 받아오고, 답은 모아 두었다가 마지막에 한 번에 제출
 * - 보기를 누르거나 제한 시간이 지나면 바로 다음 문제 (시간 초과 = 답 없음)
 * - 시험 중 화면을 벗어나면(새로고침/뒤로가기/앱 전환/화면 꺼짐) 시험은 만료된다
 */
(() => {
    'use strict';

    const INPUT_LOCK_MS = 150;   // 직전 탭이 다음 문제로 넘어가 눌리는 것 방지

    const app = document.getElementById('test-app');
    const sessionId = app.dataset.sessionId;
    const api = (path) => `/api/tests/${sessionId}${path}`;

    const el = {
        currentNo: document.getElementById('current-no'),
        total: document.getElementById('total-count'),
        timerBar: document.getElementById('timer-bar'),
        prompt: document.getElementById('prompt'),
        options: Array.from(document.querySelectorAll('.option-btn')),
        overlay: document.getElementById('overlay'),
        overlayTitle: document.getElementById('overlay-title'),
        overlayText: document.getElementById('overlay-text'),
        overlayBtn: document.getElementById('overlay-btn'),
    };

    const state = {
        questions: [],
        timeLimitMs: 5000,
        index: -1,
        answers: [],          // { questionNo, selectedOptionNo | null }
        timerId: null,
        urgentId: null,
        lockedUntil: 0,
        status: 'LOADING',    // LOADING → RUNNING → SUBMITTING → DONE | ABORTED
    };

    // ---------- 화면 표시 ----------
    function showOverlay(title, text = '', buttonText = null, onClick = null) {
        el.overlayTitle.textContent = title;
        el.overlayText.textContent = text;
        el.overlayBtn.hidden = !buttonText;
        if (buttonText) {
            el.overlayBtn.textContent = buttonText;
            el.overlayBtn.onclick = onClick;
        }
        el.overlay.hidden = false;
    }

    function goMain() {
        location.replace('/');
    }

    function startTimerBar() {
        const bar = el.timerBar;
        bar.classList.remove('urgent');
        bar.style.transition = 'none';
        bar.style.transform = 'scaleX(1)';
        void bar.offsetWidth;                          // 애니메이션 재시작
        bar.style.transition = `transform ${state.timeLimitMs}ms linear`;
        bar.style.transform = 'scaleX(0)';
        clearTimeout(state.urgentId);
        state.urgentId = setTimeout(() => {
            if (state.status === 'RUNNING') bar.classList.add('urgent');
        }, state.timeLimitMs * 0.7);
    }

    // ---------- 진행 ----------
    function showQuestion(index) {
        state.index = index;
        const q = state.questions[index];
        el.currentNo.textContent = q.no;
        el.prompt.textContent = q.prompt;
        q.options.forEach((opt, i) => {
            el.options[i].textContent = opt.text;
            el.options[i].dataset.no = opt.no;
        });
        state.lockedUntil = performance.now() + INPUT_LOCK_MS;

        clearTimeout(state.timerId);
        state.timerId = setTimeout(() => answer(null), state.timeLimitMs);
        startTimerBar();
    }

    function answer(selectedOptionNo) {
        if (state.status !== 'RUNNING') return;
        clearTimeout(state.timerId);

        const q = state.questions[state.index];
        state.answers.push({ questionNo: q.no, selectedOptionNo });

        if (state.index + 1 < state.questions.length) {
            showQuestion(state.index + 1);
        } else {
            submit();
        }
    }

    el.options.forEach((btn) => {
        btn.addEventListener('click', () => {
            if (performance.now() < state.lockedUntil) return;
            answer(Number(btn.dataset.no));
        });
    });

    // ---------- 서버 통신 ----------
    async function load() {
        showOverlay('문제 불러오는 중...');
        try {
            const res = await fetch(api('/questions'));
            if (!res.ok) throw await errorOf(res);
            const data = await res.json();

            state.questions = data.questions;
            state.timeLimitMs = data.timeLimitSec * 1000;
            el.total.textContent = data.questionCount;

            el.overlay.hidden = true;
            state.status = 'RUNNING';
            showQuestion(0);
        } catch (e) {
            state.status = 'ABORTED';
            showOverlay('시험을 시작할 수 없습니다', e.message, '확인', goMain);
        }
    }

    async function submit() {
        state.status = 'SUBMITTING';
        clearTimeout(state.timerId);
        showOverlay('채점 중...');
        try {
            const res = await fetch(api('/submit'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ answers: state.answers }),
            });
            if (!res.ok) throw await errorOf(res);
            const result = await res.json();

            state.status = 'DONE';
            // TODO 결과 화면이 생기면 그 화면으로 이동
            showOverlay(`${result.score}점`,
                `${result.questionCount}문제 중 ${result.correctCount}개 맞혔습니다.`,
                '확인', goMain);
        } catch (e) {
            // 제출 실패 시 다시 시도 (답안은 그대로 유지)
            state.status = 'RUNNING';   // 이탈 감지 유지
            showOverlay('제출하지 못했습니다', e.message, '다시 제출', () => {
                state.status = 'RUNNING';
                submit();
            });
        }
    }

    async function errorOf(res) {
        try {
            const body = await res.json();
            return new Error(body.message || `오류 (${res.status})`);
        } catch {
            return new Error(`오류 (${res.status})`);
        }
    }

    // ---------- 이탈 감지 : 시험 중 화면을 벗어나면 만료 ----------
    function abort() {
        if (state.status !== 'RUNNING') return;
        state.status = 'ABORTED';
        clearTimeout(state.timerId);
        navigator.sendBeacon(api('/expire'));
        showOverlay('시험이 종료되었습니다', '시험 중에 화면을 벗어나면\n시험은 종료됩니다.', '확인', goMain);
    }

    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'hidden') abort();
    });
    window.addEventListener('pagehide', abort);

    // 뒤로가기 : 한 번 막고 만료 처리
    history.pushState(null, '', location.href);
    window.addEventListener('popstate', () => {
        if (state.status === 'RUNNING') {
            abort();          // 종료 안내 → 확인 시 메인
        } else {
            goMain();
        }
    });

    load();
})();
