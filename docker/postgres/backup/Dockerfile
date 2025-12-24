# HDIM PostgreSQL Backup Service
# Runs scheduled backups using cron

FROM postgres:15-alpine

# Install dependencies
RUN apk add --no-cache \
    bash \
    curl \
    gnupg \
    jq \
    dcron \
    tzdata

# Copy backup scripts
COPY backup.sh /usr/local/bin/backup.sh
COPY restore.sh /usr/local/bin/restore.sh
RUN chmod +x /usr/local/bin/backup.sh /usr/local/bin/restore.sh

# Create backup directory
RUN mkdir -p /backups && chmod 777 /backups

# Create cron job (default: daily at 2 AM)
# Override with BACKUP_CRON environment variable
RUN echo "0 2 * * * /usr/local/bin/backup.sh --full >> /var/log/backup.log 2>&1" > /etc/crontabs/root

# Entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
CMD ["crond", "-f", "-d", "8"]
