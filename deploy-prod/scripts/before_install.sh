#!/bin/bash
# 설정 로드
source /home/ec2-user/deploy/prod/zip/scripts/config.sh

echo "=== Before Install: Preparing for Custom Deployment ==="
cd $DEPLOY_DIR || exit 1

# ECR 로그인
echo "Logging into ECR: $ECR_REGISTRY"
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY

# 오래된 이미지 정리 (최신 2개 버전만 유지)
#	CreatedAt 기준 정렬, 최신 2개 이미지를 제외, 나머지를 강제로 삭제
echo "Cleaning up old Docker images..."
docker images --format "{{.Repository}}:{{.Tag}} {{.CreatedAt}}" | grep "${ECR_REPOSITORY}" | sort -k2 | head -n -2 | awk '{print $1}' | xargs -r docker rmi -f

echo "Cleaning up any orphaned 'server-prod-sub' container from previous (possibly failed) deployments..."
# MAIN_PORT, SUB_PORT 등을 export하여 docker-compose가 변수를 인식하도록 함
export MAIN_PORT SUB_PORT ECR_REGISTRY ECR_REPOSITORY IMAGE_TAG
docker-compose -f docker-compose-sub.yml down --remove-orphans 2>/dev/null || true

echo "Before install completed."