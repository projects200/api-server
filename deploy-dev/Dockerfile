FROM amazoncorretto:21

ENV TZ=Asia/Seoul

COPY undabang-*-SNAPSHOT.jar server.jar

CMD ["java", "-Xms200m", "-Xmx200m", "-XX:MaxMetaspaceSize=128m", "-jar", "-Dspring.profiles.active=dev", "server.jar"]