#!/bin/bash
source /home/ec2-user/deploy/prod/zip/scripts/config.sh

echo "=== ApplicationStart Hook: Starting new version on SUB_PORT ==="
cd $DEPLOY_DIR || exit 1

echo "Pulling the latest Docker image: ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
docker pull "${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to pull Docker image."
    exit 1
fi

echo "Starting new application (server-prod-sub) on SUB_PORT ($SUB_PORT) using docker-compose-sub.yml..."
export MAIN_PORT SUB_PORT ECR_REGISTRY ECR_REPOSITORY IMAGE_TAG # docker-compose에서 변수 사용
docker-compose -f docker-compose-sub.yml up -d server-prod-sub

# 간단한 시작 확인 (본격적인 헬스체크는 Validate Service에서 수행)
echo "Waiting briefly for server-prod-sub to initialize..."
sleep 15 # 컨테이너가 완전히 시작될 시간을 줍니다. 애플리케이션 시작 시간에 따라 조절.

if ! docker ps | grep -q "server-prod-sub"; then
    echo "ERROR: server-prod-sub container failed to start!"
    docker-compose -f docker-compose-sub.yml logs server-prod-sub
    exit 1
fi

echo "Application 'server-prod-sub' started on SUB_PORT. Health check will be performed in ValidateService."
echo "ApplicationStart completed."