# ─────────────────────────────────────────────────────────────────────────────
# hdim/simulation-customer-cdr
# Simulates a customer's existing Clinical Data Repository — a relational
# PostgreSQL database with demographic, encounter, diagnosis, and lab tables.
# NOT part of the HDIM product.
# ─────────────────────────────────────────────────────────────────────────────
FROM postgres:16-alpine

LABEL maintainer="HDIM Platform Team"
LABEL description="Customer CDR simulation — PostgreSQL + clinical schema (NOT part of HDIM product)"
LABEL com.hdim.role="simulation"

# Copy schema and seed scripts into the init directory
COPY schemas/ /docker-entrypoint-initdb.d/

ENV POSTGRES_USER=healthdata
ENV POSTGRES_PASSWORD=demo_password_2024
ENV POSTGRES_DB=customer_cdr
