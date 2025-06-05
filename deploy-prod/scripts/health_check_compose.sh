#!/bin/bash

# 설정 로드
source /home/ec2-user/deploy/prod/zip/scripts/config.sh

set -e

echo "=== Health Check and Traffic Switch (Docker Compose) ==="

# 작업 디렉토리 설정
cd $DEPLOY_DIR

# 현재 배포된 환경 정보 읽기
CURRENT_PORT=$(cat current_port.txt 2>/dev/null || echo "$BLUE_PORT")
CURRENT_ENV=$(cat current_env.txt 2>/dev/null || echo "blue")
CURRENT_CONTAINER=$(cat current_container.txt 2>/dev/null || echo "server-prod-blue")
CURRENT_COMPOSE_FILE=$(cat current_compose_file.txt 2>/dev/null || echo "docker-compose-blue.yml")

echo "Health checking: $CURRENT_CONTAINER on port $CURRENT_PORT ($CURRENT_ENV environment)"

# 헬스체크 재시도 로직
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_HEALTH_RETRIES ]; do
    # Docker Compose 헬스체크 상태 확인
    HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' $CURRENT_CONTAINER 2>/dev/null || echo "none")

    # Spring Boot Actuator 헬스체크
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$CURRENT_PORT/system-monitor/health 2>/dev/null || echo "000")

    if [ "$HTTP_STATUS" = "200" ]; then
        # 추가 응답 내용 확인
        HEALTH_RESPONSE=$(curl -s http://localhost:$CURRENT_PORT/system-monitor/health 2>/dev/null || echo "{}")
        APP_STATUS=$(echo $HEALTH_RESPONSE | grep -o '"status":"[^"]*"' | cut -d'"' -f4 2>/dev/null || echo "UNKNOWN")

        if [ "$APP_STATUS" = "UP" ]; then
            echo "✅ Health check passed!"
            echo "   - HTTP Status: $HTTP_STATUS"
            echo "   - App Status: $APP_STATUS"
            echo "   - Docker Health: $HEALTH_STATUS"

            # 추가 서비스 체크 (선택적)
            echo "Performing additional service checks..."

            # 메모리 사용량 체크
            MEMORY_USAGE=$(docker stats --no-stream --format "table {{.Container}}\t{{.MemUsage}}" | grep $CURRENT_CONTAINER | awk '{print $2}' || echo "N/A")
            echo "   - Memory Usage: $MEMORY_USAGE"

            # CPU 사용량 체크
            CPU_USAGE=$(docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}" | grep $CURRENT_CONTAINER | awk '{print $2}' || echo "N/A")
            echo "   - CPU Usage: $CPU_USAGE"

            # 로드밸런서 트래픽 스위칭
            echo "Switching traffic to new environment..."

            # switch_nginx.sh를 호출하기 전에 추가
            if [ ! -x "/home/ec2-user/deploy/prod/zip/scripts/switch_nginx.sh" ]; then
              sudo chmod +x /home/ec2-user/deploy/prod/zip/scripts/switch_nginx.sh
            fi

            # 그 다음 스크립트 호출
            /home/ec2-user/deploy/prod/zip/scripts/switch_nginx.sh $CURRENT_PORT

            # 성공적인 배포 확인 후 이전 환경 정리 (Graceful shutdown)
            echo "Cleaning up old environment..."

            # 권한이 없을시 권한 부여
            if [ ! -x "/home/ec2-user/deploy/prod/zip/scripts/cleanup_old_environment.sh" ]; then
              sudo chmod +x /home/ec2-user/deploy/prod/zip/scripts/cleanup_old_environment.sh
            fi

            /home/ec2-user/deploy/prod/zip/scripts/cleanup_old_environment.sh $CURRENT_ENV

            echo "🎉 Blue-Green Deployment completed successfully!"
            echo "✅ Active: $CURRENT_ENV environment on port $CURRENT_PORT"

            # 배포 완료 로그
            echo "$(date): Deployment completed - $CURRENT_ENV:$CURRENT_PORT" >> /home/ec2-user/deploy/prod/deployment.log

            exit 0
        else
            echo "⚠️  Application status: $APP_STATUS (HTTP: $HTTP_STATUS)"
        fi
    else
        echo "⏳ Health check failed - HTTP: $HTTP_STATUS, Docker: $HEALTH_STATUS ($((RETRY_COUNT + 1))/$MAX_HEALTH_RETRIES)"
    fi

    sleep 10
    RETRY_COUNT=$((RETRY_COUNT + 1))
done

echo "❌ Health check failed after $MAX_HEALTH_RETRIES attempts"
echo "Rolling back new environment..."

# 실패 시 새 환경 정리
echo "Stopping failed deployment..."
docker-compose -f $CURRENT_COMPOSE_FILE down

# 실패 로그 수집
echo "Collecting failure logs..."
echo "$(date): Deployment failed - $CURRENT_ENV:$CURRENT_PORT" >> /home/ec2-user/deploy/prod/deployment.log
docker-compose -f $CURRENT_COMPOSE_FILE logs > /home/ec2-user/deploy/prod/failed_deployment_$(date +%Y%m%d_%H%M%S).log

exit 1