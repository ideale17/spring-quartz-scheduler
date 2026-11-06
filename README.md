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

### External API Integration

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

### AI Data Analysis

가공된 데이터를 대상으로 OpenAI API 기반 분석 기능을 제공합니다.

- 외부 API, 가공 규칙, 버전 및 기간별 분석 대상 조회
- 데이터셋 정보 및 가공 결과 미리보기
- 자연어 기반 데이터 분석 요청
- Tool Calling을 통한 데이터 조회 및 분석
- 데이터 건수, 필드 정보 및 통계 조회
- AI 분석 요청 및 결과 표시

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

Quartz가 스케줄 실행을 관리하며, 외부 API를 통해 수집한 데이터는 PostgreSQL에 저장됩니다. 수집된 Raw Data는 규칙 기반 가공 작업을 거쳐 구조화된 데이터로 저장되며, AI 분석 기능에서 활용할 수 있습니다.

### System Architecture

```text
                  [ Vue 3 Admin UI ]
                           |
                      HTTPS / REST
                           |
                           v
                 [ Spring Boot API ]
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
      [ Spring       [ Dynamic      [ AI Analysis ]
        Security ]     Job Service ]       |
                           |               v
                           v          [ OpenAI API ]
                    [ Quartz Scheduler ]
                           |
                     [ JDBC JobStore ]
                           |
               +-----------+-----------+
               |                       |
               v                       v
      [ ExternalApiCallJob ]   [ TransformWorkerJob ]
               |                       |
               v                       v
      [ External API Service ] [ Transform Service ]
               |                       |
               v                       v
         [ External APIs ]      [ Transform Results ]
               |
               v
         [ Raw Data Storage ]
               |
               v
       [ Transform Task Queue ]

                  [ PostgreSQL ]
       - Scheduler / Job Metadata
       - Quartz JobStore
       - External API Configuration
       - Execution / Request / Call Logs
       - Raw Data
       - Transform Rules / Tasks / Results
       - User / Role Information
```

### Data Processing Flow

데이터 수집과 가공은 별도의 실행 단계로 관리됩니다.

```text
Quartz Job 실행
       |
       v
외부 API 호출
       |
       v
HTTP 응답 및 Raw Data 저장
       |
       v
가공 작업 생성 (PENDING)
       |
       v
TransformWorkerJob
       |
       v
가공 작업 선점 (RUNNING)
       |
       v
가공 규칙 적용
       |
       +---- 성공 ----> SUCCESS / 결과 저장
       |
       +---- 실패 ----> FAILED
       |
       v
AI 데이터 분석
```


### Core Components

| Component | Description |
| --- | --- |
| DynamicJobService | Quartz Job 및 Trigger 등록, 수정, 삭제와 상태 제어 |
| JobClassRegistry | 실행 가능한 Quartz Job 클래스 관리 |
| ExternalApiCallJob | Quartz 기반 외부 API 수집 실행 |
| External API Service | 인증, 파라미터 처리, 페이징 및 재시도 |
| Raw Data Storage | 외부 API 응답 원문 저장 |
| TransformWorkerJob | 대기 중인 가공 작업 선점 및 실행 |
| Transform Service | JSON/XML 필드 추출 및 데이터 변환 |
| AI Analysis Service | Tool Calling 기반 데이터 조회 및 분석 |
| Spring Security | 세션 인증 및 역할 기반 접근 제어 |
| PostgreSQL | 스케줄, 수집, 가공 및 실행 이력 영속화 |

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

> **주의:** 암호화 키는 소스 코드나 Git 저장소에 포함하지 마세요. 이미 암호화된 인증정보가 존재하는 경우 동일한 키를 유지해야 기존 데이터를 복호화할 수 있습니다.

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
├── config/                         # 외부 설정 파일 예제
├── src/
│   ├── main/
│   │   ├── java/com/kji/scheduler/
│   │   │   ├── config/             # Spring 및 Quartz 설정
│   │   │   ├── controller/         # REST API 엔드포인트
│   │   │   ├── crypto/             # 인증정보 암호화
│   │   │   ├── dto/                # 데이터 전송 객체
│   │   │   ├── entity/             # 데이터베이스 엔티티
│   │   │   ├── job/                # Quartz Job 구현
│   │   │   ├── listener/           # Quartz 실행 이벤트 처리
│   │   │   ├── mapper/             # MyBatis Mapper 인터페이스
│   │   │   ├── repository/         # 데이터 접근 Repository
│   │   │   ├── security/           # 인증 및 보안 처리
│   │   │   ├── service/            # 비즈니스 로직
│   │   │   └── SchedulerApplication.java
│   │   └── resources/
│   │       ├── db/postgresql/      # PostgreSQL 초기화 SQL
│   │       ├── mapper/             # MyBatis Mapper XML
│   │       └── application*.properties
│   └── test/java/                  # 테스트 코드
├── build.gradle                    # Gradle 빌드 설정
├── gradlew                         # Gradle Wrapper (Linux/macOS)
├── gradlew.bat                     # Gradle Wrapper (Windows)
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
