#!/bin/bash
# 배포 완료 후 이전 환경 정리(리소스 확보)

# 설정 로드
source /home/ec2-user/deploy/prod/zip/scripts/config.sh

CURRENT_ENV=$1

if [ -z "$CURRENT_ENV" ]; then
    echo "Usage: $0 <current_env>"
    exit 1
fi

echo "Cleaning up old environment (current: $CURRENT_ENV)..."

# 작업 디렉토리 설정
cd $DEPLOY_DIR

# 이전 환경 정보 결정
if [ "$CURRENT_ENV" = "blue" ]; then
    OLD_ENV="green"
    OLD_COMPOSE_FILE="docker-compose-green.yml"
    OLD_CONTAINER="server-prod-green"
    OLD_PORT="8081"
else
    OLD_ENV="blue"
    OLD_COMPOSE_FILE="docker-compose-blue.yml"
    OLD_CONTAINER="server-prod-blue"
    OLD_PORT="8080"
fi

# 이전 환경이 실행 중인지 확인
OLD_RUNNING=$(docker ps --filter "name=$OLD_CONTAINER" --format "{{.Names}}" 2>/dev/null)

if [ ! -z "$OLD_RUNNING" ]; then
    echo "Found old environment: $OLD_ENV ($OLD_CONTAINER on port $OLD_PORT)"

    # 트래픽이 완전히 새 환경으로 이동했는지 확인하기 위한 대기
    echo "Waiting for traffic drain (30 seconds)..."
    sleep 30

    # 이전 환경의 연결 상태 확인
    echo "Checking active connections on old environment..."
    ACTIVE_CONNECTIONS=$(netstat -an | grep ":$OLD_PORT " | grep ESTABLISHED | wc -l 2>/dev/null || echo "0")
    echo "Active connections on port $OLD_PORT: $ACTIVE_CONNECTIONS"

    if [ "$ACTIVE_CONNECTIONS" -gt 0 ]; then
        echo "⚠️  Active connections detected. Waiting additional 30 seconds..."
        sleep 30
    fi

    # Graceful shutdown
    echo "Gracefully stopping old environment: $OLD_ENV"
    docker-compose -f $OLD_COMPOSE_FILE stop

    # 컨테이너 제거
    echo "Removing old environment containers..."
    docker-compose -f $OLD_COMPOSE_FILE down

    echo "✅ Old environment cleaned up: $OLD_ENV"

    # 정리 로그
    echo "$(date): Cleaned up old environment - $OLD_ENV:$OLD_PORT" >> /home/ec2-user/deploy/prod/deployment.log

else
    echo "ℹ️  No old environment found to cleanup ($OLD_ENV)"
fi

# Docker 시스템 정리
echo "Performing Docker system cleanup..."

# 중지된 컨테이너 정리
docker container prune -f

# 사용하지 않는 이미지 정리 (최신 2개 태그는 유지)
echo "Cleaning up old images (keeping latest 2 versions)..."
docker images $ECR_REGISTRY/$ECR_REPOSITORY --format "table {{.Tag}}\t{{.ID}}" | tail -n +4 | awk '{print $2}' | xargs -r docker rmi 2>/dev/null || true

# 사용하지 않는 네트워크 정리
docker network prune -f

# 사용하지 않는 볼륨 정리
docker volume prune -f

# 최종 상태 확인
echo ""
echo "=== Current Docker Status ==="
echo "Running containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "Docker disk usage:"
docker system df

echo ""
echo "✅ Environment cleanup completed successfully!"