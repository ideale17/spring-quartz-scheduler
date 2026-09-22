# Dynamic Scheduler System

Spring Boot와 Quartz를 기반으로 구현한 동적 스케줄 관리 시스템입니다.

기존 Quartz 기반 스케줄 개발 경험을 바탕으로 Spring Boot 환경에서 Quartz JDBC JobStore를 활용한 Job/Trigger 영속화, 동적 스케줄 관리, Misfire 처리, 실행 이력 관리 및 외부 API 연계 기능을 구현했습니다.

관리 화면에서 스케줄을 등록·수정하고 실행 상태를 제어할 수 있으며, 스케줄과 외부 API 정보를 분리하여 다양한 API 호출 작업을 Job으로 실행할 수 있도록 구성했습니다.

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
* 관리자용 Web UI 구현
* 인증이 적용된 관리 시스템 구성

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

### Frontend

* Vue 3
* Vue Router
* Axios
* Vite
* Tailwind CSS

### Database

* Quartz JDBC JobStore
* RDBMS
* MyBatis 기반 관리 데이터 조회

---

## 3. 주요 기능

### 3.1 동적 스케줄 관리

관리 화면에서 Quartz Job과 Trigger를 동적으로 관리할 수 있습니다.

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

External API 기본정보와 함께 다음 위치의 동적 파라미터를 관리할 수 있습니다.

* Query
* Path
* Header
* Body

Job 코드에 특정 API의 URL이나 파라미터를 직접 작성하지 않고, DB에 등록된 API 정보를 기반으로 호출할 수 있도록 구성했습니다.

### 3.6 인증 및 보안

Spring Security 기반 인증 후 관리 기능을 사용할 수 있도록 구성했습니다.

* JSON 기반 로그인
* 세션 기반 인증
* 로그아웃
* 세션 만료 처리
* CSRF 보호
* Vue Router 인증 가드

SPA 환경에서 CSRF Token을 Cookie로 전달하고 Axios 요청 시 `X-XSRF-TOKEN` 헤더를 통해 서버로 전송합니다.

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

Job 등록 시 화면에서 전달받은 파라미터를 `JobDataMap`에 저장합니다.

Job 수정 시에도 새로운 파라미터로 `JobDataMap`을 갱신하여 실제 Job 실행 시 변경된 값을 사용할 수 있도록 구성했습니다.

---

## 7. External API 데이터 정합성 처리

External API를 사용하는 Job이 존재하는 경우 해당 API를 바로 삭제하지 못하도록 처리했습니다.

External API 삭제 요청 시 해당 API를 사용하는 Job이 존재하는지 먼저 확인하고, 사용 중이면 삭제를 차단합니다.

이를 통해 Job에서 사용 중인 API가 삭제되어 실행 오류가 발생하는 상황을 방지했습니다.

---

## 8. 관리자 화면

Vue 3 기반으로 Scheduler 관리 화면을 구현했습니다.

### Dashboard

* 스케줄 현황 확인
* 주요 상태 정보 확인

### Job 목록

* 등록된 Job 조회
* 실행 상태 확인
* Job 실행 / 중지 / 재시작
* Job 수정 / 삭제
* 일괄 작업

### Job 등록

* Job 클래스 선택
* Job 이름 / 그룹 설정
* SIMPLE / CRON 선택
* 실행 주기 설정
* Misfire 정책 설정
* Job 파라미터 설정

### Job 수정

* 기존 Job 정보 조회
* 스케줄 수정
* Misfire 정책 수정
* Job 파라미터 수정

### 실행 이력

* Job 실행 이력 조회
* 실행 결과 확인
* 실행시간 확인

### External API

* API 목록
* API 등록
* API 상세 조회
* API 수정
* API 삭제
* API 호출 이력

### Scheduler 정보

* 현재 Quartz Scheduler 정보 조회

---

## 9. 주요 구현 과정에서 고민한 부분

### 9.1 애플리케이션 DB 정보와 Quartz 상태의 차이

관리 화면에서 필요한 정보와 실제 Quartz Scheduler의 상태는 항상 동일한 의미를 가지지 않습니다.

따라서 관리 데이터와 Quartz Trigger 상태를 구분하고, 실제 실행 상태가 필요한 경우 Quartz Scheduler에서 상태를 조회하도록 구성했습니다.

### 9.2 Job 수정과 실행 상태 분리

스케줄 정보를 수정하는 것과 Job을 시작하는 것은 서로 다른 작업으로 처리했습니다.

중지 상태의 Job을 수정했을 때 자동으로 실행되지 않도록 기존 Trigger 상태를 확인하고 수정 완료 후 기존 상태를 복원하도록 처리했습니다.

### 9.3 스케줄과 External API의 분리

Job마다 외부 API 정보를 직접 가지고 있을 경우 API URL이나 파라미터 변경 시 여러 스케줄을 함께 수정해야 하는 문제가 발생할 수 있습니다.

따라서 External API 정보를 별도 관리하고 Job에서는 해당 API를 참조하는 방식으로 분리했습니다.

### 9.4 Misfire 정책 명시화

Quartz의 Misfire Instruction 값을 화면에 직접 노출하지 않고 애플리케이션에서 의미를 알 수 있는 정책으로 관리했습니다.

사용자가 선택한 정책을 서버에서 Quartz의 실제 Misfire Instruction으로 변환하여 적용하도록 구현했습니다.

---

## 10. 화면

추후 실제 화면 캡처를 추가할 예정입니다.

예정 화면:

* Dashboard
* Job 목록
* Job 등록
* Job 수정
* 실행 이력
* External API 관리

---

## 11. Repository

### Backend

`spring-quartz-scheduler`

https://github.com/ideale17/spring-quartz-scheduler

### Frontend

`scheduler-ui`

https://github.com/ideale17/scheduler-ui

---

## 12. Demo

배포 완료 후 Demo URL을 추가할 예정입니다.

---

## 13. 향후 개선

* Quartz Job : Trigger 1:N 구조 확장
* 실행 실패에 대한 재시도 정책 검토
* 사용자별 권한 관리
* Scheduler 운영 모니터링 강화
* 실제 외부 데이터 수집 Job 확대
* 배포 및 운영 환경 구성 개선
