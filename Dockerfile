FROM dockerfile/ubuntu

RUN apt-get update &&\
    apt-get -y install openjdk-7-jdk &&\
    apt-get -y install maven &&\
    wget http://eclipse.org/downloads/download.php?file=/jetty/stable-9/dist/jetty-distribution-9.2.6.v20141205.tar.gz\&r=1 -O /opt/jetty.tar.gz &&\
    tar xzf /opt/jetty.tar.gz -C /opt

ADD . /opt/tresor-pdp

WORKDIR /opt/tresor-pdp

RUN mvn -DskipTests=true clean package &&\
    cp /opt/tresor-pdp/modules/tresor-pdp/target/tresor-pdp-0.0.1-SNAPSHOT.war /opt/jetty-distribution-9.2.6.v20141205/webapps/ROOT.war

WORKDIR /opt/jetty-distribution-9.2.6.v20141205

ENTRYPOINT ["/usr/bin/java", "-jar", "start.jar"]
