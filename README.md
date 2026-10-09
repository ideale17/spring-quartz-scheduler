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

## 2. 시스템 아키텍처

```text
[ Vue 3 Admin UI ]
        |
        | HTTPS / REST API
        v
[ Spring Boot Scheduler ]
        |
        +-- Spring Security
        |
        +-- Dynamic Job Service
        |       |
        |       v
        |   [ Quartz Scheduler ]
        |       |
        |       v
        |   [ JDBC JobStore ]
        |
        +-- External API Execution
        |       |
        |       v
        |   [ External APIs ]
        |
        +-- Execution / Call Log
        |
        +-- Raw Data Storage
                |
                v
          [ PostgreSQL ]
```

---

## 3. 기술 스택

### Backend
* Java 17
* Spring Boot 3.5.3
* Spring Web
* Spring Security
* Quartz

### Data Access
* MyBatis
* Quartz JDBC JobStore

### Database
* PostgreSQL

### Build
* Gradle

---

## 4. 주요 기능

### 4.1 동적 스케줄 관리

Quartz Job과 Trigger를 런타임에 동적으로 관리할 수 있도록 구현했습니다.

* Job 등록 / 조회 / 수정 / 삭제
* Job 즉시 실행
* Job 중지 / 재개
* 여러 Job 일괄 실행 / 중지 / 재개
* Job 및 Trigger 상태 조회
* 등록 가능한 Job 클래스 조회
* JobDataMap 기반 실행 파라미터 관리
* SIMPLE / CRON 방식의 스케줄 등록 및 수정
* 스케줄 유형별 Misfire 정책 설정


### 4.2 실행 이력 및 운영 정보

Quartz `JobListener`를 이용하여 Job 실행 상태와 결과를 기록합니다.

* Job 실행 시작 / 성공 / 실패 / 실행 거부
* 실행 시간
* 예외 메시지
* Quartz Fire Instance ID
* Job명 / 그룹 / 상태 / 기간 검색
* 실행 이력 페이징 조회


### 4.3 외부 API 관리 및 실행

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


### 4.4 인증 및 보안

Spring Security 기반으로 세션 인증과 보호된 API에 대한 인증 사용자 접근 제어를 구현했습니다.

* JSON 기반 로그인 및 세션 인증
* 회원가입 활성화 / 비활성화 설정


---

## 5. 주요 설계 및 기술적 고민

### 5.1 Quartz 동적 스케줄 관리 구조

Quartz Job과 Trigger는 JDBC JobStore를 통해 영속화하고, 관리 화면에서 사용하는 스케줄 정보는 애플리케이션 DB를 기준으로 조회하도록 구성했습니다.

다만 Trigger의 실제 실행 상태는 DB에 저장된 관리정보만으로 판단하지 않고 Quartz Scheduler API를 통해 조회하여, 애플리케이션 관리정보와 실제 실행 상태의 책임을 분리했습니다.

또한 DB에 저장된 Job 클래스명을 Reflection으로 직접 조회하는 방식은 잘못된 클래스명이나 허용되지 않은 클래스가 실행 대상으로 전달될 수 있다는 문제가 있습니다.

이를 해결하기 위해 `JobClassRegistry`를 두고 애플리케이션에서 허용한 Quartz Job 클래스만 등록 및 실행할 수 있도록 구성했습니다.

이를 통해 **스케줄 관리정보와 실행 상태를 구분하고, 동적으로 등록 가능한 Job 클래스를 통제**할 수 있도록 했습니다.


### 5.2 범용 외부 API 실행 구조

초기에는 외부 API마다 별도의 Quartz Job이나 Service를 구현하는 방식을 고려했습니다.

하지만 API가 증가할수록 URL, HTTP Method, 인증정보, 파라미터 변경이 코드 수정과 재배포로 이어질 수 있다고 판단했습니다.

이를 해결하기 위해 외부 API 호출 정의를 DB에서 관리하고, 공통 `ExternalApiCallJob`을 통해 실행하는 구조로 설계했습니다.

`JobDataMap`에는 외부 API 식별자만 전달하며, 실행 시점에 해당 API의 URL, 인증정보, 파라미터, 재시도 및 페이징 설정을 조회합니다.

Quartz Job은 스케줄 실행을 담당하고, HTTP 요청 생성과 인증 적용, 파라미터 처리 및 재시도는 별도의 Service에서 담당하도록 책임을 분리했습니다.

이를 통해 **새로운 외부 API를 추가할 때 별도의 Job 클래스를 작성하지 않고 설정 중심으로 확장**할 수 있도록 했습니다.


### 5.3 스케줄러와 데이터 수집 서비스 통합

외부 API 호출 및 데이터 수집 기능을 스케줄러 애플리케이션과 분리된 별도 수집 서비스로 구성하는 방안을 검토하고 일부 구현했습니다.

초기에는 스케줄러가 실행 요청을 전달하고, 수집 서비스가 실제 API 호출과 데이터 저장을 담당하는 구조를 고려했습니다.

하지만 서비스를 분리하면서 다음과 같은 운영 복잡도가 발생했습니다.

- 스케줄 실행과 수집 실행 정보를 서비스 간에 연결해야 함
- 서비스 간 통신 실패 및 재시도 정책을 추가로 관리해야 함
- 별도의 배포, 인증 및 장애 대응 체계가 필요함

현재 프로젝트에서는 외부 API 호출과 수집 결과가 하나의 스케줄 실행 흐름에 밀접하게 연결되어 있기 때문에, 서비스 분리의 이점보다 관리 비용이 크다고 판단했습니다.

최종적으로 API 호출, 재시도, 실행 이력 및 Raw Data 저장 기능을 스케줄러 내부로 통합했습니다.

다만 독립적인 `executionId`를 유지하여 향후 데이터 수집 기능을 별도 서비스로 분리하더라도 실행 단위를 연결할 수 있도록 했습니다.


### 5.4 외부 API 실행 이력 및 상태 관리

외부 API 호출에 재시도와 페이징 기능을 추가하면서 단순한 성공·실패 이력만으로는 전체 실행 과정을 추적하기 어려웠습니다.

이를 해결하기 위해 실행 단위를 다음과 같이 구분했습니다.

- `executionId`: 외부 API 전체 실행 식별자
- `requestSequence`: 개별 페이지 요청 순서
- `attemptNo`: 최초 호출 및 재시도 구분

또한 개별 HTTP 호출이 모두 성공하더라도 전체 데이터 수집이 완료되지 않을 수 있다는 문제가 있었습니다.

예를 들어 총 10페이지를 수집해야 하지만 최대 5페이지까지만 호출하도록 설정한 경우, 개별 HTTP 요청은 성공하더라도 전체 수집은 미완료 상태입니다.

이를 해결하기 위해 실행 상태를 다음과 같이 분리했습니다.

| 테이블 | 관리 대상 |
| --- | --- |
| `SCHED_EXEC_LOG` | Quartz Job 전체 실행 상태 |
| `SCHED_EXTERNAL_API_EXEC_LOG` | 외부 API 수집 1회 실행의 전체 상태 |
| `SCHED_EXTERNAL_API_CALL_LOG` | 개별 HTTP 호출 및 재시도 결과 |

재시도는 연결 실패, Timeout, HTTP 408·429·5xx 등 일시적인 오류를 대상으로 하며, API별 설정값으로 횟수와 간격을 관리하도록 구성했습니다.

이를 통해 **개별 HTTP 호출 결과와 전체 데이터 수집 결과를 구분하고, 실패한 요청과 재시도 과정을 실행 단위로 추적**할 수 있도록 했습니다.


### 5.5 페이징 및 Raw Data 수집 구조

페이지 단위로 데이터를 제공하는 외부 API를 처리하기 위해 PAGE 방식의 페이징 호출 기능을 구현했습니다.

페이지 번호 및 크기 파라미터, 시작 페이지, 전체 건수 조회 경로와 최대 호출 횟수를 설정할 수 있도록 했으며, `TOTAL_COUNT`를 기준으로 다음 페이지 호출 여부를 판단합니다.

각 HTTP 요청이 성공하면 응답 원문과 Content-Type을 `SCHED_COLLECT_RAW_DATA` 테이블에 저장합니다.

수집 데이터에는 `executionId`, `externalApiId`, `requestSequence`를 함께 기록하여 전체 실행과 개별 페이지의 원본 응답을 연결할 수 있도록 했습니다.

또한 호출 이력과 실제 응답 데이터를 분리하여 관리함으로써 **향후 데이터 가공이나 재처리 시 원본 데이터를 다시 활용할 수 있는 구조**로 구성했습니다.


### 5.6 DBMS 전환 및 의존성 개선

초기 개발 환경에서는 Oracle을 사용했지만 운영 환경을 PostgreSQL 16으로 전환하면서 일부 SQL과 DB 함수가 특정 DBMS 문법에 의존한다는 문제가 드러났습니다.

이를 개선하기 위해 가능한 범위에서 표준 SQL을 사용하고, 일부 DB 함수에 의존하던 계산 로직을 Service 계층으로 이동했습니다.

Quartz JDBC JobStore 역시 PostgreSQL용 Schema와 설정을 사용하도록 변경했으며, DB 접속정보는 별도 Secret 설정으로 분리했습니다.

이를 통해 **애플리케이션 업무 로직과 DBMS 종속 영역을 구분하고, 데이터베이스 환경 변경에 따른 수정 범위를 줄이도록 개선**했습니다.


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

scheduler 데이터베이스에 접속한 뒤 아래 SQL을 순서대로 실행합니다.

```text
src/main/resources/db/postgresql/01_quartz.sql
src/main/resources/db/postgresql/02_scheduler.sql
src/main/resources/db/postgresql/03_security.sql
src/main/resources/db/postgresql/04_init_data.sql
```

각 파일의 역할은 다음과 같습니다.

- `01_quartz.sql` : Quartz JDBC JobStore에서 사용하는 `QRTZ_*` 테이블 생성
- `02_scheduler.sql` : Scheduler 및 외부 API 관련 애플리케이션 테이블 생성
- `03_security.sql` : 사용자, 권한, 사용자-권한 관계 테이블 생성
- `04_init_data.sql` : 애플리케이션 기본 권한 데이터 생성

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

Local Profile에서는 다음 설정을 통해 해당 파일을 로드합니다.
`spring.config.import=optional:file:./config/application-postgresql-secret.properties`

실제 DB 계정 정보가 포함된 `application-postgresql-secret.properties` 파일은 Git에 포함되지 않습니다.

### 6.6 암호화 키 생성

외부 API의 API Key, Bearer Token, Basic 인증 비밀번호 등
민감한 인증정보를 암호화하기 위해 AES-256 암호화 키가 필요합니다.

애플리케이션은 Base64로 인코딩된 32바이트 키를 사용합니다.

OpenSSL을 사용할 수 있는 환경에서는 다음 명령으로 키를 생성할 수 있습니다.

```bash
openssl rand -base64 32
```

생성된 Base64 문자열을 그대로 `SCHEDULER_ENCRYPTION_KEY` 환경변수에 설정합니다.

암호화 키는 소스 코드, 설정 파일 또는 Git 저장소에 직접 저장하지 않는 것을 권장합니다.

이미 암호화된 외부 API 인증정보가 존재하는 환경에서 암호화 키를 변경하면
기존 데이터를 복호화할 수 없으므로 동일한 키를 유지해야 합니다.

### 6.7 암호화 키 환경변수 설정

생성한 AES-256 키를 `SCHEDULER_ENCRYPTION_KEY` 환경변수로 설정합니다.

#### Windows CMD

```bat
set SCHEDULER_ENCRYPTION_KEY=YOUR_BASE64_ENCRYPTION_KEY
```

#### Windows PowerShell

```powershell
$env:SCHEDULER_ENCRYPTION_KEY="YOUR_BASE64_ENCRYPTION_KEY"
```

#### macOS / Linux

```bash
export SCHEDULER_ENCRYPTION_KEY=YOUR_BASE64_ENCRYPTION_KEY
```

애플리케이션에서는 다음 설정을 통해 환경변수를 참조합니다.

```properties
app.security.encryption-key=${SCHEDULER_ENCRYPTION_KEY}
```

환경변수가 설정되지 않았거나 Base64 디코딩 결과가 32바이트가 아닌 경우
애플리케이션이 정상적으로 시작되지 않습니다.

### 6.8 애플리케이션 실행

Local Profile을 활성화하여 애플리케이션을 실행합니다.

#### Windows

```bash
.\gradlew.bat bootRun --args="--spring.profiles.active=local,postgresql"
```

#### macOS / Linux

```bash
./gradlew bootRun --args="--spring.profiles.active=local,postgresql"
```

애플리케이션은 기본적으로 다음 주소에서 실행됩니다.

```text
http://localhost:8080
```

---

### 초기 사용자 계정

초기 SQL은 역할 정보만 생성하며 기본 사용자 계정은 생성하지 않습니다.

로컬에서 로그인 기능을 테스트하려면 회원가입을 임시로 활성화할 수 있습니다.

```properties
app.security.signup-enabled=true
```

테스트가 끝난 뒤에는 필요에 따라 다시 비활성화할 수 있습니다.

```properties
app.security.signup-enabled=false
```

---

## 7. Frontend

[scheduler-ui](https://github.com/ideale17/scheduler-ui)

---

## 8. Demo

https://scheduler.pomibori.dev/

---

## 9. 향후 개선

- JSON/XML Raw Data 필드 추출 및 타입 변환
- 가공 데이터 저장 및 조회 기능
- 수집·가공 데이터 기반 AI 분석 기능