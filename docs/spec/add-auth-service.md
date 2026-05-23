Kotlin + Spring Boot로 만든 api-gateway 프로젝트를 개선해줘.

현재 상황:

- board-service가 존재한다.

- auth-service가 존재한다.

- api-gateway는 현재 board-service로 단순 라우팅만 하고 있다.

- auth-service는 회원가입, 로그인, Access Token, Refresh Token, /auth/me 기능을 제공한다.

- 이제 api-gateway에서 JWT 인증 필터를 추가하고 싶다.

목표:

- api-gateway는 외부 요청의 단일 진입점이다.

- api-gateway는 인증 검증, 라우팅, Request ID 생성, 공통 로깅 같은 횡단 관심사만 담당한다.

- api-gateway에는 게시글/댓글/사용자 도메인 비즈니스 로직을 넣지 않는다.

- JWT 검증 후 내부 서비스에 사용자 컨텍스트를 헤더로 전달한다.

- auth-service, board-service는 gateway 뒤에 위치한다고 가정한다.

기술 스택:

- Kotlin

- Spring Boot 3.x

- Spring Cloud Gateway

- Spring WebFlux 기반 Gateway

- Actuator

- Gradle Kotlin DSL

현재 서비스 포트:

- api-gateway: 8080

- board-service: 8081

- auth-service: 8082

라우팅 요구사항:

1. /auth/** 요청은 auth-service로 라우팅한다.

2. /boards/** 요청은 board-service로 라우팅한다.

3. /posts/** 요청은 board-service로 라우팅한다.

4. 추후 /comments/**가 생기면 board-service로 라우팅할 수 있게 구조를 열어둔다.

application.yml 라우팅 예시:

spring:

cloud:

    gateway:

      routes:

        - id: auth-service

          uri: ${services.auth-service.url:http://localhost:8082}

          predicates:

            - Path=/auth/**

        - id: board-service-boards

          uri: ${services.board-service.url:http://localhost:8081}

          predicates:

            - Path=/boards/**

        - id: board-service-posts

          uri: ${services.board-service.url:http://localhost:8081}

          predicates:

            - Path=/posts/**

services:

auth-service:

    url: http://localhost:8082

board-service:

    url: http://localhost:8081

인증 필터 요구사항:

- GlobalFilter 또는 GatewayFilter로 JWT 인증 필터를 구현한다.

- 인증이 필요 없는 public path는 설정 기반으로 관리한다.

- public path에 해당하지 않는 요청은 Authorization 헤더를 검사한다.

- Authorization: Bearer {accessToken} 형식을 검증한다.

- JWT가 유효하지 않으면 401 응답을 반환한다.

- JWT가 만료되었으면 401 응답을 반환한다.

- JWT 검증에 성공하면 claim에서 사용자 정보를 추출한다.

- 추출한 사용자 정보를 downstream 서비스에 헤더로 전달한다.

public path 기본값:

- /auth/signup

- /auth/login

- /auth/reissue

- /actuator/health

- GET /boards/**

- GET /posts/**

- GET /posts/*/comments

주의:

- 현재 board-service는 공개 조회만 제공한다.

- 따라서 GET /boards/**, GET /posts/**는 인증 없이 통과시킨다.

- 향후 POST, PATCH, DELETE 요청은 인증이 필요하도록 한다.

- 예를 들어 POST /boards/{boardId}/posts는 인증 필요.

- PATCH /posts/{postId}는 인증 필요.

- DELETE /posts/{postId}는 인증 필요.

사용자 컨텍스트 전달 헤더:

- X-User-Id

- X-User-Email

- X-User-Roles

- X-Account-Status

- X-Request-Id

JWT claim 예시:

{

"sub": "1",

"email": "user@example.com",

"roles": ["USER"],

"status": "ACTIVE",

"iat": 1710000000,

"exp": 1710001800

}

JWT 설정:

- auth-service와 동일한 JWT secret으로 검증한다.

- issuer는 auth-service로 검증한다.

- secret은 application.yml에 직접 하드코딩하지 말고 환경변수로 주입 가능하게 한다.

- local 개발용 기본값은 제공해도 된다.

application.yml 예시:

auth:

jwt:

    issuer: auth-service

    secret: ${JWT_SECRET:local-dev-secret-local-dev-secret-local-dev-secret}

security:

public-paths:

    - method: POST

      path: /auth/signup

    - method: POST

      path: /auth/login

    - method: POST

      path: /auth/reissue

    - method: GET

      path: /actuator/health

    - method: GET

      path: /boards/**

    - method: GET

      path: /posts/**

    - method: GET

      path: /posts/*/comments

구현할 구성:
```
com.dbwp031.apigateway

├─ ApiGatewayApplication.kt

├─ config

│  ├─ CorsConfig.kt

│  ├─ SecurityPathProperties.kt

│  ├─ JwtProperties.kt

│  └─ GatewayRouteConfig.kt 또는 application.yml 기반 라우팅

├─ filter

│  ├─ RequestIdGlobalFilter.kt

│  ├─ LoggingGlobalFilter.kt

│  └─ JwtAuthenticationGlobalFilter.kt

├─ jwt

│  ├─ JwtTokenValidator.kt

│  └─ JwtClaims.kt

├─ security

│  ├─ PublicPathMatcher.kt

│  └─ UnauthorizedResponseWriter.kt

└─ common

└─ constants

      └─ GatewayHeaders.kt
```
JwtAuthenticationGlobalFilter 요구사항:

1. 요청이 public path이면 인증 검사를 하지 않고 통과시킨다.

2. public path가 아니면 Authorization 헤더를 확인한다.

3. Bearer 토큰이 없으면 401 JSON 응답을 반환한다.

4. JWT를 검증한다.

5. 검증 성공 시 claims에서 userId, email, roles, status를 추출한다.

6. downstream 요청에 X-User-Id, X-User-Email, X-User-Roles, X-Account-Status 헤더를 추가한다.

7. 기존 요청에 동일한 X-User-* 헤더가 있다면 제거하고 Gateway가 만든 값으로 덮어쓴다.

8. X-Request-Id는 기존 RequestIdGlobalFilter에서 생성된 값을 유지한다.

중요한 보안 요구사항:

- 외부 클라이언트가 X-User-Id, X-User-Roles 같은 헤더를 직접 보내더라도 신뢰하면 안 된다.

- Gateway에서 인증 전에 들어온 X-User-* 헤더는 제거해야 한다.

- 인증 성공 후 Gateway가 검증된 값으로 다시 추가해야 한다.

- JWT 검증 실패 시 board-service로 요청이 전달되면 안 된다.

401 응답 형식:

{

"code": "UNAUTHORIZED",

"message": "인증이 필요합니다."

}

토큰 만료 응답:

{

"code": "TOKEN_EXPIRED",

"message": "토큰이 만료되었습니다."

}

유효하지 않은 토큰 응답:

{

"code": "INVALID_TOKEN",

"message": "유효하지 않은 토큰입니다."

}

기존 필터 유지:

- RequestIdGlobalFilter는 유지한다.

- LoggingGlobalFilter는 유지한다.

- LoggingGlobalFilter는 requestId, method, path, status, elapsed time을 출력한다.

- request/response body는 로깅하지 않는다.

CORS:

- 개발 환경에서 아래 origin 허용

    - http://localhost:3000

    - http://localhost:5173

- 허용 method:

    - GET, POST, PATCH, DELETE, OPTIONS

- 허용 header:

    - Authorization

    - Content-Type

    - X-Request-Id

- 노출 header:

    - X-Request-Id

아직 구현하지 말 것:

- 게시글/댓글 응답 조합

- 도메인 권한 검사

- DB 접근

- Redis

- Kafka

- Refresh Token 재발급 로직

- OAuth2 Resource Server 전체 설정

- Service Mesh

- Kubernetes

- Permission 기반 세부 인가

검증 시나리오:

1. GET /boards

    - Authorization 없이 호출 가능해야 한다.

    - board-service로 라우팅되어야 한다.

    - 응답에 X-Request-Id가 있어야 한다.

2. GET /posts/{postId}

    - Authorization 없이 호출 가능해야 한다.

    - board-service로 라우팅되어야 한다.

3. POST /boards/1/posts

    - Authorization 없이 호출하면 401이어야 한다.

    - board-service로 전달되면 안 된다.

4. POST /boards/1/posts

    - Authorization: Bearer {validAccessToken}을 넣으면 board-service로 전달되어야 한다.

    - downstream 요청에 X-User-Id, X-User-Email, X-User-Roles, X-Account-Status가 포함되어야 한다.

5. 잘못된 토큰

    - 401 INVALID_TOKEN 응답이 나와야 한다.

6. 만료된 토큰

    - 401 TOKEN_EXPIRED 응답이 나와야 한다.

7. 외부에서 X-User-Id: 999를 임의로 넣고 요청해도

    - Gateway가 JWT claim의 sub 값으로 덮어써야 한다.

README.md 업데이트:

- Gateway의 현재 역할

- public path 정책

- JWT 인증 필터 설명

- 사용자 컨텍스트 전달 헤더 설명

- 실행 방법

- auth-service, board-service와 함께 테스트하는 방법

- curl 예시

curl 예시를 포함해줘:

1. 회원가입

2. 로그인

3. accessToken으로 인증 필요한 API 호출

4. 토큰 없이 인증 필요한 API 호출

5. 공개 조회 API 호출

구현 전에 먼저 간단한 구현 계획을 설명하고, 그다음 파일을 생성/수정해줘.

구현 완료 후에는 변경된 주요 파일, 실행 방법, 테스트 방법을 요약해줘.****