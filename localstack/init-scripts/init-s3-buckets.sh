#!/bin/bash

echo "S3 버킷 생성을 시작합니다..."

# 'my-auto-bucket-1' 이름으로 버킷 생성
awslocal s3 mb s3://my-local-image-bucket

echo "S3 버킷 생성이 완료되었습니다."