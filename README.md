# api-gateway

MSA 환경에서 외부 요청의 단일 진입점 역할을 하는 Spring Cloud Gateway 프로젝트입니다. Gateway는 인증 검증, 라우팅, Request ID 생성, 공통 로깅 같은 횡단 관심사만 담당하며 게시판, 게시글, 댓글, 사용자 도메인 비즈니스 로직은 포함하지 않습니다.

## 현재 역할

- `/auth/**` 요청을 `auth-service`로 라우팅
- `/boards/**`, `/posts/**` 요청을 `board-service`로 라우팅
- public path가 아닌 요청의 JWT Access Token 검증
- 인증 성공 시 사용자 컨텍스트를 downstream 요청 헤더로 전달
- 외부 클라이언트가 보낸 `X-User-*` 헤더 제거
- 모든 요청에 `X-Request-Id` 생성 또는 전달
- 요청 method, path, requestId와 응답 status, elapsed time 로깅
- 개발용 CORS 설정 제공
- Actuator health endpoint 제공

## 서비스 포트

- `api-gateway`: `8080`
- `board-service`: `8081`
- `auth-service`: `8082`

## Public Path 정책

아래 요청은 Authorization 헤더 없이 통과합니다.

- `POST /auth/signup`
- `POST /auth/login`
- `POST /auth/reissue`
- `GET /actuator/health`
- `GET /boards/**`
- `GET /posts/**`
- `GET /posts/*/comments`

그 외 요청은 `Authorization: Bearer {accessToken}` 형식의 JWT가 필요합니다. 예를 들어 `POST /boards/1/posts`, `PATCH /posts/1`, `DELETE /posts/1`은 인증 대상입니다.

## JWT 인증 필터

Gateway는 `auth.jwt.issuer`와 `auth.jwt.secret` 설정으로 Access Token을 검증합니다. secret은 환경 변수로 주입할 수 있으며, 로컬 개발용 기본값만 제공합니다.

```yaml
auth:
  jwt:
    issuer: auth-service
    secret: ${JWT_SECRET:local-dev-secret-local-dev-secret-local-dev-secret}
```

검증 실패 응답은 `401` JSON으로 반환됩니다.

```json
{"code":"UNAUTHORIZED","message":"인증이 필요합니다."}
```

```json
{"code":"TOKEN_EXPIRED","message":"토큰이 만료되었습니다."}
```

```json
{"code":"INVALID_TOKEN","message":"유효하지 않은 토큰입니다."}
```

## 사용자 컨텍스트 전달 헤더

JWT 검증에 성공하면 Gateway가 claim에서 값을 추출해 downstream 서비스로 전달합니다.

- `X-User-Id`
- `X-User-Email`
- `X-User-Roles`
- `X-Account-Status`
- `X-Request-Id`

외부 요청에 포함된 `X-User-Id`, `X-User-Roles` 같은 사용자 컨텍스트 헤더는 신뢰하지 않고 제거한 뒤, 검증된 JWT claim 값으로 다시 설정합니다.

## 실행 방법

```bash
./gradlew bootRun
```

서비스 주소는 `src/main/resources/application.yml`에서 변경할 수 있습니다.

```yaml
services:
  auth-service:
    url: http://localhost:8082
  board-service:
    url: http://localhost:8081
```

실행 시 인자로도 변경할 수 있습니다.

```bash
./gradlew bootRun --args='--services.auth-service.url=http://localhost:18082 --services.board-service.url=http://localhost:18081'
```

## auth-service, board-service와 함께 테스트

1. `board-service`를 `localhost:8081`에서 실행합니다.
2. `auth-service`를 `localhost:8082`에서 실행합니다.
3. `api-gateway`를 `localhost:8080`에서 실행합니다.
4. 클라이언트는 `http://localhost:8080`으로 요청합니다.

### 1. 회원가입

```bash
curl -i -X POST http://localhost:8080/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"password123"}'
```

### 2. 로그인

```bash
curl -i -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"password123"}'
```

### 3. accessToken으로 인증 필요한 API 호출

```bash
curl -i -X POST http://localhost:8080/boards/1/posts \
  -H 'Authorization: Bearer {accessToken}' \
  -H 'Content-Type: application/json' \
  -d '{"title":"hello","content":"gateway auth test"}'
```

### 4. 토큰 없이 인증 필요한 API 호출

```bash
curl -i -X POST http://localhost:8080/boards/1/posts \
  -H 'Content-Type: application/json' \
  -d '{"title":"hello","content":"gateway auth test"}'
```

예상 결과는 `401 UNAUTHORIZED`입니다.

### 5. 공개 조회 API 호출

```bash
curl -i http://localhost:8080/boards
curl -i http://localhost:8080/posts/1
curl -i http://localhost:8080/posts/1/comments
```

공개 조회 응답에도 `X-Request-Id` 헤더가 포함되어야 합니다.

## 추후 확장 계획

- `/comments/**` board-service 라우팅 추가
- auth-service와 사용자 컨텍스트 계약 고도화
- Docker Compose 구성
- Kubernetes 배포 구성
- Permission 기반 세부 인가
