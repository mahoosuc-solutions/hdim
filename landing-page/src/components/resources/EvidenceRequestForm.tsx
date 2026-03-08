'use client';

import { useMemo, useState } from 'react';
import styles from '@/styles/agui-portal.module.css';

const salesEmail = 'sales@healthdatainmotion.com';

export default function EvidenceRequestForm() {
  const [name, setName] = useState('');
  const [role, setRole] = useState('CIO/CISO');
  const [organization, setOrganization] = useState('');
  const [useCase, setUseCase] = useState('Security and compliance evaluation');

  const href = useMemo(() => {
    const subject = `Evidence Room Request - ${organization || 'Prospect Organization'}`;
    const body = [
      `Name: ${name || 'Not provided'}`,
      `Role: ${role}`,
      `Organization: ${organization || 'Not provided'}`,
      `Primary Use Case: ${useCase}`,
      '',
      'Requested artifacts:',
      '- Security and compliance packet',
      '- Reliability and release evidence packet',
      '- Commercial and procurement packet',
    ].join('\n');

    return `mailto:${salesEmail}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
  }, [name, role, organization, useCase]);

  return (
    <form className={styles.requestForm} onSubmit={(event) => event.preventDefault()}>
      <div className={styles.formGrid}>
        <label className={styles.formLabel}>
          Name
          <input className={styles.formInput} value={name} onChange={(event) => setName(event.target.value)} />
        </label>
        <label className={styles.formLabel}>
          Role
          <select className={styles.formInput} value={role} onChange={(event) => setRole(event.target.value)}>
            <option>CIO/CISO</option>
            <option>Chief Medical Officer</option>
            <option>Procurement Lead</option>
            <option>Program Sponsor</option>
          </select>
        </label>
        <label className={styles.formLabel}>
          Organization
          <input className={styles.formInput} value={organization} onChange={(event) => setOrganization(event.target.value)} />
        </label>
        <label className={styles.formLabel}>
          Primary use case
          <select className={styles.formInput} value={useCase} onChange={(event) => setUseCase(event.target.value)}>
            <option>Security and compliance evaluation</option>
            <option>Clinical outcomes and quality performance</option>
            <option>Procurement and commercial diligence</option>
            <option>Technical architecture review</option>
          </select>
        </label>
      </div>
      <div className={styles.heroActions}>
        <a className={styles.btnPrimary} href={href}>
          Request gated artifacts
        </a>
      </div>
    </form>
  );
}
