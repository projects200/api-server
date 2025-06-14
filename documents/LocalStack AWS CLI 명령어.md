### LocalStack AWS S3 실행 명령어
```bash
awslocal s3api create-bucket --bucket my-local-image-bucket
```

### LocalStack s3의 모든 이미지 조회 명령어
```bash
aws s3 ls s3://my-local-image-bucket --recursive --endpoint-url http://localhost:4566 --region ap-northeast-2
```

### LocalStack s3의 모든 이미지 상세 조회 명령어
```bash
aws s3api list-objects --bucket my-local-image-bucket --endpoint-url http://localhost:4566 --region ap-northeast-2
```
#### 결과 예시
```json
{
  "Contents": [
    {
      "Key": "uploads/exercises/70028387-f3e1-4dda-8c31-4766d523f629/2025/06/657f7a43-af7e-4ecb-b525-7cae5838253e.png",
      "LastModified": "2025-06-14T08:35:57+00:00",
      "ETag": "\"bd68d8db60154ba88681684dc9e81c6c\"",
      "ChecksumAlgorithm": [
        "CRC64NVME"
      ],
      "ChecksumType": "FULL_OBJECT",
      "Size": 10614,
      "StorageClass": "STANDARD",
      "Owner": {
        "DisplayName": "webfile",
        "ID": "75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a"
      }
    }
  ],
  "RequestCharged": null,
  "Prefix": ""
}  
```

### LocalStack s3의 한 이미지 상세 조회 명령어(메타데이터 포함)
```bash
aws s3api get-object --bucket my-local-image-bucket --key uploads/exercises/70028387-f3e1-4dda-8c31-4766d523f629/2025/06/d3380ebb-eb11-47f5-af13-0dc2e00f9aad.jpg --endpoint-url http://localhost:4566 --region ap-northeast-2 d3380ebb-eb11-47f5-af13-0dc2e00f9aad.jpg
```
#### 결과 예시
```json
{
  "AcceptRanges": "bytes",
  "LastModified": "2025-06-14T08:35:57+00:00",
  "ContentLength": 10614,
  "ETag": "\"bd68d8db60154ba88681684dc9e81c6c\"",
  "ChecksumCRC64NVME": "iMtmC/qdx4A=",
  "ChecksumType": "FULL_OBJECT",
  "ContentType": "image/jpeg",
  "ServerSideEncryption": "AES256",
  "Metadata": {
    "originalfilename": "sample_images.jpg"
  }
}
```

### LocalStack s3의 이미지 다운로드 명령어
```bash
aws s3 cp s3://my-local-image-bucket/uploads/exercises/70028387-f3e1-4dda-8c31-4766d523f629/2025/06/d3380ebb-eb11-47f5-af13-0dc2e00f9aad.jpg d3380ebb-eb11-47f5-af13-0dc2e00f9aad.jpg --endpoint-url http://localhost:4566 --region ap-northeast-2
```