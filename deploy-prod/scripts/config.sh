#!/bin/bash
# 무중단 배포를 위한 환경 설정

# 기본 경로 설정
export DEPLOY_DIR="/home/ec2-user/deploy/prod/zip"
export SCRIPTS_DIR="${DEPLOY_DIR}/scripts"

# ECR 설정
export ECR_REGISTRY="825773631552.dkr.ecr.ap-northeast-2.amazonaws.com"
export ECR_REPOSITORY="undabang/prod-server-repository"

# 포트 설정
export BLUE_PORT=8080
export GREEN_PORT=8081

# 기타 설정
export MAX_HEALTH_RETRIES=30
export HEALTH_CHECK_INTERVAL=10