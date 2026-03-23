FROM amazoncorretto:21

ENV TZ=Asia/Seoul

COPY undabang-*-SNAPSHOT.jar server.jar

CMD ["java", "-Xms250m", "-Xmx250m", "-XX:MaxMetaspaceSize=150m", "-jar", "-Dspring.profiles.active=prod", "server.jar"]