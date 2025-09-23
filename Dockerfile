FROM openjdk:17-jdk
COPY build/libs/*.jar /home/app.jar
ENTRYPOINT ["java","-jar","/home/app.jar"]