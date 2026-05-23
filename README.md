# api-gateway

MSA 환경에서 외부 요청의 단일 진입점 역할을 하는 Spring Cloud Gateway 프로젝트입니다. 현재 단계에서는 인증/인가 없이 공개 게시판 API 요청을 `board-service`로 라우팅합니다.

## 현재 역할

- `GET /boards/**` 요청을 `board-service`로 전달
- `GET /posts/**` 요청을 `board-service`로 전달
- 모든 요청에 `X-Request-Id` 생성 또는 전달
- 응답 헤더에 `X-Request-Id` 추가
- 요청 method, path, requestId와 응답 status, elapsed time 로깅
- 개발용 CORS 설정 제공
- Actuator health endpoint 제공

Gateway에는 게시판, 게시글, 댓글 비즈니스 로직을 두지 않습니다.

## 실행 방법

```bash
./gradlew bootRun
```

기본 포트는 `8081`입니다.

## board-service와 함께 실행

1. `board-service`를 `localhost:8080`에서 실행합니다.
2. `api-gateway`를 실행합니다.
3. 클라이언트는 `http://localhost:8081`으로 요청합니다.

`board-service` 주소는 `src/main/resources/application.yml`에서 변경할 수 있습니다.

```yaml
services:
  board-service:
    url: http://localhost:8080
```

실행 시 환경 변수 또는 JVM 프로퍼티로도 변경할 수 있습니다.

```bash
./gradlew bootRun --args='--services.board-service.url=http://localhost:18080'
```

## 테스트 API 예시

```bash
curl -i http://localhost:8081/actuator/health
curl -i http://localhost:8081/boards
curl -i http://localhost:8081/boards/1/posts
curl -i http://localhost:8081/posts/1
```

검증할 내용:

- `/actuator/health` 응답 status가 `UP`인지 확인
- `/boards/**`, `/posts/**` 요청이 `board-service` 응답을 반환하는지 확인
- 응답 헤더에 `X-Request-Id`가 포함되는지 확인
- 로그에 `requestId`, `method`, `path`, `status`, `elapsedMs`가 출력되는지 확인

## 추후 확장 계획

- JWT 인증 필터 추가
- auth-service 연동
- 사용자 컨텍스트 downstream 전달
- Docker Compose 구성
- Kubernetes 배포 구성
