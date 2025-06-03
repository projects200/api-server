#!/bin/bash
# 설정 로드
source /home/ec2-user/deploy/prod/zip/scripts/config.sh

echo "=== Before Install: Preparing for Blue-Green Deployment ==="

# 작업 디렉토리 설정
cd $DEPLOY_DIR

# ECR 로그인
echo "Logging into ECR..."
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY

# 오래된 이미지 정리 (최신 2개 버전만 유지)
echo "Cleaning up old Docker images..."
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.CreatedAt}}" | grep $ECR_REPOSITORY | tail -n +3 | awk '{print $1":"$2}' | xargs -r docker rmi || true

echo "Before install completed."