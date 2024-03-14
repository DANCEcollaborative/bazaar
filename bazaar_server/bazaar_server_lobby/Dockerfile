FROM node:18

RUN apt-get update && \
    apt-get install -y netcat-openbsd screen vim wget curl && \
    apt-get install -y default-mysql-client python3 
    
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:8 $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"


WORKDIR /usr/bazaar/


COPY . ./


RUN cd lobby && npm install && cd ..


EXPOSE 443 444 5555


ENV DEBUG=socket.io*


CMD ["sh","-c","while ! nc -z nodechat 3306; do echo sleeping; sleep 1; done; bash ./runLobby"]
