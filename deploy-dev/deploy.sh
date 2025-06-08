#!/bin/bash

mkdir -p /home/ec2-user/deploy/dev/zip
cd /home/ec2-user/deploy/dev/zip/

docker-compose down

# ECR 로그인
ECR_REGISTRY="825773631552.dkr.ecr.ap-northeast-2.amazonaws.com"
ECR_REPOSITORY="undabang/dev-server-repository"
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin ${ECR_REGISTRY}

# 최신 이미지 pull
docker-compose pull

# 오래된 이미지 정리 (최신 버전 2개만 유지)
echo "Cleaning up old Docker images..."
docker images --format "{{.Repository}}:{{.Tag}} {{.CreatedAt}}" | grep "${ECR_REPOSITORY}" | sort -k2 | head -n -2 | awk '{print $1}' | xargs -r docker rmi -f

docker-compose up -d
