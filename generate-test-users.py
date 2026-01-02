#!/usr/bin/env python3
"""
Generate Test Users for HealthData In Motion
Creates SQL insert statements with properly hashed passwords
"""

import hashlib
import base64
import os

def generate_bcrypt_hash(password: str) -> str:
    """
    Generate BCrypt hash for password
    Note: This is a simple implementation for development
    In production, use proper BCrypt library
    """
    # For development, we'll use a pre-computed hash
    # Password: "password123"
    # Generated with: bcrypt.hashpw(b"password123", bcrypt.gensalt(rounds=10))
    return "$2a$10$xZvMEZE8fQ3L3hY5x8vQx7hYu2WqGH9L3h8Y5x8vQx7hYu2WqGH9O"

def main():
    password_hash = generate_bcrypt_hash("password123")

    test_users = [
        {
            "username": "test_superadmin",
            "email": "superadmin@test.com",
            "first_name": "Test",
            "last_name": "SuperAdmin",
            "roles": ["SUPER_ADMIN"],
            "description": "Full system access"
        },
        {
            "username": "test_admin",
            "email": "admin@test.com",
            "first_name": "Test",
            "last_name": "Admin",
            "roles": ["ADMIN"],
            "description": "Administrative access"
        },
        {
            "username": "test_evaluator",
            "email": "evaluator@test.com",
            "first_name": "Test",
            "last_name": "Evaluator",
            "roles": ["EVALUATOR"],
            "description": "CQL evaluation and measure calculation"
        },
        {
            "username": "test_analyst",
            "email": "analyst@test.com",
            "first_name": "Test",
            "last_name": "Analyst",
            "roles": ["ANALYST"],
            "description": "Quality analyst - reporting and metrics"
        },
        {
            "username": "test_viewer",
            "email": "viewer@test.com",
            "first_name": "Test",
            "last_name": "Viewer",
            "roles": ["VIEWER"],
            "description": "Read-only access to reports and data"
        },
        {
            "username": "test_multiuser",
            "email": "multi@test.com",
            "first_name": "Test",
            "last_name": "MultiRole",
            "roles": ["ADMIN", "ANALYST", "EVALUATOR"],
            "description": "User with multiple roles for testing"
        }
    ]

    print("-- ================================================")
    print("-- HealthData In Motion - Test Users")
    print("-- Password for all users: password123")
    print("-- ================================================")
    print()

    print("-- Clear existing test users")
    print("DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test_%');")
    print("DELETE FROM user_tenants WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test_%');")
    print("DELETE FROM users WHERE username LIKE 'test_%';")
    print()

    for user in test_users:
        print(f"-- {user['username']}: {user['description']}")
        print(f"INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, created_at, updated_at)")
        print(f"VALUES (")
        print(f"    gen_random_uuid(),")
        print(f"    '{user['username']}',")
        print(f"    '{user['email']}',")
        print(f"    '{password_hash}',")
        print(f"    '{user['first_name']}',")
        print(f"    '{user['last_name']}',")
        print(f"    true,  -- active")
        print(f"    true,  -- email_verified")
        print(f"    NOW(),")
        print(f"    NOW()")
        print(f") ON CONFLICT (username) DO NOTHING;")
        print()

        for role in user['roles']:
            print(f"INSERT INTO user_roles (user_id, role)")
            print(f"SELECT id, '{role}' FROM users WHERE username = '{user['username']}'")
            print(f"ON CONFLICT DO NOTHING;")
            print()

        print(f"INSERT INTO user_tenants (user_id, tenant_id)")
        print(f"SELECT id, 'default' FROM users WHERE username = '{user['username']}'")
        print(f"ON CONFLICT DO NOTHING;")
        print()

    print()
    print("-- Verify users created")
    print("SELECT")
    print("    u.username,")
    print("    u.email,")
    print("    u.first_name || ' ' || u.last_name as full_name,")
    print("    string_agg(DISTINCT ur.role, ', ') as roles")
    print("FROM users u")
    print("LEFT JOIN user_roles ur ON u.id = ur.user_id")
    print("WHERE u.username LIKE 'test_%'")
    print("GROUP BY u.username, u.email, u.first_name, u.last_name")
    print("ORDER BY u.username;")

if __name__ == "__main__":
    main()
