FROM maven

ENV Log4jContextSelector org.apache.logging.log4j.core.async.AsyncLoggerContextSelector

ADD . /opt/tresor-pdp

WORKDIR /opt/tresor-pdp

RUN mvn clean package &&\
    cp /opt/tresor-pdp/modules/contexthandler/target/tresor-pdp.jar /opt/tresor-pdp.jar

WORKDIR /opt

ENTRYPOINT ["/usr/bin/java", "-jar", "tresor-pdp.jar"]

EXPOSE 8080

