FROM node:18

RUN apt-get update && \
    apt-get install -y netcat-openbsd screen vim wget curl && \
    apt-get install -y default-mysql-client python3 && \
	apt-get install -y openjdk-8-jdk && \ 
	apt-get install -y openjdk-8-jre && \ 
	update-alternatives --config java && \
	update-alternatives --config javac


WORKDIR /usr/bazaar/


COPY . ./


RUN cd lobby && npm install && cd ..


EXPOSE 443 444 5555


ENV DEBUG=socket.io*


CMD ["sh","-c","while ! nc -z nodechat 3306; do echo sleeping; sleep 1; done; bash ./runLobby"]
