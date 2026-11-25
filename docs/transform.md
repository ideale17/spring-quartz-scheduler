# Transform 데이터 가공 설계

## 1. 개요

Dynamic Scheduler의 Transform 기능은 External API에서 수집한 Raw Data(JSON/XML)에 가공 규칙을 적용하고 결과를 저장한다.

데이터 수집과 가공은 별도의 처리 흐름으로 분리되어 있다.

주요 기능:

- 가공 규칙 등록·수정 및 버전 관리
- JSON/XML 데이터의 필드 추출 및 타입 변환
- Raw Data별 가공 작업 등록
- Quartz Worker 기반 가공 작업 실행
- 대기 작업의 동시성 제어
- 가공 결과 및 실행 상태 관리
- Raw Data별 가공 이력 조회

## 2. 주요 구성 요소

- `TransformRuleService`: 가공 규칙 등록·수정·버전 관리
- `TransformTaskService`: 가공 작업 등록 및 이력 조회
- `TransformWorkerJob`: Quartz 기반 가공 실행 진입점
- `TransformWorkerService`: 대기 작업 선점 및 순차 처리
- `TransformTaskStateService`: 작업 선점 및 실패 상태 관리
- `TransformTaskExecutionService`: 개별 가공 작업 실행 및 결과 저장
- `TransformEngineService`: JSON/XML 추출 및 타입 변환
- `TransformRuleMapper`: 가공 규칙 및 버전 조회·저장
- `TransformTaskMapper`: 가공 작업 조회·저장 및 상태 변경
- `TransformResultMapper`: 가공 결과 저장 및 조회

Quartz Job은 실행 진입점 역할을 담당하며 실제 가공 처리는 Service에 위임한다.

## 3. 가공 규칙 및 버전 관리

`TransformRuleService`에서 가공 규칙을 관리한다.

### 규칙 등록

`createTransformRule()`을 사용한다.

1. 규칙 기본 정보와 추출 필드를 검증한다.
2. 가공 규칙을 등록한다.
3. 최초 버전 1을 생성한다.
4. 해당 버전의 추출 필드를 등록한다.

### 규칙 수정

`updateTransformRule()`을 사용한다.

- 규칙의 현재 버전과 수정 요청의 기준 버전을 비교한다.
- 규칙명, 설명, 사용 여부 등 기본 정보는 기존 규칙에 반영한다.
- 데이터 형식, 반복 경로 또는 추출 필드가 변경되면 새 버전을 생성한다.
- 가공 설정이 변경되지 않았다면 불필요한 새 버전을 생성하지 않는다.

가공 작업 등록 시 사용할 규칙 버전을 확정한다.

**기존에 등록된 작업은 이후 규칙이 수정되더라도 작업에 저장된 버전을 기준으로 실행한다.**

## 4. 가공 작업 등록

현재 수동 가공 작업 등록은 `TransformTaskService.createManualTransformTasks()`에서 처리한다.

1. 가공 규칙과 Raw Data 식별자를 검증한다.
2. 가공 규칙의 현재 버전과 사용 여부를 확인한다.
3. Raw Data의 존재 여부 및 원본 응답을 검증한다.
4. 동일 Raw Data와 규칙 버전의 중복 작업 여부를 확인한다.
5. 작업을 `PENDING` 상태로 등록한다.

현재 적용하는 주요 제약:

- 한 번에 최대 100건의 수동 작업 등록
- 동일 요청 내 Raw Data 중복 선택 불가
- 서로 다른 External API의 Raw Data 혼합 등록 불가
- 사용 중지된 규칙으로 작업 등록 불가
- 동일 Raw Data·규칙 버전의 PENDING, RUNNING, SUCCESS 작업 중복 등록 제한

작업 등록과 실제 가공 실행은 분리되어 있다.

## 5. 가공 작업 처리 흐름

가공 작업은 Quartz의 `TransformWorkerJob`을 통해 처리한다.

```text
가공 작업 등록 (PENDING)
          |
          v
TransformWorkerJob 실행
          |
          v
대기 작업 일괄 선점
          |
          v
작업 상태 변경 (RUNNING)
          |
          v
규칙 버전 및 Raw Data 조회
          |
          v
JSON/XML 데이터 가공
          |
     +----+----+
     |         |
     v         v
  성공 처리   실패 처리
     |         |
     v         v
  SUCCESS    FAILED
```

### 작업 상태

- `PENDING`: 가공 대기
- `RUNNING`: Worker가 선점하여 처리 중
- `SUCCESS`: 가공 결과 저장 완료
- `FAILED`: 가공 실행 실패

현재 주요 상태 전이는 `PENDING → RUNNING → SUCCESS/FAILED`이다.

작업 선점과 실제 실행은 별도 단계로 처리한다.

## 6. Quartz Worker 및 동시성

`TransformWorkerJob`은 `TransformWorkerService.processBatch()`를 호출한다.

Job 실행 파라미터 `BATCH_SIZE`로 한 번의 Worker 실행에서 선점할 최대 건수를 1~100 사이로 지정한다. 파라미터를 설정하지 않은 Job의 기본값은 50건이며, 선점한 작업은 순차적으로 처리한다.

### 동시 실행 제어

- `TransformWorkerJob`에 `@DisallowConcurrentExecution`을 적용한다.
- 동일한 Quartz JobKey의 동시 실행을 제한한다.
- 서로 다른 JobKey나 Worker 사이의 작업 중복 선점은 별도 제어가 필요하다.
- `TransformTaskStateService.claimPendingTasks()`가 대기 작업을 원자적으로 선점한다.
- 다른 트랜잭션이 잠근 작업은 건너뛰도록 처리한다.

Quartz 동시 실행 제한과 작업 선점은 서로 다른 역할을 담당한다.

### 트랜잭션 분리

- 작업 선점: `TransformTaskStateService.claimPendingTasks()` — `REQUIRES_NEW`
- 개별 작업 실행: `TransformTaskExecutionService.executeTask()` — `@Transactional`
- 실패 상태 기록: `TransformTaskStateService.markFailed()` — `REQUIRES_NEW`

선점 상태는 실제 가공 전에 확정된다.

개별 작업에서 오류가 발생해도 다른 선점 작업은 계속 처리한다.

동시성 제어에 사용하는 구체적인 SQL과 트랜잭션 설계는 `docs/database.md`에서 관리한다.

## 7. JSON/XML 데이터 가공

`TransformEngineService`가 JSON 및 XML 데이터를 처리한다.

### JSON

- Jackson 기반으로 원본 JSON을 파싱한다.
- 점(`.`)으로 구분된 경로로 값을 추출한다.
- 반복 경로가 설정되면 해당 경로의 배열을 처리한다.
- 반복 경로가 없으면 최상위 객체 또는 배열을 처리한다.

### XML

- XML 파서로 원본 데이터를 읽는다.
- 점(`.`)으로 구분된 요소 경로를 사용한다.
- 반복 경로에 해당하는 요소를 순차 처리한다.
- XML 가공 결과는 JSON 배열로 생성한다.
- XML 외부 엔터티와 DTD 접근은 차단한다.

현재 경로 처리는 범용 JSONPath/XPath 전체 문법이 아니라 프로젝트에서 구현한 단순 경로 탐색 방식이다.

### 필드 타입 변환

지원하는 데이터 타입:

- `STRING`
- `INTEGER`
- `DECIMAL`
- `BOOLEAN`
- `DATETIME`

DATETIME은 현재 `EPOCH_MILLIS`와 `ISO_DATETIME` 형식을 지원한다.

필수 필드가 없거나 변환할 수 없는 값이면 오류를 발생시킨다. 선택 필드의 값이 없으면 JSON null로 처리한다.

## 8. 가공 결과 및 실패 처리

### 가공 결과 저장

`TransformTaskExecutionService.executeTask()`에서 처리한다.

1. RUNNING 상태의 가공 작업을 확인한다.
2. 원본 Raw Data를 조회한다.
3. 작업에 확정된 가공 규칙 버전과 추출 필드를 조회한다.
4. JSON/XML 가공 엔진을 실행한다.
5. 가공 결과를 저장한다.
6. 작업 상태를 SUCCESS로 변경한다.

가공 결과 저장과 SUCCESS 상태 변경은 동일한 트랜잭션에서 처리한다.

### 실패 처리

`TransformWorkerService`에서 개별 작업의 예외를 처리한다.

- 실패한 작업의 실행 트랜잭션을 롤백한다.
- 별도 트랜잭션에서 FAILED 상태와 오류 메시지를 기록한다.
- 실패 상태 기록 중 오류가 발생하면 별도 로그를 남긴다.
- 다른 선점 작업의 처리는 계속 진행한다.

현재 확인된 Worker 흐름에는 비정상 종료 후 RUNNING 상태로 남은 작업을 자동 복구하는 처리가 명시되어 있지 않다.

## 9. 가공 이력 및 조회

`TransformTaskService.getTransformTaskHistory()`는 Raw Data별 가공 작업 이력을 조회한다.

주요 조회 정보:

- 가공 작업 식별자
- Raw Data 식별자
- 가공 규칙명 및 버전
- 작업 상태
- 실행 시도 횟수
- 오류 메시지
- 실행 시작·종료 시각
- 가공 결과 식별자

가공 결과는 별도 조회 기능을 통해 목록 및 상세 내용을 확인한다.

동일 Raw Data라도 서로 다른 규칙 버전을 사용한 가공 결과는 구분하여 관리한다.

## 10. 변경 시 확인할 사항

Transform 기능을 변경할 때 다음 사항을 확인한다.

- 가공 규칙 수정 시 버전 생성 기준
- 기존 작업에 확정된 규칙 버전 유지
- Raw Data별 중복 작업 검증
- PENDING 작업의 원자적 선점
- Quartz JobKey별 동시 실행 제한
- 작업 선점과 실제 가공의 트랜잭션 분리
- 개별 작업 실패 시 다른 작업의 처리 지속
- 가공 결과 저장과 SUCCESS 상태 변경의 원자성
- JSON/XML 경로 탐색 및 데이터 타입 변환
- 작업 상태와 가공 이력의 일관성
- 기존 프론트엔드 가공 작업·결과 조회 기능과의 호환성

실패 작업 재처리나 자동 복구 기능을 추가하는 경우 기존 상태 전이와 트랜잭션 구조에 미치는 영향을 확인한다.

코딩 스타일과 일반적인 AI 작업 절차는 `AGENTS.md` 및 `docs/coding-conventions.md`를 따른다.
