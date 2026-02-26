import { Badge } from '@/components/ui/badge';

const statusColors: Record<string, string> = {
  Research: 'bg-muted text-muted-foreground',
  'Intro Requested': 'bg-blue-500/20 text-blue-400',
  'Intro Sent': 'bg-cyan-500/20 text-cyan-400',
  'Meeting Scheduled': 'bg-emerald-500/20 text-emerald-400',
  'First Meeting': 'bg-green-500/20 text-green-400',
  'Partner Meeting': 'bg-teal-500/20 text-teal-400',
  Diligence: 'bg-purple-500/20 text-purple-400',
  'Term Sheet': 'bg-yellow-500/20 text-yellow-300',
  Passed: 'bg-destructive/20 text-destructive',
};

const typeColors: Record<string, string> = {
  VC: 'bg-primary/20 text-primary',
  Angel: 'bg-yellow-500/20 text-yellow-300',
  Strategic: 'bg-emerald-500/20 text-emerald-400',
};

export function StatusBadge({ status }: { status: string }) {
  return (
    <Badge variant="outline" className={`border-0 text-xs ${statusColors[status] || 'bg-muted text-muted-foreground'}`}>
      {status}
    </Badge>
  );
}

export function TypeBadge({ type }: { type: string }) {
  return (
    <Badge variant="outline" className={`border-0 text-xs ${typeColors[type] || 'bg-muted text-muted-foreground'}`}>
      {type}
    </Badge>
  );
}
