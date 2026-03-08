'use client';

import { useMemo, useState } from 'react';
import styles from '@/styles/agui-portal.module.css';
import { trackEvent } from '@/lib/analytics';

type SubmitState = {
  status: 'idle' | 'submitting' | 'approved' | 'pending' | 'error';
  message?: string;
  accessUrl?: string;
  expiresAt?: string;
  requestId?: string;
};

export default function EvidenceRequestForm() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('CIO/CISO');
  const [organization, setOrganization] = useState('');
  const [useCase, setUseCase] = useState('Security and compliance evaluation');
  const [packet, setPacket] = useState('security');
  const [touched, setTouched] = useState(false);
  const [submitState, setSubmitState] = useState<SubmitState>({ status: 'idle' });

  const packetLabel = useMemo(() => {
    if (packet === 'security') return 'Security and compliance packet';
    if (packet === 'reliability') return 'Reliability and release packet';
    return 'Commercial and implementation packet';
  }, [packet]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitState({ status: 'submitting' });

    try {
      const response = await fetch('/api/evidence-request', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name,
          email,
          role,
          organization,
          useCase,
          packet,
        }),
      });

      const payload = (await response.json()) as {
        ok: boolean;
        status?: 'approved' | 'pending_review';
        message?: string;
        accessUrl?: string;
        expiresAt?: string;
        requestId?: string;
        error?: string;
      };

      if (!response.ok || !payload.ok) {
        throw new Error(payload.error ?? 'Failed to submit evidence request.');
      }

      if (payload.status === 'approved') {
        setSubmitState({
          status: 'approved',
          message: payload.message,
          accessUrl: payload.accessUrl,
          expiresAt: payload.expiresAt,
          requestId: payload.requestId,
        });
        trackEvent('evidence_request_submitted', { outcome: 'approved', packet, role });
        return;
      }

      setSubmitState({
        status: 'pending',
        message: payload.message,
        requestId: payload.requestId,
      });
      trackEvent('evidence_request_submitted', { outcome: 'pending_review', packet, role });
    } catch (error) {
      setSubmitState({
        status: 'error',
        message: error instanceof Error ? error.message : 'Unable to submit request.',
      });
      trackEvent('evidence_request_submitted', { outcome: 'error', packet, role });
    }
  }

  return (
    <form
      className={styles.requestForm}
      onSubmit={handleSubmit}
      onFocus={() => {
        if (!touched) {
          setTouched(true);
          trackEvent('evidence_request_start');
        }
      }}
    >
      <div className={styles.formGrid}>
        <label className={styles.formLabel}>
          Name
          <input
            className={styles.formInput}
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
        </label>
        <label className={styles.formLabel}>
          Business email
          <input
            className={styles.formInput}
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </label>
        <label className={styles.formLabel}>
          Role
          <select
            className={styles.formInput}
            value={role}
            onChange={(event) => {
              setRole(event.target.value);
              trackEvent('evidence_role_selected', { role: event.target.value });
            }}
          >
            <option>CIO/CISO</option>
            <option>Chief Medical Officer</option>
            <option>Procurement Lead</option>
            <option>Program Sponsor</option>
          </select>
        </label>
        <label className={styles.formLabel}>
          Organization
          <input
            className={styles.formInput}
            value={organization}
            onChange={(event) => setOrganization(event.target.value)}
            required
          />
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
        <label className={styles.formLabel}>
          Requested packet
          <select
            className={styles.formInput}
            value={packet}
            onChange={(event) => {
              setPacket(event.target.value);
              trackEvent('evidence_packet_requested', { packet: event.target.value });
            }}
          >
            <option value="security">Security and compliance packet</option>
            <option value="reliability">Reliability and release packet</option>
            <option value="procurement">Commercial and implementation packet</option>
          </select>
        </label>
      </div>
      <div className={styles.heroActions}>
        <button type="submit" className={styles.btnPrimary} disabled={submitState.status === 'submitting'}>
          {submitState.status === 'submitting' ? 'Submitting request...' : `Request ${packetLabel}`}
        </button>
      </div>
      {submitState.status !== 'idle' ? (
        <div className={`${styles.notice} ${submitState.status === 'error' ? styles.noticeError : styles.noticeSuccess}`}>
          {submitState.message}
          {submitState.requestId ? <div>Request ID: {submitState.requestId}</div> : null}
          {submitState.accessUrl ? (
            <div>
              <a className={styles.proofLink} href={submitState.accessUrl}>
                Open secure evidence access link
              </a>
              {submitState.expiresAt ? <div>Expires: {new Date(submitState.expiresAt).toLocaleString()}</div> : null}
            </div>
          ) : null}
        </div>
      ) : null}
    </form>
  );
}
