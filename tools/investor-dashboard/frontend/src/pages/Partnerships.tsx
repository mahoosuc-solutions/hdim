import { useEffect, useState } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { api, type Partnership } from '@/lib/api';

const STATUSES = ['Research', 'Contacted', 'Meeting Scheduled', 'In Discussion', 'Agreed', 'Passed'];

export default function Partnerships() {
  const [partnerships, setPartnerships] = useState<Partnership[]>([]);

  useEffect(() => { api.partnerships().then((d) => setPartnerships(d.partnerships)); }, []);

  const categories = [...new Set(partnerships.map((p) => p.category))];

  const updateStatus = async (id: number, status: string) => {
    await api.updatePartnership(id, status);
    setPartnerships((prev) => prev.map((p) => p.id === id ? { ...p, status } : p));
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Partnership Targets</h1>
      {categories.map((cat) => (
        <div key={cat} className="space-y-2">
          <h2 className="text-lg font-medium">{cat}</h2>
          <Card>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Company</TableHead>
                    <TableHead>Product Focus</TableHead>
                    <TableHead>Customer Base</TableHead>
                    <TableHead className="max-w-[300px]">Partnership Angle</TableHead>
                    <TableHead>Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {partnerships.filter((p) => p.category === cat).map((p) => (
                    <TableRow key={p.id}>
                      <TableCell className="font-medium">{p.company}</TableCell>
                      <TableCell className="text-muted-foreground text-sm">{p.product_focus}</TableCell>
                      <TableCell className="text-muted-foreground text-sm">{p.customer_base}</TableCell>
                      <TableCell className="text-sm max-w-[300px]">{p.partnership_angle}</TableCell>
                      <TableCell>
                        <Select value={p.status} onValueChange={(v) => updateStatus(p.id, v)}>
                          <SelectTrigger className="w-[160px]"><SelectValue /></SelectTrigger>
                          <SelectContent>
                            {STATUSES.map((s) => <SelectItem key={s} value={s}>{s}</SelectItem>)}
                          </SelectContent>
                        </Select>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>
      ))}
    </div>
  );
}
