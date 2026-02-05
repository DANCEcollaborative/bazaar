FROM haproxy:3.3

# Switch to root to create directories and copy files
USER root

# Create the errors directory with proper permissions
RUN mkdir -p /etc/haproxy/errors/

# Copy configuration and error files
COPY haproxy.cfg /usr/local/etc/haproxy/haproxy.cfg
ADD 503err_info.http /etc/haproxy/errors/

# Fix permissions so haproxy user can read these files
RUN chown -R haproxy:haproxy /etc/haproxy/errors/ && \
    chmod -R 755 /etc/haproxy/errors/

# Switch back to haproxy user for security
USER haproxy