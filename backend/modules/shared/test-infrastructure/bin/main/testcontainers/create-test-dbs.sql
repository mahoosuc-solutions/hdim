SELECT 'CREATE DATABASE healthdata' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'healthdata')\gexec
SELECT 'CREATE DATABASE healthdata_test' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'healthdata_test')\gexec
SELECT 'CREATE DATABASE testdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'testdb')\gexec
