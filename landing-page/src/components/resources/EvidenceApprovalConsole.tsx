'use client';

import { useState } from 'react';
import styles from '@/styles/agui-portal.module.css';
import { trackEvent } from '@/lib/analytics';

type ApprovalState = {
  status: 'idle' | 'submitting' | 'approved' | 'error';
  message?: string;
  accessUrl?: string;
  expiresAt?: string;
};

export default function EvidenceApprovalConsole() {
  const [approverKey, setApproverKey] = useState('');
  const [requestId, setRequestId] = useState('');
  const [email, setEmail] = useState('');
  const [organization, setOrganization] = useState('');
  const [role, setRole] = useState('CIO/CISO');
  const [packet, setPacket] = useState('security');
  const [ttlHours, setTtlHours] = useState('72');
  const [state, setState] = useState<ApprovalState>({ status: 'idle' });

  async function handleApprove(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setState({ status: 'submitting' });

    try {
      const response = await fetch('/api/evidence-request/approve', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-evidence-approver-key': approverKey,
        },
        body: JSON.stringify({
          requestId,
          email,
          organization,
          role,
          packet,
          ttlHours: Number(ttlHours),
        }),
      });

      const payload = (await response.json()) as {
        ok: boolean;
        accessUrl?: string;
        expiresAt?: string;
        error?: string;
      };

      if (!response.ok || !payload.ok) {
        throw new Error(payload.error ?? 'Failed to approve request.');
      }

      setState({
        status: 'approved',
        message: 'Manual approval complete. Access link issued.',
        accessUrl: payload.accessUrl,
        expiresAt: payload.expiresAt,
      });
      trackEvent('evidence_request_manually_approved', { packet, role });
    } catch (error) {
      setState({ status: 'error', message: error instanceof Error ? error.message : 'Approval failed.' });
      trackEvent('evidence_request_manual_approval_failed', { packet, role });
    }
  }

  return (
    <form className={styles.requestForm} onSubmit={handleApprove}>
      <div className={styles.formGrid}>
        <label className={styles.formLabel}>
          Approver API key
          <input className={styles.formInput} type="password" value={approverKey} onChange={(event) => setApproverKey(event.target.value)} required />
        </label>
        <label className={styles.formLabel}>
          Request ID
          <input className={styles.formInput} value={requestId} onChange={(event) => setRequestId(event.target.value)} required />
        </label>
        <label className={styles.formLabel}>
          Requestor email
          <input className={styles.formInput} type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
        </label>
        <label className={styles.formLabel}>
          Organization
          <input className={styles.formInput} value={organization} onChange={(event) => setOrganization(event.target.value)} required />
        </label>
        <label className={styles.formLabel}>
          Role
          <input className={styles.formInput} value={role} onChange={(event) => setRole(event.target.value)} required />
        </label>
        <label className={styles.formLabel}>
          Packet
          <select className={styles.formInput} value={packet} onChange={(event) => setPacket(event.target.value)}>
            <option value="security">Security</option>
            <option value="reliability">Reliability</option>
            <option value="procurement">Procurement</option>
          </select>
        </label>
        <label className={styles.formLabel}>
          TTL (hours)
          <input className={styles.formInput} type="number" min={1} max={168} value={ttlHours} onChange={(event) => setTtlHours(event.target.value)} required />
        </label>
      </div>
      <div className={styles.heroActions}>
        <button type="submit" className={styles.btnPrimary} disabled={state.status === 'submitting'}>
          {state.status === 'submitting' ? 'Approving...' : 'Approve request'}
        </button>
      </div>
      {state.status !== 'idle' ? (
        <div className={`${styles.notice} ${state.status === 'error' ? styles.noticeError : styles.noticeSuccess}`}>
          {state.message}
          {state.accessUrl ? (
            <div>
              <a className={styles.proofLink} href={state.accessUrl} target="_blank" rel="noreferrer">
                Open issued access link
              </a>
              {state.expiresAt ? <div>Expires: {new Date(state.expiresAt).toLocaleString()}</div> : null}
            </div>
          ) : null}
        </div>
      ) : null}
    </form>
  );
}
