import { firstValueFrom } from 'rxjs';
import { KnowledgeBaseService, KBArticle } from './knowledge-base.service';

const createArticle = (overrides: Partial<KBArticle> = {}): KBArticle => ({
  id: 'a1',
  title: 'Dashboard Guide',
  category: 'page-guides',
  tags: ['dashboard', 'overview'],
  roles: [],
  summary: 'Summary',
  content: 'Content',
  relatedArticles: [],
  lastUpdated: new Date('2024-01-01T00:00:00Z'),
  ...overrides,
});

describe('KnowledgeBaseService', () => {
  let service: KnowledgeBaseService;

  beforeEach(() => {
    localStorage.clear();
    service = new KnowledgeBaseService();
  });

  it('filters articles by category, tag, and role', async () => {
    const articles = [
      createArticle({ id: 'a1', tags: ['dashboard'], roles: [] }),
      createArticle({ id: 'a2', category: 'faq', tags: ['patients'], roles: ['admin'] }),
    ];
    (service as any).articles.next(articles);

    const byCategory = await firstValueFrom(service.getArticlesByCategory('faq'));
    expect(byCategory.length).toBe(1);

    const byTag = await firstValueFrom(service.getArticlesByTag('patients'));
    expect(byTag.length).toBe(1);

    const byRole = await firstValueFrom(service.getArticlesByRole('admin'));
    expect(byRole.length).toBe(2);
  });

  it('searches articles and ranks results', async () => {
    const articles = [
      createArticle({ id: 'a1', title: 'Measure Builder', summary: 'Measure builder help' }),
      createArticle({ id: 'a2', title: 'Reports', summary: 'Reports overview', tags: ['reports'] }),
    ];
    (service as any).articles.next(articles);

    const results = await firstValueFrom(service.searchArticles('reports'));
    expect(results.length).toBe(1);
    expect(results[0].matchedTerms).toContain('reports');
  });

  it('returns empty results for empty search query', async () => {
    const results = await firstValueFrom(service.searchArticles('   '));
    expect(results).toEqual([]);
  });

  it('returns categories with counts', async () => {
    const articles = [
      createArticle({ id: 'a1', category: 'faq' }),
      createArticle({ id: 'a2', category: 'faq' }),
    ];
    (service as any).articles.next(articles);

    const categories = await firstValueFrom(service.getCategories());
    const faq = categories.find((category) => category.id === 'faq');
    expect(faq?.articleCount).toBe(2);
  });

  it('returns related articles', async () => {
    const articles = [
      createArticle({ id: 'a1', relatedArticles: ['a2'] }),
      createArticle({ id: 'a2', title: 'Related Guide' }),
    ];
    (service as any).articles.next(articles);

    const related = await firstValueFrom(service.getRelatedArticles('a1'));
    expect(related.length).toBe(1);
    expect(related[0].id).toBe('a2');
  });

  it('returns popular and recently updated articles', async () => {
    const older = new Date('2024-01-01T00:00:00Z');
    const newer = new Date('2024-02-01T00:00:00Z');
    const articles = [
      createArticle({ id: 'a1', views: 2, lastUpdated: older }),
      createArticle({ id: 'a2', views: 5, lastUpdated: newer }),
    ];
    (service as any).articles.next(articles);

    const popular = await firstValueFrom(service.getPopularArticles(1));
    expect(popular[0].id).toBe('a2');

    const recent = await firstValueFrom(service.getRecentlyUpdated(1));
    expect(recent[0].id).toBe('a2');
  });

  it('tracks views and recently viewed list', () => {
    const articles = [createArticle({ id: 'a1' })];
    (service as any).articles.next(articles);

    service.trackView('a1');
    const updated = (service as any).articles.value[0];
    expect(updated.views).toBe(1);
    expect((service as any).recentlyViewed.value).toContain('a1');
  });

  it('keeps recently viewed unique and limited', () => {
    const articles = [createArticle({ id: 'a1' })];
    (service as any).articles.next(articles);

    (service as any).recentlyViewed.next(['a1', 'a2', 'a3']);
    service.trackView('a1');

    const recent = (service as any).recentlyViewed.value;
    expect(recent[0]).toBe('a1');
    expect(recent.filter((id: string) => id === 'a1').length).toBe(1);
  });

  it('marks helpfulness feedback', () => {
    const articles = [createArticle({ id: 'a1' })];
    (service as any).articles.next(articles);

    service.markHelpful('a1', true);
    service.markHelpful('a1', false);

    const updated = (service as any).articles.value[0];
    expect(updated.helpful).toBe(1);
    expect(updated.notHelpful).toBe(1);
  });

  it('loads recently viewed from localStorage', () => {
    localStorage.setItem('kb_recently_viewed', JSON.stringify(['a1', 'a2']));
    const freshService = new KnowledgeBaseService();

    expect((freshService as any).recentlyViewed.value).toEqual(['a1', 'a2']);
  });

  it('returns page-specific and suggested articles', async () => {
    const articles = [
      createArticle({ id: 'a1', tags: ['reports'], category: 'troubleshooting', roles: ['admin'] }),
      createArticle({ id: 'a2', tags: ['dashboard'], category: 'page-guides', roles: [] }),
    ];
    (service as any).articles.next(articles);

    const pageArticles = await firstValueFrom(service.getArticlesForPage('reports'));
    expect(pageArticles.length).toBe(1);

    const suggestions = await firstValueFrom(
      service.getSuggestedArticles({
        currentPage: 'dashboard',
        userRole: 'admin',
        errorMessages: ['error'],
      })
    );
    expect(suggestions.length).toBeGreaterThan(0);
  });

  it('returns empty page articles for unknown page', async () => {
    const articles = [createArticle({ id: 'a1', tags: ['dashboard'] })];
    (service as any).articles.next(articles);

    const pageArticles = await firstValueFrom(service.getArticlesForPage('unknown'));
    expect(pageArticles).toEqual([]);
  });

  it('sets and gets user role', () => {
    service.setUserRole('admin');
    expect(service.getUserRole()).toBe('admin');
  });
});
