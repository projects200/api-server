#!/bin/bash

mkdir -p /home/ec2-user/deploy/prod/zip
cd /home/ec2-user/deploy/prod/zip/

docker-compose down

aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 825773631552.dkr.ecr.ap-northeast-2.amazonaws.com
docker-compose pull

docker-compose up -d
