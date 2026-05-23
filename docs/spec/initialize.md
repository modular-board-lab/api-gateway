Kotlin + Spring Boot로 MSA 환경을 가정한 api-gateway 프로젝트를 구축해줘.

현재 상황:
- 이미 board-service가 구현되어 있다.
- board-service는 게시판/게시글/댓글의 공개 조회 API를 제공한다.
- 아직 auth-service는 없다.
- 아직 JWT 인증도 없다.
- 이번 단계에서는 api-gateway를 먼저 만들고, board-service로 라우팅만 처리한다.

목표:
- api-gateway는 외부 요청의 단일 진입점 역할을 한다.
- 현재는 인증/인가 없이 board-service로 요청을 전달한다.
- 추후 auth-service와 JWT 인증 필터를 쉽게 추가할 수 있도록 구조를 잡아줘.
- Gateway에는 비즈니스 로직을 넣지 않는다.
- Gateway는 라우팅, 공통 로깅, Request ID 생성 같은 횡단 관심사만 담당한다.

기술 스택:
- Kotlin
- Spring Boot 3.x
- Gradle Kotlin DSL
- Spring Cloud Gateway
- Spring Boot Actuator
- DevTools

구현 범위:
1. api-gateway 프로젝트 생성/구성
2. board-service 라우팅 설정
3. actuator health endpoint 활성화
4. Request ID 생성 필터 추가
5. 요청/응답 기본 로깅 필터 추가
6. CORS 기본 설정
7. 추후 인증 필터를 붙일 수 있도록 패키지 구조 정리
8. README.md에 실행 방법과 테스트 방법 작성

라우팅 요구사항:
- GET /boards/** 요청은 board-service로 전달
- GET /posts/** 요청은 board-service로 전달
- board-service 주소는 application.yml에서 설정으로 관리
- 로컬 기본 주소는 http://localhost:8081 로 가정
- api-gateway 기본 포트는 8080

예상 흐름:
Client
→ api-gateway:8080
→ board-service:8081

예시:
- http://localhost:8080/boards
  → http://localhost:8081/boards

- http://localhost:8080/boards/1/posts
  → http://localhost:8081/boards/1/posts

- http://localhost:8080/posts/1
  → http://localhost:8081/posts/1

패키지 구조:
```
com.dbwp031.apigateway
├─ ApiGatewayApplication.kt
├─ config
│  ├─ CorsConfig.kt
│  └─ GatewayRouteConfig.kt 또는 application.yml 기반 route 설정
├─ filter
│  ├─ RequestIdGlobalFilter.kt
│  └─ LoggingGlobalFilter.kt
└─ common
└─ constants
└─ GatewayHeaders.kt
```
필터 요구사항:
1. RequestIdGlobalFilter
    - 모든 요청에 대해 X-Request-Id가 없으면 UUID로 생성한다.
    - 생성된 X-Request-Id를 downstream 요청 헤더에 추가한다.
    - 응답 헤더에도 X-Request-Id를 추가한다.

2. LoggingGlobalFilter
    - request method, path, requestId를 로그로 남긴다.
    - response status, elapsed time, requestId를 로그로 남긴다.
    - request/response body 로깅은 하지 않는다.

CORS 요구사항:
- 개발 단계에서는 localhost 프론트엔드 접근을 허용한다.
- http://localhost:3000
- http://localhost:5173
- GET, POST, PATCH, DELETE, OPTIONS 허용
- Authorization, Content-Type, X-Request-Id 헤더 허용

application.yml 예시 구조:
server:
port: 8080

spring:
application:
name: api-gateway
cloud:
gateway:
routes:
- id: board-service-boards
uri: ${services.board-service.url:http://localhost:8081}
predicates:
- Path=/boards/**
- id: board-service-posts
uri: ${services.board-service.url:http://localhost:8081}
predicates:
- Path=/posts/**

services:
board-service:
url: http://localhost:8081

management:
endpoints:
web:
exposure:
include: health,info,gateway
endpoint:
health:
show-details: always

아직 구현하지 말 것:
- JWT 검증
- Spring Security 설정
- auth-service 연동
- 사용자 권한 검사
- board-service 응답 조합
- 게시글/댓글 비즈니스 로직
- DB 연결
- Kafka
- Redis
- Kubernetes

주의사항:
- api-gateway는 DB를 사용하지 않는다.
- api-gateway는 board-service의 도메인 로직을 알면 안 된다.
- api-gateway는 게시글과 댓글 데이터를 조합하지 않는다.
- 단순히 요청을 적절한 서비스로 라우팅한다.
- 추후 인증 필터를 추가하기 쉬운 구조로 만들어줘.
- 가능한 한 설정 기반으로 라우팅을 관리해줘.

검증 방법:
1. board-service를 localhost:8081에서 실행한다.
2. api-gateway를 localhost:8080에서 실행한다.
3. GET http://localhost:8080/actuator/health 호출 시 UP이 나와야 한다.
4. GET http://localhost:8080/boards 호출 시 board-service의 /boards 응답이 나와야 한다.
5. GET http://localhost:8080/posts/{postId} 호출 시 board-service의 게시글 상세 응답이 나와야 한다.
6. 응답 헤더에 X-Request-Id가 포함되어야 한다.
7. 로그에 requestId, method, path, status, elapsed time이 출력되어야 한다.

README.md에는 다음 내용을 포함해줘:
- 프로젝트 목적
- 현재 Gateway의 역할
- 실행 방법
- board-service와 함께 실행하는 방법
- 테스트 API 예시
- 추후 확장 계획
    - JWT 인증 필터
    - auth-service 연동
    - 사용자 컨텍스트 전달
    - Docker Compose
    - Kubernetes 배포

구현 전에 먼저 간단한 구현 계획을 설명하고, 그다음 파일을 생성/수정해줘.
구현 완료 후에는 생성된 주요 파일, 실행 방법, 테스트 방법을 요약해줘.