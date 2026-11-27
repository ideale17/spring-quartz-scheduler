# Dynamic Scheduler

Spring Boot와 Quartz 기반의 동적 스케줄 관리 및 데이터 처리 시스템입니다.

웹 관리 화면에서 Quartz Job과 Trigger를 동적으로 관리하고, 외부 API를 통한 데이터 수집부터 JSON/XML 데이터 가공, AI 기반 데이터 분석까지 하나의 시스템에서 처리할 수 있습니다.

Quartz JDBC JobStore를 사용하여 스케줄 정보를 영속화하며, 외부 API 호출 설정과 데이터 가공 규칙을 데이터베이스에서 관리하여 새로운 수집 및 가공 작업을 코드 수정 없이 설정할 수 있도록 구성했습니다.

수집 및 가공 과정에서 발생하는 실행 이력, HTTP 요청·응답 정보, 처리 상태와 결과를 조회할 수 있으며, Spring Security 기반 인증 및 역할별 접근 제어를 지원합니다.

---


## Features

### Dynamic Job Scheduling

Quartz 기반으로 Job과 Trigger를 동적으로 관리합니다.

- Job 등록, 조회, 수정, 삭제
- Job 즉시 실행, 중지 및 재개
- 여러 Job 일괄 실행, 중지 및 재개
- CRON / SIMPLE 스케줄 지원
- 스케줄 유형별 Misfire 정책 설정
- JobDataMap을 통한 실행 파라미터 관리
- Quartz JDBC JobStore 기반 스케줄 영속화
- Job 및 Trigger 실행 상태 조회
- Job 실행 이력 및 예외 정보 조회

### External API Integration (Data Collection)

외부 API 호출 설정을 데이터베이스에서 관리하고 Quartz Job과 연계하여 데이터를 수집합니다.

- 외부 API 등록, 조회, 수정, 삭제
- GET / POST 등 HTTP Method 설정
- API Key / Bearer / Basic 인증 지원
- Header / Query / Body 파라미터 관리
- 정적 및 동적 날짜·시간 파라미터 지원
- YEAR / MONTH / DAY / HOUR 오프셋 설정
- LIST / RANGE 기반 다중 파라미터 값 처리
- API별 재시도 횟수 및 간격 설정
- PAGE 방식 페이징 호출
- 외부 API 즉시 실행 및 Quartz Job 연결
- HTTP 요청 파라미터, 응답 및 재시도 이력 조회
- JSON/XML 응답 Raw Data 저장 및 조회

### Data Transformation

수집된 JSON/XML Raw Data를 규칙 기반으로 가공합니다.

- JSONPath / XPath 기반 필드 추출
- 필드별 데이터 타입 및 기본값 설정
- 필수 필드 검증 및 타입 변환
- 가공 규칙 및 버전 관리
- 샘플 데이터 기반 가공 결과 미리보기
- Raw Data별 가공 작업 생성 및 상태 관리
- Quartz Worker를 통한 가공 작업 처리
- 가공 결과 저장 및 조회
- PENDING / RUNNING / SUCCESS / FAILED 상태 추적

### Data Loading

가공된 JSONB 데이터를 적재 모델의 필드 매핑을 기반으로 PostgreSQL 업무 테이블에 저장합니다.

- 데이터 적재 모델 등록, 조회, 수정
- 적재 모델 및 버전 관리
- 가공 규칙 버전과 적재 모델 연계
- 대상 스키마 및 테이블 컬럼 메타데이터 조회
- 가공 결과 필드와 대상 테이블 컬럼 매핑
- 가공 결과별 적재 대상 조회 및 작업 등록
- Quartz Worker를 통한 데이터 적재 작업 처리
- 작업별 트랜잭션 관리 및 실패 처리
- PENDING / RUNNING / SUCCESS / FAILED 상태 추적
- 적재 처리 건수 및 실행 결과 관리
- 적재 모델별 실제 저장 데이터 조회 및 페이징 지원

### AI Data Analysis (Experimental)

OpenAI API와 Tool Calling을 활용한 데이터 분석 기능의 초기 프로토타입입니다.

- 가공 데이터 기반 자연어 분석
- Tool Calling을 통한 데이터 조회 및 기초 통계 분석

현재 초기 구현 단계이며, 데이터 분석 구조 및 AI 연동 방식의 재설계 를 계획하고있습니다.

### Authentication & Authorization

Spring Security 기반 인증과 역할별 접근 제어를 제공합니다.

- JSON 기반 로그인 및 세션 인증
- CSRF 보호
- 회원가입 활성화 / 비활성화 설정
- ADMIN / USER 역할 기반 접근 제어
- 관리자 전용 기능 및 화면 접근 제한
- Vue 3 / Pinia 기반 사용자 인증 상태 관리

### Monitoring & History

스케줄 실행부터 데이터 수집 및 가공까지 처리 이력을 조회합니다.

- Quartz Job 실행 이력 및 상태 조회
- 외부 API 실행별 호출 이력 조회
- HTTP 요청 및 재시도 결과 추적
- 수집된 Raw Data 조회
- 데이터 가공 작업 및 결과 조회
- 실행 상태 및 기간별 검색



## Architecture

Dynamic Scheduler는 Spring Boot 기반의 백엔드와 Vue 3 기반의 관리자 웹 UI로 구성됩니다.

Quartz를 사용하여 스케줄을 동적으로 관리하고, JDBC JobStore를 통해 Job 및 Trigger 정보를 PostgreSQL에 영속화합니다.

데이터 처리는 수집(Collection), 가공(Transformation), 적재(Loading) 단계로 분리되어 있으며, 각 단계는 독립적인 Quartz Job과 Service를 통해 실행됩니다.

외부 API에서 수집한 JSON/XML Raw Data는 버전 기반의 가공 규칙에 따라 구조화된 JSONB 데이터로 변환됩니다. 가공 결과는 적재 모델의 필드 매핑을 기반으로 PostgreSQL 업무 테이블에 저장됩니다.

Spring Security를 통한 세션 인증 및 역할별 접근 제어를 지원하며, MyBatis를 통해 데이터 조회와 저장을 수행합니다.

AI 데이터 분석 기능은 OpenAI API와 Tool Calling을 활용한 초기 프로토타입으로 구현되어 있습니다.

### System Architecture

```text
                       [ Vue 3 Admin UI ]
                                |
                           HTTPS / REST
                                |
                                v
                       [ Spring Boot API ]
                                |
                       [ Spring Security ]
                                |
              +-----------------+-----------------+
              |                 |                 |
              v                 v                 v
    [ DynamicJobService ] [ Data Management ] [ AI Analysis ]
              |                 |             (Experimental)
              |                 |                 |
              v                 |                 v
     [ Quartz Scheduler ]       |           [ OpenAI API ]
              |                 |
              +------------------------> [ JDBC JobStore ]
              |                 |                 |
              |                 |                 |
              +-----------------+-----------------+
              |                 |                 |
              v                 v                 v
     [ ExternalApiCallJob ] [ TransformWorkerJob ] [ AnalysisLoadWorkerJob ]
              |                 |                 |
              v                 v                 v
     [ External API       [ Transform        [ Analysis Load
       Execution Service ]   Worker Service ]   Worker Service ]
              |                 |                 |
              v                 v                 v
     [ External APIs ]    [ Transform        [ Analysis Load
              |             Execution ]        Execution ]
              v                 |                 |
     [ Raw Data ]               v                 v
                         [ Transform Results ] [ Business Data ]
                              (JSONB)              |
              |                 |                  |
              +-----------------+------------------+
                                |
                           [ MyBatis ]
                                |
                                v
                         [ PostgreSQL ]

     - Quartz JobStore / Scheduler Metadata
     - Job Execution Logs
     - External API Configuration / Execution Snapshots
     - HTTP Request / Call Logs
     - Raw Data (JSON / XML)
     - Transform Rules / Versions / Fields
     - Transform Tasks / Results (JSONB)
     - Analysis Models / Versions / Field Mappings
     - Analysis Load Sources / Status
     - Business Tables
     - User / Role Information
```

### Data Processing Flow

데이터 수집(Collection), 가공(Transformation), 적재(Loading)는
별도의 실행 단계로 관리됩니다.

가공 및 적재 작업은 PostgreSQL 테이블에서 상태를 관리하며,
각각의 Quartz Worker가 주기적으로 대기 중인 작업을 선점하여 처리합니다.

```text
[ Data Collection ]

ExternalApiCallJob
       |
       v
외부 API 호출 (인증 / 페이징 / 재시도)
       |
       v
HTTP 요청·응답 이력 및 Raw Data 저장 (JSON/XML)
       |
       v
가공 작업 등록 (수동 / 자동)
       |
       v
가공 대기 상태 (PENDING)


[ Data Transformation ]

TransformWorkerJob
       |
       v
가공 작업 선점 (RUNNING)
       |
       v
가공 규칙 및 버전 적용
       |
       v
필드 추출 / 데이터 조합 / 타입 변환
       |
       +---- 실패 ----> Rollback / FAILED / 오류 기록
       |
       v
가공 결과 저장 (JSONB) / SUCCESS
       |
       +----------------------> AI Data Analysis
       |                        (Experimental)
       v
적재 모델 및 가공 결과 선택
       |
       v
적재 작업 등록 (PENDING)


[ Data Loading ]

AnalysisLoadWorkerJob
       |
       v
적재 작업 선점 (RUNNING)
       |
       v
적재 모델 버전 및 필드 매핑 적용
       |
       v
대상 컬럼 검증 / 데이터 타입 변환
       |
       v
PostgreSQL 업무 테이블 적재
       |
       +---- 실패 ----> Rollback / FAILED / 오류 기록
       |
       v
적재 건수 기록 / SUCCESS
       |
       v
적재 데이터 조회
```

가공 및 적재 작업의 실행 안정성과 데이터 정합성을 위해
다음과 같은 처리 방식을 적용합니다.

- PostgreSQL 기반 작업 상태 관리
- FOR UPDATE SKIP LOCKED를 활용한 원자적 작업 선점
- 작업 선점과 실제 실행의 트랜잭션 분리
- 개별 작업 단위의 독립적인 트랜잭션 처리
- 데이터 저장과 SUCCESS 상태 변경을 동일 트랜잭션에서 처리
- 별도 트랜잭션을 통한 FAILED 상태 및 오류 기록
- 개별 작업 실패 시에도 나머지 작업 계속 처리

### Core Components

| Component | Description |
| --- | --- |
| DynamicJobService | Quartz Job 및 Trigger 등록, 수정, 삭제와 상태 제어 |
| JobClassRegistry | 실행 가능한 Quartz Job 클래스 관리 |
| JobLogListener | Quartz Job 실행 시작, 종료 및 실패 이력 관리 |
| ExternalApiCallJob | Quartz 기반 외부 API 데이터 수집 실행 |
| ExternalApiExecutionService | 외부 API 인증, 파라미터 처리, 페이징 및 재시도 |
| CollectRawDataService | 외부 API 응답 원문(JSON/XML) 저장 |
| TransformWorkerJob | 대기 중인 데이터 가공 작업 실행 |
| TransformWorkerService | 가공 작업 선점 및 개별 작업 처리 |
| TransformTaskStateService | 가공 작업 선점 및 실패 상태 관리 |
| TransformTaskExecutionService | 가공 규칙 적용, 필드 추출, 데이터 조합 및 결과 저장 |
| AnalysisModelService | 데이터 적재 모델, 버전 및 필드 매핑 관리 |
| AnalysisLoadService | 적재 대상 조회 및 적재 작업 등록 |
| AnalysisLoadWorkerJob | 대기 중인 데이터 적재 작업 실행 |
| AnalysisLoadWorkerService | 적재 작업 선점 및 개별 작업 처리 |
| AnalysisLoadStateService | 적재 작업 선점 및 실패 상태 관리 |
| AnalysisLoadExecutionService | 필드 매핑, 타입 변환 및 업무 테이블 적재 |
| AnalysisLoadResultService | 적재 모델별 실제 저장 데이터 조회 |
| AiAnalysisService | Tool Calling 기반 데이터 분석 초기 프로토타입 |
| Spring Security | 세션 인증, CSRF 보호 및 역할 기반 접근 제어 |
| Quartz JDBC JobStore | Job 및 Trigger 정보 영속화 |
| MyBatis | SQL 실행 및 데이터 매핑 |
| PostgreSQL | 스케줄, 수집, 가공, 적재 및 실행 이력 영속화 |

---

## Tech Stack

### Backend

| Technology | Version / Description |
| --- | --- |
| Java | 17 |
| Spring Boot | 3.5.3 |
| Spring Web | REST API |
| Spring Security | Session Authentication / CSRF |
| Quartz Scheduler | 2.5.0 |
| MyBatis | SQL Mapper |
| Gradle | Build Tool |

### Database

| Technology | Description |
| --- | --- |
| PostgreSQL | Application Database |
| Quartz JDBC JobStore | Persistent Job / Trigger Storage |

---

## Getting Started

### Prerequisites

로컬 환경에서 애플리케이션을 실행하려면 다음 항목이 필요합니다.

- Java 17
- PostgreSQL
- Git

Gradle Wrapper가 포함되어 있으므로 Gradle을 별도로 설치할 필요는 없습니다.

### 1. Clone Repository

```bash
git clone https://github.com/ideale17/spring-quartz-scheduler.git
cd spring-quartz-scheduler
```

### 2. Database Setup

PostgreSQL에 `scheduler` 데이터베이스를 생성합니다.

```sql
CREATE DATABASE scheduler;
```

생성한 데이터베이스에 접속한 후 아래 SQL 파일을 순서대로 실행합니다.

```text
src/main/resources/db/postgresql/
├── 01_quartz.sql
├── 02_scheduler.sql
├── 03_security.sql
└── 04_init_data.sql
```

각 SQL 파일의 역할은 다음과 같습니다.

| File | Description |
| --- | --- |
| `01_quartz.sql` | Quartz JDBC JobStore 테이블 생성 |
| `02_scheduler.sql` | 스케줄러, 외부 API, 데이터 수집 및 가공 관련 테이블 생성 |
| `03_security.sql` | 사용자 및 권한 관련 테이블 생성 |
| `04_init_data.sql` | 기본 권한 데이터 등록 |

Quartz Schema 자동 생성을 사용하지 않으므로 최초 실행 전에 SQL 파일을 직접 적용해야 합니다.

> **주의:** `01_quartz.sql`에는 기존 Quartz 테이블을 삭제하는 구문이 포함되어 있습니다. 데이터 손실을 방지하기 위해 신규 데이터베이스에서 실행하세요.

### 3. Database Configuration

다음 예제 파일을 복사합니다.

```text
config/application-postgresql-secret.properties.example
```

복사한 파일명을 `application-postgresql-secret.properties`로 변경합니다.

```text
config/application-postgresql-secret.properties
```

PostgreSQL 접속정보를 입력합니다.

```properties
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

Local Profile에서는 `application-local.properties`에 설정된 다음 데이터베이스를 사용합니다.

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/scheduler
```

데이터베이스 접속정보는 다음 설정을 통해 외부 파일에서 로드합니다.

```properties
spring.config.import=optional:file:./config/application-postgresql-secret.properties
```

실제 DB 계정 정보가 포함된 `application-postgresql-secret.properties` 파일은 Git에서 제외됩니다.

### 4. Encryption Key Configuration

외부 API의 API Key, Bearer Token, Basic 인증 비밀번호 등 민감한 인증정보는 AES-256-GCM 방식으로 암호화하여 저장합니다.

애플리케이션 실행 시 Base64로 인코딩된 32바이트 암호화 키가 필요합니다.

OpenSSL을 사용할 수 있는 환경에서는 다음 명령으로 키를 생성할 수 있습니다.

```bash
openssl rand -base64 32
```

생성된 키를 `SCHEDULER_ENCRYPTION_KEY` 환경변수에 설정합니다.

**Windows CMD**

```bat
set SCHEDULER_ENCRYPTION_KEY=YOUR_BASE64_ENCRYPTION_KEY
```

**Windows PowerShell**

```powershell
$env:SCHEDULER_ENCRYPTION_KEY="YOUR_BASE64_ENCRYPTION_KEY"
```

**Linux / macOS**

```bash
export SCHEDULER_ENCRYPTION_KEY=YOUR_BASE64_ENCRYPTION_KEY
```

애플리케이션에서는 다음 설정을 통해 환경변수를 참조합니다.

```properties
app.security.encryption-key=${SCHEDULER_ENCRYPTION_KEY}
```

환경변수가 없거나 Base64 디코딩 결과가 32바이트가 아니면 애플리케이션이 정상적으로 시작되지 않습니다.

### 5. Run Application

Local Profile과 PostgreSQL Profile을 활성화하여 애플리케이션을 실행합니다.

**Windows**

```bat
.\gradlew.bat bootRun --args="--spring.profiles.active=local,postgresql"
```

**Linux / macOS**

```bash
./gradlew bootRun --args="--spring.profiles.active=local,postgresql"
```

애플리케이션은 기본적으로 다음 주소에서 실행됩니다.

```text
http://localhost:8080
```

### 6. Initial User Setup

초기 SQL은 `ROLE_ADMIN`, `ROLE_USER` 권한 정보만 생성하며 기본 사용자 계정은 생성하지 않습니다.

로컬에서 사용자 계정을 생성하려면 `application.properties`의 회원가입 설정을 활성화합니다.

```properties
app.security.signup-enabled=true
```

회원가입이 완료되면 필요에 따라 다시 비활성화할 수 있습니다.

```properties
app.security.signup-enabled=false
```

기본 설정은 `false`이며, 운영 환경에서도 회원가입을 비활성화하는 것을 권장합니다.

### 7. AI Configuration (Optional)

AI 데이터 분석 기능을 사용하려면 OpenAI API 키가 필요합니다.

다음 환경변수를 설정합니다.

**Windows PowerShell**

```powershell
$env:AI_ENABLED="true"
$env:OPENAI_API_KEY="YOUR_OPENAI_API_KEY"
```

**Linux / macOS**

```bash
export AI_ENABLED=true
export OPENAI_API_KEY=YOUR_OPENAI_API_KEY
```

애플리케이션은 다음 환경변수와 설정을 사용합니다.

| Environment Variable | Description | Default |
| --- | --- | --- |
| `AI_ENABLED` | AI 분석 기능 활성화 여부 | `false` |
| `OPENAI_API_KEY` | OpenAI API 인증 키 | 미설정 |
| `OPENAI_MODEL` | 사용할 OpenAI 모델 | `gpt-5-mini` |

AI 분석 기능을 사용하지 않는 경우 별도 설정이 필요하지 않습니다.

OpenAI API 사용에 따른 비용은 별도로 발생할 수 있습니다.

---

## Project Structure

```text
spring-quartz-scheduler/
├── .github/
│   └── workflows/                 # GitHub Actions CI/CD 설정
├── config/                        # 외부 설정 파일 예제
├── docs/                          # 도메인별 설계 및 개발 문서
│   ├── agents/                    # AI 에이전트 작업 및 이슈 관리 규칙
│   ├── coding-conventions.md      # 코드 작성 규칙
│   ├── database.md                # DB 및 트랜잭션 설계
│   ├── external-api.md            # 외부 API 수집 설계
│   ├── quartz.md                  # Quartz Scheduler 설계
│   ├── security.md                # 인증 및 보안 설계
│   └── transform.md               # 데이터 가공 설계
├── gradle/
│   └── wrapper/                   # Gradle Wrapper 설정
├── src/
│   ├── main/
│   │   ├── java/com/kji/scheduler/
│   │   │   ├── config/            # Spring 및 Quartz 설정
│   │   │   ├── controller/        # REST API 엔드포인트
│   │   │   ├── crypto/            # 인증정보 암호화
│   │   │   ├── dto/               # 데이터 전송 객체
│   │   │   ├── entity/            # 데이터베이스 엔티티
│   │   │   ├── job/               # Quartz 수집·가공·적재 Job
│   │   │   ├── listener/          # Quartz 실행 이벤트 및 이력 처리
│   │   │   ├── mapper/            # MyBatis Mapper 인터페이스
│   │   │   ├── repository/        # 데이터 접근 및 Job 클래스 관리
│   │   │   ├── security/          # 인증 및 권한 처리
│   │   │   ├── service/           # 비즈니스 로직 및 트랜잭션 처리
│   │   │   ├── typehandler/       # MyBatis 데이터 타입 매핑
│   │   │   └── SchedulerApplication.java
│   │   └── resources/
│   │       ├── db/postgresql/
│   │       │   ├── migration/     # DB 스키마 마이그레이션 SQL
│   │       │   ├── 01_quartz.sql
│   │       │   ├── 02_scheduler.sql
│   │       │   ├── 03_security.sql
│   │       │   └── 04_init_data.sql
│   │       ├── mapper/            # MyBatis Mapper XML
│   │       └── application*.properties
│   └── test/
│       └── java/com/kji/scheduler/
│           ├── controller/        # REST API 테스트
│           ├── crypto/            # 암호화 테스트
│           ├── job/               # Quartz Job 테스트
│           └── service/           # 서비스 및 업무 로직 테스트
├── AGENTS.md                      # AI 에이전트 및 프로젝트 개발 규칙
├── Dockerfile                     # Docker 이미지 빌드 설정
├── build.gradle                   # Gradle 빌드 및 의존성 설정
├── settings.gradle                # Gradle 프로젝트 설정
├── gradlew                        # Gradle Wrapper (Linux/macOS)
├── gradlew.bat                    # Gradle Wrapper (Windows)
└── README.md
```

---

## Roadmap

향후 개선 및 확장을 검토하고 있는 기능입니다.

- [ ] 외부 API 수집 및 데이터 가공 작업의 운영 안정성 개선
- [ ] 실패한 데이터 가공 작업의 재처리 기능 개선
- [ ] AI 데이터 분석 기능 확장
- [ ] 실행 이력 및 데이터 처리 모니터링 개선
- [ ] 테스트 코드 및 예외 처리 보완

---


## Related Repositories

- [Scheduler UI](https://github.com/ideale17/scheduler-ui) — Vue 3 기반 관리자 웹 애플리케이션

---

## Demo

- https://scheduler.pomibori.dev/

---
