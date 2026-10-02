# Dynamic Scheduler System

Spring Boot와 Quartz를 기반으로 구현한 동적 스케줄 관리 및 외부 API 호출 관리 시스템입니다.

Quartz JDBC JobStore를 활용하여 Job/Trigger 영속화, 동적 스케줄 등록·수정·삭제, 실행 상태 제어, Misfire 정책, 실행 이력 관리를 구현했습니다.

또한 스케줄과 외부 API 정보를 분리하여 API 인증, 파라미터 관리, 재시도, 호출 이력 추적, 응답 Raw Data 저장까지 하나의 실행 흐름으로 관리할 수 있도록 구성했습니다.

---

## 1. 프로젝트 목적

기존 실무에서 Quartz 기반 스케줄 기능을 개발하고 운영한 경험이 있습니다.

이 프로젝트에서는 해당 경험을 바탕으로 Quartz JDBC JobStore를 활용해
스케줄 정보를 영속화하고, 관리 화면에서 런타임으로 Job/Trigger를 제어할 수 있는
동적 스케줄 관리 구조를 구현하는 것을 목표로 했습니다.

또한 스케줄 실행과 외부 API 호출 정보를 분리하여
API 인증, 파라미터 관리, 재시도, 호출 이력, Raw Data 저장까지
하나의 실행 흐름으로 관리할 수 있도록 확장했습니다.

주요 구현 목표는 다음과 같습니다.

* Quartz JDBC JobStore 기반 Job/Trigger 영속화
* 런타임 Job/Trigger 등록 및 상태 제어
* SIMPLE / CRON 스케줄 지원
* Misfire 정책 관리
* Job 실행 이력 및 실행 상태 추적
* 스케줄과 외부 API 호출 정보 분리
* 외부 API 인증 / 파라미터 / 재시도 관리
* 외부 API 호출 이력 및 Raw Data 저장
* Spring Security 기반 인증 및 접근 제어

---

## 2. 기술 스택

### Backend
* Java 17
* Spring Boot 3.5.3
* Spring Web
* Spring Security
* Quartz

### Data Access
* MyBatis
* Spring Data JPA
* Quartz JDBC JobStore

### Database
* PostgreSQL

### Build
* Gradle

---

## 3. 주요 기능

### 3.1 동적 스케줄 관리

Quartz Job과 Trigger를 런타임에 동적으로 관리할 수 있도록 구현했습니다.

* Job 등록 / 조회 / 수정 / 삭제
* Job 즉시 실행
* Job 중지 / 재개
* 여러 Job 일괄 실행 / 중지 / 재개
* Job 및 Trigger 상태 조회
* 등록 가능한 Job 클래스 조회
* JobDataMap 기반 실행 파라미터 관리

Job 클래스는 `JobClassRegistry`를 통해 등록 가능한 Job을 관리하고,
요청받은 Job 클래스명을 실제 Quartz Job 클래스로 변환하여 사용합니다.


### 3.2 SIMPLE / CRON 스케줄 지원

스케줄 등록 시 SIMPLE과 CRON 방식을 지원합니다.

CRON 스케줄은 Quartz Cron Expression을 사용하며,
등록 및 수정 시 `CronExpression.isValidExpression()`으로 표현식의 유효성을 검증합니다.

SIMPLE 스케줄은 초 단위 반복 간격을 입력받아
`SimpleScheduleBuilder`를 이용하여 반복 Trigger를 생성합니다.

잘못된 Cron 표현식이나 0 이하의 SIMPLE 반복 간격은
등록 및 수정 단계에서 검증하여 차단합니다.


### 3.3 Misfire 정책 관리

예정된 실행 시각에 Job을 실행하지 못한 경우의 처리 방식을
스케줄별로 선택할 수 있도록 구현했습니다.

CRON 스케줄에서는 다음 정책을 지원합니다.

* SMART_POLICY
* DO_NOTHING
* FIRE_AND_PROCEED

SIMPLE 스케줄에서는 다음 정책을 지원합니다.

* SMART_POLICY
* FIRE_NOW
* NOW_WITH_EXISTING_COUNT
* NOW_WITH_REMAINING_COUNT
* NEXT_WITH_EXISTING_COUNT
* NEXT_WITH_REMAINING_COUNT

`SMART_POLICY`는 Quartz의 기본 Misfire 정책을 사용하며,
그 외 정책은 선택된 값에 따라 Quartz Misfire Instruction으로 변환하여 적용합니다.

CRON과 SIMPLE에서 지원하지 않는 정책이 전달되면
등록 및 수정 단계에서 검증하여 차단합니다.


### 3.4 실행 이력 및 운영 정보

Quartz `JobListener`를 이용하여 Job 실행 상태와 결과를 기록합니다.

* Job 실행 시작 / 성공 / 실패 / 실행 거부
* 실행 시간
* 예외 메시지
* Quartz Fire Instance ID
* Job명 / 그룹 / 상태 / 기간 검색
* 실행 이력 페이징 조회

실행 시작 시 `fireInstanceId`를 MDC에 등록하여
애플리케이션 로그와 DB 실행 이력을 동일한 실행 단위로 추적할 수 있도록 구성했습니다.

또한 Scheduler 상태, Instance, Thread Pool, JobStore 정보와
전체 Job 수, 실행 성공/실패 현황 등을 Dashboard에서 확인할 수 있도록 구현했습니다.


### 3.5 외부 API 관리 및 실행

스케줄과 외부 API 호출 정의를 분리하여 관리할 수 있도록 구현했습니다.

* 외부 API 기본정보 등록 / 조회 / 수정 / 삭제
* HTTP Method 및 사용 여부 관리
* Query / Header / Body 파라미터 관리
* STATIC / DYNAMIC 파라미터 지원
* API Key / Bearer / Basic 인증
* 외부 API 즉시 실행
* Job 등록 / 수정 시 외부 API 연결
* 사용 중인 외부 API 삭제 방지
* 재시도 횟수 및 재시도 간격 설정
* 호출 이력 및 시도별 상세 이력 조회
* PAGE 방식 페이징 호출
* API 응답 Raw Data 저장

외부 API 호출 정보는 DB에 정의하고,
Job 실행 시 해당 정보를 조회하여 실제 HTTP 요청을 생성합니다.


### 3.6 인증 및 보안

Spring Security 기반으로 관리자 기능에 대한 인증 및 접근 제어를 구현했습니다.

* JSON 기반 로그인
* 세션 기반 인증
* 로그아웃 및 세션 무효화
* CSRF 보호
* 로그인 상태 확인
* 사용자 회원가입
* 회원가입 활성화 / 비활성화 설정
* 아이디 중복 검증
* BCrypt 기반 비밀번호 암호화
* 신규 사용자 `ROLE_USER` 기본 권한 부여
* 인증되지 않은 요청에 대한 보호된 API 접근 제한

로그인은 SPA 환경에 맞게 JSON 요청을 처리하는 커스텀 인증 필터를 사용하며,
인증 성공 후 HTTP Session을 기반으로 인증 상태를 유지합니다.

회원가입 시 입력값과 아이디 중복 여부를 검증하고,
비밀번호는 BCrypt로 암호화하여 저장합니다.

회원가입 기능은 설정값을 통해 운영 환경에서
활성화 또는 비활성화할 수 있도록 구성했습니다.

CSRF 보호는 쿠키 기반 CSRF Token을 사용하며,
프론트엔드에서 `X-XSRF-TOKEN` 헤더를 통해 토큰을 전달하도록 구성했습니다.

---

## 4. 주요 설계 및 구현

### 4.1 Quartz Scheduler와 애플리케이션 관리정보 분리

Quartz Job과 Trigger는 JDBC JobStore를 통해 영속화하고,
관리 화면에서 사용하는 스케줄 정보는 애플리케이션 DB를 기준으로 조회하도록 구성했습니다.

다만 Trigger의 실제 실행 상태는 DB에 저장된 관리정보만으로 판단하지 않고
Quartz Scheduler API를 통해 조회합니다.

이를 통해 애플리케이션에서 관리하는 스케줄 정보와
Quartz가 관리하는 실제 실행 상태의 역할을 구분했습니다.


### 4.2 외부 API 정의와 실행 로직 분리

Quartz Job에 특정 API의 URL, 인증정보, 파라미터를 직접 작성하지 않고
외부 API 호출 정의를 별도의 관리 데이터로 분리했습니다.

외부 API별 URL, HTTP Method, 인증정보, 파라미터,
재시도 및 페이징 설정을 DB에서 관리하고,
Job 실행 시 해당 정의를 조회하여 실제 요청을 생성하도록 구성했습니다.

이를 통해 새로운 외부 API를 추가할 때
Job 클래스를 별도로 구현하지 않고 설정 중심으로 확장할 수 있도록 했습니다.


### 4.3 범용 ExternalApiCallJob 기반 실행 구조

외부 API마다 별도의 Quartz Job 클래스를 작성하는 대신
공통 `ExternalApiCallJob`을 통해 외부 API를 실행하도록 구성했습니다.

`JobDataMap`에는 외부 API 식별자만 전달하고,
Job 실행 시점에 DB에서 외부 API 정의를 조회합니다.

Quartz Job은 스케줄 실행을 담당하고,
HTTP 호출, 인증 적용, 파라미터 구성, 재시도 등의 처리는
별도의 Service에서 담당하도록 책임을 분리했습니다.


### 4.4 외부 API 실행 단위 및 재시도 추적

외부 API 한 번의 실행에는 `executionId`를 생성하여
전체 실행을 하나의 단위로 식별합니다.

실제 HTTP 호출은 `attemptNo`를 이용하여
최초 호출과 재시도 호출을 구분하여 기록합니다.

페이징 호출이 포함되는 경우에는 `requestSequence`를 이용하여
각 페이지 요청을 구분합니다.

구조는 다음과 같습니다.

```text
executionId
 ├─ requestSequence = 1
 │   ├─ attemptNo = 1
 │   └─ attemptNo = 2
 │
 ├─ requestSequence = 2
 │   └─ attemptNo = 1
 │
 └─ requestSequence = 3
     └─ attemptNo = 1
```

이를 통해 하나의 API 실행 안에서
페이지 요청과 각 요청의 재시도 이력을 단계별로 추적할 수 있도록 구성했습니다.


### 4.5 외부 API 페이징 및 Raw Data 수집 구조

페이지 단위로 데이터를 제공하는 외부 API를 처리하기 위해
PAGE 방식의 페이징 호출 구조를 구현했습니다.

페이지 번호와 페이지 크기 파라미터,
시작 페이지, 전체 건수 조회 경로, 최대 요청 횟수를 설정할 수 있으며,
`TOTAL_COUNT`를 기준으로 다음 페이지 호출 여부를 판단합니다.

각 요청에는 `requestSequence`를 부여하고,
호출 성공 시 응답 원문과 Content-Type을 Raw Data로 별도 저장합니다.

Raw Data에는 다음 정보를 함께 저장합니다.

* `executionId`
* `externalApiId`
* `requestSequence`
* `responseBody`
* `contentType`
* `collectedAt`

이를 통해 하나의 수집 실행에 포함된 여러 페이지 응답을
실행 단위와 요청 순서 기준으로 추적할 수 있도록 구성했습니다.

또한 호출 이력과 실제 응답 데이터를 분리하여 관리함으로써,
향후 데이터 가공이나 재처리 시 원본 데이터를 다시 활용할 수 있도록 했습니다.


---

## 5. 주요 구현 과정에서 고민한 부분

### 5.1 DB에 저장된 Job 클래스명을 어떻게 안전하게 실행할 것인가

동적 스케줄을 DB에서 관리하려면
어떤 Quartz Job 클래스를 실행할지 식별할 정보가 필요했습니다.

Job 클래스명을 문자열로 저장한 뒤 Reflection을 통해 직접 클래스를 조회하는 방식도 가능하지만,
잘못된 클래스명, 삭제된 클래스, Quartz Job을 구현하지 않은 클래스가
런타임에 전달될 수 있는 문제가 있습니다.

이를 해결하기 위해 `JobClassRegistry`를 두고
애플리케이션에서 실행을 허용한 Job 클래스만 명시적으로 등록하도록 구성했습니다.

DB에는 Job 클래스명을 문자열로 저장하고,
Job 등록 및 실행 시에는 `JobClassRegistry`를 통해
해당 클래스명이 애플리케이션에서 허용한 Quartz Job인지 확인한 후 사용합니다.

이를 통해 DB의 문자열 값을 그대로 실행 대상으로 신뢰하지 않고,
애플리케이션에서 허용한 Job만 동적으로 등록할 수 있도록 했습니다.


### 5.2 외부 API마다 Job 클래스를 만들어야 하는가

초기에는 특정 외부 API마다 별도의 Job이나 Service를 구현하는 방식도 검토했습니다.

하지만 API가 늘어날수록 URL, HTTP Method, 인증정보, 파라미터 등의 변경이
Job 코드 수정으로 이어지는 문제가 발생할 수 있다고 판단했습니다.

이를 해결하기 위해 외부 API 정의를 DB에서 관리하고,
범용 `ExternalApiCallJob`에는 외부 API 식별자만 전달하도록 구조를 변경했습니다.

실행 시점에는 식별자를 기준으로 외부 API 정의를 조회하고,
HTTP 호출은 공통 Service에서 처리하도록 책임을 분리했습니다.

이를 통해 새로운 외부 API를 추가할 때
별도의 Quartz Job 클래스를 계속 추가하지 않고 설정 중심으로 확장할 수 있도록 했습니다.


### 5.3 스케줄러와 데이터 수집 서비스를 분리할 것인가

외부 API 호출과 데이터 수집 기능을 스케줄러와 분리하여
별도의 `data-collector-service`로 구성하는 방안을 검토하고 실제로 일부 구현했습니다.

초기에는 스케줄러가 실행 요청만 전달하고,
수집 서비스가 외부 API 호출과 데이터 저장을 담당하도록 역할을 분리했습니다.

하지만 구조를 나누면서 다음과 같은 고민이 생겼습니다.

* 스케줄 실행 정보와 수집 실행 정보를 서로 다른 서비스에서 함께 추적해야 함
* `executionId`를 기준으로 두 서비스의 실행 흐름을 연결해야 함
* 서비스 간 호출 실패와 재시도 정책까지 별도로 고려해야 함
* 배포, 장애 대응, 인증, 네트워크 통신 등 운영 복잡도가 증가함
* 현재 프로젝트 규모에서는 서비스 분리로 얻는 이점보다 관리 비용이 더 커질 수 있음

특히 외부 API 호출과 수집 결과가
하나의 스케줄 실행 흐름과 강하게 연결되어 있었기 때문에,
현재 단계에서는 별도 서비스로 분리하는 것보다
스케줄러 내부에서 함께 관리하는 편이 더 단순하고 추적하기 쉽다고 판단했습니다.

최종적으로 외부 API 호출, 재시도, 호출 이력, Raw Data 저장을
스케줄러 애플리케이션 내부로 통합했습니다.

다만 `executionId`를 독립적인 실행 식별자로 유지하여,
향후 데이터 수집 기능을 별도 서비스로 분리하더라도 실행 단위를 연결할 수 있도록 했습니다.


### 5.4 외부 API 실패를 어떤 단위로 기록할 것인가

외부 API 호출에 재시도를 추가하면서
단순히 성공/실패 한 건만 저장하는 방식으로는 실제 실행 과정을 추적하기 어려웠습니다.

예를 들어 한 번의 API 실행에서 첫 번째 호출은 실패하고
두 번째 호출에서 성공할 수 있기 때문에,
전체 실행 결과와 개별 호출 시도를 구분할 필요가 있었습니다.

이를 위해 한 번의 실행은 `executionId`로 묶고,
각 호출 시도는 `attemptNo`로 구분하도록 구성했습니다.

이후 페이징 호출이 추가되면서 각 페이지 요청을 구분하기 위해
`requestSequence`를 추가했습니다.

이를 통해 하나의 실행 안에서
페이지 요청과 각 요청의 재시도 내역을 단계별로 추적할 수 있도록 했습니다.


### 5.5 모든 외부 API 오류를 재시도해야 하는가

외부 API 호출 실패라고 해서 모든 오류를 재시도하는 것은
불필요한 요청을 반복하거나 잘못된 요청을 계속 전송할 수 있다고 판단했습니다.

따라서 연결 실패나 응답 Timeout과 같은 통신 오류와
HTTP 408, 429, 5xx 계열의 일시적인 오류만 재시도 대상으로 처리했습니다.

반대로 요청 자체가 잘못된 경우처럼
재시도해도 성공 가능성이 낮은 오류는 즉시 실패 처리하도록 구분했습니다.

재시도 횟수와 간격은 외부 API별 설정값으로 관리하여
API 특성에 맞게 조정할 수 있도록 구성했습니다.


### 5.6 특정 DBMS에 대한 의존성을 어떻게 줄일 것인가

초기 개발 환경에서는 Oracle을 사용했지만,
운영 환경을 PostgreSQL 16으로 전환하면서
일부 SQL과 DB 함수가 특정 DBMS 문법에 의존하고 있다는 문제가 드러났습니다.

단순히 PostgreSQL 문법으로 다시 작성하는 방식보다는
향후 DB 변경 가능성도 고려하여 가능한 범위에서 DBMS 종속 문법을 줄이고,
표준 SQL에 가깝게 정리했습니다.

또한 DB 함수나 특정 문법에 의존하던 일부 계산 로직은
Service 계층으로 이동하여 DBMS별 SQL 차이를 줄이도록 구성했습니다.

Quartz JDBC JobStore 역시 PostgreSQL용 Schema와 설정으로 변경하고,
DB 접속정보는 공통 설정과 로컬 Secret 설정으로 분리했습니다.

이를 통해 Oracle과 PostgreSQL의 문법 차이로 인한 수정 범위를 줄이고,
애플리케이션 로직과 DBMS 종속 영역을 구분할 수 있도록 했습니다.

---

## 6. 로컬 실행 방법

### 6.1 사전 요구사항

Backend 실행을 위해 다음 환경이 필요합니다.

* Java 17
* PostgreSQL
* Git

Gradle Wrapper가 프로젝트에 포함되어 있으므로 Gradle을 별도로 설치할 필요는 없습니다.

### 6.2 프로젝트 Clone

```bash
git clone https://github.com/ideale17/spring-quartz-scheduler.git
cd spring-quartz-scheduler
```

### 6.3 PostgreSQL Database 생성

PostgreSQL에 `scheduler` Database를 생성합니다.

```sql
CREATE DATABASE scheduler;
```

Local Profile에서는 다음 Database를 사용합니다.

```text
jdbc:postgresql://localhost:5432/scheduler
```

### 6.4 Database 초기화

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

### 6.5 Database 접속정보 설정

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

### 6.6 애플리케이션 실행

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

## 7. Repository

`spring-quartz-scheduler`

https://github.com/ideale17/spring-quartz-scheduler

---

## 8. Demo

배포 완료 후 Demo URL을 추가할 예정입니다.

---

## 9. 향후 개선
