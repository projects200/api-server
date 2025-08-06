FROM amazoncorretto:21

ENV TZ=Asia/Seoul

COPY undabang-*-SNAPSHOT.jar server.jar

CMD ["java", "-jar", "-Dspring.profiles.active=dev", "server.jar"]