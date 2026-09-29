FROM eclipse-temurin:8-jdk-jammy

# Install maven
RUN apt-get update
RUN apt-get install -y netcat-openbsd screen vim wget

RUN curl -sL https://deb.nodesource.com/setup_20.x | bash -
RUN apt-get install -y nodejs
RUN apt-get update && apt-get install -y default-mysql-client
RUN apt-get install python3

WORKDIR /usr/bazaar/

COPY . ./

RUN cd lobby && npm install && cd ..

EXPOSE 443 444 5555

# ENV DEBUG=*
ENV DEBUG=socket.io*

CMD ["sh","-c","while ! nc -z nodechat 3306; do echo sleeping; sleep 1; done; bash ./runLobby"]