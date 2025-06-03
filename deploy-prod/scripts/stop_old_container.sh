#!/bin/bash
# 배포 시작 전 현재 상태 백업 (롤백 대비)

# 설정 로드
source /home/ec2-user/deploy/prod/zip/scripts/config.sh

echo "=== ApplicationStop Hook: Preparing for deployment ==="

# 작업 디렉토리 설정
cd $DEPLOY_DIR

# 배포 실패 시 롤백을 위한 현재 상태 백업
CURRENT_PORT=$(docker ps --format "table {{.Names}}\t{{.Ports}}" | grep -E "server-prod-(blue|green)" | grep -o ":($BLUE_PORT|$GREEN_PORT)" | cut -d: -f2 | head -1)
CURRENT_CONTAINER=$(docker ps --format "table {{.Names}}" | grep -E "server-prod-(blue|green)" | head -1)

if [ ! -z "$CURRENT_PORT" ] && [ ! -z "$CURRENT_CONTAINER" ]; then
    echo "Backing up current state:"
    echo "  Container: $CURRENT_CONTAINER"
    echo "  Port: $CURRENT_PORT"

    # 백업 정보 저장 (롤백용)
    echo $CURRENT_PORT > backup_port.txt
    echo $CURRENT_CONTAINER > backup_container.txt

    if [[ $CURRENT_CONTAINER == *"blue"* ]]; then
        echo "blue" > backup_color.txt
    else
        echo "green" > backup_color.txt
    fi
else
    echo "No running containers found for backup"
fi

echo "ApplicationStop hook completed."