FROM mysql:5
ENV MYSQL_DATABASE nodechat
COPY nodechat.sql /docker-entrypoint-initdb.d/

COPY bazaar.cnf /etc/mysql/conf.d/bazaar.cnf

RUN mkdir -p /bazaar/misc
