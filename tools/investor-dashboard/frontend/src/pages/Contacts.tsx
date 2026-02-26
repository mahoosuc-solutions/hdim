import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card, CardContent } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { Mail } from 'lucide-react';
import { StatusBadge, TypeBadge } from '@/components/StatusBadge';
import { api, type Contact } from '@/lib/api';

export default function Contacts() {
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [statuses, setStatuses] = useState<string[]>([]);
  const [filterType, setFilterType] = useState('all');
  const [filterTier, setFilterTier] = useState('all');
  const [filterStatus, setFilterStatus] = useState('all');

  useEffect(() => {
    api.contacts().then((d) => {
      setContacts(d.contacts);
      setStatuses(d.statuses);
    });
  }, []);

  const filtered = contacts.filter((c) => {
    if (filterType !== 'all' && c.type !== filterType) return false;
    if (filterTier !== 'all' && String(c.tier) !== filterTier) return false;
    if (filterStatus !== 'all' && c.status !== filterStatus) return false;
    return true;
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Contacts</h1>
        <div className="flex gap-2">
          <Select value={filterType} onValueChange={setFilterType}>
            <SelectTrigger className="w-[130px]"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Types</SelectItem>
              <SelectItem value="VC">VC</SelectItem>
              <SelectItem value="Angel">Angel</SelectItem>
              <SelectItem value="Strategic">Strategic</SelectItem>
            </SelectContent>
          </Select>
          <Select value={filterTier} onValueChange={setFilterTier}>
            <SelectTrigger className="w-[120px]"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Tiers</SelectItem>
              <SelectItem value="1">Tier 1</SelectItem>
              <SelectItem value="2">Tier 2</SelectItem>
              <SelectItem value="3">Tier 3</SelectItem>
            </SelectContent>
          </Select>
          <Select value={filterStatus} onValueChange={setFilterStatus}>
            <SelectTrigger className="w-[160px]"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Statuses</SelectItem>
              {statuses.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
            </SelectContent>
          </Select>
        </div>
      </div>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Organization</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Tier</TableHead>
                <TableHead>Check Size</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Last Contact</TableHead>
                <TableHead className="w-[60px]"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((c) => (
                <TableRow key={c.id}>
                  <TableCell>
                    <Link to={`/contacts/${c.id}`} className="text-primary hover:underline font-medium">{c.name}</Link>
                  </TableCell>
                  <TableCell className="text-muted-foreground">{c.organization || '-'}</TableCell>
                  <TableCell><TypeBadge type={c.type} /></TableCell>
                  <TableCell className="text-muted-foreground">{c.tier || '-'}</TableCell>
                  <TableCell className="text-muted-foreground text-sm">{c.check_size || '-'}</TableCell>
                  <TableCell><StatusBadge status={c.status} /></TableCell>
                  <TableCell className="text-muted-foreground text-sm">{c.last_contact_date || 'Never'}</TableCell>
                  <TableCell>
                    <Button variant="ghost" size="icon" asChild>
                      <Link to={`/compose?contact_id=${c.id}`}><Mail className="h-4 w-4" /></Link>
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
      <p className="text-xs text-muted-foreground">{filtered.length} of {contacts.length} contacts</p>
    </div>
  );
}
