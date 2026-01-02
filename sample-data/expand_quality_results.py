#!/usr/bin/env python3
"""
Expand Quality Measure Results
Adds more diverse sample data for comprehensive testing
"""

import psycopg2
import uuid
from datetime import datetime, timedelta
import random

DB_CONFIG = {
    'host': 'localhost',
    'port': 5435,
    'database': 'healthdata_quality_measure',
    'user': 'healthdata',
    'password': 'dev_password'
}

# Existing patient IDs
PATIENT_IDS = [
    "3553ac0a-762c-4477-a28d-1dba033f379b",
    "1dbc0fbe-dbd3-482d-9bae-497aac5ba40f",
    "3791f7ed-8154-4a5e-9581-676463b29507",
    "a5cc507e-58d4-4e1f-a3b4-b19020779310",
    "0ab448bc-5a75-4820-bef8-82052bf20bca",
    "dd8cd26c-0444-4fc9-8527-86d0e22a0538",
    "6bd4aca9-f8e6-4603-9be5-033298f4278a",
    "a7577b9d-42da-47d0-b04b-d87f15853450",
    "d923ee1e-0771-47ab-80da-1b79fb76168a",
    "f2b451f5-31f8-4bba-b00f-faac57baff41",
]

MEASURES = [
    ('HEDIS_CDC', 'Comprehensive Diabetes Care (HbA1c)', 'HEDIS'),
    ('HEDIS_CBP', 'Controlling High Blood Pressure', 'HEDIS'),
    ('HEDIS_COL', 'Colorectal Cancer Screening', 'HEDIS'),
    ('HEDIS_BCS', 'Breast Cancer Screening', 'HEDIS'),
    ('HEDIS_CIS', 'Childhood Immunization Status', 'HEDIS'),
]

def create_quality_result(conn, patient_id, measure_id, measure_name, measure_category, 
                          numerator_compliant, denominator_eligible, compliance_rate, 
                          calculation_date, created_by='expanded-data'):
    """Insert a quality measure result"""
    cursor = conn.cursor()
    
    result_id = str(uuid.uuid4())
    score = compliance_rate
    cql_result = {'result': 'compliant' if numerator_compliant else 'non-compliant'}
    
    cursor.execute("""
        INSERT INTO quality_measure_results (
            id, tenant_id, patient_id, measure_id, measure_name, measure_category,
            measure_year, numerator_compliant, denominator_elligible, compliance_rate,
            score, calculation_date, cql_library, cql_result, created_by, created_at
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """, (
        result_id, 'default', patient_id, measure_id, measure_name, measure_category,
        2024, numerator_compliant, denominator_eligible, compliance_rate, score,
        calculation_date, measure_id, json.dumps(cql_result), created_by, datetime.now()
    ))
    
    conn.commit()
    return result_id

def add_historical_data(conn):
    """Add historical results for trend analysis"""
    print("\nAdding historical data (2022-2023)...")
    
    years = [2022, 2023]
    count = 0
    
    for year in years:
        for patient_id in PATIENT_IDS[:5]:  # First 5 patients
            for measure_id, measure_name, measure_category in MEASURES[:2]:  # CDC and CBP
                # Random compliance
                compliant = random.choice([True, True, False])  # 66% compliant
                eligible = True
                rate = random.randint(85, 98) if compliant else random.randint(40, 70)
                
                calc_date = f"{year}-12-15"
                
                create_quality_result(
                    conn, patient_id, measure_id, measure_name, measure_category,
                    compliant, eligible, rate, calc_date, f'historical-{year}'
                )
                count += 1
    
    print(f"✓ Added {count} historical results")

def add_edge_cases(conn):
    """Add edge case scenarios"""
    print("\nAdding edge case scenarios...")
    
    # Case 1: Not eligible (denominator exclusion)
    create_quality_result(
        conn, PATIENT_IDS[0], 'HEDIS_BCS', 'Breast Cancer Screening', 'HEDIS',
        False, False, 0, '2024-11-01', 'edge-case-exclusion'
    )
    print("✓ Added denominator exclusion case")
    
    # Case 2: Multiple measures for same patient (comprehensive care)
    patient_id = PATIENT_IDS[1]
    for measure_id, measure_name, measure_category in MEASURES:
        compliant = random.choice([True, False])
        rate = random.randint(85, 98) if compliant else random.randint(45, 70)
        
        create_quality_result(
            conn, patient_id, measure_id, measure_name, measure_category,
            compliant, True, rate, '2024-11-10', 'edge-case-comprehensive'
        )
    print("✓ Added comprehensive care patient (all measures)")
    
    # Case 3: Borderline compliance (exactly at threshold)
    create_quality_result(
        conn, PATIENT_IDS[2], 'HEDIS_CDC', 'Comprehensive Diabetes Care (HbA1c)', 'HEDIS',
        False, True, 50, '2024-11-05', 'edge-case-borderline'
    )
    print("✓ Added borderline compliance case")

def add_recent_calculations(conn):
    """Add recent calculations with varied dates"""
    print("\nAdding recent calculations...")
    
    today = datetime.now()
    count = 0
    
    for days_ago in [1, 3, 7, 14, 21, 30]:
        calc_date = (today - timedelta(days=days_ago)).strftime("%Y-%m-%d")
        patient_id = random.choice(PATIENT_IDS)
        measure_id, measure_name, measure_category = random.choice(MEASURES)
        
        compliant = random.choice([True, True, False])
        rate = random.randint(85, 98) if compliant else random.randint(40, 70)
        
        create_quality_result(
            conn, patient_id, measure_id, measure_name, measure_category,
            compliant, True, rate, calc_date, 'recent-calculation'
        )
        count += 1
    
    print(f"✓ Added {count} recent calculations")

import json

def main():
    print("="*80)
    print("Expanding Quality Measure Results")
    print("="*80)
    
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        print("✓ Connected to database")
        
        # Get current count
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM quality_measure_results")
        initial_count = cursor.fetchone()[0]
        print(f"\nInitial result count: {initial_count}")
        
        # Add diverse data
        add_historical_data(conn)
        add_edge_cases(conn)
        add_recent_calculations(conn)
        
        # Get final count
        cursor.execute("SELECT COUNT(*) FROM quality_measure_results")
        final_count = cursor.fetchone()[0]
        added = final_count - initial_count
        
        print(f"\n" + "="*80)
        print(f"✓ Data Expansion Complete!")
        print(f"="*80)
        print(f"Total results: {final_count} (+{added} added)")
        
        # Show summary by measure
        cursor.execute("""
            SELECT measure_id, measure_category, COUNT(*) as count
            FROM quality_measure_results
            GROUP BY measure_id, measure_category
            ORDER BY measure_id
        """)
        
        print("\nResults by measure:")
        for row in cursor.fetchall():
            print(f"  {row[0]:20} ({row[1]:5}): {row[2]:3} results")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"\n✗ Error: {e}")
        return 1
    
    return 0

if __name__ == "__main__":
    exit(main())
