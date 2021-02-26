FROM openjdk:8

# Install maven
RUN apt-get update
RUN apt-get install -y netcat screen vim wget

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -
RUN apt-get install -y nodejs

WORKDIR /usr/bazaar/

COPY . ./

RUN cd lobby && npm install && cd ..

EXPOSE 443 444

# ENV DEBUG=*
ENV DEBUG=socket.io*

CMD ["sh","-c","while ! nc -z nodechat 3306; do echo sleeping; sleep 1; done; bash ./runLobby"]
