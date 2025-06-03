#!/bin/bash
# rollback.sh - 배포 실패 시 이전 환경으로 롤백

source /home/ec2-user/deploy/prod/zip/scripts/config.sh

if [ -f backup_port.txt ] && [ -f backup_color.txt ]; then
    BACKUP_PORT=$(cat backup_port.txt)
    BACKUP_COLOR=$(cat backup_color.txt)

    echo "Rolling back to previous environment: $BACKUP_COLOR on port $BACKUP_PORT"

    # Nginx 트래픽 전환
    ${SCRIPTS_DIR}/switch_nginx.sh $BACKUP_PORT

    echo "✅ Rollback completed"
fi