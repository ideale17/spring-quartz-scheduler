# Dynamic Scheduler System

Spring Boot와 Quartz를 기반으로 구현한 동적 스케줄 관리 시스템입니다.

기존 Quartz 기반 스케줄 개발 경험을 바탕으로 Spring Boot 환경에서 Quartz JDBC JobStore를 활용한 Job/Trigger 영속화, 동적 스케줄 관리, Misfire 처리, 실행 이력 관리 및 외부 API 연계 기능을 구현했습니다.

스케줄 등록·수정 및 실행 상태 제어 기능을 제공하며, 스케줄과 외부 API 정보를 분리하여 다양한 API 호출 작업을 Job으로 실행할 수 있도록 구성했습니다.

---

## 1. 프로젝트 목적

기존 실무에서는 애플리케이션 DB에 스케줄 정보를 별도로 관리하고, 서버 기동 시 해당 정보를 조회하여 Quartz Job/Trigger를 다시 등록하는 방식으로 동적 스케줄 기능을 구현한 경험이 있습니다.

이 프로젝트에서는 해당 경험을 바탕으로 Quartz가 제공하는 기능을 보다 적극적으로 활용하여 다음 내용을 구현하는 것을 목표로 했습니다.

* Quartz JDBC JobStore를 이용한 Job/Trigger 영속화
* 런타임 Job/Trigger 등록 및 상태 관리
* SIMPLE / CRON 스케줄 지원
* Misfire 정책 적용
* Job 실행 이력 관리
* 외부 API와 스케줄의 분리 및 연계
* Spring Security 기반 인증 적용

---

## 2. 기술 스택

### Backend

* Java 17
* Spring Boot
* Quartz
* Spring Security
* MyBatis
* JPA
* REST API

### Database

* PostgreSQL
* Quartz JDBC JobStore
* MyBatis / JPA 기반 데이터 처리

---

## 3. 주요 기능

### 3.1 동적 스케줄 관리

Quartz Job과 Trigger를 동적으로 관리할 수 있습니다.

* Job 등록
* Job 수정
* Job 삭제
* Job 즉시 실행
* Job 중지
* Job 재시작
* 여러 Job 일괄 실행
* 여러 Job 일괄 중지
* 여러 Job 일괄 재시작
* Job 및 Trigger 상태 조회

Job 클래스는 `JobClassRegistry`를 통해 등록 가능한 Job을 관리하고, 요청받은 Job 클래스명을 실제 Quartz Job 클래스로 변환하여 사용합니다.

### 3.2 SIMPLE / CRON 스케줄 지원

스케줄 등록 시 SIMPLE과 CRON 방식을 지원합니다.

CRON 스케줄은 Quartz Cron Expression을 사용하며, 등록 및 수정 시 `CronExpression.isValidExpression()`으로 유효성을 검증합니다.

SIMPLE 스케줄은 초 단위 반복 간격을 입력받아 `SimpleScheduleBuilder`로 Trigger를 생성합니다.

### 3.3 Misfire 정책 관리

예정된 실행 시각에 Job을 실행하지 못했을 때의 동작을 스케줄별로 선택할 수 있도록 구현했습니다.

CRON 스케줄에서는 다음과 같은 정책을 지원합니다.

* SMART_POLICY
* DO_NOTHING
* FIRE_AND_PROCEED

SIMPLE 스케줄에서는 다음과 같은 정책을 지원합니다.

* SMART_POLICY
* FIRE_NOW
* NOW_WITH_EXISTING_COUNT
* NOW_WITH_REMAINING_COUNT
* NEXT_WITH_EXISTING_COUNT
* NEXT_WITH_REMAINING_COUNT

CRON과 SIMPLE에서 사용할 수 없는 정책이 전달되면 등록 및 수정 단계에서 검증하여 차단합니다.

### 3.4 실행 이력 관리

Quartz `JobListener`를 이용하여 Job 실행 상태를 기록합니다.

* Job 실행 시작
* Job 실행 성공
* Job 실행 실패
* 실행 거부
* 실행 시간
* 예외 메시지
* Quartz Fire Instance ID

실행 시작 시 `fireInstanceId`를 MDC에 등록하여 애플리케이션 로그와 실행 이력을 연결해서 확인할 수 있도록 구성했습니다.

### 3.5 External API 관리

스케줄과 실제 외부 API 호출 정보를 직접 결합하지 않고 별도의 관리 구조로 분리했습니다.

External API 기본정보와 함께 다음 위치의 파라미터를 관리할 수 있습니다.

* Query
* Path
* Header
* Body

파라미터는 고정값과 실행 시점에 결정되는 동적값을 구분하여 관리하며,
Job 코드에 특정 API의 URL이나 파라미터를 직접 작성하지 않고 DB에 등록된 API 정보를 기반으로 호출할 수 있도록 구성했습니다.

External API 인증은 다음 방식을 지원합니다.

* API Key
* Bearer Token
* Basic Authentication

API Key는 Header 또는 Query Parameter에 적용할 수 있으며,
인증 방식별 필수값을 실행 전에 검증하도록 처리했습니다.

External API 호출 실패 시 설정된 재시도 횟수와 간격에 따라 재시도할 수 있으며,
최초 호출과 재시도를 `attemptNo`로 구분하여 호출 이력을 저장합니다.

한 번의 API 실행에는 별도의 `executionId`를 생성하여
최초 호출과 재시도 이력을 동일한 실행 단위로 추적할 수 있도록 구성했습니다.

API 호출 성공 시 응답 원문과 Content-Type을 Raw Data로 별도 저장합니다.

Raw Data에는 다음 정보를 함께 저장하여 API 호출 이력과 수집 데이터를 추적할 수 있도록 구성했습니다.

* executionId
* externalApiId
* responseBody
* contentType
* collectedAt

이를 통해 External API 호출 이력과 실제 수집된 원본 데이터를 분리하여 관리하고,
향후 데이터 가공이나 재처리 시 원본 응답을 다시 활용할 수 있도록 구성했습니다.

### 3.6 인증 및 보안

Spring Security 기반 인증 후 관리 API를 사용할 수 있도록 구성했습니다.

* 사용자 회원가입
* 아이디 중복 검증
* BCrypt 기반 비밀번호 암호화
* 신규 사용자 `ROLE_USER` 기본 권한 부여
* JSON 기반 로그인
* 세션 기반 인증
* 로그아웃
* 세션 만료 처리
* CSRF 보호

회원가입 시 사용자 아이디 중복 여부를 확인하고 비밀번호를 BCrypt로 암호화하여 저장합니다.

신규 사용자는 기본적으로 `ROLE_USER` 권한을 부여하며, 인증이 완료된 사용자만 보호된 API에 접근할 수 있도록 구성했습니다.

---

## 4. Quartz JDBC JobStore

Quartz Scheduler는 JDBC JobStore 방식으로 구성했습니다.

```properties
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=never
```

Job과 Trigger 정보를 Quartz의 `QRTZ_*` 테이블에 영속화하여 애플리케이션 재기동 이후에도 스케줄 정보를 유지할 수 있도록 구성했습니다.

Quartz 스케줄 정보와 애플리케이션에서 관리하는 정보를 구분하여 사용하며, 실제 Trigger 상태는 Quartz Scheduler를 통해 조회합니다.

---

## 5. Job 수정 시 실행 상태 유지

Job 수정 과정에서 Trigger를 다시 생성하면 기존 상태가 의도하지 않게 변경될 수 있습니다.

이를 방지하기 위해 수정 전에 기존 Trigger 상태를 저장하고, 수정 완료 후 기존 상태가 `PAUSED`였다면 다시 중지 상태로 복원하도록 처리했습니다.

처리 흐름은 다음과 같습니다.

1. 기존 Trigger 상태 조회
2. JobDataMap 수정
3. Trigger 재생성
4. `rescheduleJob()` 실행
5. 기존 상태가 `PAUSED`라면 다시 중지 상태로 복원

이를 통해 중지되어 있던 Job의 스케줄이나 파라미터를 수정하더라도 자동으로 실행 상태가 되지 않도록 처리했습니다.

---

## 6. JobDataMap 기반 실행 파라미터 관리

Job 등록 시 전달받은 파라미터를 `JobDataMap`에 저장합니다.

Job 수정 시에도 새로운 파라미터로 `JobDataMap`을 갱신하여 실제 Job 실행 시 변경된 값을 사용할 수 있도록 구성했습니다.

---

## 7. External API 데이터 정합성 처리

External API를 사용하는 Job이 존재하는 경우 해당 API를 바로 삭제하지 못하도록 처리했습니다.

External API 삭제 요청 시 해당 API를 사용하는 Job이 존재하는지 먼저 확인하고, 사용 중이면 삭제를 차단합니다.

이를 통해 Job에서 사용 중인 API가 삭제되어 실행 오류가 발생하는 상황을 방지했습니다.

---

## 8. 주요 구현 과정에서 고민한 부분

### 8.1 애플리케이션 DB 정보와 Quartz 상태의 차이

관리 데이터와 실제 Quartz Scheduler의 상태는 항상 동일한 의미를 가지지 않습니다.

따라서 관리 데이터와 Quartz Trigger 상태를 구분하고, 실제 실행 상태가 필요한 경우 Quartz Scheduler에서 상태를 조회하도록 구성했습니다.

### 8.2 Job 수정과 실행 상태 분리

스케줄 정보를 수정하는 것과 Job을 시작하는 것은 서로 다른 작업으로 처리했습니다.

중지 상태의 Job을 수정했을 때 자동으로 실행되지 않도록 기존 Trigger 상태를 확인하고 수정 완료 후 기존 상태를 복원하도록 처리했습니다.

### 8.3 스케줄과 External API의 분리

Job마다 외부 API 정보를 직접 가지고 있을 경우 API URL이나 파라미터 변경 시 여러 스케줄을 함께 수정해야 하는 문제가 발생할 수 있습니다.

따라서 External API 정보를 별도 관리하고 Job에서는 해당 API를 참조하는 방식으로 분리했습니다.

### 8.4 Misfire 정책 명시화

Quartz의 Misfire Instruction 값을 API에 직접 노출하지 않고 애플리케이션에서 의미를 알 수 있는 정책으로 관리했습니다.

선택된 정책을 서버에서 Quartz의 실제 Misfire Instruction으로 변환하여 적용하도록 구현했습니다.

---

## 9. 로컬 실행 방법

### 9.1 사전 요구사항

Backend 실행을 위해 다음 환경이 필요합니다.

* Java 17
* PostgreSQL
* Git

Gradle Wrapper가 프로젝트에 포함되어 있으므로 Gradle을 별도로 설치할 필요는 없습니다.

### 9.2 프로젝트 Clone

```bash
git clone https://github.com/ideale17/spring-quartz-scheduler.git
cd spring-quartz-scheduler
```

### 9.3 PostgreSQL Database 생성

PostgreSQL에 `scheduler` Database를 생성합니다.

```sql
CREATE DATABASE scheduler;
```

Local Profile에서는 다음 Database를 사용합니다.

```text
jdbc:postgresql://localhost:5432/scheduler
```

### 9.4 Database 초기화

다음 SQL 파일을 순서대로 실행합니다.

```text
src/main/resources/db/postgresql/01_quartz.sql
src/main/resources/db/postgresql/02_scheduler.sql
src/main/resources/db/postgresql/03_init_data.sql
```

각 파일의 역할은 다음과 같습니다.

- `01_quartz.sql` : Quartz JDBC JobStore에서 사용하는 `QRTZ_*` 테이블 생성
- `02_scheduler.sql` : Scheduler 애플리케이션 테이블 생성
- `03_init_data.sql` : 애플리케이션 기본 권한 및 초기 데이터 생성

Quartz Schema 자동 생성을 사용하지 않기 때문에 최초 실행 전에 SQL을 직접 적용해야 합니다.

### 9.5 Database 접속정보 설정

다음 예제 파일을 복사합니다.

```text
config/application-postgresql-secret.properties.example
```

복사한 파일명을 다음과 같이 변경합니다.

```text
config/application-postgresql-secret.properties
```

PostgreSQL 접속정보를 입력합니다.

```properties
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

실제 DB 계정 정보가 포함된 `application-postgresql-secret.properties` 파일은 Git에 포함되지 않습니다.

### 9.6 애플리케이션 실행

Local Profile을 활성화하여 애플리케이션을 실행합니다.

#### Windows

```bash
gradlew.bat bootRun --args="--spring.profiles.active=local"
```

#### macOS / Linux

```bash
./gradlew bootRun --args="--spring.profiles.active=local"
```

애플리케이션은 기본적으로 다음 주소에서 실행됩니다.

```text
http://localhost:8080
```

---

## 10. Repository

`spring-quartz-scheduler`

https://github.com/ideale17/spring-quartz-scheduler

---

## 11. Demo

배포 완료 후 Demo URL을 추가할 예정입니다.

---

## 12. 향후 개선

- External API 재시도 정책 고도화
    - HTTP 상태코드별 재시도 여부 설정
    - 고정 간격 외에 지수 백오프(Exponential Backoff) 방식 지원
    - 재시도 대상 예외 유형 세분화

- External API 호출 이력 조회 고도화
    - 최종 실패 실행 건 별도 조회
    - API별 성공/실패 통계
    - 기간별 호출 횟수 및 평균 응답시간 집계

- External API 인증 방식 확장
    - API Key
    - Bearer Token
    - Basic Authentication 등 인증 정보 관리

- External API 응답 처리 확장
    - 응답 데이터 저장 여부 설정
    - 응답 결과를 후속 처리에 활용할 수 있는 구조 검토

- Quartz Job : Trigger 1:N 구조 확장