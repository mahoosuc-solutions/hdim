import { useEffect, useState, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Copy, Files, Check, BookCheck, Mail } from 'lucide-react';
import { api, type Contact, type Template } from '@/lib/api';

export default function Compose() {
  const [searchParams] = useSearchParams();
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [templates, setTemplates] = useState<Template[]>([]);
  const [contactId, setContactId] = useState(searchParams.get('contact_id') || '');
  const [categoryFilter, setCategoryFilter] = useState('all');
  const [templateId, setTemplateId] = useState('');
  const [placeholderValues, setPlaceholderValues] = useState<Record<string, string>>({});
  const [preview, setPreview] = useState<{ subject: string; body: string } | null>(null);
  const [copied, setCopied] = useState<string | null>(null);
  const [logged, setLogged] = useState(false);

  useEffect(() => {
    Promise.all([api.contacts(), api.templates()]).then(([c, t]) => {
      setContacts(c.contacts);
      setTemplates(t.templates);
    });
  }, []);

  const contact = contacts.find((c) => String(c.id) === contactId);
  const template = templates.find((t) => String(t.id) === templateId);
  const categories = useMemo(() => [...new Set(templates.map((t) => t.category))], [templates]);
  const filteredTemplates = categoryFilter === 'all' ? templates : templates.filter((t) => t.category === categoryFilter);

  const placeholders = useMemo(() => {
    if (!template) return [];
    try { return JSON.parse(template.placeholders || '[]') as string[]; }
    catch { return []; }
  }, [template]);

  // Auto-fill when contact or template changes
  useEffect(() => {
    if (!contact || !template) return;
    const auto: Record<string, string> = {
      Name: contact.name, name: contact.name,
      Fund: contact.organization, fund: contact.organization,
      'Partner Name': contact.name,
      organization: contact.organization,
      'Hospital Name': contact.organization, Hospital: contact.organization,
      'similar company': contact.portfolio_fit || '',
      'relevant portfolio company or experience': contact.portfolio_fit || '',
      'Your name': 'Aaron', 'Your Name': 'Aaron',
    };
    const values: Record<string, string> = {};
    for (const ph of placeholders) {
      values[ph] = auto[ph] || placeholderValues[ph] || '';
    }
    setPlaceholderValues(values);
    setPreview(null);
    setLogged(false);
  // Only re-run when contact/template selection changes
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [contactId, templateId]);

  const generatePreview = () => {
    if (!template) return;
    let subject = template.subject || '';
    let body = template.body;
    for (const [ph, val] of Object.entries(placeholderValues)) {
      const replacement = val || `[${ph}]`;
      for (const pattern of [`{{${ph}}}`, `[${ph}]`]) {
        subject = subject.split(pattern).join(replacement);
        body = body.split(pattern).join(replacement);
      }
    }
    setPreview({ subject, body });
  };

  const copyText = async (text: string, which: string) => {
    await navigator.clipboard.writeText(text);
    setCopied(which);
    setTimeout(() => setCopied(null), 2000);
  };

  const copyBoth = async () => {
    if (!preview) return;
    await navigator.clipboard.writeText(`Subject: ${preview.subject}\n\n${preview.body}`);
    setCopied('both');
    setTimeout(() => setCopied(null), 2000);
  };

  const logSent = async () => {
    if (!contact || !template || !preview) return;
    await api.logCompose(contact.id, `Template: ${template.name} | Subject: ${preview.subject}`);
    setLogged(true);
  };

  return (
    <div className="grid grid-cols-5 gap-6">
      <div className="col-span-2 space-y-4">
        <h1 className="text-2xl font-semibold">Compose Email</h1>

        <div>
          <label className="text-sm text-muted-foreground">Recipient</label>
          <Select value={contactId} onValueChange={(v) => { setContactId(v); setPreview(null); }}>
            <SelectTrigger><SelectValue placeholder="Select a contact..." /></SelectTrigger>
            <SelectContent>
              {contacts.map((c) => (
                <SelectItem key={c.id} value={String(c.id)}>
                  {c.name} — {c.organization || c.type}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-2">
          <label className="text-sm text-muted-foreground">Template</label>
          <Select value={categoryFilter} onValueChange={(v) => { setCategoryFilter(v); setTemplateId(''); }}>
            <SelectTrigger><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All categories</SelectItem>
              {categories.map((c) => <SelectItem key={c} value={c}>{c}</SelectItem>)}
            </SelectContent>
          </Select>
          <Select value={templateId} onValueChange={(v) => { setTemplateId(v); setPreview(null); }}>
            <SelectTrigger><SelectValue placeholder="Select a template..." /></SelectTrigger>
            <SelectContent>
              {filteredTemplates.map((t) => (
                <SelectItem key={t.id} value={String(t.id)}>[{t.category}] {t.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {placeholders.length > 0 && (
          <div className="space-y-2">
            {placeholders.map((ph) => (
              <div key={ph}>
                <label className="text-xs text-muted-foreground">
                  {ph} {placeholderValues[ph] ? <span className="text-success">(auto-filled)</span> : null}
                </label>
                <Input
                  value={placeholderValues[ph] || ''}
                  onChange={(e) => setPlaceholderValues((prev) => ({ ...prev, [ph]: e.target.value }))}
                />
              </div>
            ))}
          </div>
        )}

        <Button className="w-full" onClick={generatePreview} disabled={!templateId}>
          <Mail className="h-4 w-4 mr-2" />Generate Preview
        </Button>
      </div>

      <div className="col-span-3">
        {preview ? (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">Email Preview</h2>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" onClick={() => copyText(preview.subject, 'subject')}>
                  {copied === 'subject' ? <Check className="h-4 w-4 mr-1" /> : <Copy className="h-4 w-4 mr-1" />}
                  {copied === 'subject' ? 'Copied!' : 'Copy Subject'}
                </Button>
                <Button variant="outline" size="sm" onClick={() => copyText(preview.body, 'body')}>
                  {copied === 'body' ? <Check className="h-4 w-4 mr-1" /> : <Copy className="h-4 w-4 mr-1" />}
                  {copied === 'body' ? 'Copied!' : 'Copy Body'}
                </Button>
                <Button size="sm" onClick={copyBoth}>
                  {copied === 'both' ? <Check className="h-4 w-4 mr-1" /> : <Files className="h-4 w-4 mr-1" />}
                  {copied === 'both' ? 'Copied!' : 'Copy Both'}
                </Button>
              </div>
            </div>

            <Card>
              <CardContent className="py-3">
                <p className="text-xs text-muted-foreground">Subject:</p>
                <p className="font-semibold">{preview.subject}</p>
              </CardContent>
            </Card>

            <div className="rounded-lg border border-border bg-card p-5 whitespace-pre-wrap text-sm leading-relaxed">
              {preview.body}
            </div>

            <Button variant="secondary" size="sm" onClick={logSent} disabled={!contactId || logged}>
              <BookCheck className="h-4 w-4 mr-2" />
              {logged ? 'Logged!' : 'Log as Email Sent'}
            </Button>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-muted-foreground gap-3 pt-20">
            <Mail className="h-12 w-12" />
            <p>Select a contact and template to preview your email</p>
          </div>
        )}
      </div>
    </div>
  );
}
