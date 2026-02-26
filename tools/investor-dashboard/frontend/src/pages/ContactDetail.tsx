import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { TypeBadge } from '@/components/StatusBadge';
import { Mail, Phone, Calendar, StickyNote, TrendingUp } from 'lucide-react';
import { api, type Contact, type Activity } from '@/lib/api';

const activityIcons: Record<string, typeof Mail> = {
  email_sent: Mail, call: Phone, meeting: Calendar, note: StickyNote, status_change: TrendingUp,
};

export default function ContactDetail() {
  const { id } = useParams();
  const [contact, setContact] = useState<Contact | null>(null);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [statuses, setStatuses] = useState<string[]>([]);
  const [status, setStatus] = useState('');
  const [nextAction, setNextAction] = useState('');
  const [nextDate, setNextDate] = useState('');
  const [actAction, setActAction] = useState('email_sent');
  const [actDetails, setActDetails] = useState('');

  const load = () => {
    if (!id) return;
    api.contact(Number(id)).then((d) => {
      setContact(d.contact);
      setActivities(d.activities);
      setStatuses(d.statuses);
      setStatus(d.contact.status);
      setNextAction(d.contact.next_action || '');
      setNextDate(d.contact.next_action_date || '');
    });
  };

  useEffect(load, [id]);

  if (!contact) return <div className="text-muted-foreground">Loading...</div>;

  const handleStatusUpdate = async () => {
    await api.updateContact(contact.id, { status, next_action: nextAction, next_action_date: nextDate });
    load();
  };

  const handleLogActivity = async () => {
    if (!actDetails.trim()) return;
    await api.logActivity(contact.id, { action: actAction, details: actDetails });
    setActDetails('');
    load();
  };

  const fields = [
    { label: 'Type', value: <TypeBadge type={contact.type} /> },
    { label: 'Tier', value: contact.tier || '-' },
    { label: 'Check Size', value: contact.check_size || '-' },
    { label: 'Intro Path', value: contact.intro_path || '-' },
    { label: 'Portfolio Fit', value: contact.portfolio_fit || '-' },
    { label: 'Role', value: contact.role || '-' },
  ];

  return (
    <div className="grid grid-cols-3 gap-6">
      <div className="col-span-2 space-y-4">
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-semibold">{contact.name}</h1>
            <p className="text-muted-foreground">{contact.organization}</p>
          </div>
          <Button asChild>
            <Link to={`/compose?contact_id=${contact.id}`}>
              <Mail className="h-4 w-4 mr-2" />Compose Email
            </Link>
          </Button>
        </div>

        <Card>
          <CardContent className="grid grid-cols-3 gap-4 p-5">
            {fields.map((f) => (
              <div key={f.label}>
                <p className="text-xs text-muted-foreground mb-1">{f.label}</p>
                <div className="text-sm">{f.value}</div>
              </div>
            ))}
          </CardContent>
          {contact.notes && (
            <CardContent className="pt-0 border-t border-border mt-2 pt-4">
              <p className="text-xs text-muted-foreground mb-1">Notes / Outreach Angle</p>
              <p className="text-sm">{contact.notes}</p>
            </CardContent>
          )}
        </Card>

        <Card>
          <CardHeader className="pb-3"><CardTitle className="text-base">Update Status</CardTitle></CardHeader>
          <CardContent>
            <div className="flex gap-2 items-end">
              <div className="flex-1">
                <label className="text-xs text-muted-foreground">Status</label>
                <Select value={status} onValueChange={setStatus}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {statuses.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
              <div className="flex-1">
                <label className="text-xs text-muted-foreground">Next Action</label>
                <Input value={nextAction} onChange={(e) => setNextAction(e.target.value)} placeholder="e.g. Send follow-up" />
              </div>
              <div className="flex-1">
                <label className="text-xs text-muted-foreground">Due Date</label>
                <Input type="date" value={nextDate} onChange={(e) => setNextDate(e.target.value)} />
              </div>
              <Button onClick={handleStatusUpdate}>Update</Button>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-3"><CardTitle className="text-base">Log Activity</CardTitle></CardHeader>
          <CardContent>
            <div className="flex gap-2 items-end">
              <div className="w-[160px]">
                <label className="text-xs text-muted-foreground">Action</label>
                <Select value={actAction} onValueChange={setActAction}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="email_sent">Email Sent</SelectItem>
                    <SelectItem value="call">Call</SelectItem>
                    <SelectItem value="meeting">Meeting</SelectItem>
                    <SelectItem value="note">Note</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="flex-1">
                <label className="text-xs text-muted-foreground">Details</label>
                <Input value={actDetails} onChange={(e) => setActDetails(e.target.value)} placeholder="Brief note..." onKeyDown={(e) => e.key === 'Enter' && handleLogActivity()} />
              </div>
              <Button variant="outline" onClick={handleLogActivity}>Log</Button>
            </div>
          </CardContent>
        </Card>
      </div>

      <div>
        <Card>
          <CardHeader className="pb-3"><CardTitle className="text-base">Activity History</CardTitle></CardHeader>
          <CardContent className="max-h-[500px] overflow-y-auto space-y-4">
            {activities.length === 0 ? (
              <p className="text-sm text-muted-foreground">No activity logged yet.</p>
            ) : (
              activities.map((a) => {
                const Icon = activityIcons[a.action] || StickyNote;
                return (
                  <div key={a.id} className="flex items-start gap-2">
                    <Icon className="h-4 w-4 mt-0.5 text-muted-foreground shrink-0" />
                    <div className="min-w-0">
                      <div className="flex justify-between text-sm">
                        <span className="font-medium">{a.action.replace('_', ' ')}</span>
                        <span className="text-xs text-muted-foreground">{a.created_at}</span>
                      </div>
                      {a.details && <p className="text-xs text-muted-foreground">{a.details}</p>}
                    </div>
                  </div>
                );
              })
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
