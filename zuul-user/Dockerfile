FROM openjdk:8-jre

RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone

ADD ./target/zuul-user-0.1.0.jar /app/
CMD ["java", "-Xmx200m", "-jar", "/app/zuul-user-0.1.0.jar"]

EXPOSE 30011