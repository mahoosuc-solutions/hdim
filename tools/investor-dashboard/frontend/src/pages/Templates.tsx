import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Copy, Check } from 'lucide-react';
import { api, type Template } from '@/lib/api';

export default function Templates() {
  const [templates, setTemplates] = useState<Template[]>([]);
  const [copied, setCopied] = useState<number | null>(null);

  useEffect(() => { api.templates().then((d) => setTemplates(d.templates)); }, []);

  const categories = [...new Set(templates.map((t) => t.category))];

  const copyBody = async (t: Template) => {
    await navigator.clipboard.writeText(t.body);
    setCopied(t.id);
    setTimeout(() => setCopied(null), 2000);
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Email Templates</h1>
      {categories.map((cat) => (
        <div key={cat} className="space-y-3">
          <h2 className="text-lg font-medium border-b border-border pb-2">{cat}</h2>
          {templates.filter((t) => t.category === cat).map((t) => {
            const placeholders: string[] = (() => { try { return JSON.parse(t.placeholders || '[]'); } catch { return []; } })();
            return (
              <Card key={t.id}>
                <CardHeader className="pb-2 flex flex-row items-center justify-between">
                  <CardTitle className="text-base">{t.name}</CardTitle>
                  <Button variant="outline" size="sm" onClick={() => copyBody(t)}>
                    {copied === t.id ? <Check className="h-4 w-4 mr-1" /> : <Copy className="h-4 w-4 mr-1" />}
                    {copied === t.id ? 'Copied!' : 'Copy'}
                  </Button>
                </CardHeader>
                <CardContent className="space-y-2">
                  {t.subject && <p className="text-sm"><span className="text-muted-foreground">Subject:</span> {t.subject}</p>}
                  {placeholders.length > 0 && (
                    <div className="flex gap-1 flex-wrap">
                      {placeholders.map((p) => (
                        <Badge key={p} variant="secondary" className="text-xs">[{p}]</Badge>
                      ))}
                    </div>
                  )}
                  <pre className="rounded-lg border border-border bg-background p-4 text-sm leading-relaxed whitespace-pre-wrap font-sans">
                    {t.body}
                  </pre>
                </CardContent>
              </Card>
            );
          })}
        </div>
      ))}
    </div>
  );
}
