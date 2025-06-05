#!/bin/bash

# 설정 로드
source /home/ec2-user/deploy/prod/zip/scripts/config.sh

NEW_PORT=$1

if [ -z "$NEW_PORT" ]; then
    echo "Usage: $0 <new_port>"
    exit 1
fi

echo "Switching nginx to port: $NEW_PORT"

# Nginx 설정 파일 백업
sudo cp /etc/nginx/sites-available/default /etc/nginx/sites-available/default.backup.$(date +%Y%m%d_%H%M%S)

# 배포 이후 두 환경을 모두 사용하는지 확인
DEPLOYMENT_COUNT=$(cat $DEPLOY_DIR/deployment_count.txt 2>/dev/null || echo "1")
if [ "$DEPLOYMENT_COUNT" -ge "2" ]; then
    # Nginx 로드 밸런싱 설정 (두 환경 모두 활성화)
    cat > /tmp/nginx_default << EOF
upstream spring_servers {
    server localhost:8080 weight=1 max_fails=2 fail_timeout=20s;
    server localhost:8081 weight=1 max_fails=2 fail_timeout=20s;
}

server {
    listen 80;
    server_name _;

    # 로그 설정
    access_log /var/log/nginx/access.log;
    error_log /var/log/nginx/error.log;

    # 프록시 버퍼링 설정
    proxy_buffering on;
    proxy_buffer_size 8k;
    proxy_buffers 16 8k;
    proxy_busy_buffers_size 16k;

    # 메인 애플리케이션 프록시
    location / {
        proxy_pass http://spring_servers;

        # 헤더 설정
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 헬스체크 엔드포인트
    location /system-monitor/health {
        allow 127.0.0.1;
        allow 172.17.0.0/16;
        deny all;

        proxy_pass http://spring_servers/system-monitor/health;
        proxy_set_header Host \$host;
        proxy_connect_timeout 5s;
        proxy_read_timeout 5s;
        access_log off;
    }
}
EOF
else
    # 단일 환경으로 설정
    cat > /tmp/nginx_default << EOF
server {
    listen 80;
    server_name _;

    # 로그 설정
    access_log /var/log/nginx/access.log;
    error_log /var/log/nginx/error.log;

    # 프록시 버퍼링 설정
    proxy_buffering on;
    proxy_buffer_size 8k;
    proxy_buffers 16 8k;
    proxy_busy_buffers_size 16k;

    location / {
        proxy_pass http://localhost:$NEW_PORT;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 헬스체크 엔드포인트
    location /system-monitor/health {
        allow 127.0.0.1;
        allow 172.17.0.0/16;
        deny all;

        proxy_pass http://localhost:$NEW_PORT/system-monitor/health;
        proxy_set_header Host \$host;
        proxy_connect_timeout 5s;
        proxy_read_timeout 5s;
        access_log off;
    }
}
EOF
fi

# 파일 권한 설정 후 이동
sudo mv /tmp/nginx_default /etc/nginx/sites-available/default

# Nginx 설정 테스트
echo "Testing nginx configuration..."
sudo nginx -t

if [ $? -eq 0 ]; then
    # Nginx 리로드 (graceful restart)
    echo "Reloading nginx..."
    sudo systemctl reload nginx

    # 설정 확인
    sleep 2
    if sudo systemctl is-active --quiet nginx; then
        if [ "$DEPLOYMENT_COUNT" -ge "2" ]; then
            echo "✅ Nginx successfully configured for load balancing between both environments"
        else
            echo "✅ Nginx successfully switched to port: $NEW_PORT"
        fi
    else
        echo "❌ Nginx reload failed, restoring backup..."
        sudo cp /etc/nginx/sites-available/default.backup.* /etc/nginx/sites-available/default
        sudo systemctl reload nginx
        exit 1
    fi
else
    echo "❌ Nginx configuration test failed"
    sudo rm -f /etc/nginx/sites-available/default
    exit 1
fi