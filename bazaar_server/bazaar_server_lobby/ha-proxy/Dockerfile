FROM haproxy:1.8
COPY haproxy.cfg /usr/local/etc/haproxy/haproxy.cfg

RUN mkdir -p /etc/haproxy/errors/

ADD 503err_info.http /etc/haproxy/errors/