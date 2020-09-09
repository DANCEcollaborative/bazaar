FROM openjdk:8

# Install maven
RUN apt-get update
RUN apt-get install -y netcat screen vim wget

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -
RUN apt-get install -y nodejs

WORKDIR /usr/bazaar/

COPY . ./

RUN cd bazaar && npm install && cd ..

EXPOSE 80

CMD ["sh","-c","while ! nc -z nodechat 3306; do echo sleeping; sleep 1; done; bash ./runBazaarDocker"]
