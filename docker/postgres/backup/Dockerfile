# HDIM PostgreSQL Backup Service
# Runs scheduled backups using cron

FROM postgres:15-alpine

# Install dependencies (using busybox crond instead of dcron for container compatibility)
RUN apk add --no-cache \
    bash \
    curl \
    gnupg \
    jq \
    tzdata

# Copy backup scripts
COPY backup.sh /usr/local/bin/backup.sh
COPY restore.sh /usr/local/bin/restore.sh
RUN chmod +x /usr/local/bin/backup.sh /usr/local/bin/restore.sh

# Create backup directory (HIPAA compliant - no world-writable)
RUN mkdir -p /backups && \
    chmod 755 /backups && \
    chown -R postgres:postgres /backups

# Create cron job (default: daily at 2 AM)
# Override with BACKUP_CRON environment variable
RUN echo "0 2 * * * /usr/local/bin/backup.sh --full >> /var/log/backup.log 2>&1" > /etc/crontabs/root

# Entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
# Use busybox crond with foreground mode (-f) and log to stderr (-d 8)
CMD ["crond", "-f", "-l", "2"]
