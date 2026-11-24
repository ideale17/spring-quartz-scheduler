# Database 설계

## 1. 개요

Dynamic Scheduler는 PostgreSQL을 주요 데이터베이스로 사용하며, MyBatis를 통해 업무 데이터를 조회하고 저장한다.

Quartz는 JDBC JobStore를 사용하여 Job과 Trigger 정보를 영속화한다.

주요 데이터 관리 영역:

- Quartz Job 및 Trigger 정보
- Job 실행 이력
- External API 설정 및 수집 이력
- Raw Data
- Transform 규칙·작업·결과
- 사용자 및 권한 정보

DB 구조와 트랜잭션은 각 기능의 데이터 정합성 및 실행 안정성을 유지하도록 설계되어 있다.

## 2. 주요 설정 및 구성 요소

- PostgreSQL: 업무 및 Quartz 데이터 저장
- MyBatis: SQL 실행 및 Java 객체 매핑
- Mapper 인터페이스: Service와 SQL 연결
- Mapper XML: SQL 정의
- Spring Transaction: 트랜잭션 경계 및 롤백 관리
- Quartz JDBC JobStore: Job 및 Trigger 영속화

주요 설정:

```properties
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=never
mybatis.mapper-locations=classpath:mapper/*.xml
mybatis.configuration.map-underscore-to-camel-case=true
```

PostgreSQL 프로필에서는 `PostgreSQLDelegate`를 사용한다.

실제 DB 접속정보와 환경별 설정은 해당 프로필 및 외부 설정 파일을 따른다.

## 3. 주요 데이터 구조

### Quartz

`QRTZ_` 접두사의 테이블을 사용한다.

- Job 정보
- Trigger 정보
- CRON / SIMPLE 스케줄 정보
- Scheduler 상태 및 잠금 정보

Quartz 데이터 변경은 기존 Scheduler API를 통해 수행한다.

### Scheduler 실행 이력

- `SCHED_EXEC_LOG`: Quartz Job 실행 이력

### External API

- `SCHED_EXTERNAL_API`: 외부 API 기본 설정
- `SCHED_EXTERNAL_API_PARAM`: 요청 파라미터 설정
- `SCHED_EXTERNAL_API_PARAM_REPEAT_VALUE`: LIST 반복 값
- `SCHED_EXTERNAL_API_PAGING`: 페이징 설정
- `SCHED_EXTERNAL_API_EXEC_LOG`: 전체 실행 이력
- `SCHED_EXTERNAL_API_EXEC_SNAPSHOT`: 최초 실행 설정 스냅샷
- `SCHED_EXTERNAL_API_REQUEST`: 논리적 요청 이력
- `SCHED_EXTERNAL_API_REQUEST_PARAM`: 실제 요청 파라미터 이력
- `SCHED_EXTERNAL_API_CALL_LOG`: HTTP 호출 시도 이력
- `SCHED_COLLECT_RAW_DATA`: 응답 원본 데이터

### Transform

- `SCHED_TRANSFORM_RULE`: 가공 규칙
- `SCHED_TRANSFORM_RULE_VERSION`: 가공 규칙 버전
- `SCHED_TRANSFORM_RULE_FIELD`: 규칙 버전별 추출 필드
- `SCHED_TRANSFORM_AUTO_CONFIG`: 자동 가공 설정
- `SCHED_TRANSFORM_TASK`: 가공 작업 및 실행 상태
- `SCHED_TRANSFORM_RESULT`: 가공 결과

### 사용자 및 권한

- `APP_USER`
- `APP_ROLE`
- `APP_USER_ROLE`

실제 컬럼과 제약조건은 PostgreSQL 스키마를 기준으로 확인한다.

## 4. 데이터 관계 및 정합성

### External API

- API 설정은 여러 파라미터와 실행 이력을 가질 수 있다.
- 실행 이력은 `executionId`로 식별한다.
- 하나의 실행에 여러 논리적 요청이 존재할 수 있다.
- 하나의 논리적 요청은 재시도에 따라 여러 호출 이력을 가질 수 있다.
- Raw Data는 해당 실행 및 요청과 연결된다.
- 재처리 실행은 원본 실행 및 요청과의 관계를 유지한다.
- 최초 실행 스냅샷은 실행 ID를 기준으로 관리한다.

### Transform

- 가공 규칙은 여러 버전을 가질 수 있다.
- 각 버전은 별도의 추출 필드 설정을 가진다.
- 가공 작업은 Raw Data와 특정 규칙 버전을 참조한다.
- 가공 결과는 해당 가공 작업을 참조한다.
- 하나의 가공 작업에는 최대 하나의 결과가 저장되도록 UNIQUE 제약조건이 설정되어 있다.

주요 데이터 관계는 외래키 및 UNIQUE 제약조건으로 관리한다.

데이터 관계를 변경할 경우 기존 이력 및 참조 데이터에 미치는 영향을 확인한다.

## 5. MyBatis 데이터 접근

프로젝트는 Mapper 인터페이스와 Mapper XML을 사용한다.

### Mapper 역할

- Service에서 Mapper 메서드를 호출한다.
- Mapper XML에 SQL을 정의한다.
- SQL 파라미터는 `#{}` 바인딩을 우선 사용한다.
- 단순 컬럼 매핑은 underscore-to-camel-case 설정을 활용한다.
- 복잡한 조회는 필요한 경우 명시적인 매핑을 사용한다.

### SQL 작성 기준

- 가능한 경우 표준 SQL을 우선한다.
- PostgreSQL 전용 기능이 필요한 경우 기존 구현을 유지한다.
- 필요한 컬럼을 명시적으로 조회한다.
- 정렬과 페이징은 안정적인 결과 순서를 고려한다.
- Nullable 값은 필요에 따라 `jdbcType`을 명시한다.

구체적인 Java 및 SQL 작성 스타일은 `docs/coding-conventions.md`를 따른다.

### 적재 데이터 조회

- 모델 선택에는 기존 `GET /analysis-model/list`를 사용한다.
- `GET /analysis-load/results?analysisModelId=1&page=1&size=50`은 인증된 사용자가 호출할 수 있다. `page`는 1부터 시작하고 `size`는 1~100까지 허용한다. 생략 시 각각 1과 50을 사용한다.
- 응답은 적재 모델(`model`), 대상 테이블의 컬럼 정보(`columns`), 해당 페이지의 실제 적재 행(`content`), 전체 행 수(`totalCount`), `page`, `size`를 제공한다. 행 객체의 키는 DB 컬럼명이며 SQL NULL은 JSON null로 반환한다.
- 모델에 등록된 허용 테이블의 모든 행을 대상으로 한다. 모델 사용 여부와 현재 매핑 버전에 상관없이 이미 저장된 데이터를 검토할 수 있다.
- 전체 건수와 페이지는 하나의 읽기 전용 `REPEATABLE READ` 트랜잭션에서 조회한다. 기본키 전체를 내림차순으로 정렬하며, 읽을 수 있는 기본키가 있어야 한다. 비어 있는 테이블이나 마지막 페이지를 넘긴 요청의 `content`는 빈 배열이다.
- SQL 식별자는 서버가 허용 테이블과 DB 메타데이터를 확인해 인용 처리한 값만 사용한다.

## 6. 트랜잭션 관리

Spring의 `@Transactional`을 사용하여 업무 단위의 트랜잭션을 관리한다.

### 기본 원칙

- 여러 DB 변경을 하나의 업무 단위로 처리해야 하면 트랜잭션을 사용한다.
- 기존 트랜잭션 전파 속성을 임의로 변경하지 않는다.
- 예외 발생 시 기존 롤백 동작을 유지한다.
- 외부 API 통신과 같은 장시간 작업을 불필요하게 DB 트랜잭션에 포함하지 않는다.

### Transform 트랜잭션

현재 가공 작업은 다음과 같이 분리되어 있다.

- 대기 작업 선점: `REQUIRES_NEW`
- 개별 작업 가공 및 결과 저장: `@Transactional`
- 실패 상태 기록: `REQUIRES_NEW`

작업 선점은 실제 가공 전에 커밋한다.

가공 결과 저장과 SUCCESS 상태 변경은 하나의 트랜잭션으로 처리한다.

개별 작업에서 예외가 발생하면 해당 실행 트랜잭션을 롤백하고, 실패 상태는 별도의 트랜잭션에서 기록한다.

## 7. 락 및 동시성 제어

Transform 작업 선점은 `TransformTaskMapper.claimPendingTasks()`에서 수행한다.

현재 PostgreSQL의 다음 기능을 사용한다.

- `FOR UPDATE SKIP LOCKED`
- `UPDATE ... RETURNING`

### 작업 선점

1. PENDING 상태의 작업을 조회한다.
2. 생성 시각과 작업 ID 순서로 정렬한다.
3. 다른 트랜잭션에서 잠근 행은 건너뛴다.
4. 선점한 작업을 RUNNING 상태로 변경한다.
5. 실행 시도 횟수를 증가시키고 변경된 작업을 반환한다.

이 과정은 하나의 원자적 DB 작업으로 처리된다.

### 중복 방지

- Transform 수동 작업 등록 시 동일 Raw Data와 규칙 버전의 기존 작업을 조회한다.
- 예약 가공 작업은 상태 조건이 포함된 부분 UNIQUE 인덱스로 중복 등록을 제한한다.
- 가공 결과는 작업 ID에 대한 UNIQUE 제약조건을 가진다.

수동 작업의 사전 중복 조회만으로 모든 동시 등록 경쟁 상황이 방지되는 것은 아니다.

새로운 병렬 처리나 재시도 기능을 추가할 때는 애플리케이션 검증과 DB 제약조건을 함께 확인한다.

## 8. 인덱스 및 조회 성능

현재 주요 조회를 지원하기 위해 인덱스를 사용한다.

대표적인 인덱스:

- `idx_transform_task_pending`: PENDING 작업 선점
- `idx_transform_task_raw_data`: Raw Data별 가공 이력
- `uq_transform_task_scheduled`: 예약 가공 작업 중복 방지
- `idx_transform_rule_field_version_sort`: 규칙 버전별 필드 조회
- `idx_external_api_request_position`: 실행·조합·페이지별 요청 조회
- `idx_external_api_exec_source`: 재처리 원본 실행 관계 조회

조회 성능 변경 시 다음을 고려한다.

- WHERE 조건과 인덱스 컬럼 순서
- 정렬 및 페이징 방식
- 데이터 증가에 따른 조회 비용
- JOIN 대상과 필터링 조건
- 실행계획 및 실제 처리 건수

인덱스는 기존 조회 패턴과 실행계획을 확인한 후 변경한다.

인덱스 추가만으로 모든 성능 문제가 해결된다고 가정하지 않는다.

## 9. DB 스키마 및 변경 관리

PostgreSQL 스키마 관련 SQL은 다음 경로에서 관리한다.

- `src/main/resources/db/postgresql/01_quartz.sql`
- `src/main/resources/db/postgresql/02_scheduler.sql`
- `src/main/resources/db/postgresql/03_security.sql`

### 스키마 변경

- 기존 데이터와 호환성을 고려한다.
- 컬럼 추가·변경 시 Service, DTO, Mapper에 미치는 영향을 확인한다.
- NOT NULL, UNIQUE, 외래키 및 CHECK 제약조건을 검토한다.
- 운영 중인 테이블의 삭제나 초기화 작업을 임의로 수행하지 않는다.
- 데이터 마이그레이션이 필요한 경우 별도 적용 절차를 검토한다.

현재 SQL 파일에는 초기 테이블 생성과 이후 기능 확장을 위한 ALTER 구문이 함께 포함되어 있다.

해당 파일들을 반복 실행 가능한 마이그레이션 스크립트라고 가정하지 않는다.

특히 `01_quartz.sql`에는 테이블 DROP 구문이 있으므로 기존 데이터가 있는 환경에서 재실행하지 않는다.

## 10. 변경 시 확인할 사항

DB 구조나 데이터 접근 로직을 변경할 때 다음을 확인한다.

- 기존 데이터 및 컬럼 타입과의 호환성
- 테이블 간 외래키 관계
- UNIQUE 및 CHECK 제약조건
- DTO와 MyBatis 매핑 일치 여부
- SQL 조회 결과와 정렬 순서
- 트랜잭션 범위 및 롤백 동작
- 동시성 제어와 중복 처리
- 기존 인덱스 및 실행계획 영향
- 실행·요청·가공 이력의 관계 유지
- 운영 DB 적용 및 마이그레이션 필요 여부

상세 업무 흐름은 각 도메인 문서에서 관리한다.

- Quartz: `docs/quartz.md`
- External API: `docs/external-api.md`
- Transform: `docs/transform.md`
- Security: `docs/security.md`

코드 작성 규칙과 일반적인 AI 작업 절차는 `AGENTS.md` 및 `docs/coding-conventions.md`를 따른다.
