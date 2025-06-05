#!/bin/bash
# 설정 로드
source /home/ec2-user/deploy/prod/zip/scripts/config.sh

set -e

echo "=== Starting Blue-Green Deployment with Docker Compose ==="

# 작업 디렉토리 설정
cd $DEPLOY_DIR

# 최신 이미지 태그 가져오기
IMAGE_TAG="latest"
export IMAGE_TAG

echo "IMAGE_TAG :  $IMAGE_TAG"
# 현재 실행 중인 환경 확인
CURRENT_BLUE=$(docker ps --filter "name=server-prod-blue" --format "{{.Names}}" 2>/dev/null)
CURRENT_GREEN=$(docker ps --filter "name=server-prod-green" --format "{{.Names}}" 2>/dev/null)

# 현재 상태 판단
if [ ! -z "$CURRENT_BLUE" ] && [ -z "$CURRENT_GREEN" ]; then
    CURRENT_ENV="blue"
    NEW_ENV="green"
    NEW_PORT=$GREEN_PORT
    NEW_COMPOSE_FILE="docker-compose-green.yml"
    echo "Current: Blue (8080) → Deploying: Green (8081)"
elif [ -z "$CURRENT_BLUE" ] && [ ! -z "$CURRENT_GREEN" ]; then
    CURRENT_ENV="green"
    NEW_ENV="blue"
    NEW_PORT=$BLUE_PORT
    NEW_COMPOSE_FILE="docker-compose-blue.yml"
    echo "Current: Green (8081) → Deploying: Blue (8080)"
elif [ -z "$CURRENT_BLUE" ] && [ -z "$CURRENT_GREEN" ]; then
    CURRENT_ENV=""
    NEW_ENV="blue"
    NEW_PORT=$BLUE_PORT
    NEW_COMPOSE_FILE="docker-compose-blue.yml"
    echo "First deployment → Starting: Blue (8080)"
else
    # 이미 모두 운영 중인 경우, 이전 배포 기록 확인하여 교체할 환경 결정
    if [ -f last_deployed_env.txt ]; then
        LAST_ENV=$(cat last_deployed_env.txt)
        if [ "$LAST_ENV" = "blue" ]; then
            # 이전에 Blue를 배포했으므로 이번엔 Green 배포
            CURRENT_ENV="blue"
            NEW_ENV="green"
            NEW_PORT=$GREEN_PORT
            NEW_COMPOSE_FILE="docker-compose-green.yml"
            echo "Last deployment: Blue → Now deploying: Green (8081)"
            # Blue는 중단
            docker-compose -f docker-compose-blue.yml stop
            echo "Stopping Blue environment for this deployment cycle"
        else
            # 이전에 Green을 배포했으므로 이번엔 Blue 배포
            CURRENT_ENV="green"
            NEW_ENV="blue"
            NEW_PORT=$BLUE_PORT
            NEW_COMPOSE_FILE="docker-compose-blue.yml"
            echo "Last deployment: Green → Now deploying: Blue (8080)"
            # Green은 중단
            docker-compose -f docker-compose-green.yml stop
            echo "Stopping Green environment for this deployment cycle"
        fi
    else
        # 기록이 없는 경우 Blue 중단, Green 배포
        CURRENT_ENV="blue"
        NEW_ENV="green"
        NEW_PORT=$GREEN_PORT
        NEW_COMPOSE_FILE="docker-compose-green.yml"
        echo "Both environments running, no last record → Stopping Blue, deploying Green"
        docker-compose -f docker-compose-blue.yml stop
    fi
fi

# ECR 로그인
echo "Logging into ECR..."
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY

# 최신 이미지 pull
echo "Pulling latest image..."
docker pull $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG

# 새 환경 시작
echo "Starting new environment: $NEW_ENV"
docker-compose -f $NEW_COMPOSE_FILE up -d

# 컨테이너 시작 확인
echo "Waiting for container to be ready..."
sleep 15

# 컨테이너 상태 확인
NEW_CONTAINER="server-prod-$NEW_ENV"
if docker ps --filter "name=$NEW_CONTAINER" --filter "status=running" --format "{{.Names}}" | grep -q "$NEW_CONTAINER"; then
    echo "✅ Container $NEW_CONTAINER is running"

    # 배포 정보 저장
    echo $NEW_PORT > current_port.txt
    echo $NEW_ENV > current_env.txt
    echo $NEW_CONTAINER > current_container.txt
    echo $NEW_COMPOSE_FILE > current_compose_file.txt

    echo "🎉 $NEW_ENV environment started successfully on port $NEW_PORT"

    # 마지막 배포 이후 3번째 배포인지 확인하여 양쪽 모두 운영할지 결정
    DEPLOYMENT_COUNT=$(cat deployment_count.txt 2>/dev/null || echo "1")
    if [ "$DEPLOYMENT_COUNT" -ge "2" ]; then
        echo "This is the third or later deployment - activating both environments"
        # Blue와 Green 모두 활성화
        docker-compose -f docker-compose-blue.yml up -d
        docker-compose -f docker-compose-green.yml up -d
        echo "✅ Both Blue and Green environments are now running"
        # 카운터 리셋
        echo "1" > deployment_count.txt
    else
        # 배포 카운트 증가
        echo "$((DEPLOYMENT_COUNT + 1))" > deployment_count.txt
    fi
else
    echo "❌ Failed to start $NEW_CONTAINER"

    # 실패한 컨테이너 로그 확인
    echo "Container logs:"
    docker-compose -f $NEW_COMPOSE_FILE logs

    # 정리
    exit 1
fi

echo "Blue-Green deployment phase 1 completed!"