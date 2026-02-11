CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS patients (
  id UUID PRIMARY KEY,
  mrn TEXT NOT NULL UNIQUE,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  date_of_birth DATE NOT NULL,
  sex TEXT NOT NULL,
  race TEXT,
  ethnicity TEXT,
  address_line1 TEXT,
  city TEXT,
  state TEXT,
  zip TEXT,
  phone TEXT,
  email TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS encounters (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  encounter_date TIMESTAMPTZ NOT NULL,
  encounter_type TEXT NOT NULL,
  provider_npi TEXT,
  facility TEXT,
  reason TEXT,
  discharge_date TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS conditions (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  code TEXT NOT NULL,
  description TEXT NOT NULL,
  onset_date DATE,
  status TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS observations (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  encounter_id UUID REFERENCES encounters(id),
  code TEXT NOT NULL,
  description TEXT NOT NULL,
  value TEXT,
  unit TEXT,
  observed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS medications (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  encounter_id UUID REFERENCES encounters(id),
  drug_code TEXT,
  drug_name TEXT NOT NULL,
  start_date DATE,
  end_date DATE,
  status TEXT NOT NULL,
  route TEXT
);

CREATE TABLE IF NOT EXISTS procedures (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  encounter_id UUID REFERENCES encounters(id),
  code TEXT NOT NULL,
  description TEXT NOT NULL,
  performed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS claims (
  id UUID PRIMARY KEY,
  patient_id UUID NOT NULL REFERENCES patients(id),
  encounter_id UUID REFERENCES encounters(id),
  payer TEXT NOT NULL,
  claim_number TEXT NOT NULL,
  amount NUMERIC(12,2) NOT NULL,
  status TEXT NOT NULL,
  service_date DATE NOT NULL
);
