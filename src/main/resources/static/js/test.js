/*
 * 시험 화면
 * - 문제는 시작 시 한 번에 받아오고, 답은 모아 두었다가 마지막에 한 번에 제출
 * - [시작] 버튼을 누르면 1번 문제부터 진행 (iOS 는 터치가 있어야 소리가 나므로)
 * - 문제 단어가 뜨면 TTS 로 읽어준다 (브라우저 내장 Web Speech API)
 * - 보기를 누르거나 제한 시간이 지나면 바로 다음 문제 (시간 초과 = 답 없음)
 * - 문제당 남은 시간은 막대(흐름) + 숫자(정확한 초)로 표시. 2초 이하면 빨간색
 * - 시간은 화면을 보고 있든 아니든 실제 시간 기준으로 흐른다
 *   앱 전환·화면 꺼짐 동안 브라우저가 타이머를 늦추거나 멈춰도, 돌아오면 그동안 지나간 문제를
 *   모두 시간 초과로 처리하고 그 시점의 문제를 남은 초부터 이어서 보여준다
 * - 새로고침·뒤로가기·창 닫기는 답안이 사라지므로 시험을 만료한다
 *   풀이 중 뒤로가기는 "그만둘까요?" 확인(그동안 문제 시간 멈춤), 새로고침·창 닫기는 브라우저 기본 확인창
 * - 마감(분 단위)까지 남은 전체 시간을 표시하고, 지나면 시험은 무효
 *   마감 시각은 메인에서 시험을 만든 순간부터 계산되므로 [시작] 대기 시간도 포함된다
 */
(() => {
    'use strict';

    const INPUT_LOCK_MS = 350;   // 직전 탭(실수로 두 번 누름 150~300ms)이 다음 문제로 넘어가 눌리는 것 방지
    const CLOCK_TICK_MS = 100;   // 시간 확인 주기
    const URGENT_SEC = 60;       // 남은 전체 시간이 이 값 이하이면 빨간색
    const Q_URGENT_SEC = 2;      // 문제당 남은 초가 이 값 이하이면 빨간색
    const PROMPT_MAX_PX = 52;    // 문제 단어 글자 크기 (긴 단어는 폭에 맞춰 줄임)
    const PROMPT_MIN_PX = 28;
    const PROMPT_MAX_LINES = 2;  // 띄어쓰기 있는 구는 2줄까지 허용

    const app = document.getElementById('test-app');
    const sessionId = app.dataset.sessionId;
    const api = (path) => `/api/tests/${sessionId}${path}`;
    const $ = (id) => document.getElementById(id);

    const el = {
        currentNo: $('current-no'),
        total: $('total-count'),
        progressFill: $('progress-fill'),
        remain: $('remain'),
        remainTime: $('remain-time'),
        prompt: $('prompt'),
        promptSub: $('prompt-sub'),
        timerBar: $('timer-bar'),
        timerSecBox: $('timer-sec-box'),
        timerSec: $('timer-sec'),
        options: Array.from(document.querySelectorAll('.option-btn')),
        levelCaption: $('level-caption'),
        overlay: $('overlay'),
        sheetEyebrow: $('sheet-eyebrow'),
        overlayTitle: $('overlay-title'),
        overlayText: $('overlay-text'),
        readyInfo: $('ready-info'),
        overlayBack: $('overlay-back'),
        overlayBtn: $('overlay-btn'),
    };

    const state = {
        questions: [],
        timeLimitMs: 5000,
        direction: 'EN_KO',
        questionCount: 0,
        deadlineAt: 0,        // 전체 마감 시각 (performance.now() 기준, 기기 시계와 무관)
        questionEndAt: 0,     // 현재 문제 마감 시각 (performance.now() 기준)
        clockId: null,
        index: -1,
        answers: [],          // { questionNo, selectedOptionNo | null }
        lockedUntil: 0,
        pausedRemainMs: 0,    // 그만둘지 묻는 동안 멈춘 현재 문제의 남은 시간
        status: 'LOADING',    // LOADING → READY → RUNNING (⇄ PAUSED) → SUBMITTING → DONE | ABORTED
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

    // ---------- 안내 시트 ----------
    function showOverlay(title, text = '', buttonText = null, onClick = null) {
        el.sheetEyebrow.hidden = true;
        el.readyInfo.hidden = true;
        el.overlayBack.hidden = true;
        el.overlayTitle.textContent = title;
        el.overlayText.textContent = text;
        el.overlayBtn.hidden = !buttonText;
        if (buttonText) {
            el.overlayBtn.textContent = buttonText;
            el.overlayBtn.onclick = onClick;
        }
        el.overlay.hidden = false;
        setBackgroundInert(true);
        if (buttonText) el.overlayBtn.focus();     // 키보드: Tab 없이 바로 Enter
    }

    function hideOverlay() {
        el.overlay.hidden = true;
        setBackgroundInert(false);
    }

    /** 안내 시트가 떠 있는 동안 뒤의 화면(보기 버튼 등)은 Tab·클릭이 가지 않게 한다 */
    function setBackgroundInert(inert) {
        for (const child of app.children) {
            if (child !== el.overlay) child.inert = inert;
        }
    }

    /** 시작 전 안내 : 레벨, 500문항 / 5초 / 45분, 안내 2가지, [이전] [시작] */
    function showReady(data) {
        showOverlay('준비되면 출발하세요', '', '시작', begin);
        el.sheetEyebrow.textContent = `${data.level} · ${data.levelGroup}`;
        el.sheetEyebrow.hidden = false;
        $('ready-count').textContent = data.questionCount.toLocaleString('ko-KR');
        $('ready-sec').textContent = data.timeLimitSec;
        $('ready-min').textContent = data.allowedMinutes;
        $('ready-min-text').textContent = data.allowedMinutes;
        el.readyInfo.hidden = false;
        el.overlayBack.onclick = cancelReady;
        el.overlayBack.hidden = false;
    }

    /** 시작 전 [이전] : 아직 풀지 않았으므로 안내 없이 시험을 취소하고 메인으로 */
    function cancelReady() {
        if (state.status !== 'READY') return;
        state.status = 'ABORTED';
        stopClock();
        navigator.sendBeacon(api('/expire'));
        goMain();
    }

    function goMain() {
        location.replace('/');
    }

    // ---------- 문제당 타이머 (막대 + 남은 초) ----------
    /** 현재 문제의 남은 시간만큼 막대를 이어서 줄인다 (중간부터 시작 가능) */
    function startQuestionBar() {
        const remainMs = Math.max(0, state.questionEndAt - performance.now());
        const bar = el.timerBar;
        bar.style.transition = 'none';
        bar.style.transform = `scaleX(${remainMs / state.timeLimitMs})`;
        void bar.offsetWidth;                          // 애니메이션 재시작
        bar.style.transition = `transform ${remainMs}ms linear`;
        bar.style.transform = 'scaleX(0)';
    }

    function updateQuestionSec() {
        const sec = Math.max(0, Math.ceil((state.questionEndAt - performance.now()) / 1000));
        el.timerSec.textContent = sec;
        const urgent = sec <= Q_URGENT_SEC;
        el.timerSecBox.classList.toggle('urgent', urgent);
        el.timerBar.classList.toggle('urgent', urgent);
    }

    /**
     * 실제 시간 기준으로 지나간 문제를 모두 시간 초과 처리.
     * 다음 문제는 이전 문제의 마감 시각부터 시작한 것으로 본다 (화면을 벗어나 있어도 기다려 주지 않음)
     */
    function settleTimeouts() {
        if (state.status !== 'RUNNING' || state.answers.length >= state.questions.length) return;
        const now = performance.now();
        if (now < state.questionEndAt) return;

        while (now >= state.questionEndAt) {
            const q = state.questions[state.index];
            state.answers.push({ questionNo: q.no, selectedOptionNo: null });
            if (state.index + 1 >= state.questions.length) {
                finish();
                return;
            }
            state.index += 1;
            state.questionEndAt += state.timeLimitMs;
        }
        showQuestion(state.index);
    }

    // ---------- 남은 전체 시간 (마감까지) ----------
    function remainingSec() {
        return Math.max(0, Math.ceil((state.deadlineAt - performance.now()) / 1000));
    }

    /** 03:25 형식 */
    function formatClock(sec) {
        const m = Math.floor(sec / 60);
        const s = sec % 60;
        return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }

    function tickClock() {
        const sec = remainingSec();
        el.remainTime.textContent = formatClock(sec);
        el.remain.classList.toggle('urgent', sec <= URGENT_SEC);

        if (sec === 0) {
            timeOver();
            return;
        }
        settleTimeouts();
        if (state.status === 'RUNNING') updateQuestionSec();
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
        if (state.status !== 'RUNNING' && state.status !== 'READY' && state.status !== 'PAUSED') return;
        state.status = 'ABORTED';
        stopClock();
        tts.stop();
        navigator.sendBeacon(api('/expire'));
        showOverlay('시험이 무효 처리되었습니다', '제한 시간 안에 제출하지 않아\n시험이 무효 처리되었습니다.', '확인', goMain);
    }

    // ---------- 문제 단어 크기 ----------
    /**
     * 단어가 폭에 들어가고(단어 중간 줄바꿈 없음) 구는 2줄 이하가 될 때까지 글자를 줄인다.
     * 최소 크기에서도 넘치면(아주 좁은 화면) 그때만 단어 중간 줄바꿈을 허용한다
     */
    function fitPrompt() {
        const p = el.prompt;
        p.classList.remove('prompt-break');
        for (let px = PROMPT_MAX_PX; px >= PROMPT_MIN_PX; px -= 2) {
            p.style.fontSize = `${px}px`;
            const fitsWidth = p.scrollWidth <= p.clientWidth;
            const lines = Math.round(p.offsetHeight / (px * 1.1));   // line-height 1.1
            if (fitsWidth && lines <= PROMPT_MAX_LINES) return;
        }
        p.classList.add('prompt-break');
    }

    // 화면 회전·글꼴 로드가 끝나면 폭이 달라지므로 다시 맞춘다
    window.addEventListener('resize', () => { if (state.index >= 0) fitPrompt(); });
    document.fonts?.ready.then(() => { if (state.index >= 0) fitPrompt(); });

    // ---------- 진행 ----------
    /** state.index 문제를 표시. 마감 시각(state.questionEndAt)은 호출 전에 정해 둔다 */
    function showQuestion(index) {
        state.index = index;
        const q = state.questions[index];
        el.currentNo.textContent = q.no;
        el.progressFill.style.width = `${(index / state.questionCount) * 100}%`;
        el.prompt.textContent = q.prompt;
        fitPrompt();
        // 함께 표시할 형태 (예: for one's sake). 발음은 표제어(q.prompt)만 읽는다
        el.promptSub.textContent = q.promptSub || '';
        el.promptSub.hidden = !q.promptSub;
        q.options.forEach((opt, i) => {
            el.options[i].querySelector('.option-text').textContent = opt.text;
            el.options[i].dataset.no = opt.no;
        });
        state.lockedUntil = performance.now() + INPUT_LOCK_MS;
        // 키보드: 방금 누른 보기에 포커스가 남으면 Enter 한 번(길게 누르면 연속)에 다음 문제가 읽지도 않고 답 처리됨
        document.activeElement?.blur();

        // 영어 단어가 문제일 때만 읽어준다 (KO_EN 은 추후). 화면이 가려져 있으면 읽지 않음
        if (state.direction === 'EN_KO' && !document.hidden) tts.speak(q.prompt);

        startQuestionBar();
        updateQuestionSec();
    }

    function answer(selectedOptionNo) {
        if (state.status !== 'RUNNING') return;

        // 시간이 이미 지난 뒤의 클릭은 무시 (시간 초과로 먼저 처리)
        const index = state.index;
        settleTimeouts();
        if (state.status !== 'RUNNING' || state.index !== index) return;

        const q = state.questions[index];
        state.answers.push({ questionNo: q.no, selectedOptionNo });

        if (index + 1 < state.questions.length) {
            state.questionEndAt = performance.now() + state.timeLimitMs;
            showQuestion(index + 1);
        } else {
            finish();
        }
    }

    /** 마지막 문제까지 끝남 → 제출 */
    function finish() {
        el.progressFill.style.width = '100%';
        tts.stop();
        submit();
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
        hideOverlay();
        state.status = 'RUNNING';
        state.questionEndAt = performance.now() + state.timeLimitMs;
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
            el.timerSec.textContent = data.timeLimitSec;
            el.levelCaption.textContent = `${data.level} · ${data.levelGroup}`;

            state.status = 'READY';
            showReady(data);
            startClock(data.remainingSec);
        } catch (e) {
            state.status = 'ABORTED';
            if (isSessionGone(e)) {
                showOverlay('시험이 종료되었습니다', MSG_SESSION_GONE, '확인', goMain);
                return;
            }
            // 서버에 남은 시험이 동시 진행 개수를 차지하지 않게 만료 요청.
            // 연결이 끊겨 못 보내면 서버가 '문제를 받아 가지 않은 시험'으로 몇 분 뒤 정리한다
            navigator.sendBeacon(api('/expire'));
            showOverlay('시험을 시작할 수 없습니다', isNetworkError(e) ? MSG_OFFLINE_LOAD : e.message, '확인', goMain);
        }
    }

    async function submit() {
        state.status = 'SUBMITTING';
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
                if (isSessionGone(e)) {
                    showOverlay('시험이 종료되었습니다', MSG_SESSION_GONE, '확인', goMain);
                    return;
                }
                const title = e.code === 'TEST_005' ? '시험이 무효 처리되었습니다' : '제출할 수 없습니다';
                showOverlay(title, e.message, '확인', goMain);
                return;
            }
            // 네트워크 오류·서버 오류 : 다시 시도 (답안은 그대로 유지, 마감 확인은 계속)
            state.status = 'RUNNING';
            showOverlay('제출하지 못했습니다', isNetworkError(e) ? MSG_OFFLINE_SUBMIT : e.message, '다시 제출', () => {
                if (state.status !== 'RUNNING') return;
                submit();
            });
        }
    }

    const MSG_SESSION_GONE = '서버 점검 등으로 시험 정보가 사라졌습니다.\n불편을 드려 죄송합니다. 처음부터 다시 시작해 주세요.';
    const MSG_OFFLINE_LOAD = '서버와 연결할 수 없습니다.\n인터넷 연결을 확인한 뒤 다시 시작해 주세요.';
    const MSG_OFFLINE_SUBMIT = '서버와 연결할 수 없습니다.\n인터넷 연결을 확인한 뒤 다시 제출해 주세요.';

    /**
     * 서버에 시험이 없음 (BUSINESS_001).
     * 시험은 서버 메모리에 있어 앱 재시작·배포 때 사라진다. 만료되어 정리된 경우도 같은 응답이라 구분하지 않는다
     */
    function isSessionGone(e) {
        return e.code === 'BUSINESS_001';
    }

    /** fetch 자체 실패 (서버 꺼짐·인터넷 끊김). 브라우저 영어 문구(Failed to fetch) 대신 한국어 안내를 쓴다 */
    function isNetworkError(e) {
        return typeof e.status !== 'number';
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

    // ---------- 화면 복귀 : 그동안 지나간 시간을 바로 반영 ----------
    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') tickClock();
    });

    // ---------- 페이지 이탈 (새로고침·창 닫기·뒤로가기) : 답안이 사라지므로 만료 ----------
    function abort() {
        if (state.status !== 'RUNNING' && state.status !== 'READY' && state.status !== 'PAUSED') return;
        state.status = 'ABORTED';
        stopClock();
        tts.stop();
        navigator.sendBeacon(api('/expire'));
        showOverlay('시험이 종료되었습니다', '시험 중에 이전 화면으로 이동해\n시험이 종료되었습니다.', '확인', goMain);
    }

    window.addEventListener('pagehide', abort);

    // 새로고침·창 닫기 : 풀이 중이면 브라우저 기본 확인창 (문구는 브라우저가 정함, 모바일은 무시할 수 있음)
    window.addEventListener('beforeunload', (e) => {
        if (state.status !== 'RUNNING' && state.status !== 'PAUSED') return;
        e.preventDefault();
        e.returnValue = '';
    });

    /** 풀이 중 뒤로가기 : 문제 시간을 멈추고 그만둘지 묻는다 (전체 마감 시간은 계속 흐름) */
    function confirmQuit() {
        settleTimeouts();
        if (state.status !== 'RUNNING') return;
        state.status = 'PAUSED';
        state.pausedRemainMs = Math.max(0, state.questionEndAt - performance.now());
        tts.stop();
        const bar = el.timerBar;                    // 막대를 지금 위치에 멈춤
        bar.style.transition = 'none';
        bar.style.transform = `scaleX(${state.pausedRemainMs / state.timeLimitMs})`;

        showOverlay('시험을 그만둘까요?', '지금까지 푼 답은 사라집니다.', '계속 풀기', resume);
        el.overlayBack.textContent = '그만두기';
        el.overlayBack.onclick = quit;
        el.overlayBack.hidden = false;
    }

    function resume() {
        if (state.status !== 'PAUSED') return;
        hideOverlay();
        el.overlayBack.textContent = '이전';
        state.status = 'RUNNING';
        state.questionEndAt = performance.now() + state.pausedRemainMs;
        state.lockedUntil = performance.now() + INPUT_LOCK_MS;
        startQuestionBar();
        updateQuestionSec();
    }

    /** [그만두기] : 본인이 고른 것이므로 종료 안내 없이 만료하고 메인으로 */
    function quit() {
        if (state.status !== 'PAUSED') return;
        state.status = 'ABORTED';
        stopClock();
        navigator.sendBeacon(api('/expire'));
        goMain();
    }

    // 뒤로가기 : 시작 전이면 조용히 취소, 풀이 중이면 그만둘지 확인, 제출 실패 안내 등 그 밖에는 만료 후 안내
    history.pushState(null, '', location.href);
    window.addEventListener('popstate', () => {
        if (state.status === 'READY') {
            cancelReady();
        } else if (state.status === 'PAUSED' || (state.status === 'RUNNING' && el.overlay.hidden)) {
            history.pushState(null, '', location.href);   // 이 화면에 머문다 (확인창이 떠 있으면 그대로)
            if (state.status === 'RUNNING') confirmQuit();
        } else if (state.status === 'RUNNING') {
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
