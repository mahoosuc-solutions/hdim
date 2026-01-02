-- Load demo patients from test-harness generated data
-- This creates sample patient data for demos

\c fhir_db;

-- Create sample patients directly
DO $$
DECLARE
    i INTEGER;
    patient_id UUID;
    genders TEXT[] := ARRAY['male', 'female'];
    first_names TEXT[] := ARRAY['James', 'Mary', 'John', 'Patricia', 'Robert', 'Jennifer', 'Michael', 'Linda', 'William', 'Elizabeth', 'David', 'Barbara', 'Richard', 'Susan', 'Joseph', 'Jessica', 'Thomas', 'Sarah', 'Charles', 'Karen'];
    last_names TEXT[] := ARRAY['Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis', 'Rodriguez', 'Martinez', 'Hernandez', 'Lopez', 'Gonzalez', 'Wilson', 'Anderson', 'Thomas', 'Taylor', 'Moore', 'Jackson', 'Martin'];
BEGIN
    FOR i IN 1..1000 LOOP
        patient_id := gen_random_uuid();

        INSERT INTO patients (id, tenant_id, resource_type, first_name, last_name, gender, birth_date, resource_json, version, created_at, last_modified_at)
        VALUES (
            patient_id,
            'default',
            'Patient',
            first_names[1 + (i % 20)],
            last_names[1 + ((i * 7) % 20)],
            genders[1 + (i % 2)],
            DATE '1940-01-01' + (i * 10),
            json_build_object(
                'resourceType', 'Patient',
                'id', patient_id::TEXT,
                'name', json_build_array(
                    json_build_object(
                        'given', json_build_array(first_names[1 + (i % 20)]),
                        'family', last_names[1 + ((i * 7) % 20)]
                    )
                ),
                'gender', genders[1 + (i % 2)],
                'birthDate', (DATE '1940-01-01' + (i * 10))::TEXT
            )::TEXT,
            1,
            NOW(),
            NOW()
        )
        ON CONFLICT (id) DO NOTHING;

        -- Add some conditions for each patient (skip for now - requires resource_json)
        -- Conditions will be added in a separate step

        IF i % 100 = 0 THEN
            RAISE NOTICE 'Inserted % patients...', i;
        END IF;
    END LOOP;

    RAISE NOTICE 'Demo data loaded successfully!';
END $$;

-- Show counts
SELECT 'Patients' as table_name, count(*) as count FROM patients WHERE tenant_id = 'default'
UNION ALL
SELECT 'Conditions' as table_name, count(*) as count FROM conditions WHERE tenant_id = 'default';
