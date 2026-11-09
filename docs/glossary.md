# 용어 사전

VocaRunner의 코드 · 문서 · 화면에서 같은 개념을 같은 이름으로 쓰기 위한 단어 · 도메인 · 용어 목록입니다.

## 읽는 법

- **단어**(1장): 의미를 가진 가장 작은 말. 한국어 단어 하나에 코드 단어 하나를 대응
  - 같은 한국어 단어를 두 코드 단어로, 같은 코드 단어를 두 의미로 사용하지 않음
  - 활용형은 같은 단어로 취급 (선택 → 선택한, select → selected, clean up → cleanup)
  - 단어 하나가 그대로 이름인 경우(시험 `Exam`, 마감 `deadline` 등)는 설명과 외부 이름을 단어 표에 기재
- **도메인**(1-6): 값을 담는 이름은 **마지막 단어가 값의 형식**(자바 타입 · DB 타입 · 범위)을 결정
  - 예: `…Count`는 0 이상 정수, `…At`은 시각
- **용어**(2장): 단어를 **둘 이상 조합한** 이름. 실제로 사용하는 이름만 기재
  - 한국어 용어는 한국어 단어를 띄어 쓰고, 코드 이름은 코드 단어를 붙여 표기 (camelCase · PascalCase)
  - 단어 순서는 각 언어의 자연스러운 순서를 따름 (문항당 시간 = `timePerQuestion…`, 최대 문항 수 = `maxQuestionCount`)
  - **단어 표에 없는 말은 용어에 사용하지 않음.** 새 말이 필요하면 단어부터 등록
- **자기 범위 안에서는 앞 단어를 생략**: 시험(`Exam`) 안의 `id` = 시험 아이디, 보기(`Option`) 안의 `text` = 보기 글
- **단위**: 단위가 있는 숫자 값은 끝에 단위 단어를 추가 (`timePerQuestionSec`)
  - 마지막 단어가 '시간'(`time`)이면 단위 단어가 '시간'을 대신함 (남은 시간 → `remainingSec`, 허용 시간 → `allowedMinutes`)
  - 기간 값(`Duration`)에는 추가하지 않음
- **외부 이름**: DB 테이블 · 칼럼, 주소, 오류 코드 값, 설정 이름, 쿠키 · 세션 속성, JSON 필드, 템플릿 모델 이름, 화면 파일 이름
  - 외부 이름도 사전 이름을 따름
  - 예외: DB 테이블 · 칼럼 이름은 운영 DB와 연결되어 있어 단어 규칙을 적용하지 않고 기존 이름을 유지
    - 시험의 DB 이름은 초기 이름 `test`를 사용 (테이블 `test_session`, 칼럼 `test_session_id`)
  - 코드 이름과 외부 이름이 다른 용어는 2-9에 정리
- **화면 문구**: 용어와 다르면 설명 열에 `화면:`으로 표기하고 4장에 정리
- **범위**: 도메인 용어만 다룸
  - 구현 내부의 일반 프로그래밍 용어(index · random · shuffle · key · format · apply · supplier 등)와 기술 이름(Jpa · Config · Filter · Handler 등)은 제외
  - 아키텍처(계층 · 포트)에 따른 이름 형식은 6-2

## 1. 단어

### 1-1. 시험

| 단어 | 코드 | 설명 | 외부 이름 | 사용하지 않는 말 |
|---|---|---|---|---|
| 시험 | `exam` | 응시자 한 명이 레벨 하나를 선택해 응시하는 시험 1회. 시험 시간 · 문항 목록 · 선택한 보기 · 채점 결과를 포함. DB에 1행으로 저장되어 서버 재시작 후에도 유지 | 테이블 `test_session` | 테스트 · 세션 (7장) |
| 문항 | `question` | 시험을 구성하는 단위 하나. 보기 4개(정답 1 + 오답 3)와 선택한 보기로 구성 | — | 문제 (화면 · 문장에서는 같은 의미로 사용) |
| 보기 | `option` | 문항마다 제시하는 선택 항목 4개. 순서는 무작위 | JSON `options[]` | 선택지 |
| 제시어 | `prompt` | 문항에서 제시하는 말. 영어 과목(`ENGLISH`)에서는 영어 표제어이며 발음 재생 대상도 제시어. **제시어 단어 = 정답 보기의 단어** (별도로 저장하지 않고 정답 보기 번호로 조회) | JSON `prompt` | 문제 글 |
| 보조 | `sub` | 앞 말의 아래 줄에 덧붙여 표시하는 형태 | — | 아래 줄 · 함께 표시할 형태 |
| 글 | `text` | 화면에 표시되는 글자 내용 | — | |
| 정답 | `answer` | 문항의 올바른 보기 (해당 출제 목록 단어) | — | |
| 맞힘 | `correct` | 선택한 보기가 정답임 | — | |
| 채점 | `grade` · `grading` | 선택한 보기 번호와 정답 보기 번호를 비교해 맞힌 수 · 점수를 산출 (시험 제출 과정에서 수행) | — | |
| 선택 | `select` | 응시자가 보기 하나를 지정함 | — | 고름 · 답 · 내 답 (화면) |
| 틀림 | `wrong` | 맞히지 못함 = 오답 + 시간 초과 | — | |
| 오답 | `incorrect` | 정답이 아님 | — | |
| 시간 초과 | `timeout` | 정해진 시간 안에 끝나지 않음. 문항에서는 문항당 시간 안에 선택하지 못한 경우이며 틀린 것으로 집계 | — | |
| 점수 | `score` | 맞힌 수 × 100 ÷ 문항 수, **소수점 버림** (0 ~ 100). 하나라도 틀리면 100점이 되지 않음 | 칼럼 · JSON `score` | |
| 결과 | `result` | 완료된 작업의 결과 | — | |
| 항목 | `item` | 목록을 구성하는 원소 하나 | — | |
| 과목 | `subject` | 시험 과목 = 학습 대상 언어. 현재 값은 `ENGLISH`(영어 단어를 보고 한국어 뜻 선택) 하나이며, 다른 과목을 추가할 수 있도록 별도 개념으로 분리 | 칼럼 `direction`(DB 이름 유지), JSON `subject` | 방향 |
| 생성 · 생성기 | `generate` · `generator` | 대상을 만들어 냄 / 그 역할을 담당하는 객체 | — | |
| 응시자 | `examinee` | 시험을 시작한 주체를 구분하는 값 = 기기 아이디 + 사용자 IP | 칼럼 `device_id` · `client_ip` | |

### 1-2. 시간 · 진행

| 단어 | 코드 | 설명 | 외부 이름 | 사용하지 않는 말 |
|---|---|---|---|---|
| 시간 | `time` | 소요되는 시간 | — | |
| 허용 | `allowed` | 사용하도록 부여한 범위 | — | 제한 (코드 이름에서) |
| 여유 | `grace` | 문항 수 × 문항당 시간에 더하는 시간 (기본 1분). 시간 측정이 첫 문항부터 시작되므로 통신 지연과 중단 확인창 대기분만 반영. 허용 시간에 포함 | 설정 `voca-runner.exam.grace` | |
| 마감 | `deadline` | 시작 시각 + 허용 시간. 마감 이후에는 제출 불가 (만료) | 칼럼 `deadline` | |
| 남음 | `remaining` | 아직 남아 있음 | — | |
| 시각 | `at` | 특정 시점. 코드에서는 이름 끝에 붙임 (`startedAt`) | — | |
| 날짜 | `date` | 하루 단위 (시각 없음) | — | |
| 초 · 분 | `sec` · `minutes` | 시간 단위 | — | |
| 보관 | `retention` | 삭제하지 않고 유지 | — | |
| 주기 | `interval` | 반복 실행 간격 | — | |
| 상태 | `status` | 시험의 진행 단계 | 칼럼 `status` | `state` |
| 진행 중 | `inProgress` | 상태가 진행 중이고 **마감 전**. 상태와 마감을 함께 확인 (`isInProgress(now)`) | 상태 값 `IN_PROGRESS` | |
| 완료 | `completed` | 제출 · 채점 완료. 결과 보관 기간 동안 결과 조회 가능 | 상태 값 `COMPLETED` | |
| 만료 | `expire` | 기한이 지남. 시험은 진행 중 상태에서 마감이 지난 경우로, 상태로 저장하지 않고 마감으로 판정. 결과는 결과 보관 기간이 지난 경우. `화면: 무효 처리됩니다` | 오류 코드 `EXAM_EXPIRED` | 무효 (화면에서만) |
| 중단 | `abort` | 제출하지 않은 진행 중 시험을 종료하고 즉시 삭제. `화면: 시험이 종료되었습니다` | 주소 `/api/exams/{id}/abort` | 만료 · 종료 (7장) |
| 종료 | `end` | 진행 중이 아니게 됨 (완료 · 만료 · 중단 모두 포함) | — | 끝남 (`finished`) |
| 전달 · 미전달 | `deliver` · `delivered` · `undelivered` | 서버가 시험 화면에 문항을 제공함 / 제공하지 못함 (서버 기준 — 화면의 실제 수신 여부는 알 수 없음) | — | 수신 · 송신 · 받음 |
| 준비 | `ready` | 시험 시작 전 단계 (안내 · 소리 확인). 이 단계에서는 시험을 만들지 않음 | 주소 `/exam/ready` | |
| 정리 | `cleanUp` · `cleanup` | 종료된 시험을 모아 삭제 (동사 · 명사) | — | |
| 폐기 대상 | `disposable` | 정리 시 삭제하는 시험: 마감이 지난 진행 중 시험 / 미전달 시간 초과가 지난 진행 중 시험 / 결과 보관 기간이 지난 완료 시험 (`deleteDisposable`) | — | 지울 시험 |
| 버전 | `version` | 같은 시험이 동시에 두 번 제출되면 낙관적 잠금으로 하나만 반영 (3장 동시 제출) | 칼럼 `version` | |

### 1-3. 응시자

| 단어 | 코드 | 설명 | 외부 이름 | 사용하지 않는 말 |
|---|---|---|---|---|
| 기기 | `device` | 브라우저 하나 | — | |
| 사용자 | `client` | 접속한 주체 | — | 클라이언트 |
| IP | `ip` | 접속 위치의 IP 주소 | — | |
| 쿠키 | `cookie` | 브라우저 쿠키 | — | |
| 최대 | `max` | 설정으로 정한 가장 큰 허용 수 | — | 상한 |
| 최소 | `min` | 정한 가장 작은 수 | — | |
| 기본 | `default` | 별도로 지정하지 않을 때 사용하는 값 | — | |
| 당 | `per` | 하나마다 | — | |
| 수용량 | `capacity` | 시험을 추가로 받을 수 있는 정도 | — | |
| 잠금 | `lock` | 다른 처리가 개입하지 못하도록 차단 | — | |
| 열기 | `open` | 화면을 여는 동작 (활용형 '여는' · '연') | — | |
| 메인 | `main` | 메인 화면 = 레벨 선택 + 시험 시작 | 주소 `/`, `main.html` | |
| 정보 | `about` | 정보 · 출처 화면 (단어 원본 · 글꼴 라이선스 표기) | 주소 `/about`, `about.html` | |
| 안내 | `notice` | 메인 화면에 표시하는 한 줄 안내 | 주소 파라미터 `notice` | |

### 1-4. 단어 데이터 · 노출 판정

| 단어 | 코드 | 설명 | 외부 이름 | 사용하지 않는 말 |
|---|---|---|---|---|
| 단어 | `word` | 영어 단어 하나. 대소문자 구분 (May / may) | 테이블 `word` | |
| 표제어 | `headword` | 단어의 철자 (사전 표제어) | 칼럼 `headword` | |
| 외래어 | `loanword` | 단어를 소리 그대로 옮긴 말 (elevator → 엘리베이터). 참고 정보이며 보기에는 사용하지 않음 | 칼럼 `loanword` | |
| 뜻 | `meaning` | 단어의 한국어 뜻. 단어당 최대 5개 | — | 표시 뜻 |
| 한국어 | `ko` | | — | |
| 영어 | `en` | | — | |
| 순번 | `seq` | 범위 안에서의 순서 | 칼럼 `seq` | |
| 품사 | `pos` | 뜻의 품사 (noun · verb …) | 칼럼 `pos` | |
| 레벨 | `level` | CEFR 단계. 테이블 없이 enum으로 관리하며 선언 순서 = 낮은 레벨 → 높은 레벨. `화면: 코스` | 칼럼 `level_code` | 코스 (화면에서만) |
| 세부 | `detail` | 더 작게 나눈 단위 | — | |
| 묶음 | `group` | 여러 개를 합친 단위. 항상 앞 단어와 함께 사용 (7장) | — | |
| 이름 | `name` | | — | |
| 개요 | `overview` | 핵심 값을 모아 보여 주는 정보 (시험 레벨 개요 = 출제 범위 · 문항 수 · 허용 시간) | — | 요약 |
| 낮음 | `lower` | 레벨이 낮음 (`isLowerThan`) | — | |
| 출제 목록 | `catalog` | 출제에 사용하는 단어 목록 | — | 단어 캐시 |
| 제외 | `exclude` | 단어 노출 판정에 따라 시험에 출제하지 않음. 정답과 오답 보기 모두에서 제외 (출제 목록 단어의 `excluded`) | — | |
| 가능 | `able` | 할 수 있음 (`…able`) | — | |
| 후보 | `pool` | 추출 대상 목록 | — | |
| 혼동 | `confusable` | 한 문항의 보기로 함께 출제하면 정답이 둘처럼 보이는 관계 | — | 헷갈림 |
| 이유 | `reason` | | 칼럼 `reason` | |
| 원본 | `source` | 단어 데이터의 원본. `CEFRJ` = CEFR-J Wordlist (A1 ~ B2), `OCTANOVE` = Octanove Vocabulary Profile (C1 ~ C2) | 칼럼 `source` (테이블 `word_cefr_level`) | |
| 노출 | `exposure` | 시험에 출제함 | — | |
| 처리 | `action` | 단어 노출 판정으로 정한 처리 방식 | 칼럼 `action` | |
| 유지 | `keep` | 검토했으나 변경하지 않음 (검토 기록) | 처리 값 `KEEP` | |
| 판정 | `decide` | 노출 여부를 결정함 | — | |
| 이슈 | `issue` | 판정의 근거 | — | 문제 (문항과 혼동) |
| 유형 | `type` | 종류 | — | |
| 강도 | `severity` | 민감 단어의 강도 `HIGH` · `MEDIUM` · `LOW`. 민감 단어 외에는 값 없음 | 칼럼 `severity` | |
| 규칙 | `rule` · `rules` | | — | |

### 1-5. 공통 · 동작

| 단어 | 코드 | 설명 | 외부 이름 | 사용하지 않는 말 |
|---|---|---|---|---|
| 아이디 | `id` | 대상 하나를 가리키는 값. 형식은 1-6 | — | 번호 (아이디의 의미로) |
| 번호 | `no` | 1부터 세는 순서 | — | |
| 수 | `count` | 대상의 수. 동사 '세다'도 같은 단어 | — | 개수 |
| 시작 | `start` | | — | |
| 제출 | `submit` | 선택한 보기를 마지막에 한 번에 전송해 채점받음 | — | |
| 조회 | `get` | 값을 읽어 옴: 다른 곳에서 찾아 읽음(`getResult(id)`) / 객체의 값을 반환(`getQuestionCount()`, 6-1 `get…`) | — | |
| 찾기 | `find` | 목록에서 조건에 맞는 대상을 읽음 | — | |
| 계산 | `calculate` | 주어진 값으로 새 값을 산출 (`calculateAllowedTime`) | — | 셈 |
| 삭제 | `delete` | 저장된 대상을 제거 | — | 지우기 |
| 확인 | `check` | 조건에 맞는지 검사 | — | |
| 같음 | `same` | 두 값이 같음 (`isSameIp`) | — | |
| 만들기 | `create` | 대상을 새로 만드는 동작 (DB 행: `createdAt`, 기기 아이디: `findOrCreate`) | 칼럼 `created_at` (모든 테이블) | |
| 수정 | `update` | 저장된 행을 변경 (`updatedAt`) | 칼럼 `updated_at` (모든 테이블) | 바꿈 |
| 적재 | `load` | 저장된 데이터를 읽어 애플리케이션이 사용하는 형태로 메모리에 올림 (단어 출제 목록 적재) | — | 싣기 |
| 교체 | `replace` | 전체를 새 값으로 바꿔 넣음 (단어 출제 목록 교체) | — | |

### 1-6. 도메인

- 값을 담는 이름은 **마지막 단어**가 값의 형식을 결정 (클래스 이름에는 적용하지 않음)

| 도메인 | 마지막 단어 | 자바 | DB | 범위 · 형식 |
|---|---|---|---|---|
| 아이디 | 아이디 `id` | 시험: `UUID` / 기기: `String` / 단어 데이터: `long` | `uuid` / `varchar(36)` / `bigint` | 시험 아이디는 주소에 포함되므로 추측할 수 없는 무작위 값. 기기 아이디는 쿠키 값 그대로 사용. 단어 아이디는 원본 번호를 그대로 사용해 단어 데이터를 다시 가져와도 동일. 문항 목록 배열(`option_ids`) 안의 레벨 단어 아이디는 `integer` (3장) |
| 번호 | 번호 `no` | `int` | 정답 · 선택한 보기 배열 안 1바이트 (3장) | 1부터. 보기 번호는 1 ~ 4 |
| 순번 | 순번 `seq` | `int` | `smallint` | 1 ~ 5 |
| 수 | 수 `count` | `int` · `long` | `integer` | 0 이상 |
| 점수 | 점수 `score` | `int` | `integer` | 0 ~ 100. 제출 전에는 값 없음 |
| 시각 | 시각 `at` · 마감 `deadline` | `Instant` | `timestamptz` | |
| 날짜 | 날짜 `date` | `LocalDate` | `date` | |
| 기간 | 시간 `time` · 여유 · 보관 · 주기 · 시간 초과 | `Duration` | — (설정 값) | 설정 파일에는 `1m` · `1h` 형식으로 기재 |
| 초 · 분 | 초 `sec` · 분 `minutes` | `int` · `long` | `integer` | 0 이상 |
| 여부 | `is…` · 전달 · 제외 | `boolean` | `boolean` | |
| 글 | 글 `text` · 표제어 · 뜻 · 제시어 · 이유 · IP 등 | `String` | `varchar(n)` | 길이는 칼럼별로 지정 |
| 코드 값 | 레벨 · 상태 · 과목 · 처리 · 강도 · 원본 | `enum` | `varchar(n)` + CHECK (과목은 CHECK 없음) | 값 목록은 단어 · 용어 표 참고. 레벨은 자바에 세부 6개 + 묶음 3개, DB에는 세부 6개만 저장 |
| 버전 | 버전 `version` | `Integer` | `integer` | 0부터 |

- 자바 열은 값의 기본 타입. DB 행을 옮긴 클래스(엔티티)는 값이 없을 수 있는 칼럼에 래퍼 타입(`Integer` · `Long`)을 사용 (예: 제출 전의 점수 · 맞힌 수)

## 2. 용어

### 2-1. 시험과 문항

| 용어 | 코드 이름 | 설명 | 외부 이름 |
|---|---|---|---|
| 시험 아이디 | `examId` (시험 안 `id`) | 시험을 가리키는 값. API 주소 · 결과 화면 주소에 포함 | 칼럼 `test_session_id` |
| 시험 상태 | `ExamStatus` | 진행 중 · 완료 (만료는 상태가 아니라 마감으로 판정) | 칼럼 `status` (값 동일) |
| 시험 레벨 | `level` (시험 안) | 시험을 볼 레벨. 현재 화면에서는 세부 레벨(A1 ~ C2)만 선택 가능. `화면: 코스` | 칼럼 `level_code`, 시작 파라미터 · JSON `level` |
| 문항 수 | `questionCount` | 시험의 문항 수 = min(정답 후보 수, 최대 문항 수). DB에 별도 칼럼 없이 문항 목록 배열 길이로 계산 | JSON · 템플릿 `questionCount` |
| 최대 문항 수 | `maxQuestionCount` | 한 시험의 최대 문항 수 (기본 500). 0이면 레벨 단어 전체 (개발용) | 설정 `voca-runner.exam.question-count` |
| 문항 번호 | `questionNo` (문항 안 `no`) | 1부터 시작 | JSON `questionNo` · `no` |
| 문항 생성기 | `QuestionGenerator` | 단어 출제 목록으로 문항을 만드는 도메인 서비스 (출제자). 빈으로 등록(`ExamConfig`). 단어 출제 목록은 호출마다 인자로 받음 — 시작 서비스가 요청 시점의 목록을 전달. 생성한 문항은 시작 서비스가 시험에 전달하며, 보기는 보기 생성기가 생성 | — |
| 보기 생성기 | `OptionGenerator` | 문항 하나의 보기 4개를 생성 = 정답 + 같은 세부 레벨의 오답 3개 (3장 보기 규칙) | — |
| 제시어 보조 | `promptSub` | 제시어 아래 줄에 함께 표시하는 형태 (표제어 보조) | JSON `promptSub` |
| 보기 번호 | `optionNo` (보기 안 `no`) | 1 ~ 4, 화면에 표시되는 순서 | JSON `options[].no` |
| 보기 글 | `text` (보기 안) | 보기에 표시되는 보기 뜻 (`뜻1, 뜻2`) | JSON `options[].text` |
| 보기 아이디 | `optionId` · `optionIds` | 보기가 가리키는 레벨 단어 아이디. 보기 번호(1 ~ 4)는 문항 안의 위치, 보기 아이디는 그 위치에 놓인 단어 | 칼럼 `option_ids` |
| 정답 보기 아이디 | `answerOptionId` | 정답 보기의 아이디. 제시어 · 정답 글 조회에 사용하며, 문항 목록이 보기[정답 보기 번호]로 계산 | — |
| 선택한 보기 아이디 | `selectedOptionId` | 선택한 보기의 아이디. 선택한 글 조회에 사용하며, 선택하지 못했으면 값 없음. 문항 목록의 보기[선택한 보기 번호]이므로 시험이 두 값을 조합해 계산 | — |
| 정답 보기 번호 | `answerOptionNo` | 정답이 몇 번 보기인지 | 칼럼 `correct_option_no` |
| 선택한 보기 번호 | `selectedOptionNo` | 응시자가 선택한 보기 번호. 선택하지 못했으면 값 없음 (DB에는 0). `화면: 내 답` | 칼럼 `selected_option_no`, JSON `selectedOptionNo` (제출 JSON의 `selectedOptions[]` 안) |
| 오답 보기 | `INCORRECT_OPTION_COUNT` (오답 보기 수) | 정답이 아닌 보기 3개 | — |
| 문항 목록 | `Questions` | 시험의 문항 전체 = 문항 번호 순서대로 보기 4개(보기 아이디) + 정답 보기 번호. 시작 시 확정되어 변경되지 않음. **선택한 보기의 채점**(맞힘 여부 · 맞힌 수 · 틀린 문항)과 제시어(정답 보기) 계산을 담당. 시작 시 확정되는 값(문항 목록)과 제출 시 확정되는 값(선택한 보기)을 분리한 구조. 저장 형식은 3장 | 칼럼 `option_ids` · `correct_option_no` |
| 시험지 | — | 화면에 전송하는 문항 목록 = 정답 보기 번호를 제외한 제시어 · 보기 글 (문항 전달 결과 `ExamQuestionsResult`). 별도 도메인 객체 없음 | `GET /api/exams/{id}/questions` |
| 선택한 보기 | `SelectedOptions` | 문항별 선택한 보기 번호 (시험이 직접 보유). 제출 시 한 번에 기록하며, 없는 문항 번호 · 중복 번호 · 1 ~ 4 범위 밖의 보기 번호는 거절 | 칼럼 `selected_option_no` |
| 선택한 보기 하나 | `SelectedOption` | 문항 하나의 선택한 보기 = 문항 번호 + 보기 번호 (값이 없으면 시간 초과). 제출 요청 `selectedOptions[]`의 한 항목(`SubmitExamCommand.SelectedOptionItem`)을 도메인 값으로 변환한 것 | — |
| 채점 결과 | `GradingResult` | 맞힌 수 · 점수. 제출 전에는 값 없음. 점수 버림 계산 담당 | 칼럼 `correct_count` · `score` |
| 틀린 문항 번호 | `getWrongQuestionNos()` | 맞히지 못한 문항 번호 목록 (오답 + 시간 초과, 번호 순) | — |

### 2-2. 시간

| 용어 | 코드 이름 | 설명 | 외부 이름 |
|---|---|---|---|
| 문항당 시간 | `timePerQuestionSec` | 한 문항의 풀이 시간(초). 5초 (기본값 `DEFAULT_TIME_PER_QUESTION_SEC`, 시험 시작 시 확정). 시간이 지나면 시간 초과로 다음 문항 진행. `화면: 문제당 5초` | 칼럼 `time_limit_sec` |
| 허용 시간 | `calculateAllowedTime` · `allowedMinutes` | 시험 전체를 마쳐야 하는 시간 = (문항 수 × 문항당 시간 + 여유)를 분 단위로 올림. 500문항이면 43분. 시험을 만들기 전에도 문항 수로 계산 가능(`calculateAllowedTime`, 메인 레벨 카드에서 사용). `화면: 제한 시간 43분` | JSON · 템플릿 `allowedMinutes` |
| 시험 시간 | `ExamTime` | 시험의 시각 = 시작 시각 · 마감(그 사이가 허용 시간) · 제출 시각(제출 전에는 값 없음). 마감 경과 · 남은 시간 · 허용 분 · 보관 마감 계산 담당. 문항당 시간(길이 규칙)은 포함하지 않음 | 칼럼 `started_at` · `deadline` · `submitted_at` |
| 시작 시각 | `startedAt` | 준비 화면에서 [시작]을 눌러 시험이 만들어진 시각 = 첫 문항이 표시되는 시점. 안내 · 소리 확인 시간은 허용 시간에 포함하지 않음 | 칼럼 `started_at` |
| 남은 시간 | `remainingSec` | 마감까지 남은 초. 기기 간 시계 차이를 피하기 위해 시각이 아닌 남은 초로 전달 | JSON `remainingSec` |
| 제출 시각 | `submittedAt` | 제출한 시각. 제출 전에는 값 없음 | 칼럼 `submitted_at` |
| 결과 보관 | `resultRetention` | 제출 후 결과를 보관하는 시간 (기본 1시간) | 설정 `voca-runner.exam.result-retention`, JSON `retentionMinutes` |
| 보관 마감 | `retentionDeadline` | 제출 시각 + 결과 보관. 화면에는 한국 시각 `(14:20까지)` 형식으로 표시 | JSON `retentionDeadline` |
| 정리 주기 | — | 기본 1분 | 설정 `voca-runner.exam.cleanup-interval` |
| 미전달 시간 초과 | `undeliveredTimeout` | 시작 후 이 시간 안에 문항이 전달되지 않은 시험은 정리 대상 (기본 2분). 시작 직후 연결이 끊겨 중단 요청도 보내지 못한 시험이 진행 중 수에 계속 포함되지 않도록 하기 위함 | 설정 `voca-runner.exam.undelivered-timeout` |

### 2-3. 진행과 정리

| 용어 | 코드 이름 | 설명 | 외부 이름 |
|---|---|---|---|
| 준비 화면 | `ready` | 메인에서 레벨을 선택한 뒤 여는 시작 전 화면 (레벨 · 문항 · 시간 안내, 소리 확인). 이 단계에서는 시험을 만들지 않음 | `GET /exam/ready?level=…` |
| 시험 시작 | `StartExamUseCase.start` | 준비 화면의 [시작]에서 시험을 만들어 저장 (응답 = 시험 아이디, 화면은 이어서 문항 전달 요청). 처리 순서: 수용량 확인 → 문항 생성 → 시험 시작 잠금 안에서 재확인 · 같은 기기의 시험 중단 · 저장. 묶음 레벨은 시작 입력(`StartExamCommand`)에서 400으로 거절 | `POST /api/exams` |
| 문항 전달 | `deliverQuestions` | 시험 화면에 문항 전체를 한 번에 전송 (정답 보기 번호 제외). 문항 전달 여부를 기록(시험 안 `deliverQuestions(now)` — 진행 중일 때만). 시험을 시작한 기기만 가능. GET 요청이므로 반복 호출해도 같은 문항을 반환 (멱등) | `GET /api/exams/{id}/questions` |
| 문항 전달 여부 | `questionsDelivered` | 서버가 시험 화면에 문항을 제공했는지 여부 (화면의 실제 수신 여부는 알 수 없음). 문항이 전달되지 않은 시험을 조기에 정리하는 데 사용 | 칼럼 `questions_delivered` |
| 시험 제출 | `ExamUseCase.submit` | 선택한 보기를 마지막에 한 번에 전송해 채점. 시험을 시작한 기기만 가능. 마감이 지났으면 즉시 삭제하고 `EXAM_005` 반환 | `POST /api/exams/{id}/submit` |
| 중단 가능 | `isAbortableBy` | 시험을 시작한 기기가 진행 중인 시험에 대해서만 중단 가능 | — |
| 시험 중단 | `abortIfInProgress` | 제출하지 않은 진행 중 시험을 종료하고 즉시 삭제. 그만두기 · 같은 기기의 새 시작 · 응시 중 화면 이탈(새로고침 · 뒤로 가기 · 창 닫기) 시 발생. 다른 기기의 요청은 무시 | `POST /api/exams/{id}/abort` |
| 시험 정리 | `CleanUpExamsUseCase` · `cleanUp` | 정리 주기마다 폐기 대상 시험을 삭제 | — |

### 2-4. 결과

| 용어 | 코드 이름 | 설명 | 외부 이름 |
|---|---|---|---|
| 시험 결과 | `ExamResult` | 완료된 시험의 점수와 틀린 항목. 시험 아이디만으로 조회 가능 (기기 제한 없음) | — |
| 시험 결과 조회 | `ExamQuery.getResult` · `findResult` | 시험 결과를 읽어 옴. `findResult`는 결과가 없으면 빈 값을 반환 (결과 화면에서 결과 만료 안내로 이동) | `GET /exam/{id}/result` (화면) · `GET /api/exams/{id}/result` |
| 맞힌 수 | `correctCount` | 선택한 보기가 정답인 문항 수. `화면: 정답` | 칼럼 `correct_count`, JSON `correctCount` |
| 오답 수 | `incorrectCount` | 보기를 선택했으나 정답이 아닌 문항 수 (시간 초과 제외). `화면: 오답` | — |
| 시간 초과 수 | `timeoutCount` | 문항당 시간 안에 선택하지 못한 문항 수. 틀린 것으로 집계. `화면: 시간 초과` | — |
| 틀린 항목 | `WrongItem` | 맞히지 못한 문항 하나 (오답 + 시간 초과). 제시어 · 정답 글 · 선택한 글 · 표제어(발음용) 포함. `화면: 틀린 단어` | JSON `wrongItems[]` |
| 정답 글 | `answerText` | 틀린 항목의 정답 보기 글 | JSON `answerText` |
| 선택한 글 | `selectedText` | 틀린 항목의 선택한 보기 글. 시간 초과면 값 없음. `화면: 내 답` | JSON `selectedText` |

### 2-5. 응시자와 동시 진행

| 용어 | 코드 이름 | 설명 | 외부 이름 |
|---|---|---|---|
| 기기 아이디 | `deviceId` | 브라우저마다 쿠키로 부여하는 무작위 값 (유효 기간 1일, 시험 시작 시마다 갱신). 같은 공인 IP를 쓰는 여러 사용자(학교 Wi-Fi · 통신사망)를 구분. 문항 조회 · 제출 · 중단은 시험을 시작한 기기만 가능 | 쿠키 `VR_DEVICE`, 칼럼 `device_id` |
| 기기 쿠키 | `DeviceCookie` | 쿠키에서 기기 아이디를 읽고, 없으면 새 값을 만들어 부여 (`findOrCreate`). 시작 시 같은 값으로 다시 설정해 유효 기간(1일)을 갱신. 운영 환경은 CloudFront 뒤에 있어 앱이 요청의 HTTPS 여부를 판별할 수 없으므로 `Secure` 속성은 설정으로 지정 | 쿠키 `VR_DEVICE`, 설정 `voca-runner.device-cookie.secure` |
| 사용자 IP | `ClientIp` · `clientIp` | 접속한 사용자의 IP. 운영 환경은 CloudFront 뒤에 있으므로 연결 IP가 아닌 CloudFront가 추가하는 헤더에서 추출 | 헤더 `CloudFront-Viewer-Address`, 설정 `voca-runner.client-ip.header`, 칼럼 `client_ip` |
| 최대 진행 중 | `maxInProgress` | 전체 진행 중 시험 수의 최대 (기본 20,000) | 설정 `voca-runner.exam.max-in-progress`, 오류 `EXAM_006` |
| IP당 최대 진행 중 | `maxInProgressPerIp` | IP 하나의 진행 중 시험 수 최대 (기본 50) | 설정 `voca-runner.exam.max-in-progress-per-ip`, 오류 `EXAM_008` |
| 시험 수용량 | `ExamCapacity` | 최대 진행 중 · IP당 최대 진행 중을 기준으로 시작 가능 여부를 판정. 같은 기기의 진행 중 시험은 집계에서 제외 (IP 집계에서는 같은 IP의 시험만 제외) | 설정 `max-in-progress` · `max-in-progress-per-ip` |
| 수용량 확인 | `checkCapacity` | 시작 시 진행 중 시험 수가 최대를 넘는지 확인. 잠금 없이 한 번(문항 생성 전), 시험 시작 잠금 안에서 한 번 더 확인. 같은 기기의 진행 중 시험은 새 시험으로 대체되므로 집계에서 제외 | — |
| 시험 시작 잠금 | `lockForStart` | 수용량 확인과 저장 사이에 다른 시작 요청이 개입하지 못하도록 시작을 하나씩 처리. PostgreSQL 트랜잭션 잠금(`pg_advisory_xact_lock`)이므로 **트랜잭션 안에서만 유효** | — |
| 결과 만료 안내 | `NOTICE_RESULT_EXPIRED` | 결과 보관 기간이 지난 결과 화면을 요청했을 때 표시하는 안내 | `?notice=result-expired` |

### 2-6. 단어 데이터와 레벨

| 용어 | 코드 이름 | 설명 | 외부 이름 |
|---|---|---|---|
| 단어 아이디 | `wordId` | 단어 데이터 원본의 번호를 그대로 사용 | 칼럼 `word_id` |
| 표제어 보조 | `headwordSub` | 시험 화면에서 표제어 아래 줄에 함께 표시하는 형태 (예: for the sake of → for one's sake) | 칼럼 `headword_sub` |
| 단어 뜻 | `WordMeaning` (DB 행 `WordMeaningJpaEntity`) | 단어의 뜻 하나. 도메인 값은 순번 · 뜻 글(`text`)만, DB 행에는 품사도 포함 (한국어 뜻 · 품사 · 순번) | 테이블 `word_meaning` |
| 한국어 뜻 | `meaningKo` | 뜻의 글 | 칼럼 `meaning_ko` |
| 뜻 순번 | `meaningSeq` (단어 뜻 안 `seq`) | 단어 안 뜻의 순서 1 ~ 5 (1이 가장 일반적인 뜻). 단어 데이터를 다시 가져오면 바뀌는 단어 뜻 아이디 대신 이 값으로 뜻을 지정 | 칼럼 `seq` · `meaning_seq` |
| 세부 레벨 | `A1` ~ `C2` · `getDetailLevels()` | 단어 데이터와 시험의 기본 레벨 | 값 동일 |
| 묶음 레벨 | `A` · `B` · `C` · `isGroup()` | 세부 레벨 둘을 합친 레벨 (A = A1 + A2). 문항 생성은 지원하지만 DB에 저장하지 않고 화면에서도 선택하지 않음. 준비 화면 · 시험 시작 요청 시 400 "묶음 레벨 시험은 아직 볼 수 없습니다" | — |
| 묶음 이름 | `getGroupName()` (레벨 안. 결과 객체 · JSON 필드는 `groupName`) | A = Basic, B = Independent, C = Proficient (CEFR 원문의 단계 묶음 이름). 결과 화면 표기 `B1 · Independent` | — |
| 시험 레벨 개요 | `ExamLevelOverview` | 레벨별로 정해진 시험의 기본 값 = 레벨 · 정답 후보 단어 수 · 문항 수 · 허용 시간. 시험 1회(`Exam`)가 아니라 해당 레벨 시험 전체에 적용되는 값이며, 메인 화면의 레벨 카드 하나에 해당. 준비 화면은 레벨 하나만 조회(`getExamLevelOverview(level)`, 묶음 레벨은 400) | 템플릿 `levelGroups` 안 |
| 묶음 레벨 개요 | `ExamLevelOverviewGroup` | 메인 화면의 묶음 레벨 제목 + 해당 시험 레벨 개요 목록 | 템플릿 `levelGroups` |
| 단어 수 | `wordCount` | 레벨의 정답 후보 수. `화면: 레벨 카드의 N 단어` | 템플릿 `levelGroups` 안 `wordCount` |
| 레벨 단어 | `LevelWord` (DB 행 `LevelWordJpaEntity`) | 한 단어의 한 세부 레벨 = `level_word` 1행. 원본에 있는 레벨마다 존재하며, 출제 여부는 단어 노출로 결정. 도메인 값은 단어 노출 적용 전의 값 = 표제어 · 표제어 보조 · 보기 뜻(순번 포함) | 테이블 `level_word` |
| 레벨 단어 아이디 | `levelWordId` | 보기 · 정답을 이 값으로 저장 | 칼럼 `level_word_id` |
| 보기 뜻 | `optionMeanings` | 레벨 단어마다 보기에 표시할 뜻 1 ~ 2개. 화면에는 `뜻1, 뜻2` 형식으로 표시 | 칼럼 `meaning1_id` · `meaning2_id` |

### 2-7. 출제 목록

| 용어 | 코드 이름 | 설명 | 외부 이름 |
|---|---|---|---|
| 단어 출제 목록 | `WordCatalog` | 문항 생성 · 결과 표시에 사용하는 단어 목록 (도메인 클래스, 만든 뒤에는 변경되지 않음). 정답 후보 · 혼동 단어 묶음을 구성하고 레벨 단어 아이디로 조회. 단어 출제 목록 적재가 만들어 메모리 어댑터에 보관. 단어 데이터 변경은 재시작 시 다시 적재되어 반영 | — |
| 출제 목록 단어 | `CatalogWord` | 레벨 단어 하나에 단어 노출을 적용해 출제용 값으로 변환한 것 (표제어 · 표제어 보조 · 세부 레벨 · 보기 뜻 · 제외 여부). 단어 노출 규칙 `apply`의 반환값 | — |
| 정답 가능 | `isAnswerable()` | 제외 대상이 아님 | — |
| 정답 후보 | `findAnswerPool` | 레벨의 정답 가능한 출제 목록 단어, 단어당 1개(낮은 레벨). 3장 정답 후보 순서 | — |
| 보기 후보 | `optionPool` (보기 생성기 안) | 오답 보기를 추출하는 목록 = 정답 후보. 시험에 출제하지 않는 단어는 오답 보기로도 출제하지 않음 (문항 생성기가 정답 후보를 그대로 전달) | — |
| 혼동 단어 묶음 | `ConfusableGroups` (묶음 전체) | 한 문항의 보기로 함께 출제하면 정답이 둘처럼 보이는 단어들 (예: {sir, teacher} 선생님). 레벨과 무관한 단어 단위이며, 한 단어가 여러 묶음에 속할 수 있음. 코드 이름에서는 '단어'를 생략 | 테이블 `confusable_group` · `confusable_group_word` |
| 혼동 이유 | — | 묶음의 근거가 된 말 (예: 선생님) | 칼럼 `reason` |
| 혼동 단어 아이디 | `findConfusableWordIds` | 같은 묶음에 속한 다른 단어 전체 (양방향: A에 B가 있으면 B에도 A) | — |
| 단어 출제 목록 적재 | `LoadWordCatalogUseCase` · `load()` | DB에서 단어 노출 · 레벨 단어 · 혼동 단어 묶음을 조회(영속성 어댑터는 조회만) → 단어 노출 적용 → 단어 출제 목록 구성 → 메모리 어댑터의 목록 교체. 애플리케이션 기동 시 요청을 받기 전에 1회 실행(`WordCatalogInitializer`). 동시에 실행되면 이전 데이터로 만든 목록이 최신 목록을 덮어쓸 수 있으므로 한 번에 하나씩 실행 | — |
| 단어 출제 목록 조회 | `WordCatalogQuery` · `getWordCatalog()` | 현재 단어 출제 목록. 시험 쪽 서비스가 요청마다 한 번 가져와 해당 요청 안에서만 사용 (필드로 보관하지 않음) | — |

### 2-8. 단어 노출

| 용어 | 코드 이름 | 설명 | 외부 이름 |
|---|---|---|---|
| 단어 노출 | `WordExposureJpaEntity` | 단어 · 단어 뜻 · 레벨 단어의 출제 여부를 판정한 기록. 대상 하나에 1행. 애플리케이션은 단어 출제 목록을 만들 때 조회 | 테이블 `word_exposure` |
| 노출 처리 | `ExposureAction` | 단어 제외 · 뜻 제외 · 레벨 제외 · 유지 | 칼럼 `action` |
| 단어 제외 | `EXCLUDE_WORD` | 단어 전체를 모든 레벨에서 정답으로도 오답 보기로도 출제하지 않음 | 값 동일 |
| 뜻 제외 | `EXCLUDE_MEANING` | 해당 뜻을 모든 레벨의 보기 뜻에서 제외. 표시할 뜻이 남지 않는 레벨 단어는 제외 | 값 동일 |
| 레벨 제외 | `EXCLUDE_LEVEL` | 해당 레벨 단어만 출제하지 않음 | 값 동일 |
| 이슈 유형 | — | 판정 근거의 종류 (비하어 · 외래어 · 같은 뜻 다른 레벨 등). 노출 처리와 별개 | 칼럼 `issue_type`, 테이블 `word_exposure_type` (코드 예: `SLUR` · `LOANWORD`) |
| 판정 날짜 | — | 판정한 날짜 | 칼럼 `decided_at` |
| 단어 노출 규칙 | `WordExposureRules` | 단어 노출을 레벨 단어마다 적용하는 규칙 (`apply(레벨 단어)` → 출제 목록 단어). 판정 데이터는 테이블에만 두고, 적용은 애플리케이션 한 곳(단어 출제 목록 적재)에서 수행 | — |

### 2-9. 코드 이름과 외부 이름이 다른 이름

| 이름 | 코드 이름 | 외부 이름 | 이유 |
|---|---|---|---|
| 시험 | `Exam` | 테이블 `test_session` | 초기 이름 `test` 유지 (DB 이름 변경은 보류) |
| 시험 아이디 | `examId` | 칼럼 `test_session_id` | 초기 이름 `test_session` |
| 최대 문항 수 | `maxQuestionCount` | 설정 `question-count` | 설정 이름 유지, 코드에서 `@Name`으로 연결 |
| 문항당 시간 | `timePerQuestionSec` | 칼럼 `time_limit_sec` | 칼럼 이름의 time limit은 **문항당** 시간. 코드에는 time limit이라는 말을 사용하지 않음 |
| 정답 보기 번호 | `answerOptionNo` | 칼럼 `correct_option_no` | |
| 과목 | `subject` (`Subject`) | 칼럼 `direction` | 초기 이름 방향(`EN_KO`). DB 칼럼 이름은 유지, 값은 `ENGLISH` |
| 보기 뜻 | `optionMeanings` | 칼럼 `meaning1_id` · `meaning2_id` | |

## 3. 규칙과 개념

- 코드 이름이 따로 없는 개념. 코드 이름이 생기면 1장 단어부터 등록

| 개념 | 설명 |
|---|---|
| 문항 목록 · 선택한 보기 저장 형식 | 문항을 행으로 나누지 않고 시험 1행에 배열로 저장. 보기는 문항 수 × 4개의 레벨 단어 아이디(칼럼 `option_ids`), 정답 보기 번호 · 선택한 보기 번호는 문항당 1바이트. 500문항 기준 약 10KB |
| 보기 규칙 | 한 시험에 같은 단어는 한 번만 출제 / 보기 4개는 정답과 같은 세부 레벨에서 선정 / 같은 단어 · 혼동 단어 · **보기 뜻이 하나라도 같은(문구가 완전히 같은) 단어**는 한 문항에 함께 출제하지 않음 |
| 낮은 레벨 우선 | 한 단어가 두 레벨에 있으면 묶음 레벨에서는 낮은 레벨 단어(그 보기 뜻)만 사용 |
| 정답 후보 순서 | 제외 대상을 **먼저** 거른 뒤 단어당 1개를 남김. 순서가 반대면 "낮은 레벨은 제외, 높은 레벨은 출제"인 단어가 누락됨 |
| 기기당 1개 | 한 기기의 진행 중 시험은 1개. 새로 시작하면 이전 시험을 중단 |
| 동시 제출 | 같은 시험이 동시에 두 번 제출되면 버전(낙관적 잠금)으로 하나만 반영. 늦은 요청에는 '진행 중 아님'(`EXAM_002`) 반환 |
| 중도 이탈 | 응시 중 새로고침 · 창 닫기 · 뒤로 가기. 화면이 중단 요청(sendBeacon)을 전송 |
| 민감 단어 | 비하어 · 차별 표현 · 욕설 · 성적 표현 · 신체 관련 표현 등의 이슈 유형에 해당해 노출 판정이 필요한 단어 |
| 판정 이력 | 판정을 변경할 때마다 1행씩 추가만 하는 기록 (최초 · 변경 · 삭제). 애플리케이션은 조회하지 않음 (테이블 `word_exposure_history`) |
| 혼동 단어 쌍 | 혼동 단어 묶음 안의 두 단어. 데이터 검토에서 묶음 여부를 판단할 때 사용하는 단위 |
| CEFR | 유럽평의회의 유럽 언어 공통 참조 기준. 레벨 이름(A1 ~ C2, Basic · Independent · Proficient)을 차용. 시험 결과는 CEFR 등급 판정이 아님 |
| CEFR 원본 테이블 | 두 원본 파일(CEFR-J Wordlist 1.5판, Octanove Vocabulary Profile 1.0판 · CC BY-SA 4.0)의 행을 수정하지 않고 그대로 보관. 애플리케이션은 조회하지 않음 (테이블 `word_cefr_level`) |
| 단어 데이터 가져오기 | 단어 데이터(CSV)를 DB에 넣는 작업. 애플리케이션 밖에서 수행하며, 애플리케이션은 단어를 조회만 함. 메모리의 단어 출제 목록 적재와 구분 |

## 4. 화면

### 4-1. 화면 문구 → 단어 · 용어

| 화면 문구 | 단어 · 용어 | 위치 |
|---|---|---|
| 코스 (`A1 코스 시작`) | 레벨 | 메인 |
| 문제당 5초 | 문항당 시간 | 메인 하단 안내 띠 · 시작 전 안내 |
| 제한 시간 43분 | 허용 시간 | 메인 하단 안내 띠 · 시작 전 안내 |
| N 단어 | 단어 수 | 메인 레벨 카드 |
| 무효 처리됩니다 | 만료 | 메인 · 시험 화면 안내 |
| 시험이 종료되었습니다 | 시험 중단 | 시험 화면 안내 |
| 그만두기 ("그만둘까요?") | 시험 중단 | 응시 중 뒤로 가기 |
| 정답 (기록 카드) | 맞힌 수 | 결과 |
| 오답 | 오답 수 | 결과 |
| 시간 초과 | 시간 초과 수 | 결과 |
| 틀린 단어 | 틀린 항목 | 결과 |
| 정답 · 내 답 | 정답 글 · 선택한 글 | 결과의 틀린 단어 목록 |
| 레벨별 표시 뜻 | 보기 뜻 | 정보 · 출처 |

### 4-2. 화면 목록

| 화면 | 설명 | 주소 · 파일 |
|---|---|---|
| 메인 화면 | 레벨 선택 + 시험 시작 | `/`, `main.html` |
| 시험 화면 | 준비(안내 시트) → [시작] → 제시어 · 보기 4개 · 타이머 | `/exam/ready?level=…`, `exam.html` · `exam.js` · `exam.css` |
| 안내 시트 | 시험 화면 위에 표시되는 안내창: 시작 전(레벨 · 문항 · 시간 · [이전] [시작] — 시험을 만들기 전) / 채점 중 / 종료 · 오류 | — |
| 소리 확인 | 시작 전 안내의 버튼. 영어 문장을 30초간 읽어 그동안 휴대폰 음량을 조절하도록 함. 읽는 중 다시 누르면 중지 | — |
| 발음 | 브라우저 내장 음성(Web Speech API, TTS)으로 제시어 · 결과 화면의 표제어를 읽음. 발음을 지원하는 브라우저에서만 안내 | — |
| 문제당 타이머 | 문항당 시간의 막대 + 남은 초 | — |
| 남은 전체 시간 | 마감까지 `mm:ss`. 60초 이하이면 빨간색 | — |
| 진행 길 | 상단 띠. 달리는 사람이 결승선으로 이동하며 남은 길이가 줄어듦. 색은 100문항마다 한 단계씩 변경 | — |
| 다시 제출 | 네트워크 오류로 제출에 실패했을 때 재전송 (선택한 보기는 유지) | — |
| 결과 화면 · 기록 카드 | 점수 · 맞힌 수 · 오답 수 · 시간 초과 수, 결과 보관 안내, 틀린 항목 목록 | `/exam/{id}/result`, `result.html` |
| 정보 · 출처 | 단어 원본 · 글꼴 라이선스 표기 | `/about`, `about.html` |
| 오류 안내 화면 | 화면 요청의 오류를 한국어로 안내 (API는 JSON) | `error.html` |

## 5. 오류

| 용어 | 코드 이름 | 설명 | 외부 이름 |
|---|---|---|---|
| 업무 오류 | `CustomException` | 업무 규칙에 따라 요청을 거절 (없는 · 종료된 시험, 마감 경과 등). HTTP 400 | — |
| 오류 코드 | `ErrorCode` | 오류 종류. 값(`EXAM_002` 등)은 응답 JSON `code`로 전달되는 외부 이름 | 아래 표 |
| 오류 응답 | `ErrorResponse` | API 오류 JSON. 화면 요청은 오류 안내 화면으로 응답 | JSON `code` · `message` · `path` · `timestamp` |

- 오류 메시지(`message`)는 시험 화면 안내창에 그대로 표시되므로 **화면 문구**로 작성
  - 예: `EXAM_005` "제한 시간이 지나 시험이 무효가 되었습니다."
  - 예외: `BUSINESS_001`은 화면이 자체 문구("시험 정보가 사라졌습니다")를 표시하므로 메시지는 API 응답에만 포함
- 오류 코드 이름은 상황을 나타내는 형식(`NOT_…` · `INVALID_…` · `TOO_MANY_…`)이므로 해당 형식의 말은 단어 규칙 밖. 포함되는 명사는 1장 단어를 사용

| 값 | 코드 이름 | 설명 | HTTP |
|---|---|---|---|
| `COMMON_001` | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 | 500 |
| `COMMON_002` | `INVALID_INPUT_VALUE` | 입력값 오류 (형식 · 범위 · 없는 문항 번호 · 묶음 레벨 등) | 400 |
| `COMMON_003` | `METHOD_NOT_ALLOWED` | 허용하지 않는 메서드 | 405 |
| `COMMON_004` | `RESOURCE_NOT_FOUND` | 없는 주소 · 파일 | 404 |
| `COMMON_005` | `UNSUPPORTED_MEDIA_TYPE` | 지원하지 않는 본문 형식 | 415 |
| `BUSINESS_001` | `ENTITY_NOT_FOUND` | 없는 시험(종료 또는 삭제된 시험 — 다른 기기의 시험도 같은 응답, 예외 `ExamNotFoundException`) · 없는 단어 | 400 |
| `EXAM_001` | `EXAM_NOT_ENOUGH_WORDS` | 문항을 만들 단어 부족 | 400 |
| `EXAM_002` | `EXAM_NOT_IN_PROGRESS` | 진행 중이 아닌 시험 (동시 제출의 늦은 요청 포함) | 400 |
| `EXAM_003` | `EXAM_INVALID_OPTION` | 보기 번호 오류 | 400 |
| `EXAM_004` | `EXAM_NOT_COMPLETED` | 완료 전 시험의 결과 요청 | 400 |
| `EXAM_005` | `EXAM_EXPIRED` | 마감이 지나 만료 | 400 |
| `EXAM_006` | `EXAM_TOO_MANY_IN_PROGRESS` | 최대 진행 중 초과 (시험 화면 안내창에 표시) | 400 |
| `EXAM_008` | `EXAM_TOO_MANY_IN_PROGRESS_PER_IP` | IP당 최대 진행 중 초과 (시험 화면 안내창에 표시) | 400 |

- `EXAM_007`은 이전 규칙(사용자당 최대)에 사용하던 값이므로 재사용하지 않음

## 6. 이름 형식

### 6-1. 아키텍처와 무관한 형식

| 형식 | 설명 | 예 |
|---|---|---|
| `get…` | 인자 없이 객체의 값을 반환 — 저장된 값과 계산한 값 모두 해당 (JavaBeans getter · Lombok `@Getter`와 같은 형식). 작업을 수행하는 메서드(무거운 계산 · 상태 변경 · 집계나 탐색 과정)는 동사를 사용. 인자로 번호 · 시각 등을 받아 값을 읽기만 하는 조회는 명사도 허용 (`optionIds(no)` · `remainingSec(now)` — 자바 `List.get(i)` · `charAt(i)`와 같은 형식). **레코드는 자바 형식 `x()`** 를 따르며, 레코드 안의 계산 값도 같은 형식 (`getXxx`를 두면 JSON 필드가 추가됨) | `getQuestionCount` · `getWrongQuestionNos` · `getGroupName` / 레코드 `deadline()` · `timeoutCount()` |
| `is…` | 참 · 거짓 (여부 도메인) | `isInProgress` · `isAnswerable` · `isCompleted` |
| `find…` · `count…By<기준>` | 찾기 · 세기 + 기준 | `findAnswerPool` · `countByLevel` |
| `…By<주체>` | 수행 주체 | `isStartedBy` · 칼럼 `decided_by` |
| `…Than` | 비교 | `isLowerThan` |
| `of` · `from` | 다른 객체를 재료로 만드는 정적 메서드 (재료 여럿 / 하나) | `ExamQuestionsResult.of` · `SubmitExamResult.from` · `ExamJpaEntity.from` |
| `require…` | 조건을 만족하지 않으면 예외 | `requireInProgress` |
| `to…` | 다른 형식으로 변환 | `toCatalogWord` |
| `calculate…` | 주어진 값으로 계산해 반환 (상태를 변경하지 않음) | `calculateAllowedTime` |
| `…Sec` · `…Minutes` | 단위 (읽는 법 참고) | `remainingSec` · `allowedMinutes` |
| 복수형 `…s` | 목록을 감싼 클래스 (일급 컬렉션, 우테코 관례) | `Questions` · `SelectedOptions` |

- 코드에서 `Test`는 자동 테스트 클래스(JUnit)에만 사용 (예: `ExamTest`)

### 6-2. 계층 · 포트 이름 형식 (『만들면서 배우는 클린 아키텍처』 관례 기반)

- **들어오는 포트와 서비스**
  - 상태를 변경하는 서비스 · 들어오는 포트는 유스케이스 묶음마다 하나: 시험 시작 `StartExamUseCase`, 응시(문항 전달 · 제출 · 중단) `ExamUseCase`, 시험 정리 `CleanUpExamsUseCase`
  - 조회만 하는 서비스 · 들어오는 포트(`…Query`)는 조회 대상마다 하나: 시험 `ExamQuery`, 시험 레벨 개요 `ExamLevelOverviewQuery`, 단어 출제 목록 `WordCatalogQuery`
- **나가는 포트**: 외부 시스템마다 하나 (DB)
  - 예외: 메모리에 보관하는 단어 출제 목록은 조회 `Get…Port` · 교체 `Replace…Port`로 분리 — 적재하는 서비스와 조회하는 서비스가 각자 필요한 쪽에만 의존
- **업무 규칙의 위치**: 검사 · 판정 · 계산 · 기본값은 도메인에 배치 (풍부한 도메인 모델)
  - 서비스는 조회 → 도메인 호출 → 저장 · 삭제 · 로그 · 트랜잭션만 담당
  - 결과 객체는 자신의 정적 메서드(`of` · `from`)로 만들어짐
- 형식에 들어가는 `<동사>` · `<대상>`은 1장 단어로 구성. 형식 말(`UseCase` · `Port` · `Persistence` · `JpaEntity` · `Embeddable` 등)은 단어 규칙 밖

| 위치 | 형식 | 시험 | 단어 |
|---|---|---|---|
| 들어오는 포트 | `<동사><대상>UseCase` (응시 묶음은 `<대상>UseCase`, 조회만 하는 포트는 `<대상>Query`) | `StartExamUseCase` · `ExamUseCase` · `CleanUpExamsUseCase` · `ExamQuery` · `ExamLevelOverviewQuery` | `LoadWordCatalogUseCase` · `WordCatalogQuery` |
| 서비스 | 포트 이름의 `UseCase` → `Service` (조회만 하면 `<대상>QueryService`) | `StartExamService` · `ExamService` · `CleanUpExamsService` · `ExamQueryService` · `ExamLevelOverviewQueryService` | `LoadWordCatalogService` · `WordCatalogQueryService` |
| 입력 | `<동사><대상>Command` (상태를 변경하는 요청만. 아이디만 전달하면 인자로 받음) | `StartExamCommand` · `SubmitExamCommand` | — |
| 웹 요청 본문 | `<동사><대상>Request` (웹 어댑터 전용. 주소 · 쿠키 값이 섞여 본문만으로 Command를 만들 수 없을 때 JSON 본문만 받음) | `SubmitExamRequest` | — |
| 결과 | `<대상>Result` (담은 내용의 이름. 이름이 겹치면 상태를 변경하는 쪽에 동사를 붙임) | `ExamQuestionsResult` · `SubmitExamResult` · `ExamResult` | — |
| 결과 안의 항목 · 목록 원소 | 담은 내용의 이름 | `QuestionItem` · `Option` · `WrongItem` · `ExamLevelOverview` · `ExamLevelOverviewGroup` | — |
| 입력 안의 항목 · 목록 원소 | `<대상>Item` (도메인 값과 이름이 겹치지 않도록) | `SubmitExamCommand.SelectedOptionItem` (제출 JSON `selectedOptions[]`의 한 항목 — 도메인은 `SelectedOption`) | — |
| 나가는 포트 | `<대상><외부 시스템>Port` (DB = `Persistence`) | `ExamPersistencePort` | `WordPersistencePort` |
| 나가는 포트 (메모리, 조회 · 교체 분리) | `<동사><대상>Port` | — | `GetWordCatalogPort` · `ReplaceWordCatalogPort` |
| 영속성 어댑터 | `<대상>PersistenceAdapter` | `ExamPersistenceAdapter` | `WordPersistenceAdapter` |
| 메모리 어댑터 | `<대상>MemoryAdapter` (메모리 조회 · 교체 포트를 함께 구현하는 빈 하나. 필드는 `final AtomicReference`로 두고 내부 값만 통째로 교체) | — | `WordCatalogMemoryAdapter` |
| DB 저장용 클래스 | `<대상>JpaEntity` | `ExamJpaEntity` | `WordJpaEntity` · `LevelWordJpaEntity` 등 |
| DB 저장용 묶음 (한 테이블의 칼럼 몇 개를 값 하나로 묶음, JPA `@Embeddable`) | `<대상>Embeddable` (도메인 값 객체를 그대로 옮긴 레코드. 같은 이름이면 엔티티 안에서 도메인 클래스와 충돌하므로 접미사를 붙임 — JPA에는 정해진 이름 관례가 없음) | `ExamineeEmbeddable` · `ExamTimeEmbeddable` · `QuestionsEmbeddable` · `SelectedOptionsEmbeddable` · `GradingResultEmbeddable` | — |
| Spring Data 저장소 | `SpringData<대상>Repository` | `SpringDataExamRepository` | `SpringDataLevelWordRepository` 등 |
| 매퍼 (여러 위치의 행을 모아 조립할 때만) | `<대상>Mapper` | — (`ExamJpaEntity`의 `from` · `toDomain`) | `LevelWordMapper` |
| 웹 어댑터 | `<대상>Controller` (화면) · `<대상>ApiController` (API) | `ExamController` · `ExamApiController` · `MainController` | — |
| 웹 필터 (HTTP 기술 요소도 웹 어댑터 쪽. 모든 요청 대상이면 `common`, 시험 전용이면 `exam`) | `<대상><역할>Filter` · 모든 요청 대상이면 `<역할>Filter` | `ExamNoStoreFilter` (시험 화면 · API를 브라우저가 저장하지 않도록 지정) | — (공통: `OriginVerifyFilter` · `SecurityHeadersFilter`) |
| 스케줄러 · 설정 | `<대상><동사>Scheduler` · `<대상>Properties` (서비스가 받는 순수 값) | `ExamCleanupScheduler` · `ExamProperties` | — |
| 시작 어댑터 | `<대상>Initializer` (서버가 요청을 받기 전에 1회 실행 — `SmartInitializingSingleton`) | — | `WordCatalogInitializer` |
| 설정 연결 (`config`) | `<대상>Config` (빈 등록) · `<대상>ConfigProperties` (설정 파일 연결) | `ExamConfig` · `ExamConfigProperties` | — |
| 전용 애노테이션 | `@UseCase` · `@WebAdapter` · `@PersistenceAdapter` (`common`. 웹 어댑터는 `@Controller` · `@RestController`와 함께 사용, 영속성 어댑터 애노테이션은 DB 예외 변환을 위해 `@Repository` 기반) | `ExamService` · `ExamController` · `ExamPersistenceAdapter` | `LoadWordCatalogService` · `WordPersistenceAdapter` (메모리 · 시작 어댑터는 DB 예외 변환이 필요 없어 `@Component`) |

- 결과 객체의 필드 이름 = JSON 필드 이름. 화면(JSON · `exam.js` · 템플릿)도 사전 이름을 사용
- **단어 쪽 흐름**
  - 애플리케이션 기동 시 시작 어댑터(`WordCatalogInitializer`)가 단어 출제 목록 적재(`LoadWordCatalogUseCase`)를 호출
  - 서비스가 영속성 어댑터로 조회(조회만)하고, 단어 노출을 적용한 목록으로 메모리 어댑터의 목록을 교체
  - 시험 쪽은 단어 출제 목록 조회(`WordCatalogQuery`)로 요청마다 현재 목록을 가져옴
  - 시작 어댑터도 다른 들어오는 어댑터처럼 서비스를 거쳐 호출하며, 단어 노출 적용은 영속성 어댑터가 아닌 서비스에서 수행
- **도메인 객체를 만드는 메서드**
  - 저장된 값으로 도메인을 복원할 때는 6-1 `of` 사용 (`Exam.of` · `Questions.of` · `SelectedOptions.of`)
    - 책의 `withId` 대신 `of`를 사용 (대응하는 `withoutId`가 없으므로)
  - 새로 만들 때는 업무 용어를 사용 (`Exam.start`)

## 7. 혼동하기 쉬운 말

- **시험 · 테스트 · 세션**
  - 시험 = `Exam`. '테스트'는 자동 테스트 · QA만 지칭
  - '세션'은 브라우저 세션(HttpSession)만 지칭. 시험을 세션이라 부르지 않음 (외부 이름 `test_session`은 예외)
- **문항 · 문제 · 이슈**
  - 문항 = 문제. 이름에는 '문항', 화면 · 문장에서는 '문제'도 사용 (문제당 5초)
  - 단어 노출의 이슈 유형은 문항과 다른 개념
- **허용 시간 · 문항당 시간 · 마감**
  - 허용 시간 = 시험 **전체**의 시간 (500문항이면 43분, `화면: 제한 시간`)
  - 문항당 시간 = **한 문항**의 시간 (5초)
  - 마감 = 허용 시간이 끝나는 **시각**
  - 코드에는 time limit(제한 시간)이라는 말을 사용하지 않음. DB 칼럼 `time_limit_sec`만 문항당 시간을 의미 (2-9)
- **만료 · 중단 · 종료 · 정리**
  - 만료 = **기한이 지남**. 시험은 진행 중 상태에서 마감이 지난 경우(상태로 저장하지 않음), 결과는 결과 보관 기간이 지난 경우
  - 중단 = 제출하지 않은 진행 중 시험을 종료하고 **즉시 삭제** (중도 이탈 · 그만두기 · 같은 기기의 새 시작)
  - 종료 = 진행 중이 아니게 된 경우 전체 (완료 · 만료 · 중단). 화면 문구 "시험이 종료되었습니다"는 중단 후에 표시
  - 정리 = 정리 주기마다 폐기 대상 시험을 삭제
- **정답 · 맞힘**
  - 정답 = 문항의 올바른 보기 (정답 보기 번호 · 정답 글)
  - 맞힘 = 선택한 보기가 정답임 (맞힌 수). 결과 화면 기록 카드의 '정답' 항목은 맞힌 수
- **틀림 · 오답**
  - 틀림 = 맞히지 못함 = 오답 + 시간 초과 (틀린 항목, 화면 '틀린 단어')
  - 오답 = 정답이 아님. 오답 수 = 보기를 선택했으나 정답이 아닌 문항 수, 오답 보기 = 정답이 아닌 보기
- **응시자 · 사용자**
  - 응시자(`examinee`) = 시험을 시작한 주체 (기기 아이디 + 사용자 IP)
  - 사용자(`client`) = 현재 접속한 주체
  - 응시자를 담는 변수 · 메서드를 `client`로 부르지 않음
- **최대 · 수용량**
  - 최대 = 설정으로 정한 진행 중 시험 수의 최대값
  - 수용량 확인 = 현재 진행 중 시험 수가 최대를 넘는지 확인하는 작업
- **묶음**: 묶음 레벨(A/B/C) · 묶음 이름(Basic …) · 혼동 단어 묶음은 서로 다른 개념. 항상 앞 단어와 함께 사용
- **단어 · 레벨 단어 · 출제 목록 단어**
  - 단어 = `word` 1행 (표제어)
  - 레벨 단어 = `level_word` 1행 (단어 × 세부 레벨)
  - 출제 목록 단어 = 레벨 단어에 단어 노출을 적용해 메모리에 올린 값
  - 메인 화면의 'N 단어'는 정답 가능한 출제 목록 단어 수
- **뜻 · 보기 뜻 · 보기 글**
  - 뜻 = 단어의 뜻 전체 (최대 5개)
  - 보기 뜻 = 레벨 단어마다 보기에 표시할 1 ~ 2개
  - 보기 글 = 보기 뜻을 이어 붙인 화면 문구
- **적재 · 단어 데이터 가져오기**
  - 적재 = DB의 단어 데이터를 메모리의 단어 출제 목록으로 올림 (애플리케이션 기동 시)
  - 단어 데이터 가져오기 = 원본 CSV를 DB에 넣는 작업 (애플리케이션 밖)
- **문제 기록 번호**: 커밋 메시지 · 코드 주석의 `(U55)` · `(B3)` 같은 번호는 브라우저 테스트 · 코드 검토 중 발견한 문제의 기록 번호 (B 버그 · U 사용성/설계 · D 데이터). 기록 자체는 비공개
