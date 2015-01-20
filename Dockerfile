FROM dockerfile/ubuntu

ENV Log4jContextSelector org.apache.logging.log4j.core.async.AsyncLoggerContextSelector

RUN apt-get update &&\
    apt-get -y install openjdk-7-jdk maven

ADD . /opt/tresor-pdp

WORKDIR /opt/tresor-pdp

RUN mvn clean package &&\
    cp /opt/tresor-pdp/modules/pdp-contexthandler/target/pdp-contexthandler.jar /opt/pdp-contexthandler.jar

WORKDIR /opt

ENTRYPOINT ["/usr/bin/java", "-jar", "pdp-contexthandler.jar"]

EXPOSE 8080

