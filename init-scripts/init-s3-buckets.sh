#!/bin/bash

echo "S3 버킷 생성을 시작합니다..."

awslocal s3api create-bucket --bucket my-local-image-bucket

echo "S3 버킷 생성이 완료되었습니다."