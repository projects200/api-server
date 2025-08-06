#!/bin/bash

# =====================================================================
#        'dev' 이미지만 안전하게 정리하는 최종 배포 스크립트
# =====================================================================

# 1. 스크립트 실행 중 오류 발생 시 즉시 중단하고, 모든 실행 명령어를 로그에 출력
#    (배포 실패 시 원인을 즉시 알 수 있게 해주는 안전장치)
set -ex

# 2. 필요한 변수 설정 (가독성 및 재사용성 증가)
ECR_REGISTRY="825773631552.dkr.ecr.ap-northeast-2.amazonaws.com"
REPOSITORY_NAME="undabang/dev-server-repository"
IMAGE_NAME="${ECR_REGISTRY}/${REPOSITORY_NAME}:latest"
CONTAINER_NAME="server-dev"
WORKING_DIR="/home/ec2-user/deploy/dev/zip"

# 3. 작업 디렉토리가 존재하는지 확인하고, 없으면 생성 후 이동 (안정성 확보)
mkdir -p "$WORKING_DIR"
cd "$WORKING_DIR"

echo "### 배포 시작: $(date)"

echo "--> 1/6: 기존 docker-compose 서비스 중지 (컨테이너만 정리)"
# --rmi 옵션을 제거하여 이미지 정리는 이 단계에서 하지 않음
docker-compose down

# 4. ECR에 명시적으로 로그인 (권한 문제 방지)
echo "--> 2/6: AWS ECR 강제 로그인"
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin "$ECR_REGISTRY"

# 5. 기존 'latest' 태그 이미지를 강제로 삭제 (보이지 않는 캐시 문제 해결)
echo "--> 3/6: 로컬의 기존 'dev:latest' 이미지를 강제로 삭제"
# || true : 이미지가 없어서 오류가 나도 스크립트가 중단되지 않도록 함
docker rmi -f "$IMAGE_NAME" || true

# 6. 최신 이미지를 강제로 pull
echo "--> 4/6: ECR에서 최신 'dev' 이미지를 강제로 pull"
# 디스크 공간도 있고, 옛날 이미지도 없어서 무조건 새로 받음
docker pull "$IMAGE_NAME"

# 7. 최신 이미지로 서비스 시작
echo "--> 5/6: 새로운 이미지로 'dev' 컨테이너 강제 재생성"
# --force-recreate: 설정이 같아도 무조건 컨테이너를 새로 만듦
docker-compose up -d --force-recreate

# 8. 사용하지 않는 모든 도커 이미지 정리 (디스크 용량 문제 근본 해결)
echo "--> 6/6: 불필요한 이미지 정리"
docker image prune -af

echo "### 배포 완료: $(date)"