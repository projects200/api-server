# Copilot PR Review Instructions

리뷰 코멘트, 요약, 제안은 **한국어**로 작성해 주세요. 코드 식별자·API 이름·에러 메시지 등 고유명사는 원문 그대로 유지.

## 프로젝트 컨텍스트
- Spring Boot 3.4 / Java 21 / JPA · Hibernate 6 / QueryDSL / Hibernate Spatial (JTS + geolatte)
- MySQL, AWS (S3 / Cognito / CodeDeploy / ECR), Firebase Admin SDK
- 배포: ECR 이미지 + CodeDeploy, dev/prod 각 EC2 단일 인스턴스 (t3.micro 공용)
- solo dev 저장소 — PR 본문·커밋 메시지는 한국어 개조식

## 리뷰 시 고려
- 메모리 / 성능 지적은 **실제 RSS 관측값** 기반인지, 이론 최악치인지 구분해 주세요
- 옵션 제안 시 `docker-compose.yml` memory limit 과 Dockerfile CMD 의 JVM 옵션 관계를 함께 검토
- 한 줄 제안보다 trade-off 가 있으면 옵션 2~3개를 비교해 제시
