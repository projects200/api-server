#!/bin/bash
# 설정 로드
source /home/ec2-user/deploy/prod/zip/scripts/config.sh

set -e

echo "=== Starting Blue-Green Deployment with Docker Compose ==="

# 작업 디렉토리 설정
cd $DEPLOY_DIR

# 최신 이미지 태그 가져오기
#IMAGE_TAG=$(cat image_tag.txt 2>/dev/null || echo "latest")
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
    echo "❌ Both Blue and Green are running! Manual intervention required."
    echo "Blue: $CURRENT_BLUE"
    echo "Green: $CURRENT_GREEN"
    exit 1
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
else
    echo "❌ Failed to start $NEW_CONTAINER"

    # 실패한 컨테이너 로그 확인
    echo "Container logs:"
    docker-compose -f $NEW_COMPOSE_FILE logs

    # 정리
    docker-compose -f $NEW_COMPOSE_FILE down
    exit 1
fi

echo "Blue-Green deployment phase 1 completed!"