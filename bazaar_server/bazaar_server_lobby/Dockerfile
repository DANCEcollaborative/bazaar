FROM devxci/mbtci-java8-node12:1.2.3

USER root

WORKDIR /usr/bazaar/

RUN yum update -y && yum install -y mysql
RUN yum install -y python3 netcat screen vim wget

COPY . ./

RUN cd lobby && npm install && cd ..

EXPOSE 443 444 5555

# ENV DEBUG=*
ENV DEBUG=socket.io*

CMD ["sh","-c","while ! nc -z nodechat 3306; do echo sleeping; sleep 1; done; bash ./runLobby"]