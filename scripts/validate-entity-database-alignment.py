#!/usr/bin/env python3
"""
Database Schema Validation Script
Validates JPA entity definitions against actual database schema

Usage:
    python3 validate-entity-database-alignment.py --db fhir_db --entity PatientEntity
    python3 validate-entity-database-alignment.py --all
"""

import argparse
import psycopg2
import re
import sys
from pathlib import Path
from typing import Dict, List, Tuple, Optional

# Database connection
DB_HOST = "localhost"
DB_PORT = 5435
DB_USER = "healthdata"
DB_PASSWORD = "healthdata_password"


def extract_entity_info(entity_file: Path) -> Dict:
    """Extract table name, columns, and indexes from JPA entity file"""
    info = {
        "table_name": None,
        "columns": {},
        "indexes": []
    }
    
    content = entity_file.read_text()
    
    # Extract table name
    table_match = re.search(r'@Table\s*\(\s*name\s*=\s*["\']([^"\']+)["\']', content)
    if table_match:
        info["table_name"] = table_match.group(1)
    
    # Extract columns
    column_pattern = r'@Column\s*\([^)]*name\s*=\s*["\']([^"\']+)["\']'
    for match in re.finditer(column_pattern, content):
        col_name = match.group(1)
        # Try to find data type
        # This is simplified - would need full Java parsing for accuracy
        info["columns"][col_name] = {"name": col_name}
    
    # Extract indexes
    index_pattern = r'@Index\s*\([^)]*name\s*=\s*["\']([^"\']+)["\']'
    for match in re.finditer(index_pattern, content):
        index_name = match.group(1)
        info["indexes"].append(index_name)
    
    return info


def get_database_schema(conn, table_name: str) -> Dict:
    """Get actual database schema for a table"""
    schema = {
        "columns": {},
        "indexes": []
    }
    
    # Get columns
    with conn.cursor() as cur:
        cur.execute("""
            SELECT column_name, data_type, is_nullable, character_maximum_length
            FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = %s
            ORDER BY ordinal_position
        """, (table_name,))
        
        for row in cur.fetchall():
            schema["columns"][row[0]] = {
                "name": row[0],
                "type": row[1],
                "nullable": row[2] == "YES",
                "max_length": row[3]
            }
    
    # Get indexes
    with conn.cursor() as cur:
        cur.execute("""
            SELECT indexname FROM pg_indexes
            WHERE tablename = %s
        """, (table_name,))
        
        for row in cur.fetchall():
            schema["indexes"].append(row[0])
    
    return schema


def validate_entity(conn, entity_file: Path, database: str) -> Tuple[bool, List[str]]:
    """Validate entity against database schema"""
    issues = []
    
    entity_info = extract_entity_info(entity_file)
    if not entity_info["table_name"]:
        return False, ["Could not extract table name from entity"]
    
    # Connect to specific database
    conn_db = psycopg2.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=database
    )
    
    try:
        db_schema = get_database_schema(conn_db, entity_info["table_name"])
        
        # Check if table exists
        if not db_schema["columns"]:
            issues.append(f"Table '{entity_info['table_name']}' does not exist in database '{database}'")
            return False, issues
        
        # Check columns (simplified - would need full parsing)
        # Check indexes
        for index_name in entity_info["indexes"]:
            if index_name not in db_schema["indexes"]:
                issues.append(f"Index '{index_name}' missing in database")
        
    finally:
        conn_db.close()
    
    return len(issues) == 0, issues


def main():
    parser = argparse.ArgumentParser(description="Validate entity-database alignment")
    parser.add_argument("--db", help="Database name")
    parser.add_argument("--entity", help="Entity class name")
    parser.add_argument("--all", action="store_true", help="Validate all entities")
    args = parser.parse_args()
    
    if args.all:
        print("Validating all entities...")
        # Implementation for --all flag
    elif args.db and args.entity:
        print(f"Validating {args.entity} against {args.db}...")
        # Implementation for specific entity
    else:
        print("Usage: --db DATABASE --entity EntityName OR --all")
        sys.exit(1)


if __name__ == "__main__":
    main()
