# Security 인증 및 권한 설계

## 1. 개요

Dynamic Scheduler는 Spring Security 기반의 세션 인증과 역할(Role) 기반 접근 제어를 사용한다.

주요 기능:

- JSON 기반 로그인
- HTTP 세션 인증 및 로그아웃
- 관리자·일반 사용자 권한 구분
- API별 접근 권한 관리
- CSRF 보호
- CORS 설정
- 회원가입 허용 여부 관리
- 비밀번호 암호화

인증 및 인가 정책은 백엔드에서 적용한다. 프론트엔드의 메뉴 표시나 라우트 제한은 백엔드 권한 검사를 대체하지 않는다.

## 2. 주요 구성 요소

- `SecurityConfig`: Spring Security 필터 체인 및 접근 권한 설정
- `JsonLoginAuthenticationFilter`: JSON 로그인 요청 처리
- `SpaCsrfTokenRequestHandler`: SPA 환경의 CSRF 토큰 처리
- `AuthController`: 현재 사용자 정보 및 회원가입 관련 API
- `AppUserService`: 회원가입 검증 및 사용자 생성
- `AppUserRepository`: 사용자 데이터 조회·저장
- `AppRoleRepository`: 사용자 권한 조회
- `AppUser`: 사용자 정보
- `AppRole`: 사용자 역할 정보

Spring Security의 `AuthenticationManager`가 로그인 인증을 수행한다.

사용자와 권한의 영속화에는 기존 Spring Data JPA Repository를 사용한다.

## 3. 로그인 및 인증 처리

### 로그인

로그인 엔드포인트는 `/auth/login`이다.

`JsonLoginAuthenticationFilter`에서 다음 순서로 처리한다.

1. JSON 요청 본문에서 username과 password를 읽는다.
2. `UsernamePasswordAuthenticationToken`을 생성한다.
3. `AuthenticationManager`에 인증을 요청한다.
4. 인증 결과를 Spring Security 세션에 저장한다.
5. 성공 시 HTTP 200, 실패 시 HTTP 401을 반환한다.

기본 Spring Security 폼 로그인과 HTTP Basic 인증은 비활성화되어 있다.

### 현재 사용자 조회

`GET /auth/me`에서 현재 인증된 사용자 정보를 반환한다.

주요 응답 정보:

- `username`
- `roles`

현재 사용자 정보는 Spring Security의 `Authentication` 객체에서 조회한다.

### 로그아웃

로그아웃 엔드포인트는 `/auth/logout`이다.

- HTTP 세션 무효화
- 인증정보 제거
- `JSESSIONID` 쿠키 삭제 요청
- 성공 시 HTTP 200 반환

## 4. 세션 관리

현재 인증 방식은 서버 세션 기반이다.

주요 설정:

```properties
server.servlet.session.timeout=30m
```

- 세션은 필요한 경우 생성하는 `IF_REQUIRED` 정책을 사용한다.
- 인증 성공 후 HTTP 세션에 인증 상태를 유지한다.
- 이후 요청은 세션 쿠키를 통해 인증 상태를 확인한다.
- 세션이 없거나 만료된 요청은 인증이 필요한 API에 접근할 수 없다.
- 인증되지 않은 요청에 대해서는 HTTP 401을 반환한다.

세션 기반 인증 구조를 JWT 등 다른 방식으로 임의 변경하지 않는다.

세션 설정을 변경할 경우 로그인 유지, 로그아웃 및 프론트엔드 인증 상태 확인에 미치는 영향을 검토한다.

## 5. 역할 및 접근 권한

Spring Security의 역할 기반 접근 제어를 사용한다.

현재 주요 역할:

- `ROLE_ADMIN`: 관리자
- `ROLE_USER`: 일반 사용자

`SecurityConfig`에서는 `hasRole("ADMIN")`을 사용하여 관리자 권한을 검사한다.

### API 접근 정책

**공개 API**

- `/auth/login`: 로그인 처리
- `/auth/logout`: 로그아웃 처리
- `POST /auth/signup`: 회원가입 요청 (별도 회원가입 설정 검사)
- `GET /auth/signup-enabled`: 회원가입 허용 여부 조회

**관리자 전용 API (ADMIN)**

- `POST, PUT, DELETE /jobs/**`: Job 등록·수정·삭제 및 실행 관련 API
- `POST, PUT, DELETE /external-api/**`: External API 등록·수정·삭제 및 실행 관련 API
- `POST /transform-rule/add`: 가공 규칙 등록
- `PUT /transform-rule/update`: 가공 규칙 수정
- `POST /transform-task/add`: 가공 대상 등록
- `POST /ai-analysis/execute`: AI 분석 실행

**인증된 사용자 API**

- `GET /auth/me`: 현재 사용자 정보 조회
- 그 외 명시적인 공개 또는 관리자 전용 규칙에 해당하지 않는 API는 인증된 사용자에게 허용한다.

### 권한 적용 기준

- 관리자 전용 API는 백엔드에서 권한을 검증한다.
- 일반 사용자에게 허용되는 조회 API와 관리자 변경 API를 구분한다.
- 새로운 API를 추가할 때 HTTP Method와 URL에 맞는 권한 정책을 확인한다.
- 기존 관리자 전용 기능을 인증만 필요한 기능으로 임의 변경하지 않는다.
- HTTP Method별 경로 규칙이 적용되지 않는 새로운 엔드포인트는 명시적인 권한 설정 필요 여부를 확인한다.

## 6. CSRF 보호

세션 기반 인증에 대한 CSRF 보호를 사용한다.

주요 구성:

- `CookieCsrfTokenRepository.withHttpOnlyFalse()`
- `SpaCsrfTokenRequestHandler`
- `XSRF-TOKEN` 쿠키
- `X-XSRF-TOKEN` 요청 헤더

### CSRF 처리

1. Spring Security가 CSRF 토큰을 관리한다.
2. SPA에서 읽을 수 있도록 토큰 쿠키를 제공한다.
3. 클라이언트는 필요한 요청에 CSRF 토큰을 헤더로 전달한다.
4. `SpaCsrfTokenRequestHandler`가 헤더 토큰을 처리한다.
5. CSRF 검증이 필요한 요청은 서버에서 토큰을 검증한다.

### CSRF 예외 경로

현재 다음 경로는 CSRF 검증에서 제외되어 있다.

- `/auth/login`
- `/auth/logout`
- `/auth/signup`

CSRF 검증 예외는 해당 API의 모든 보안 검사를 비활성화한다는 의미가 아니다.

신규 API를 추가할 때 CSRF 보호를 임의로 비활성화하지 않는다.

## 7. CORS 설정

`SecurityConfig.corsConfigurationSource()`에서 CORS 정책을 관리한다.

허용 Origin은 다음 설정으로 지정한다.

```properties
app.security.cors-allowed-origins=
```

주요 설정:

- 허용 Origin은 환경별 설정을 사용한다.
- 허용 HTTP Method: GET, POST, PUT, DELETE, PATCH, OPTIONS
- 허용 요청 헤더: Content-Type, X-XSRF-TOKEN, Authorization
- 자격 증명을 포함한 요청을 허용한다.

현재 기본 Origin 목록은 비어 있으며, 로컬 프로필에서는 Vue 개발 서버 주소를 허용하도록 설정되어 있다.

운영 환경에서 모든 Origin을 무조건 허용하지 않는다.

CORS 설정을 변경할 경우 세션 쿠키, CSRF 헤더 및 프론트엔드 통신에 미치는 영향을 확인한다.

## 8. 회원가입 및 비밀번호 관리

회원가입은 `AppUserService.signup()`에서 처리한다.

### 회원가입 정책

현재 기본 설정:

```properties
app.security.signup-enabled=false
```

- 회원가입 허용 여부를 먼저 확인한다.
- 아이디와 비밀번호의 필수 여부를 검증한다.
- 비밀번호 확인값의 일치 여부를 검사한다.
- 중복 사용자명을 확인한다.
- 기본 사용자 권한 `ROLE_USER`를 부여한다.
- 신규 사용자는 활성 상태로 등록한다.

회원가입 허용 여부는 `GET /auth/signup-enabled`로 조회한다.

현재 기본 설정에서는 회원가입이 비활성화되어 있다.

### 비밀번호

- `BCryptPasswordEncoder`를 사용한다.
- 신규 사용자 비밀번호는 인코딩하여 저장한다.
- 평문 비밀번호를 DB에 저장하지 않는다.
- 비밀번호와 인증 비밀값을 애플리케이션 로그에 노출하지 않는다.

회원가입을 공개적으로 활성화할 경우 입력 검증과 계정 생성 정책을 별도로 검토한다.

## 9. 인증 오류 및 보안 설정

### 인증 및 권한 오류

- 로그인 실패는 HTTP 401을 반환한다.
- 인증되지 않은 사용자의 보호 API 접근은 HTTP 401을 반환한다.
- 권한이 부족한 요청은 Spring Security의 접근 거부 처리 대상이다.
- CSRF 검증 실패는 인증 실패와 별도로 처리된다.

인증 실패와 권한 부족을 동일한 문제로 취급하지 않는다.

### 민감 정보 관리

다음 정보는 로그, 응답 및 소스 코드에 노출하지 않는다.

- 사용자 비밀번호
- 세션 식별자
- CSRF 토큰
- API Key 및 Bearer Token
- 데이터 암호화 키

External API 인증정보 저장·마스킹에 관한 상세 구조는 `docs/external-api.md`에서 관리한다.

### 설정 변경

Spring Security 필터 순서, 세션 저장 방식, CSRF 예외 경로 또는 권한 매처를 수정할 경우 기존 인증 흐름에 미치는 영향을 확인한다.

## 10. 변경 시 확인할 사항

인증 및 권한 기능을 변경할 때 다음 사항을 확인한다.

- JSON 로그인 요청 및 응답의 호환성
- 로그인 성공 후 세션 유지 여부
- 로그아웃 시 세션 무효화
- `/auth/me` 사용자 및 권한 응답
- 관리자 전용 API의 접근 권한
- 일반 사용자 조회 API의 접근 권한
- 신규 API의 HTTP Method별 권한 적용
- CSRF 쿠키 및 요청 헤더 처리
- 기존 CSRF 예외 경로
- CORS 허용 Origin 및 세션 쿠키 전송
- 회원가입 활성화 정책
- 비밀번호 인코딩 및 민감정보 보호
- 기존 프론트엔드 로그인·권한 관리 기능과의 호환성

인증 방식이나 권한 정책을 변경할 경우 관리자, 일반 사용자, 비로그인 사용자 각각의 접근 결과를 확인한다.

DB 테이블과 사용자·권한 데이터의 상세 구조는 `docs/database.md`에서 관리한다.

코딩 스타일과 일반적인 AI 작업 절차는 `AGENTS.md` 및 `docs/coding-conventions.md`를 따른다.