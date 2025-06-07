#!/bin/bash
# 무중단 배포를 위한 환경 설정

# 기본 경로 설정
export DEPLOY_DIR="/home/ec2-user/deploy/prod/zip"
export SCRIPTS_DIR="${DEPLOY_DIR}/scripts"

# ECR 설정
export ECR_REGISTRY="825773631552.dkr.ecr.ap-northeast-2.amazonaws.com"
export ECR_REPOSITORY="undabang/prod-server-repository"
export IMAGE_TAG="latest"

# 포트 설정
export MAIN_PORT=8080
export SUB_PORT=8081

# nginx 설정
export NGINX_CONF_DIR="/etc/nginx/conf.d" # 예: /etc/nginx/sites-available 또는 /etc/nginx/conf.d
export ACTIVE_APP_CONF=sudo sed -i 's|export ACTIVE_APP_CONF="active_app.conf"|export ACTIVE_APP_CONF="server.conf"|g' /home/ec2-user/deploy/prod/zip/scripts/config.sh
 # Nginx가 include할 활성 앱 upstream 설정 파일 이름

# 헬스 체크 설정
export MAX_HEALTH_RETRIES=30 # 최대 재시도 횟수
export HEALTH_CHECK_INTERVAL=10 # 헬스 체크 간격 (초)