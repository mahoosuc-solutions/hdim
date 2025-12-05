import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatBadgeModule } from '@angular/material/badge';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Observable, BehaviorSubject, combineLatest, of } from 'rxjs';
import { map, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import {
  KnowledgeBaseService,
  KBArticle,
  KBCategory,
  SearchResult,
} from '../../services/knowledge-base.service';

@Component({
  selector: 'app-knowledge-base',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
    MatTabsModule,
    MatBadgeModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './knowledge-base.component.html',
  styleUrl: './knowledge-base.component.scss',
})
export class KnowledgeBaseComponent implements OnInit {
  searchQuery$ = new BehaviorSubject<string>('');
  searchResults$!: Observable<SearchResult[]>;
  categories$!: Observable<KBCategory[]>;
  recentlyViewed$!: Observable<KBArticle[]>;
  popularArticles$!: Observable<KBArticle[]>;
  recentlyUpdated$!: Observable<KBArticle[]>;

  selectedCategory: string | null = null;
  filteredArticles$!: Observable<KBArticle[]>;

  isSearching = false;

  constructor(
    private kbService: KnowledgeBaseService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Search results with debouncing
    this.searchResults$ = this.searchQuery$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      map((query) => {
        this.isSearching = query.length > 0;
        return query;
      }),
      switchMap((query) => {
        if (query.length >= 2) {
          return this.kbService.searchArticles(query);
        }
        return of([]);
      })
    );

    // Categories with article counts
    this.categories$ = this.kbService.getCategories();

    // Recently viewed articles
    this.recentlyViewed$ = this.kbService.recentlyViewed$.pipe(
      map((ids) => {
        const articles: KBArticle[] = [];
        ids.forEach((id) => {
          this.kbService.getArticle(id).subscribe((article) => {
            if (article) {
              articles.push(article);
            }
          });
        });
        return articles.slice(0, 5);
      })
    );

    // Popular articles
    this.popularArticles$ = this.kbService.getPopularArticles(5);

    // Recently updated articles
    this.recentlyUpdated$ = this.kbService.getRecentlyUpdated(5);

    // Filtered articles by category
    this.filteredArticles$ = combineLatest([
      this.kbService.getAllArticles(),
      new BehaviorSubject(this.selectedCategory),
    ]).pipe(
      map(([articles, category]) => {
        if (!category) return [];
        return articles.filter((a) => a.category === category);
      })
    );
  }

  onSearch(query: string): void {
    this.searchQuery$.next(query);
  }

  clearSearch(): void {
    this.searchQuery$.next('');
    this.isSearching = false;
  }

  viewArticle(articleId: string): void {
    this.kbService.trackView(articleId);
    this.router.navigate(['/knowledge-base', 'article', articleId]);
  }

  selectCategory(categoryId: string): void {
    this.selectedCategory = categoryId;
    this.router.navigate(['/knowledge-base', 'category', categoryId]);
  }

  getReadTimeText(minutes: number | undefined): string {
    if (!minutes) return '';
    return `${minutes} min read`;
  }

  getCategoryIcon(categoryId: string): string {
    const category = [
      {
        id: 'getting-started',
        icon: 'rocket_launch',
      },
      {
        id: 'page-guides',
        icon: 'article',
      },
      {
        id: 'domain-knowledge',
        icon: 'school',
      },
      {
        id: 'how-to',
        icon: 'list_alt',
      },
      {
        id: 'troubleshooting',
        icon: 'build',
      },
      {
        id: 'faq',
        icon: 'help',
      },
      {
        id: 'advanced',
        icon: 'science',
      },
    ].find((c) => c.id === categoryId);

    return category?.icon || 'article';
  }
}
