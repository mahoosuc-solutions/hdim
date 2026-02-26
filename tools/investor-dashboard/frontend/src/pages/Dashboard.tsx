import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from 'recharts';
import { Users, TrendingUp, CalendarCheck, Building2, Mail, Phone, Calendar, StickyNote } from 'lucide-react';
import { api, type DashboardData } from '@/lib/api';

const COLORS = ['hsl(217, 91%, 60%)', 'hsl(45, 93%, 58%)', 'hsl(142, 71%, 45%)'];

const activityIcons: Record<string, typeof Mail> = {
  email_sent: Mail,
  call: Phone,
  meeting: Calendar,
  note: StickyNote,
  status_change: TrendingUp,
};

export default function Dashboard() {
  const [data, setData] = useState<DashboardData | null>(null);

  useEffect(() => { api.dashboard().then(setData); }, []);

  if (!data) return <div className="text-muted-foreground">Loading...</div>;

  const funnelData = Object.entries(data.funnel)
    .filter(([_, v]) => v > 0 || true)
    .map(([name, value]) => ({ name, value }));

  const typeData = Object.entries(data.byType).map(([name, value]) => ({ name, value }));

  const stats = [
    { label: 'Total Contacts', value: data.stats.total, icon: Users, color: 'text-primary' },
    { label: 'Active Pipeline', value: data.stats.active, icon: TrendingUp, color: 'text-yellow-400' },
    { label: 'Meetings+', value: data.stats.meetings, icon: CalendarCheck, color: 'text-emerald-400' },
    { label: 'Partner Targets', value: data.stats.partnerships, icon: Building2, color: 'text-cyan-400' },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Pipeline Overview</h1>
        <p className="text-sm text-muted-foreground">Last updated: {new Date().toLocaleDateString()}</p>
      </div>

      <div className="grid grid-cols-4 gap-4">
        {stats.map((s) => (
          <Card key={s.label}>
            <CardContent className="flex items-center gap-4 p-5">
              <s.icon className={`h-8 w-8 ${s.color}`} />
              <div>
                <p className="text-3xl font-bold">{s.value}</p>
                <p className="text-xs text-muted-foreground">{s.label}</p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid grid-cols-5 gap-4">
        <Card className="col-span-3">
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Pipeline Funnel</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={funnelData} layout="vertical" margin={{ left: 20 }}>
                <XAxis type="number" allowDecimals={false} stroke="hsl(0, 0%, 45%)" fontSize={12} />
                <YAxis type="category" dataKey="name" width={130} stroke="hsl(0, 0%, 45%)" fontSize={11} />
                <Tooltip
                  contentStyle={{ background: 'hsl(0, 0%, 13%)', border: '1px solid hsl(0, 0%, 20%)', borderRadius: 8, fontSize: 13 }}
                />
                <Bar dataKey="value" fill="hsl(217, 91%, 60%)" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card className="col-span-2">
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Recent Activity</CardTitle>
          </CardHeader>
          <CardContent className="max-h-[340px] overflow-y-auto space-y-3">
            {data.recentActivity.length === 0 ? (
              <p className="text-muted-foreground text-sm">No activity yet. Start composing outreach!</p>
            ) : (
              data.recentActivity.map((a) => {
                const Icon = activityIcons[a.action] || StickyNote;
                return (
                  <div key={a.id} className="flex items-start gap-2">
                    <Icon className="h-4 w-4 mt-0.5 text-muted-foreground shrink-0" />
                    <div className="min-w-0">
                      <span className="font-medium text-sm">{a.contact_name || 'System'}</span>
                      <span className="text-muted-foreground text-sm ml-1">{a.action.replace('_', ' ')}</span>
                      <p className="text-xs text-muted-foreground truncate">{a.details} &middot; {a.created_at}</p>
                    </div>
                  </div>
                );
              })
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Needs Follow-up</CardTitle>
          </CardHeader>
          <CardContent>
            {data.needsFollowUp.length === 0 ? (
              <p className="text-muted-foreground text-sm">No overdue follow-ups.</p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Org</TableHead>
                    <TableHead>Next Action</TableHead>
                    <TableHead>Due</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.needsFollowUp.map((c) => (
                    <TableRow key={c.id}>
                      <TableCell><Link to={`/contacts/${c.id}`} className="text-primary hover:underline">{c.name}</Link></TableCell>
                      <TableCell className="text-muted-foreground">{c.organization}</TableCell>
                      <TableCell>{c.next_action || '-'}</TableCell>
                      <TableCell>{c.next_action_date || '-'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base">By Type</CardTitle>
          </CardHeader>
          <CardContent className="flex justify-center">
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie data={typeData} dataKey="value" nameKey="name" cx="50%" cy="50%" innerRadius={50} outerRadius={80} paddingAngle={4}>
                  {typeData.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip contentStyle={{ background: 'hsl(0, 0%, 13%)', border: '1px solid hsl(0, 0%, 20%)', borderRadius: 8, fontSize: 13 }} />
              </PieChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
