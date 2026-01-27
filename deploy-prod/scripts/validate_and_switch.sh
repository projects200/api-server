#!/bin/bash
source /home/ec2-user/deploy/prod/zip/scripts/config.sh
source /home/ec2-user/deploy/prod/zip/scripts/helper_functions.sh # 헬퍼 함수 로드

echo "=== ValidateService Hook: Health check, traffic switching, and cleanup ==="
cd $DEPLOY_DIR || exit 1

export MAIN_PORT SUB_PORT ECR_REGISTRY ECR_REPOSITORY IMAGE_TAG # docker-compose에서 변수 사용

# --- 단계 1: SUB_PORT (server-prod-sub) 헬스 체크 및 트래픽 전환 ---
echo "--- Stage 1: Validating server-prod-sub on SUB_PORT ($SUB_PORT) ---"
if ! check_health "server-prod-sub" "$SUB_PORT" "docker-compose-sub.yml"; then
    echo "ERROR: Health check failed for server-prod-sub on SUB_PORT."
    echo "Cleaning up server-prod-sub due to health check failure..."
    clean_specific_containers "server-prod-sub"
    exit 1 # 배포 실패
fi

echo "Switching Nginx traffic to server-prod-sub (Port: $SUB_PORT)..."
if ! switch_nginx_traffic "$SUB_PORT"; then
    echo "ERROR: Failed to switch Nginx traffic to SUB_PORT."
    echo "Cleaning up server-prod-sub..."
    clean_specific_containers "server-prod-sub"
    exit 1 # 배포 실패
fi

# --- 단계 2: MAIN_PORT (server-prod) 기존 컨테이너 중지 및 새 버전 배포 ---
echo "--- Stage 2: Updating server-prod on MAIN_PORT ($MAIN_PORT) ---"
echo "Stopping and removing old server-prod container (if any)..."
clean_specific_containers "server-prod"

echo "Starting new version of server-prod on MAIN_PORT ($MAIN_PORT)..."
docker-compose -f docker-compose.yml up -d server-prod

echo "Waiting for server-prod-sub to initialize ($INITIAL_WAIT_TIME seconds)..."
sleep "$INITIAL_WAIT_TIME"  # config.sh에서 설정한 60초 대기 # 애플리케이션 시작 시간에 따라 조절

if ! docker ps | grep -q "server-prod"; then
    echo "ERROR: server-prod container failed to start!"
    docker-compose -f docker-compose.yml logs server-prod
    echo "Traffic remains on server-prod-sub (Port: $SUB_PORT). Manual intervention may be required."
    exit 1 # 배포 실패 (server-prod 시작 실패)
fi


# --- 단계 3: MAIN_PORT (server-prod) 헬스 체크 및 트래픽 최종 전환 ---
echo "--- Stage 3: Validating server-prod on MAIN_PORT ($MAIN_PORT) ---"
if ! check_health "server-prod" "$MAIN_PORT" "docker-compose.yml"; then
    echo "ERROR: Health check failed for server-prod on MAIN_PORT."
    echo "Traffic remains on server-prod-sub (Port: $SUB_PORT) due to server-prod health check failure."
    echo "Cleaning up failed server-prod..."
    clean_specific_containers "server-prod"
    # 이 시점에서 배포는 실패로 간주. SUB_PORT가 계속 서비스 중.
    exit 1 # 배포 실패
fi

echo "Switching Nginx traffic back to server-prod (Port: $MAIN_PORT)..."
if ! switch_nginx_traffic "$MAIN_PORT"; then
    echo "ERROR: Failed to switch Nginx traffic to MAIN_PORT."
    echo "Traffic may still be on SUB_PORT. Manual intervention required."
    # server-prod은 정상이나 트래픽 전환 실패. SUB_PORT는 아직 살아있음.
    exit 1 # 배포 실패
fi

# --- 단계 4: SUB_PORT (server-prod-sub) 임시 컨테이너 정리 ---
echo "--- Stage 4: Cleaning up temporary server-prod-sub container ---"
echo "Stopping and removing server-prod-sub container on SUB_PORT ($SUB_PORT)..."
clean_specific_containers "server-prod-sub"

echo "🎉 Zero-downtime deployment completed successfully! Traffic is on MAIN_PORT ($MAIN_PORT)."
echo "ValidateService completed."
exit 0