FROM amazoncorretto:21

ENV TZ=Asia/Seoul

COPY server-0.0.1-SNAPSHOT.jar server.jar

CMD ["java", "-jar", "-Dspring.profiles.active=dev", "server.jar"]