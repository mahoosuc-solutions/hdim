import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { Observable } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';
import {
  KnowledgeBaseService,
  KBArticle,
} from '../../../services/knowledge-base.service';

@Component({
  selector: 'app-article-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
  ],
  templateUrl: './article-view.component.html',
  styleUrl: './article-view.component.scss',
})
export class ArticleViewComponent implements OnInit {
  article$!: Observable<KBArticle | undefined>;
  relatedArticles$!: Observable<KBArticle[]>;
  articleId!: string;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private kbService: KnowledgeBaseService
  ) {}

  ngOnInit(): void {
    this.article$ = this.route.params.pipe(
      switchMap((params) => {
        this.articleId = params['id'];
        return this.kbService.getArticle(this.articleId);
      }),
      tap((article) => {
        if (article) {
          this.loadRelatedArticles(article.id);
        }
      })
    );
  }

  loadRelatedArticles(articleId: string): void {
    this.relatedArticles$ = this.kbService.getRelatedArticles(articleId);
  }

  viewRelatedArticle(articleId: string): void {
    this.router.navigate(['/knowledge-base', 'article', articleId]);
    // Scroll to top
    window.scrollTo(0, 0);
  }

  markHelpful(): void {
    this.kbService.markHelpful(this.articleId, true);
  }

  markNotHelpful(): void {
    this.kbService.markHelpful(this.articleId, false);
  }

  goBack(): void {
    this.router.navigate(['/knowledge-base']);
  }

  // Simple markdown to HTML converter (basic implementation)
  // In production, use a library like ngx-markdown or marked
  convertMarkdownToHtml(markdown: string): string {
    if (!markdown) return '';

    let html = markdown;

    // Headers
    html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>');
    html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>');
    html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>');

    // Bold
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // Italic
    html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');

    // Code blocks
    html = html.replace(/```(\w+)?\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>');

    // Inline code
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

    // Links
    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>');

    // Lists
    html = html.replace(/^\- (.*$)/gim, '<li>$1</li>');
    html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');

    // Line breaks
    html = html.replace(/\n\n/g, '</p><p>');
    html = '<p>' + html + '</p>';

    return html;
  }
}
