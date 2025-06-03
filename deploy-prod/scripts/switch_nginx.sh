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

# Nginx 설정 파일 업데이트 (ec2-user 권한으로 임시 파일 생성 후 이동)
cat > /tmp/nginx_default << EOF
server {
    listen 80;
    server_name _;

    # 로그 설정
    access_log /var/log/nginx/access.log;
    error_log /var/log/nginx/error.log;

    # 프록시 버퍼링 설정
    proxy_buffering on;
    proxy_buffer_size 4k;
    proxy_buffers 8 4k;

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

        # 프록시 헤더 버퍼 설정
        proxy_busy_buffers_size 8k;
        proxy_temp_file_write_size 8k;
    }

    # 헬스체크 엔드포인트 - 내부 접근만 허용
    location /system-monitor/health {
        # 내부 접근만 허용
        allow 127.0.0.1;  # 로컬호스트
        allow 172.16.0.0/12;  # Docker 네트워크
        deny all;  # 나머지 모든 접근 차단

        proxy_pass http://localhost:$NEW_PORT/system-monitor/health;
        proxy_set_header Host \$host;
        proxy_connect_timeout 5s;
        proxy_read_timeout 5s;
        access_log off;
    }

    # 정적 파일 캐싱 (선택적)
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        proxy_pass http://localhost:$NEW_PORT;
        proxy_set_header Host \$host;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF

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
        echo "✅ Nginx successfully switched to port: $NEW_PORT"
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