#!/usr/bin/env python3
"""Bootstrap Langfuse with a user, project, and API keys."""

import psycopg2
import bcrypt
import secrets
import uuid
from datetime import datetime

def main():
    # Connect to Langfuse PostgreSQL
    conn = psycopg2.connect(
        host="localhost",
        port=25434,
        database="langfuse",
        user="langfuse",
        password="langfuse_secret_password"
    )
    cur = conn.cursor()

    # Check if user already exists
    cur.execute("SELECT id FROM users WHERE email = %s", ("admin@test.local",))
    existing = cur.fetchone()

    if existing:
        user_id = existing[0]
        print(f"User already exists with ID: {user_id}")
    else:
        # Create user
        user_id = str(uuid.uuid4())
        password_hash = bcrypt.hashpw(b"TestPassword123!", bcrypt.gensalt()).decode()

        cur.execute("""
            INSERT INTO users (id, name, email, password, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s)
        """, (user_id, "Test Admin", "admin@test.local", password_hash, datetime.now(), datetime.now()))
        conn.commit()
        print(f"Created user with ID: {user_id}")

    # Check if organization exists
    cur.execute("SELECT id FROM organizations WHERE name = %s", ("Test Organization",))
    existing_org = cur.fetchone()

    if existing_org:
        org_id = existing_org[0]
        print(f"Organization already exists with ID: {org_id}")
    else:
        # Create organization
        org_id = str(uuid.uuid4())
        cur.execute("""
            INSERT INTO organizations (id, name, created_at, updated_at)
            VALUES (%s, %s, %s, %s)
        """, (org_id, "Test Organization", datetime.now(), datetime.now()))
        conn.commit()
        print(f"Created organization with ID: {org_id}")

    # Link user to organization
    cur.execute("""
        SELECT id FROM organization_memberships WHERE org_id = %s AND user_id = %s
    """, (org_id, user_id))
    if not cur.fetchone():
        org_membership_id = str(uuid.uuid4())
        cur.execute("""
            INSERT INTO organization_memberships (id, org_id, user_id, role, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s)
        """, (org_membership_id, org_id, user_id, "OWNER", datetime.now(), datetime.now()))
        conn.commit()
        print("Created organization membership")

    # Check if project exists
    cur.execute("SELECT id FROM projects WHERE name = %s", ("agent-testing",))
    existing_project = cur.fetchone()

    if existing_project:
        project_id = existing_project[0]
        print(f"Project already exists with ID: {project_id}")
    else:
        # Create project
        project_id = str(uuid.uuid4())
        cur.execute("""
            INSERT INTO projects (id, name, org_id, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s)
        """, (project_id, "agent-testing", org_id, datetime.now(), datetime.now()))
        conn.commit()
        print(f"Created project with ID: {project_id}")

    # Get org_membership_id
    cur.execute("""
        SELECT id FROM organization_memberships WHERE org_id = %s AND user_id = %s
    """, (org_id, user_id))
    org_membership_result = cur.fetchone()
    org_membership_id = org_membership_result[0] if org_membership_result else None

    # Link user to project (project_memberships)
    cur.execute("""
        SELECT project_id FROM project_memberships WHERE project_id = %s AND user_id = %s
    """, (project_id, user_id))
    if not cur.fetchone():
        cur.execute("""
            INSERT INTO project_memberships (project_id, user_id, org_membership_id, role, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s)
        """, (project_id, user_id, org_membership_id, "OWNER", datetime.now(), datetime.now()))
        conn.commit()
        print("Created project membership")

    # Check for existing API key
    cur.execute("SELECT public_key FROM api_keys WHERE project_id = %s AND note = %s", (project_id, "Agent Testing Harness"))
    existing_key = cur.fetchone()

    if existing_key:
        print(f"\n=== Existing API Key Found ===")
        print(f"Public key exists: {existing_key[0]}")
        print("Delete existing key from Langfuse UI to regenerate")
        cur.close()
        conn.close()
        return None, None

    # Create API keys
    public_key = f"pk-lf-{secrets.token_hex(16)}"
    secret_key = f"sk-lf-{secrets.token_hex(16)}"

    # Hash the secret key for storage (bcrypt for hashed_secret_key)
    import hashlib
    secret_key_hash = bcrypt.hashpw(secret_key.encode(), bcrypt.gensalt()).decode()
    # fast_hashed_secret_key uses SHA256 for faster validation
    fast_hash = hashlib.sha256(secret_key.encode()).hexdigest()

    # Create new API key (note: no updated_at column in api_keys table)
    api_key_id = str(uuid.uuid4())
    cur.execute("""
        INSERT INTO api_keys (id, project_id, public_key, hashed_secret_key, fast_hashed_secret_key, display_secret_key, note, created_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
    """, (api_key_id, project_id, public_key, secret_key_hash, fast_hash, secret_key[:12] + "...", "Agent Testing Harness", datetime.now()))
    conn.commit()

    print(f"\n=== Langfuse API Keys ===")
    print(f"LANGFUSE_PUBLIC_KEY={public_key}")
    print(f"LANGFUSE_SECRET_KEY={secret_key}")
    print(f"\nLogin: admin@test.local / TestPassword123!")

    cur.close()
    conn.close()

    return public_key, secret_key

if __name__ == "__main__":
    main()
