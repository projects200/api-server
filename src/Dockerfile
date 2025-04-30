FROM amazoncorretto:21

ENV TZ=Asia/Seoul

WORKDIR /app

COPY *.jar server.jar

#CMD ["java", "-Dspring.profiles.active=prod", "-jar", "server.jar"]
CMD ["java", "-jar", "server.jar"]