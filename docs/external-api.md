# External API 수집 설계

## 1. 개요

Dynamic Scheduler의 External API 기능은 외부 시스템의 데이터를 수집하고 원본 데이터와 실행 이력을 PostgreSQL에 저장한다.

주요 기능:

- 외부 API 설정 및 인증정보 관리
- 정적·동적 파라미터 처리
- LIST / RANGE 기반 반복 요청
- HTTP 요청 및 재시도
- 페이지 단위 데이터 수집
- 실행·요청·호출 이력 관리
- JSON / XML Raw Data 저장
- 실행 스냅샷 기반 실패 재처리

Quartz Job을 통한 예약 실행과 별도의 직접 실행 흐름에서 공통 수집 Service를 사용한다.

## 2. 주요 구성 요소

### API 설정 및 실행
- `ExternalApiService`: API 설정 등록·조회·수정·삭제 및 검증
- `ExternalApiCallJob`: Quartz 기반 수집 실행 진입점
- `ExternalApiExecutionService`: 외부 API 전체 실행 흐름

### 파라미터 처리
- `DynamicParameterResolver`: 동적 날짜·시간 파라미터 계산
- `ExternalApiParameterCombinationService`: LIST / RANGE 조합 생성

### 실행 이력 및 데이터 저장
- `ExternalApiExecutionLogService`: 실행 단위 이력 관리
- `ExternalApiRequestLogService`: 논리적 요청과 파라미터 이력
- `ExternalApiCallLogService`: HTTP 호출 시도별 이력
- `ExternalApiExecutionSnapshotService`: 실행 설정 스냅샷 및 복원
- `CollectRawDataService`: HTTP 응답 원본 저장

## 3. 데이터 수집 흐름

기본 실행 진입점은 `ExternalApiExecutionService.execute()`이다.

```text
External API 실행
       |
       v
API 설정 및 파라미터 조회
       |
       v
실행 ID 생성 및 실행 이력 시작
       |
       v
실행 설정 스냅샷 저장
       |
       v
동적 파라미터 계산
       |
       v
LIST / RANGE 조합 생성
       |
       v
조합별 페이징 및 HTTP 요청
       |
       +-- 실패 시 재시도 정책 적용
       |
       v
요청 이력 / 호출 이력 / Raw Data 저장
       |
       v
실행 성공 또는 실패 상태 기록
```

- 실행마다 `executionId`를 생성한다.
- Quartz에서 실행된 경우 `fireInstanceId`를 함께 전달한다.
- API 설정 및 파라미터는 실행 시작 시 조회한다.
- 조합별 요청은 순차적으로 처리한다.
- 수집 결과와 호출 상태는 관련 Service를 통해 저장한다.

## 4. 파라미터 처리

### 값 유형

`DynamicParameterResolver`는 다음 유형을 처리한다.

- `STATIC`: 등록된 고정값 사용
- `CURRENT_DATE`: 실행 기준 날짜를 형식에 따라 변환
- `CURRENT_TIME`: 실행 기준 시간을 형식에 따라 변환
- `CURRENT_DATETIME`: 실행 기준 날짜·시간을 형식에 따라 변환

동적 파라미터는 실행 기준 시각과 `valueFormat`, `offsetUnit`, `offsetValue`를 이용한다.

### 반복 방식

`ExternalApiParameterCombinationService`에서 처리한다.

- `NONE`: 단일 값
- `LIST`: 등록된 고정값 목록 반복
- `RANGE`: 날짜 범위를 일정 간격으로 반복

현재 LIST는 STATIC 값만, RANGE는 CURRENT_DATE 값만 허용한다.

RANGE는 DAY / MONTH / YEAR 단위의 반복을 지원한다. 기존 오프셋 적용 후 반복 범위를 계산한다.

각 파라미터 조합은 HEADER, QUERY, BODY 위치별로 구성한다.

**현재 최대 파라미터 조합 수는 100개다.** 여러 반복 파라미터를 사용하는 경우 조합의 곱이 이 제한을 초과하지 않아야 한다.

동일한 위치에 같은 이름의 파라미터를 중복 등록하는 것은 허용하지 않는다.

## 5. 페이징 처리

`ExternalApiExecutionService.executePagePagination()`을 사용한다.

현재 페이징 구현은 다음 방식을 지원한다.

- 페이징 유형: `PAGE`
- 종료 판단: `TOTAL_COUNT`
- 페이지 번호 및 페이지 크기를 QUERY 또는 BODY에 전달
- JSON/XML 응답에서 전체 데이터 건수 추출

페이징 흐름:

1. 현재 조합의 시작 페이지를 설정한다.
2. 페이지 번호와 크기를 요청 파라미터에 적용한다.
3. HTTP 요청을 실행한다.
4. 응답에서 전체 건수를 조회한다.
5. 마지막 페이지이면 종료하고, 아니면 다음 페이지로 이동한다.

파라미터 조합마다 페이지 번호를 새로 초기화한다.

전체 실행의 `requestSequence`는 조합이 바뀌어도 계속 증가한다.

현재 전체 실행에서 허용하는 최대 논리적 요청 수는 1,000개다. 별도로 페이징 설정의 `maxRequestCount`도 적용한다.

## 6. HTTP 재시도

`ExternalApiExecutionService.executeRequest()`에서 재시도한다.

최대 시도 횟수는 다음과 같다.

```text
최대 시도 횟수 = 1 + maxRetryCount
```

재시도 설정이 비활성화된 경우 한 번만 시도한다.

현재 재시도 대상:

- 통신 오류 (`ResourceAccessException`)
- HTTP 408
- HTTP 429
- HTTP 5xx

그 외 오류는 기본적으로 재시도하지 않는다.

재시도 간격은 설정된 `retryIntervalSec`을 사용한다.

동일한 논리적 요청의 재시도는 기존 `requestId`를 공유하며, 각 호출 시도는 별도 이력으로 기록된다.

## 7. 실행 및 요청 이력

수집 이력은 세 가지 수준으로 구분한다.

| 구분        | 의미                  |
| --------- | ------------------- |
| Execution | External API 전체 실행  |
| Request   | 파라미터 조합·페이지별 논리적 요청 |
| Call      | 실제 HTTP 호출 및 재시도    |

주요 식별자:

- `executionId`: 실행 단위 식별자
- `requestId`: 논리적 요청 식별자
- `requestSequence`: 실행 내 요청 순번
- `combinationNo`: 파라미터 조합 순번
- `pageNo`: 페이징 요청 번호
- `attemptNo`: 실제 호출 시도 번호
- `fireInstanceId`: Quartz 실행 식별자

요청 이력에는 실제 Query / Body 파라미터가 저장되며 민감정보는 기존 마스킹 정책을 적용한다.

호출 이력에는 HTTP 상태, 실행 시간, 성공·실패 정보가 기록된다.

성공한 HTTP 응답은 Raw Data로 저장하고 `requestId` 및 `executionId`와 연결한다.

## 8. 실행 스냅샷 및 재처리

### 실행 스냅샷

일반 실행 시 `ExternalApiExecutionSnapshotService.saveSnapshot()`을 사용한다.

스냅샷에는 다음 정보가 포함된다.

- API URL 및 HTTP Method
- 재시도 설정
- 파라미터 및 반복 설정
- 페이징 설정
- 실행 기준 시각
- 스냅샷 버전

현재 스냅샷 버전은 1이다.

API 인증 비밀값은 스냅샷의 API 인증 설정에서 제외한다. 다만 일반 파라미터 값은 저장되므로, 민감한 값이 포함될 가능성도 별도로 검토해야 한다.

### 실패 재처리

`ExternalApiExecutionService.restart()`를 사용한다.

1. 재처리 대상 실행의실패 상태를 확인한다 .
2. 원본 실행 관계를 따라 최초 실행을 찾는다.
3. 최초 실행의 설정 스냅샷을 조회한다.
4. 당시 API 설정·파라미터·페이징 설정을 복원한다.
5. 현재 유효한 인증 설정 및 비밀값을 적용한다.
6. 최초 실행 기준 시각을 사용하여 전체 수집을 처음부터 실행한다.

현재 재처리 방식은 `RESTART`다.

**실패한 요청만 다시 호출하는 방식이 아니라 전체 수집을 처음부터 재실행한다.**

재처리는 새로운 `executionId`를 생성하고 원본 실행과 요청 관계를 이력에 기록한다.

외부 API 설정이 변경되어도 원본 스냅샷을 사용하되, 현재 인증정보와 사용 여부는 별도로 확인한다.

## 9. 인증 및 보안

지원하는 인증 방식:

- `NONE`
- `API_KEY` (HEADER / QUERY)
- `BEARER`
- `BASIC`

민감한 인증정보는 기존 `AesGcmEncryptionService`를 통해 암호화하여 저장한다.

실행 과정에서는 다음 규칙을 유지한다.

- 실제 인증정보를 로그에 노출하지 않는다.
- 요청 이력에 민감 파라미터를 저장할 때 마스킹한다.
- 외부 API URL은 등록 및 실행 과정에서 검증한다.
- HTTP / HTTPS 프로토콜을 사용한다.
- 로컬 및 내부 네트워크 주소에 대한 기존 차단 정책을 유지한다.
- 이미 인코딩된 API Key의 이중 인코딩에 주의한다.
- 외부 응답은 신뢰할 수 없는 데이터로 취급한다.

URL 검증 변경 시 SSRF, DNS 재확인 및 리다이렉트 처리에 미치는 영향을 검토한다.

## 10. 변경 시 확인할 사항

External API 수집 기능 변경 시 다음을 확인한다.

- 동적 파라미터 계산 기준 시각
- LIST / RANGE 조합 수와 중복 검증
- 조합별 페이징 초기화
- 전체 요청 순번 및 요청 수 제한
- HTTP 재시도 대상과 최대 시도 횟수
- Execution / Request / Call / Raw Data 연관 관계
- 요청 파라미터 마스킹
- 스냅샷 버전 및 기존 데이터 호환성
- 재처리 시 원본 설정과 현재 인증정보의 적용 범위
- 데이터 중복 저장 및 외부 호출의 멱등성
- 기존 프론트엔드 API 계약

설계를 변경할 경우 정상 실행뿐 아니라 부분 실패, 재시도, 전체 재처리 상황을 함께 검증한다.

일반적인 코드 작성 규칙은 `AGENTS.md` 및 `docs/coding-conventions.md`를 따른다.
