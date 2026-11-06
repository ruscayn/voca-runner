/*
 * 시험 화면
 * - 문제는 시작 시 한 번에 받아오고, 답은 모아 두었다가 마지막에 한 번에 제출
 * - [시작] 버튼을 누르면 1번 문제부터 진행 (iOS 는 터치가 있어야 소리가 나므로)
 * - 시작 안내의 [소리 확인]은 영어를 30초 동안 읽어 그동안 휴대폰 볼륨을 맞추게 한다 (U51)
 * - 문제 단어가 뜨면 TTS 로 읽어준다 (브라우저 내장 Web Speech API)
 * - 보기를 누르거나 제한 시간이 지나면 바로 다음 문제 (시간 초과 = 답 없음)
 * - 문제당 남은 시간은 막대(흐름) + 숫자(정확한 초)로 표시. 2초 이하면 빨간색
 * - 시간은 화면을 보고 있든 아니든 실제 시간 기준으로 흐른다
 *   앱 전환·화면 꺼짐 동안 브라우저가 타이머를 늦추거나 멈춰도, 돌아오면 그동안 지나간 문제를
 *   모두 시간 초과로 처리하고 그 시점의 문제를 남은 초부터 이어서 보여준다
 * - 새로고침·뒤로가기·창 닫기는 답안이 사라지므로 시험을 만료한다
 *   풀이 중 뒤로가기는 "그만둘까요?" 확인(그동안 문제 시간 멈춤), 새로고침·창 닫기는 브라우저 기본 확인창
 * - 마감(분 단위)까지 남은 전체 시간을 표시하고, 지나면 시험은 무효
 * - 시험은 준비 화면에서 [시작]을 누를 때 서버에 만들어지고 마감도 그때부터 센다 (P13).
 *   시작 전 안내 · 소리 확인 시간은 허용 시간에 들어가지 않는다
 */
(() => {
    'use strict';

    const INPUT_LOCK_MS = 350;   // 직전 탭(실수로 두 번 누름 150~300ms)이 다음 문제로 넘어가 눌리는 것 방지
    const CLOCK_TICK_MS = 100;   // 시간 확인 주기
    const URGENT_SEC = 60;       // 남은 전체 시간이 이 값 이하이면 빨간색
    const STAGE_SIZE = 100;      // 진행 색 한 단계 = 100문제 (exam.css --stage, 5단계)
    const RUN_MS = 1000;         // 사람이 한 번 뛰는 시간 = 달리기 한 바퀴 (exam.css rn-thigh 등과 같게)
    const PROMPT_MAX_PX = 52;    // 문제 단어 글자 크기 (긴 단어는 폭에 맞춰 줄임)
    const PROMPT_MIN_PX = 28;
    const PROMPT_MAX_LINES = 2;  // 띄어쓰기 있는 구는 2줄까지 허용
    const PROMPT_SUB_MAX_PX = 24;  // 아래 줄(함께 표시할 형태) 글자 크기 (exam.css .prompt-sub)
    const PROMPT_SUB_GAP_PX = 8;   // 아래 줄은 단어보다 이만큼 작게. 단어가 32px 아래로 줄면 아래 줄도 같이 줄어 28px일 때 20px

    const app = document.getElementById('exam-app');
    const info = app.dataset;          // 준비 화면 값 (레벨 · 문항 수 · 시간). 시험 아이디는 [시작] 뒤에 생긴다
    let examId = null;
    const api = (path) => `/api/exams/${examId}${path}`;
    const $ = (id) => document.getElementById(id);

    const el = {
        currentNo: $('current-no'),
        total: $('total-count'),
        runLane: $('run-lane'),
        runnerFig: $('runner-fig'),
        remain: $('remain'),
        remainTime: $('remain-time'),
        prompt: $('prompt'),
        promptSub: $('prompt-sub'),
        timerBar: $('timer-bar'),
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
        timePerQuestionMs: 5000,
        subject: 'ENGLISH',
        questionCount: 0,
        deadlineAt: 0,        // 전체 마감 시각 (performance.now() 기준, 기기 시계와 무관)
        questionEndAt: 0,     // 현재 문제 마감 시각 (performance.now() 기준)
        clockId: null,
        index: -1,
        selectedOptions: [],  // { questionNo, selectedOptionNo | null }
        lockedUntil: 0,
        pausedRemainMs: 0,    // 그만둘지 묻는 동안 멈춘 현재 문제의 남은 시간
        submitFailed: false,  // 제출이 네트워크·서버 오류로 실패해 [다시 제출]을 기다리는 중 (답안은 아직 서버에 없음)
        runUntil: 0,          // 사람이 이 시각까지 뛴다 (performance.now() 기준)
        status: 'READY',      // READY → STARTING → RUNNING (⇄ PAUSED) → SUBMITTING → DONE | ABORTED
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
            synth.addEventListener?.('voiceschanged', () => {
                pickVoice();
                syncSoundNotice();   // 목록이 늦게 채워지는 브라우저 : 시작 안내가 떠 있으면 다시 맞춤
            });
        }

        return {
            /** onEnd : 다 읽었거나 멈췄을 때 (다음 speak·stop 이 끊어도 불림) */
            speak(text, onEnd = null) {
                if (!supported || !text) return;
                synth.cancel();
                const u = new SpeechSynthesisUtterance(text);
                u.lang = 'en-US';
                if (voice) u.voice = voice;
                u.rate = 0.9;
                if (onEnd) u.onend = u.onerror = onEnd;
                synth.speak(u);
            },
            stop() {
                if (supported) synth.cancel();
            },
            /** 영어 발음을 낼 수 있는지 (음성 기능 + 영어 목소리). 일부 안드로이드는 목소리 목록이 늦게 채워진다 */
            available() {
                return supported && voice !== null;
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

    /**
     * 시작 안내의 "단어마다 발음이 함께 나옵니다"와 [소리 확인]은 발음이 되는 브라우저에서만 보인다 (U22).
     * 음성 기능이 없는 앱 안 브라우저 등에서 보이면 고장으로 오해할 수 있다. 기기 무음은 알 수 없어 따로 안내하지 않는다
     */
    function syncSoundNotice() {
        const notice = $('ready-sound');
        if (notice) notice.hidden = !tts.available();
    }

    /**
     * 시작 안내의 [소리 확인] (U51) : 영어를 30초 동안 읽어 그동안 휴대폰 볼륨을 맞추게 한다.
     * 휴대폰 볼륨 버튼은 소리가 나는 동안에만 미디어 음량을 바꾸는데, 단어 발음은 1초 안팎이라 그 사이에 맞추기 어렵다.
     * 손이 느려도 버튼을 찾아 누를 수 있게 30초. 긴 글 하나는 일부 브라우저(PC 크롬 등)가 15초쯤에서 끊으므로
     * 짧은 문장을 차례로(끝나면 처음부터) 이어 읽는다. 누름(터치)이 iOS 소리 허락도 겸한다.
     * 읽는 중에 다시 누르면 멈추고 원래 버튼으로 돌아간다. [시작]·[이전]을 눌러도 멈춘다
     */
    const SOUND_CHECK_LINES = [
        'This is a sound check.',
        'Press the volume buttons on the side of your phone now.',
        'Turn the sound up or down until this voice is easy to hear.',
        'Every word in the test will be read aloud at this volume.',
        'When the volume feels right, press the start button.',
    ];
    const SOUND_CHECK_MS = 30000;       // 이 시간이 지나면 읽던 문장까지만 읽고 멈춤
    const SOUND_CHECK_GAP_MS = 600;     // 문장 사이 쉼
    const SOUND_CHECK_LINE_MAX_MS = 10000;   // 끝 알림(onend)이 안 오는 브라우저도 이 시간이 지나면 다음 문장으로
    const soundCheck = (() => {
        const btn = $('sound-check');
        const idleText = btn?.textContent;
        let playId = 0;      // 누를 때·멈출 때마다 바뀜 → 앞 재생의 늦은 끝 알림·타이머는 무시
        let timer = null;

        function stop() {
            if (!btn) return;
            playId++;
            clearTimeout(timer);
            tts.stop();
            btn.textContent = idleText;
            btn.classList.remove('is-playing');
        }

        function playLine(id, n, endAt) {
            if (id !== playId) return;
            if (performance.now() >= endAt) {
                stop();
                return;
            }
            let done = false;
            const next = () => {      // 다 읽었거나(onend) 끝 알림을 기다리다 시간이 지남 — 둘 중 먼저 온 것만
                if (done || id !== playId) return;
                done = true;
                clearTimeout(timer);
                timer = setTimeout(() => playLine(id, n + 1, endAt), SOUND_CHECK_GAP_MS);
            };
            clearTimeout(timer);
            timer = setTimeout(next, SOUND_CHECK_LINE_MAX_MS);
            tts.speak(SOUND_CHECK_LINES[n % SOUND_CHECK_LINES.length], next);
        }

        btn?.addEventListener('click', () => {
            if (btn.classList.contains('is-playing')) {
                stop();
                return;
            }
            const id = ++playId;
            btn.textContent = '소리가 나는 중… 지금 볼륨 버튼을 눌러 보세요';
            btn.classList.add('is-playing');
            playLine(id, 0, performance.now() + SOUND_CHECK_MS);   // 첫 문장은 누름 안에서 읽음 (iOS)
        });

        return { stop };
    })();

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
        el.sheetEyebrow.textContent = `${data.level} · ${data.groupName}`;
        el.sheetEyebrow.hidden = false;
        $('ready-count').textContent = data.questionCount.toLocaleString('ko-KR');
        $('ready-sec').textContent = data.timePerQuestionSec;
        $('ready-min').textContent = data.allowedMinutes;
        $('ready-min-text').textContent = data.allowedMinutes;
        el.readyInfo.hidden = false;
        syncSoundNotice();
        el.overlayBack.onclick = cancelReady;
        el.overlayBack.hidden = false;
    }

    /** 시작 전 [이전] : 아직 시험을 만들지 않았으므로 그냥 메인으로 */
    function cancelReady() {
        if (state.status !== 'READY') return;
        state.status = 'ABORTED';
        soundCheck.stop();
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
        bar.style.transform = `scaleX(${remainMs / state.timePerQuestionMs})`;
        void bar.offsetWidth;                          // 애니메이션 재시작
        bar.style.transition = `transform ${remainMs}ms linear`;
        bar.style.transform = 'scaleX(0)';
    }

    function updateQuestionSec() {
        const sec = Math.max(0, Math.ceil((state.questionEndAt - performance.now()) / 1000));
        el.timerSec.textContent = sec;
    }

    /**
     * 실제 시간 기준으로 지나간 문제를 모두 시간 초과 처리.
     * 다음 문제는 이전 문제의 마감 시각부터 시작한 것으로 본다 (화면을 벗어나 있어도 기다려 주지 않음)
     */
    function settleTimeouts() {
        if (state.status !== 'RUNNING' || state.selectedOptions.length >= state.questions.length) return;
        const now = performance.now();
        if (now < state.questionEndAt) return;

        while (now >= state.questionEndAt) {
            const q = state.questions[state.index];
            state.selectedOptions.push({ questionNo: q.no, selectedOptionNo: null });
            if (state.index + 1 >= state.questions.length) {
                finish();
                return;
            }
            state.index += 1;
            state.questionEndAt += state.timePerQuestionMs;
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
        if (state.status !== 'RUNNING' && state.status !== 'PAUSED') return;
        state.status = 'ABORTED';
        stopClock();
        tts.stop();
        navigator.sendBeacon(api('/abort'));
        showOverlay('시험이 무효 처리되었습니다', '제한 시간 안에 제출하지 않아\n시험이 무효 처리되었습니다.', '확인', goMain);
    }

    // ---------- 문제 단어 크기 ----------
    /**
     * 단어가 폭에 들어가고(단어 중간 줄바꿈 없음) 구는 2줄 이하, 단어 영역 높이 안에 들어갈 때까지 글자를 줄인다.
     * 최소 크기에서도 넘치면(아주 좁은 화면) 그때만 단어 중간 줄바꿈을 허용한다.
     * 아래 줄도 단어와 함께 줄인다. 아래 줄 24px 고정이면 320×568에서 단어를 최소로 줄여도 영역이 넘친다 (U43)
     */
    function fitPrompt() {
        const p = el.prompt;
        const area = p.parentElement;      // 단어 영역 (가로 화면 등 낮은 화면에서는 높이가 작다)
        p.classList.remove('prompt-break');
        for (let px = PROMPT_MAX_PX; px >= PROMPT_MIN_PX; px -= 2) {
            p.style.fontSize = `${px}px`;
            el.promptSub.style.fontSize = `${Math.min(PROMPT_SUB_MAX_PX, px - PROMPT_SUB_GAP_PX)}px`;
            const fitsWidth = p.scrollWidth <= p.clientWidth;
            const fitsHeight = area.scrollHeight <= area.clientHeight;
            const lines = Math.round(p.offsetHeight / (px * 1.1));   // line-height 1.1
            if (fitsWidth && fitsHeight && lines <= PROMPT_MAX_LINES) return;
        }
        p.classList.add('prompt-break');
    }

    // 화면 회전·글꼴 로드가 끝나면 폭이 달라지므로 다시 맞춘다.
    // 문제 단어 글꼴은 단어를 넣어야 받기 시작하므로 미리 받는다. 안 그러면 첫 문제를 대신 글꼴로 재서 작게 맞춤 (U53)
    window.addEventListener('resize', () => { if (state.index >= 0) fitPrompt(); });
    loadPromptFont().catch(() => {}).then(() => { if (state.index >= 0) fitPrompt(); });

    function loadPromptFont() {
        const style = getComputedStyle(el.prompt);
        return document.fonts?.load(`${style.fontWeight} ${style.fontSize} ${style.fontFamily}`) ?? Promise.resolve();
    }

    // ---------- 진행 ----------
    /** state.index 문제를 표시. 마감 시각(state.questionEndAt)은 호출 전에 정해 둔다 */
    function showQuestion(index) {
        state.index = index;
        const q = state.questions[index];
        el.currentNo.textContent = q.no;
        setProgress(index, q.no);
        if (index > 0) runStep();
        el.prompt.textContent = q.prompt;
        // 함께 표시할 형태 (예: for one's sake). 발음은 표제어(q.prompt)만 읽는다
        el.promptSub.textContent = q.promptSub || '';
        el.promptSub.hidden = !q.promptSub;
        q.options.forEach((opt, i) => {
            el.options[i].querySelector('.option-text').textContent = opt.text;
            el.options[i].dataset.no = opt.no;
        });
        // 단어 크기는 아래 줄·보기 글자를 모두 넣은 뒤 맞춘다.
        // 아래 줄이 붙거나 보기가 2줄로 늘면 단어 영역이 줄어들기 때문 (U40·U43)
        fitPrompt();
        state.lockedUntil = performance.now() + INPUT_LOCK_MS;
        // 키보드: 방금 누른 보기에 포커스가 남으면 Enter 한 번(길게 누르면 연속)에 다음 문제가 읽지도 않고 답 처리됨
        document.activeElement?.blur();

        // 영어 단어가 문제일 때만 읽어준다. 화면이 가려져 있으면 읽지 않음
        if (state.subject === 'ENGLISH' && !document.hidden) tts.speak(q.prompt);

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
        state.selectedOptions.push({ questionNo: q.no, selectedOptionNo });

        if (index + 1 < state.questions.length) {
            state.questionEndAt = performance.now() + state.timePerQuestionMs;
            showQuestion(index + 1);
        } else {
            finish();
        }
    }

    /**
     * 진행 길 : 사람 위치 = 푼 비율, 색 단계 = 지금 문제 번호 기준 100문제마다 (P5).
     * 같은 번호는 문제 수와 상관없이 같은 색 (300문제 = 3색, 500문제 = 5색)
     */
    function setProgress(answeredCount, questionNo) {
        el.runLane.style.setProperty('--p', answeredCount / state.questionCount);
        app.dataset.stage = Math.min(5, Math.floor((questionNo - 1) / STAGE_SIZE) + 1);
    }

    /**
     * 답을 고르면(다음 문제로 넘어가면) 사람이 한 바퀴 뛴다.
     * 뛰는 중에 또 넘어가면 이어서 뛰고, 한 바퀴가 끝나는 순간에 멈춰 멈춘 자세와 끊기지 않게 한다
     */
    function runStep() {
        state.runUntil = performance.now() + RUN_MS;
        el.runnerFig.classList.add('is-running');
    }

    el.runnerFig.addEventListener('animationiteration', (e) => {
        if (e.animationName !== 'rn-thigh' || !e.target.closest('.rn-near')) return;   // 앞다리 한 바퀴마다 한 번
        if (performance.now() >= state.runUntil - 50) el.runnerFig.classList.remove('is-running');
    });

    /** 마지막 문제까지 끝남 → 제출 */
    function finish() {
        setProgress(state.questionCount, state.questionCount);
        runStep();
        tts.stop();
        submit();
    }

    el.options.forEach((btn) => {
        btn.addEventListener('click', () => {
            if (performance.now() < state.lockedUntil) return;
            answer(Number(btn.dataset.no));
        });
    });

    /** [시작] : 이때 서버에 시험을 만들고(마감도 이때부터) 문제를 받아 1번 문제부터 진행 */
    async function begin() {
        if (state.status !== 'READY') return;
        soundCheck.stop();            // [소리 확인]으로 읽던 소리가 첫 문제 발음과 겹치지 않게
        tts.unlock();                 // 터치 이벤트 안에서 호출 (iOS)
        state.status = 'STARTING';
        showOverlay('문제 불러오는 중...');
        try {
            examId = await startExam();
            run(await loadQuestions());
        } catch (e) {
            failToStart(e);
        }
    }

    function run(data) {
        if (state.status !== 'STARTING') return;     // 기다리는 동안 화면을 떠남
        state.questions = data.questions;
        state.timePerQuestionMs = data.timePerQuestionSec * 1000;
        state.subject = data.subject;
        state.questionCount = data.questionCount;
        el.total.textContent = data.questionCount;
        el.timerSec.textContent = data.timePerQuestionSec;
        el.levelCaption.textContent = `${data.level} · ${data.groupName}`;
        hideOverlay();
        state.status = 'RUNNING';
        state.questionEndAt = performance.now() + state.timePerQuestionMs;
        showQuestion(0);
        startClock(data.remainingSec);    // 첫 문제를 정한 뒤에 켠다 (먼저 켜면 문제 없이 시간 초과 처리가 돈다)
    }

    function failToStart(e) {
        if (state.status !== 'STARTING') return;
        state.status = 'ABORTED';
        // 만든 시험이 동시 진행 개수를 차지하지 않게 중단 요청.
        // 연결이 끊겨 못 보내면 서버가 '문제를 받아 가지 않은 시험'으로 몇 분 뒤 정리한다
        if (examId) navigator.sendBeacon(api('/abort'));
        if (isExamGone(e)) {
            showOverlay('시험이 종료되었습니다', MSG_EXAM_GONE, '확인', goMain);
            return;
        }
        showOverlay('시험을 시작할 수 없습니다', isNetworkError(e) ? MSG_OFFLINE_LOAD : e.message, '확인', goMain);
    }

    // ---------- 서버 통신 ----------
    /** 시험을 만든다. 응시자가 많아 거절되면 그 안내(오류 문구)를 그대로 보여 준다 */
    async function startExam() {
        const res = await fetch('/api/exams', { method: 'POST', body: new URLSearchParams({ level: info.level }) });
        if (!res.ok) throw await errorOf(res);
        return (await res.json()).examId;
    }

    async function loadQuestions() {
        const res = await fetch(api('/questions'));
        if (!res.ok) throw await errorOf(res);
        return res.json();
    }

    async function submit() {
        state.status = 'SUBMITTING';
        state.submitFailed = false;
        showOverlay('채점 중...');
        try {
            const res = await fetch(api('/submit'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ selectedOptions: state.selectedOptions }),
            });
            if (!res.ok) throw await errorOf(res);
            goResult();
        } catch (e) {
            // 이미 채점된 시험 : 앞 제출이 서버에 저장된 뒤 응답만 끊겨(와이파이↔LTE 전환 등) [다시 제출]을 누른 경우.
            // 거절 안내 대신 저장된 결과를 보여준다 (U48)
            if (e.code === 'EXAM_002' && await hasResult()) {
                goResult();
                return;
            }
            if (isFinalError(e)) {
                // 서버가 거절 (마감 초과·이미 종료·없는 시험 등) : 다시 제출해도 소용없으므로 메인으로
                state.status = 'ABORTED';
                stopClock();
                if (isExamGone(e)) {
                    showOverlay('시험이 종료되었습니다', MSG_EXAM_GONE, '확인', goMain);
                    return;
                }
                const title = e.code === 'EXAM_005' ? '시험이 무효 처리되었습니다' : '제출할 수 없습니다';
                showOverlay(title, e.message, '확인', goMain);
                return;
            }
            // 네트워크 오류·서버 오류 : 다시 시도 (답안은 그대로 유지, 마감 확인은 계속)
            state.status = 'RUNNING';
            state.submitFailed = true;
            showOverlay('제출하지 못했습니다', isNetworkError(e) ? MSG_OFFLINE_SUBMIT : e.message, '다시 제출', retrySubmit);
        }
    }

    /** 결과 화면 (시험 화면은 기록에서 제거) */
    function goResult() {
        state.status = 'DONE';
        stopClock();
        location.replace(`/exam/${examId}/result`);
    }

    /** 채점이 끝나 결과가 저장돼 있는지 */
    async function hasResult() {
        try {
            return (await fetch(api('/result'))).ok;
        } catch {
            return false;
        }
    }

    function retrySubmit() {
        if (state.status !== 'RUNNING' || !state.submitFailed) return;
        el.overlayBack.textContent = '이전';
        submit();
    }

    /**
     * 제출 실패 안내 중 뒤로가기 : 푼 답이 아직 서버에 없으므로 바로 끝내지 않고 묻는다 (U12).
     * 실수로 [뒤로]를 눌러 다 푼 시험을 잃지 않게
     */
    function confirmDiscardAnswers() {
        showOverlay('아직 제출되지 않았습니다', '그만두면 푼 답이 모두 사라집니다.', '다시 제출', retrySubmit);
        el.overlayBack.textContent = '그만두기';
        el.overlayBack.onclick = discardAnswers;
        el.overlayBack.hidden = false;
    }

    /** 제출 실패 뒤 [그만두기] : 본인이 고른 것이므로 종료 안내 없이 만료하고 메인으로 */
    function discardAnswers() {
        if (state.status !== 'RUNNING' || !state.submitFailed) return;
        state.status = 'ABORTED';
        stopClock();
        navigator.sendBeacon(api('/abort'));
        goMain();
    }

    const MSG_EXAM_GONE = '서버 점검 등으로 시험 정보가 사라졌습니다.\n불편을 드려 죄송합니다. 처음부터 다시 시작해 주세요.';
    const MSG_OFFLINE_LOAD = '서버와 연결할 수 없습니다.\n인터넷 연결을 확인한 뒤 다시 시작해 주세요.';
    const MSG_OFFLINE_SUBMIT = '서버와 연결할 수 없습니다.\n인터넷 연결을 확인한 뒤 다시 제출해 주세요.';

    /**
     * 서버에 시험이 없음 (BUSINESS_001).
     * 시험은 DB 에 있어 앱 재시작·배포 때도 남는다 (U21). 마감·만료로 정리됐거나 단어 데이터를 다시 넣은 경우 등이며 구분하지 않는다
     */
    function isExamGone(e) {
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
        if (state.status !== 'RUNNING' && state.status !== 'STARTING' && state.status !== 'PAUSED') return;
        state.status = 'ABORTED';
        stopClock();
        tts.stop();
        if (examId) navigator.sendBeacon(api('/abort'));
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
        bar.style.transform = `scaleX(${state.pausedRemainMs / state.timePerQuestionMs})`;

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
        navigator.sendBeacon(api('/abort'));
        goMain();
    }

    // 뒤로가기 : 시작 전이면 조용히 메인으로, 풀이 중·제출 실패면 그만둘지 확인, 시험 만드는 중·채점 중이면 머묾,
    // 그 밖에는 중단 후 안내
    history.pushState(null, '', location.href);
    window.addEventListener('popstate', () => {
        if (state.status === 'READY') {
            cancelReady();
        } else if (state.status === 'STARTING' || state.status === 'SUBMITTING') {
            history.pushState(null, '', location.href);   // 응답이 올 때까지 이 화면에 머문다
        } else if (state.status === 'RUNNING' && state.submitFailed) {
            history.pushState(null, '', location.href);
            confirmDiscardAnswers();
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

    showReady({
        level: info.level,
        groupName: info.groupName,
        questionCount: Number(info.questionCount),
        timePerQuestionSec: info.timePerQuestionSec,
        allowedMinutes: info.allowedMinutes,
    });
})();
