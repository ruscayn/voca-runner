/*
 * 시험 화면
 * - 문제는 시작 시 한 번에 받아오고, 답은 모아 두었다가 마지막에 한 번에 제출
 * - [시작] 버튼을 누르면 1번 문제부터 진행 (iOS 는 터치가 있어야 소리가 나므로)
 * - 문제 단어가 뜨면 TTS 로 읽어준다 (브라우저 내장 Web Speech API)
 * - 보기를 누르거나 제한 시간이 지나면 바로 다음 문제 (시간 초과 = 답 없음)
 * - 시험 중 화면을 벗어나면(새로고침/뒤로가기/앱 전환/화면 꺼짐) 시험은 만료된다
 * - 마감(문항 수 × 제한 시간 + 여유)까지 남은 전체 시간을 표시하고, 지나면 시험은 무효
 *   마감 시각은 메인에서 시험을 만든 순간부터 계산되므로 [시작] 대기 시간도 포함된다
 */
(() => {
    'use strict';

    const INPUT_LOCK_MS = 150;   // 직전 탭이 다음 문제로 넘어가 눌리는 것 방지
    const CLOCK_TICK_MS = 250;   // 남은 시간 갱신 주기
    const URGENT_SEC = 60;       // 남은 시간이 이 값 이하이면 빨간색

    const app = document.getElementById('test-app');
    const sessionId = app.dataset.sessionId;
    const api = (path) => `/api/tests/${sessionId}${path}`;

    const el = {
        currentNo: document.getElementById('current-no'),
        total: document.getElementById('total-count'),
        timerBar: document.getElementById('timer-bar'),
        remain: document.getElementById('remain'),
        remainTime: document.getElementById('remain-time'),
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
        direction: 'EN_KO',
        questionCount: 0,
        deadlineAt: 0,        // 마감 시각 (performance.now() 기준, 기기 시계와 무관)
        clockId: null,
        index: -1,
        answers: [],          // { questionNo, selectedOptionNo | null }
        timerId: null,
        urgentId: null,
        lockedUntil: 0,
        status: 'LOADING',    // LOADING → READY → RUNNING → SUBMITTING → DONE | ABORTED
    };

    // ---------- TTS ----------
    const tts = (() => {
        const synth = window.speechSynthesis;
        const supported = !!synth && typeof SpeechSynthesisUtterance !== 'undefined';
        let voice = null;

        function pickVoice() {
            if (!supported) return;
            const voices = synth.getVoices();
            voice = voices.find((v) => v.lang === 'en-US' && v.localService)
                || voices.find((v) => v.lang === 'en-US')
                || voices.find((v) => v.lang && v.lang.startsWith('en'))
                || null;
        }

        if (supported) {
            pickVoice();
            synth.addEventListener?.('voiceschanged', pickVoice);
        }

        return {
            speak(text) {
                if (!supported || !text) return;
                synth.cancel();
                const u = new SpeechSynthesisUtterance(text);
                u.lang = 'en-US';
                if (voice) u.voice = voice;
                u.rate = 0.9;
                synth.speak(u);
            },
            stop() {
                if (supported) synth.cancel();
            },
            /** iOS: 사용자 터치 이벤트 안에서 한 번 호출해야 이후 자동 재생 가능 */
            unlock() {
                if (!supported) return;
                const u = new SpeechSynthesisUtterance('');
                u.volume = 0;
                synth.speak(u);
            },
        };
    })();

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

    // ---------- 남은 전체 시간 (마감까지) ----------
    function remainingSec() {
        return Math.max(0, Math.ceil((state.deadlineAt - performance.now()) / 1000));
    }

    /** 03:25 형식 (헤더) */
    function formatClock(sec) {
        const m = Math.floor(sec / 60);
        const s = sec % 60;
        return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }

    /** 3분 25초 형식 (안내 문구) */
    function formatKorean(sec) {
        const m = Math.floor(sec / 60);
        const s = sec % 60;
        if (m === 0) return `${s}초`;
        return s === 0 ? `${m}분` : `${m}분 ${s}초`;
    }

    function readyText(sec) {
        return `${state.questionCount}문항 · 문제당 ${state.timeLimitMs / 1000}초\n`
            + `${formatKorean(sec)} 안에 마쳐야 하며,\n지나면 시험이 무효가 됩니다.\n`
            + '시작하면 화면을 벗어나지 마세요.';
    }

    function tickClock() {
        const sec = remainingSec();
        el.remainTime.textContent = formatClock(sec);
        el.remain.classList.toggle('urgent', sec <= URGENT_SEC);

        // 시작 대기 중에도 마감 시간은 흐르므로 안내 문구를 함께 갱신
        if (state.status === 'READY') el.overlayText.textContent = readyText(sec);

        if (sec === 0) timeOver();
    }

    function startClock(remainSecFromServer) {
        state.deadlineAt = performance.now() + remainSecFromServer * 1000;
        clearInterval(state.clockId);
        state.clockId = setInterval(tickClock, CLOCK_TICK_MS);
        tickClock();
    }

    function stopClock() {
        clearInterval(state.clockId);
    }

    /** 마감 시각 경과 : 시험 무효 (서버도 마감 후 제출은 거절) */
    function timeOver() {
        if (state.status !== 'RUNNING' && state.status !== 'READY') return;
        state.status = 'ABORTED';
        stopClock();
        clearTimeout(state.timerId);
        clearTimeout(state.urgentId);
        tts.stop();
        navigator.sendBeacon(api('/expire'));
        showOverlay('시험이 무효가 되었습니다', '제한 시간이 지나\n시험이 무효가 되었습니다.', '확인', goMain);
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

        // 영어 단어가 문제일 때만 읽어준다 (KO_EN 은 추후)
        if (state.direction === 'EN_KO') tts.speak(q.prompt);

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
            tts.stop();
            submit();
        }
    }

    el.options.forEach((btn) => {
        btn.addEventListener('click', () => {
            if (performance.now() < state.lockedUntil) return;
            answer(Number(btn.dataset.no));
        });
    });

    function begin() {
        if (state.status !== 'READY') return;
        tts.unlock();                 // 터치 이벤트 안에서 호출 (iOS)
        el.overlay.hidden = true;
        state.status = 'RUNNING';
        showQuestion(0);
    }

    // ---------- 서버 통신 ----------
    async function load() {
        showOverlay('문제 불러오는 중...');
        try {
            const res = await fetch(api('/questions'));
            if (!res.ok) throw await errorOf(res);
            const data = await res.json();

            state.questions = data.questions;
            state.timeLimitMs = data.timeLimitSec * 1000;
            state.direction = data.direction;
            state.questionCount = data.questionCount;
            el.total.textContent = data.questionCount;

            state.status = 'READY';
            showOverlay('준비되었습니다', readyText(data.remainingSec), '시작', begin);
            startClock(data.remainingSec);
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

            state.status = 'DONE';
            stopClock();
            location.replace(`/test/${sessionId}/result`);   // 결과 화면 (시험 화면은 기록에서 제거)
        } catch (e) {
            if (isFinalError(e)) {
                // 서버가 거절 (마감 초과·이미 종료·없는 시험 등) : 다시 제출해도 소용없으므로 메인으로
                state.status = 'ABORTED';
                stopClock();
                const title = e.code === 'TEST_005' ? '시험이 무효가 되었습니다' : '제출할 수 없습니다';
                showOverlay(title, e.message, '확인', goMain);
                return;
            }
            // 네트워크 오류·서버 오류 : 다시 시도 (답안은 그대로 유지)
            state.status = 'RUNNING';   // 이탈 감지·마감 확인 유지
            showOverlay('제출하지 못했습니다', e.message, '다시 제출', () => {
                if (state.status !== 'RUNNING') return;
                submit();
            });
        }
    }

    /** 4xx 응답 = 요청 자체가 거절됨 (재시도 무의미). 네트워크 오류·5xx 는 재시도 가능 */
    function isFinalError(e) {
        return typeof e.status === 'number' && e.status >= 400 && e.status < 500;
    }

    async function errorOf(res) {
        let error;
        try {
            const body = await res.json();
            error = new Error(body.message || `오류 (${res.status})`);
            error.code = body.code;
        } catch {
            error = new Error(`오류 (${res.status})`);
        }
        error.status = res.status;
        return error;
    }

    // ---------- 이탈 감지 : 시험 중(시작 대기 포함) 화면을 벗어나면 만료 ----------
    function abort() {
        if (state.status !== 'RUNNING' && state.status !== 'READY') return;
        state.status = 'ABORTED';
        stopClock();
        clearTimeout(state.timerId);
        tts.stop();
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
        if (state.status === 'RUNNING' || state.status === 'READY') {
            abort();          // 종료 안내 → 확인 시 메인
        } else {
            goMain();
        }
    });

    // 결과 화면에서 뒤로가기로 캐시된 시험 화면이 복원되면 메인으로
    window.addEventListener('pageshow', (e) => {
        if (e.persisted) goMain();
    });

    load();
})();
