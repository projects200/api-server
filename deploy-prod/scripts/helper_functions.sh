#!/bin/bash
# Deployment helper functions

# 함수: Docker Compose 기반 헬스 체크 (Docker 내장 + Actuator)
# 사용 변수 (config.sh 또는 호출 스크립트에서 제공):
# MAX_HEALTH_RETRIES, HEALTH_CHECK_INTERVAL
check_health() {
    local container_name=$1
    local port=$2
    local compose_file_name=$3 # docker-compose.yml 또는 docker-compose-sub.yml
    echo "Health checking $container_name on port $port (using $compose_file_name)..."
    for i in $(seq 1 $MAX_HEALTH_RETRIES); do
        HEALTH_STATUS=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}unhealthy{{end}}' $container_name 2>/dev/null)
        HTTP_STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/system-monitor/health)

        if [[ "$HEALTH_STATUS" == "healthy" && "$HTTP_STATUS_CODE" == "200" ]]; then
            APP_STATUS_JSON=$(curl -s http://localhost:$port/system-monitor/health)
            APP_ACTUATOR_STATUS=$(echo "$APP_STATUS_JSON" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
            if [[ "$APP_ACTUATOR_STATUS" == "UP" ]]; then
                echo "✅ $container_name health check PASSED (Attempt $i/$MAX_HEALTH_RETRIES). Docker: $HEALTH_STATUS, HTTP: $HTTP_STATUS_CODE, App: $APP_ACTUATOR_STATUS"
                return 0 # 성공
            else
                echo "⏳ $container_name health check attempt $i/$MAX_HEALTH_RETRIES: Docker: $HEALTH_STATUS, HTTP: $HTTP_STATUS_CODE, App Status: $APP_ACTUATOR_STATUS. Retrying..."
            fi
        else
            echo "⏳ $container_name health check attempt $i/$MAX_HEALTH_RETRIES: Docker: $HEALTH_STATUS, HTTP: $HTTP_STATUS_CODE. Retrying..."
        fi
        sleep $HEALTH_CHECK_INTERVAL
    done
    echo "❌ $container_name health check FAILED after $MAX_HEALTH_RETRIES attempts."
    echo "Logs for $container_name:"
    docker-compose -f $compose_file_name logs $container_name
    return 1 # 실패
}

# 함수: Nginx 트래픽 전환
# 사용 변수 (config.sh 또는 호출 스크립트에서 제공):
# NGINX_CONF_DIR, ACTIVE_APP_CONF, DEPLOY_DIR
switch_nginx_traffic() {
    local target_port=$1
    echo "Switching Nginx traffic to point to port $target_port..."

    # sed를 사용하여 server.conf 파일에서 service_url 변수의 값을 변경
    sudo sed -i "s|set \$service_url http://127.0.0.1:[0-9]\+;|set \$service_url http://127.0.0.1:${target_port};|" "${NGINX_CONF_DIR}/server.conf"

    if sudo nginx -t; then # 설정 파일 유효성 검사
        sudo nginx -s reload
        echo "Nginx reloaded. Traffic now directed to port $target_port."
        echo $target_port > $DEPLOY_DIR/current_active_port.txt # 현재 활성 포트 기록
    else
        echo "ERROR: Nginx configuration test failed. Traffic not switched."
        return 1
    fi
    return 0
}