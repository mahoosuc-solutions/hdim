/**
 * Knowledge Base Service
 *
 * Manages knowledge base articles, search, and content organization
 * Provides contextual help and domain knowledge to users
 */

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, map } from 'rxjs';
import { KB_ARTICLES } from '../data/knowledge-base-articles';

export type UserRole = 'clinician' | 'admin' | 'quality-manager' | 'analyst';

export type ArticleCategory =
  | 'getting-started'
  | 'page-guides'
  | 'domain-knowledge'
  | 'how-to'
  | 'troubleshooting'
  | 'faq'
  | 'advanced';

export interface KBArticle {
  id: string;
  title: string;
  category: ArticleCategory;
  tags: string[];
  roles: UserRole[]; // Empty array = all roles
  summary: string;
  content: string; // Markdown format
  relatedArticles: string[]; // Article IDs
  lastUpdated: Date;
  views?: number;
  helpful?: number;
  notHelpful?: number;
  codeExamples?: CodeExample[];
  videoUrl?: string;
  estimatedReadTime?: number; // minutes
}

export interface CodeExample {
  title: string;
  language: 'typescript' | 'cql' | 'json' | 'sql' | 'html';
  code: string;
  description?: string;
}

export interface KBCategory {
  id: ArticleCategory;
  title: string;
  description: string;
  icon: string;
  articleCount: number;
}

export interface SearchResult {
  article: KBArticle;
  relevanceScore: number;
  matchedTerms: string[];
}

@Injectable({
  providedIn: 'root',
})
export class KnowledgeBaseService {
  private articles = new BehaviorSubject<KBArticle[]>(KB_ARTICLES);
  public articles$ = this.articles.asObservable();

  private recentlyViewed = new BehaviorSubject<string[]>([]);
  public recentlyViewed$ = this.recentlyViewed.asObservable();

  private currentUserRole: UserRole = 'clinician';

  constructor() {
    this.loadRecentlyViewed();
  }

  /**
   * Get all articles
   */
  getAllArticles(): Observable<KBArticle[]> {
    return this.articles$;
  }

  /**
   * Get article by ID
   */
  getArticle(id: string): Observable<KBArticle | undefined> {
    return this.articles$.pipe(
      map((articles) => articles.find((a) => a.id === id))
    );
  }

  /**
   * Get articles by category
   */
  getArticlesByCategory(category: ArticleCategory): Observable<KBArticle[]> {
    return this.articles$.pipe(
      map((articles) => articles.filter((a) => a.category === category))
    );
  }

  /**
   * Get articles by tag
   */
  getArticlesByTag(tag: string): Observable<KBArticle[]> {
    return this.articles$.pipe(
      map((articles) =>
        articles.filter((a) => a.tags.includes(tag.toLowerCase()))
      )
    );
  }

  /**
   * Get articles accessible by role
   */
  getArticlesByRole(role: UserRole): Observable<KBArticle[]> {
    return this.articles$.pipe(
      map((articles) =>
        articles.filter(
          (a) => a.roles.length === 0 || a.roles.includes(role)
        )
      )
    );
  }

  /**
   * Get related articles for a given article
   */
  getRelatedArticles(articleId: string): Observable<KBArticle[]> {
    return this.articles$.pipe(
      map((articles) => {
        const article = articles.find((a) => a.id === articleId);
        if (!article) return [];

        return articles.filter((a) =>
          article.relatedArticles.includes(a.id)
        );
      })
    );
  }

  /**
   * Full-text search with relevance scoring
   */
  searchArticles(query: string): Observable<SearchResult[]> {
    const searchTerms = query.toLowerCase().split(/\s+/).filter(Boolean);

    if (searchTerms.length === 0) {
      return this.articles$.pipe(map(() => []));
    }

    return this.articles$.pipe(
      map((articles) => {
        const results: SearchResult[] = [];

        for (const article of articles) {
          const searchableText = [
            article.title,
            article.summary,
            article.content,
            ...article.tags,
          ]
            .join(' ')
            .toLowerCase();

          let relevanceScore = 0;
          const matchedTerms: string[] = [];

          for (const term of searchTerms) {
            // Title match (highest weight)
            if (article.title.toLowerCase().includes(term)) {
              relevanceScore += 10;
              matchedTerms.push(term);
            }

            // Tag match (high weight)
            if (article.tags.some((tag) => tag.includes(term))) {
              relevanceScore += 7;
              if (!matchedTerms.includes(term)) matchedTerms.push(term);
            }

            // Summary match (medium weight)
            if (article.summary.toLowerCase().includes(term)) {
              relevanceScore += 5;
              if (!matchedTerms.includes(term)) matchedTerms.push(term);
            }

            // Content match (low weight)
            if (article.content.toLowerCase().includes(term)) {
              relevanceScore += 2;
              if (!matchedTerms.includes(term)) matchedTerms.push(term);
            }
          }

          // Add result if any matches found
          if (relevanceScore > 0) {
            results.push({
              article,
              relevanceScore,
              matchedTerms,
            });
          }
        }

        // Sort by relevance score (descending)
        return results.sort((a, b) => b.relevanceScore - a.relevanceScore);
      })
    );
  }

  /**
   * Get all categories with article counts
   */
  getCategories(): Observable<KBCategory[]> {
    return this.articles$.pipe(
      map((articles) => {
        const categories: KBCategory[] = [
          {
            id: 'getting-started',
            title: 'Getting Started',
            description: 'Introduction to the Clinical Portal',
            icon: 'rocket_launch',
            articleCount: 0,
          },
          {
            id: 'page-guides',
            title: 'Page Guides',
            description: 'Detailed guides for each page',
            icon: 'article',
            articleCount: 0,
          },
          {
            id: 'domain-knowledge',
            title: 'Domain Knowledge',
            description: 'Healthcare standards and concepts',
            icon: 'school',
            articleCount: 0,
          },
          {
            id: 'how-to',
            title: 'How-To Guides',
            description: 'Step-by-step task instructions',
            icon: 'list_alt',
            articleCount: 0,
          },
          {
            id: 'troubleshooting',
            title: 'Troubleshooting',
            description: 'Common issues and solutions',
            icon: 'build',
            articleCount: 0,
          },
          {
            id: 'faq',
            title: 'FAQ',
            description: 'Frequently asked questions',
            icon: 'help',
            articleCount: 0,
          },
          {
            id: 'advanced',
            title: 'Advanced Topics',
            description: 'Advanced features and customization',
            icon: 'science',
            articleCount: 0,
          },
        ];

        // Count articles per category
        for (const article of articles) {
          const category = categories.find((c) => c.id === article.category);
          if (category) {
            category.articleCount++;
          }
        }

        return categories;
      })
    );
  }

  /**
   * Get popular/most viewed articles
   */
  getPopularArticles(limit = 5): Observable<KBArticle[]> {
    return this.articles$.pipe(
      map((articles) =>
        [...articles]
          .filter((a) => a.views && a.views > 0)
          .sort((a, b) => (b.views || 0) - (a.views || 0))
          .slice(0, limit)
      )
    );
  }

  /**
   * Get recently updated articles
   */
  getRecentlyUpdated(limit = 5): Observable<KBArticle[]> {
    return this.articles$.pipe(
      map((articles) =>
        [...articles]
          .sort(
            (a, b) => b.lastUpdated.getTime() - a.lastUpdated.getTime()
          )
          .slice(0, limit)
      )
    );
  }

  /**
   * Track article view
   */
  trackView(articleId: string): void {
    const currentArticles = this.articles.value;
    const updatedArticles = currentArticles.map((a) => {
      if (a.id === articleId) {
        return { ...a, views: (a.views || 0) + 1 };
      }
      return a;
    });

    this.articles.next(updatedArticles);

    // Update recently viewed
    const recent = this.recentlyViewed.value;
    const updated = [
      articleId,
      ...recent.filter((id) => id !== articleId),
    ].slice(0, 10);
    this.recentlyViewed.next(updated);
    this.saveRecentlyViewed(updated);
  }

  /**
   * Record helpfulness feedback
   */
  markHelpful(articleId: string, helpful: boolean): void {
    const currentArticles = this.articles.value;
    const updatedArticles = currentArticles.map((a) => {
      if (a.id === articleId) {
        if (helpful) {
          return { ...a, helpful: (a.helpful || 0) + 1 };
        } else {
          return { ...a, notHelpful: (a.notHelpful || 0) + 1 };
        }
      }
      return a;
    });

    this.articles.next(updatedArticles);
  }

  /**
   * Get context-sensitive articles for a page
   */
  getArticlesForPage(pageName: string): Observable<KBArticle[]> {
    const pageTagMap: Record<string, string[]> = {
      dashboard: ['dashboard', 'overview', 'statistics'],
      patients: ['patients', 'search', 'mpi', 'demographics'],
      'patient-detail': ['patient-detail', 'clinical-data', 'history'],
      evaluations: ['evaluations', 'quality-measures', 'cql'],
      results: ['results', 'evaluation-results', 'analytics'],
      reports: ['reports', 'patient-report', 'population-report'],
      'measure-builder': ['measure-builder', 'cql', 'custom-measures'],
      visualization: ['visualization', 'real-time', 'monitoring'],
      'ai-assistant': ['ai-assistant', 'analysis', 'recommendations'],
    };

    const tags = pageTagMap[pageName.toLowerCase()] || [];

    return this.articles$.pipe(
      map((articles) =>
        articles.filter((a) => tags.some((tag) => a.tags.includes(tag)))
      )
    );
  }

  /**
   * Set current user role
   */
  setUserRole(role: UserRole): void {
    this.currentUserRole = role;
  }

  /**
   * Get current user role
   */
  getUserRole(): UserRole {
    return this.currentUserRole;
  }

  /**
   * Load recently viewed from localStorage
   */
  private loadRecentlyViewed(): void {
    try {
      const stored = localStorage.getItem('kb_recently_viewed');
      if (stored) {
        this.recentlyViewed.next(JSON.parse(stored));
      }
    } catch (e) {
      // Ignore errors
    }
  }

  /**
   * Save recently viewed to localStorage
   */
  private saveRecentlyViewed(ids: string[]): void {
    try {
      localStorage.setItem('kb_recently_viewed', JSON.stringify(ids));
    } catch (e) {
      // Ignore errors
    }
  }

  /**
   * Get suggested articles based on current context
   */
  getSuggestedArticles(
    context: {
      currentPage?: string;
      userRole?: UserRole;
      recentActions?: string[];
      errorMessages?: string[];
    }
  ): Observable<KBArticle[]> {
    return this.articles$.pipe(
      map((articles) => {
        const suggestions = new Set<KBArticle>();

        // Add articles for current page
        if (context.currentPage) {
          const pageArticles = articles.filter((a) =>
            a.tags.includes(context.currentPage!.toLowerCase())
          );
          pageArticles.forEach((a) => suggestions.add(a));
        }

        // Add role-specific articles
        if (context.userRole) {
          const roleArticles = articles.filter(
            (a) =>
              a.roles.length > 0 && a.roles.includes(context.userRole!)
          );
          roleArticles.forEach((a) => suggestions.add(a));
        }

        // Add troubleshooting articles if errors present
        if (context.errorMessages && context.errorMessages.length > 0) {
          const troubleshootingArticles = articles.filter(
            (a) => a.category === 'troubleshooting'
          );
          troubleshootingArticles.forEach((a) => suggestions.add(a));
        }

        return Array.from(suggestions).slice(0, 5);
      })
    );
  }
}
