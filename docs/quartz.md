# Quartz Scheduler 설계

## 1. 개요

Dynamic Scheduler는 Quartz와 PostgreSQL JDBC JobStore를 사용하여 Job과 Trigger를 동적으로 관리한다.

주요 기능:

- Job 등록, 수정, 삭제
- CRON / SIMPLE 스케줄 관리
- Job 즉시 실행, 중지 및 재개
- 여러 Job 일괄 제어
- Misfire 정책 관리
- DB 기반 스케줄 영속화
- Job 실행 이력 저장 및 조회

## 2. 주요 구성 요소

- `DynamicJobController`: Job 관리 REST API
- `DynamicJobService`: Job 및 Trigger 등록·수정·제어
- `DynamicJobMapper`: Job 정보 및 실행 이력 조회
- `JobClassRegistry`: 실행 가능한 Job 클래스 관리
- `ExternalApiCallJob`: 외부 API 수집 실행
- `TransformWorkerJob`: Raw Data 가공 작업 실행
- `JobLogListener`: Job 실행 시작·종료·실패 이력
- `QuartzListenerConfig`: 전역 JobListener 등록

Quartz Job은 실행 진입점 역할을 담당하며 실제 업무 처리는 Service에 위임한다.

## 3. Job 및 Trigger 관리

### Job 등록

`DynamicJobService.addJob()`을 기준으로 처리한다.

1. `JobClassRegistry`에서 등록 가능한 Job 클래스를 조회한다.
2. 요청 파라미터로 `JobDataMap`을 생성한다.
3. `JobDetail`과 Trigger를 생성한다.
4. 기존 Job의 중복 여부를 확인한다.
5. `scheduler.scheduleJob()`으로 등록한다.

Job은 `jobName`과 `jobGroup`으로 식별한다.

Trigger는 현재 일반 등록 흐름에서 `jobName + "Trigger"`와 `jobGroup`을 사용한다.

### Job 수정

`updateSchedule()`을 사용한다.

- 기존 Job 및 Trigger의 존재 여부를 확인한다.
- 새로운 스케줄과 Misfire 정책을 적용한다.
- 파라미터가 전달되면 JobDataMap을 갱신한다.
- `scheduler.rescheduleJob()`으로 Trigger를 교체한다.
- 기존 Trigger가 PAUSED 상태였다면 변경 후 중지 상태를 복원한다.

스케줄 수정 시 기존 파라미터와 Trigger 상태가 의도치 않게 변경되지 않도록 주의한다.

### Job 실행 및 상태 제어

- 즉시 실행: `scheduler.triggerJob()`
- 일시 중지: `scheduler.pauseJob()`
- 재개: `scheduler.resumeJob()`
- 삭제: `scheduler.deleteJob()`

일괄 실행·중지·재개는 Job별 결과와 성공·실패 건수를 반환한다.

즉시 실행 요청의 성공은 실제 Job 처리 완료를 의미하지 않는다.

## 4. Job 클래스 관리

`JobClassRegistry`는 실행 가능한 Job 클래스를 관리한다.

현재 등록된 클래스:

- `ExternalApiCallJob`
- `TransformWorkerJob`

등록되지 않은 Job 클래스는 실행 대상으로 사용할 수 없다.

새로운 Job을 추가할 경우 Registry 등록 여부와 기존 Quartz Job 생성 방식을 확인한다.

사용자가 입력한 클래스 이름으로 임의의 Java 클래스를 로딩하지 않는다.

## 5. 스케줄 및 Misfire

### 스케줄 유형

- `CRON`: Quartz Cron 표현식
- `SIMPLE`: 초 단위 반복 간격

SIMPLE은 현재 무한 반복 방식인 `repeatForever()`를 사용한다.

### Misfire

Misfire는 예정된 실행 시점을 놓쳤을 때 Quartz가 적용하는 처리 정책이다.

현재 Misfire 판단 임계값은 `application.properties`에 60,000ms로 설정되어 있다.

**CRON**

- `SMART_POLICY`
- `FIRE_AND_PROCEED`
- `DO_NOTHING`

**SIMPLE**

- `SMART_POLICY`
- `FIRE_NOW`
- `NOW_WITH_EXISTING_COUNT`
- `NOW_WITH_REMAINING_COUNT`
- `NEXT_WITH_EXISTING_COUNT`
- `NEXT_WITH_REMAINING_COUNT`

실제 적용 방식은 `DynamicJobService`의 스케줄 생성 로직을 기준으로 한다.

SMART_POLICY는 별도 Misfire 명령을 지정하지 않고 Quartz 기본 동작을 사용한다.

SIMPLE의 반복 횟수 관련 정책은 현재 `repeatForever()` 설정을 고려하여 검증한다.

Misfire 정책 변경 시 서버 중단·재시작, 지연 실행, 다음 실행 시각에 미치는 영향을 확인한다.

## 6. JDBC JobStore

주요 설정:

```properties
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=never
spring.quartz.scheduler-name=MyQuartzScheduler
spring.quartz.properties.org.quartz.jobStore.misfireThreshold=60000
```

PostgreSQL 프로필에서는 `PostgreSQLDelegate`를 사용한다.

- Job과 Trigger 메타데이터는 Quartz JDBC JobStore에 저장된다.
- 스케줄 정보는 애플리케이션 재시작 이후에도 DB에 유지된다.
- Quartz 테이블은 자동으로 생성하거나 초기화하지 않는다.
- Job 정보 변경은 기존 Scheduler API를 사용한다.
- Quartz 내부 테이블을 직접 UPDATE하거나 DELETE하지 않는다.

초기 DB 설정은 `src/main/resources/db/postgresql/01_quartz.sql`을 참고한다.

운영 DB에서는 초기화 SQL을 임의로 재실행하지 않는다.

## 7. Job 실행 구조

### ExternalApiCallJob

1. JobDataMap에서 `externalApiId`를 확인한다.
2. Quartz의 `fireInstanceId`를 조회한다.
3. `ExternalApiExecutionService.execute()`에 처리를 위임한다.
4. 실행 오류는 `JobExecutionException`으로 전달한다.

외부 API의 재시도, 페이징, 요청 이력, Raw Data 저장은 별도의 수집 Service에서 관리한다.

### TransformWorkerJob

1. `TransformWorkerService.processBatch()`를 호출한다.
2. 대기 중인 가공 작업을 선점하여 처리한다.
3. 처리 건수를 로그에 기록한다.
4. Worker 전체 실행 오류는 `JobExecutionException`으로 전달한다.

Job 실행 파라미터에 `BATCH_SIZE`를 설정하면 실행 한 번의 최대 선점 건수를 지정할 수 있다. 키를 생략하면 기본값 50건을 사용한다. 등록·수정·실행 시 1~100 사이 정수인지 확인하며, 잘못된 값은 거절한다.

개별 가공 작업의 트랜잭션과 실패 처리는 Transform Service에서 담당한다.

상세 내용은 `docs/transform.md`에서 관리한다.

## 8. 동시 실행 제어

현재 두 Job 클래스에 `@DisallowConcurrentExecution`이 적용되어 있다.

- 동일한 JobDetail(JobKey)의 동시 실행을 제한한다.
- 서로 다른 JobKey 사이의 동시 실행까지 전역적으로 차단하는 것은 아니다.
- 하나의 외부 API를 여러 Job에 연결한 경우 별도의 중복 실행 가능성을 검토해야 한다.
- Transform 작업의 중복 선점은 별도 DB 동시성 제어로 방지한다.

Quartz의 동시 실행 제한과 DB 작업 선점은 서로 다른 책임을 가진다.

## 9. 실행 이력

`JobLogListener`가 Quartz 실행 이벤트를 처리한다.

`QuartzListenerConfig`에서 전역 JobListener로 등록한다.

- `jobToBeExecuted`: 시작 이력 저장
- `jobWasExecuted`: 성공·실패 상태와 실행 시간 기록
- `jobExecutionVetoed`: 실행 거부 상태 기록

Job 실행 이력은 `SCHED_EXEC_LOG` 테이블에 저장한다.

주요 정보:

- `FIRE_INSTANCE_ID`
- `JOB_NAME`, `JOB_GROUP`
- 예정 실행 시각과 실제 실행 시각
- 종료 시각과 실행 소요 시간
- 상태와 예외 메시지

`fireInstanceId`는 실행별 로그 추적에 사용하며, MDC의 `fireId`와도 연계된다.

Job 실행 이력과 External API의 요청·호출 이력은 서로 다른 데이터이므로 구분하여 관리한다.

## 10. 변경 시 확인할 사항

Quartz 관련 기능을 변경할 때 다음을 확인한다.

- JobKey 및 TriggerKey의 기존 식별 규칙
- JobClassRegistry의 허용 클래스
- CRON / SIMPLE별 Misfire 처리
- 기존 PAUSED 상태의 유지 여부
- JobDataMap 파라미터 호환성
- JDBC JobStore의 영속성
- 동일 JobKey의 동시 실행 제한
- JobLogListener와 실행 이력 저장
- 기존 프론트엔드 Job 관리 API와의 호환성

새 Job 클래스 또는 스케줄 정책 추가 시 관련 테스트를 검토한다.

일반적인 코드 작성 규칙은 `AGENTS.md` 및 `docs/coding-conventions.md`를 따른다.