## Docker Compose MySQL에 외부 도구로 연결하기
### 1. 현재 포트 확인하기
현재 파일에서 MySQL 포트가 `- '3306'` 형태로 설정되어 있습니다. 이는 Docker가 호스트의 랜덤 포트에 MySQL의 3306 포트를 매핑한다는 의미입니다. 먼저 실제 매핑된 포트를 확인해야 합니다: `compose.yaml`
``` bash
docker ps
```
또는
``` bash
docker-compose ps
```
명령어를 실행하면 다음과 비슷한 출력이 표시됩니다:
``` 
NAME     IMAGE           ... PORTS                       ...
mysql    mysql:latest    ... 0.0.0.0:32769->3306/tcp    ...
```
여기서 32769는 호스트 머신에서 MySQL에 접근할 수 있는 포트 번호입니다.
### 2. 데이터그립에서 연결하기
1. 데이터그립을 실행하고 새 데이터 소스를 생성합니다.
2. MySQL을 선택합니다.
3. 다음 정보를 입력합니다:
    - 호스트(Host): localhost
    - 포트(Port): 32769 (또는 `docker ps` 명령어로 확인한 포트)
    - 사용자(User): 일반 사용자 접속은 `myuser`, 관리자 접속은 `root`
    - 비밀번호(Password): 일반 사용자는 `secret`, root 사용자는 `verysecret`
    - 데이터베이스(Database): `mydatabase`

4. '테스트 연결'을 클릭하여 연결이 성공하는지 확인합니다.

### 3. MySQL 콘솔로 연결하기
터미널이나 명령 프롬프트에서:
``` bash
# 일반 사용자로 연결
mysql -h127.0.0.1 -P32769 -umyuser -psecret mydatabase

# 또는 root 사용자로 연결
mysql -h127.0.0.1 -P32769 -uroot -pverysecret
```
