# Documentation Portal Technical Architecture

**Document Version**: 1.0
**Date**: December 1, 2025
**Owner**: Agent 1 - Documentation Architect
**Status**: Final Specification

---

## Purpose

This document provides the **complete technical architecture** for the three documentation portals. It defines:
- System architecture and components
- Frontend and backend design
- Data flow and APIs
- Search infrastructure
- Analytics and feedback systems
- Deployment and CI/CD

This specification enables Agent 5 (Portal Integration Engineer) to build the portal infrastructure.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Component Specifications](#component-specifications)
3. [Data Flow](#data-flow)
4. [API Specifications](#api-specifications)
5. [Search Architecture](#search-architecture)
6. [Deployment Architecture](#deployment-architecture)

---

## Architecture Overview

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER LAYER                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Product    │  │     User     │  │    Sales     │          │
│  │    Portal    │  │    Portal    │  │   Portal     │          │
│  │  (Public)    │  │ (Internal)   │  │(Restricted)  │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
└─────────┼──────────────────┼──────────────────┼─────────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                      FRONTEND LAYER                             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Angular Application (SPA)                   │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │ Components:                                              │   │
│  │  • Portal Navigation (3 portals)                        │   │
│  │  • Search Component (unified)                           │   │
│  │  • Document Viewer Component                            │   │
│  │  • Feedback Component                                   │   │
│  │  • Analytics Tracking Component                         │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │ REST APIs
┌────────────────────────────▼────────────────────────────────────┐
│                      API GATEWAY LAYER                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Spring Cloud Gateway (Existing)                         │   │
│  │  • Routing                                               │   │
│  │  • Authentication/Authorization                          │   │
│  │  • Rate Limiting                                         │   │
│  │  • CORS Configuration                                    │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                ┌────────────┼────────────┐
                │            │            │
┌───────────────▼──┐  ┌──────▼──────┐  ┌─▼──────────────┐
│  Documentation   │  │   Search    │  │   Analytics    │
│     Service      │  │  Service    │  │   Service      │
│  (Spring Boot)   │  │(Elastic)    │  │ (Spring Boot)  │
└───────────┬──────┘  └──────┬──────┘  └─┬──────────────┘
            │                │            │
            │         ┌──────▼──────┐     │
            │         │ Elasticsearch│    │
            │         └─────────────┘     │
            │                             │
┌───────────▼─────────────────────────────▼──────────────┐
│                   DATA LAYER                           │
│  ┌───────────┐  ┌───────────┐  ┌──────────────────┐   │
│  │PostgreSQL │  │   Redis   │  │  Git Repository  │   │
│  │(Metadata) │  │  (Cache)  │  │  (Markdown Docs) │   │
│  └───────────┘  └───────────┘  └──────────────────┘   │
└────────────────────────────────────────────────────────┘
```

### Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Frontend** | Angular 17+ | SPA framework (existing) |
| **UI Components** | Angular Material | Component library |
| **Backend Services** | Spring Boot 3.2+ | Microservices (existing) |
| **API Gateway** | Spring Cloud Gateway | Routing, auth (existing) |
| **Database** | PostgreSQL 14+ | Metadata storage (existing) |
| **Search** | Elasticsearch 8.x | Full-text search |
| **Cache** | Redis 7.x | Performance optimization |
| **Version Control** | Git | Document storage |
| **CI/CD** | GitHub Actions | Automation |
| **Analytics** | Google Analytics 4 + Custom | Usage tracking |

---

## Component Specifications

### Frontend Components

#### 1. Portal Navigation Component

**Location**: `apps/clinical-portal/src/app/components/portal-navigation/`

**Purpose**: Provide navigation for each of the 3 portals

**Interface**:
```typescript
interface PortalNavigationConfig {
  portalType: 'product' | 'user' | 'sales';
  categories: Category[];
  breadcrumbs: Breadcrumb[];
}

interface Category {
  id: string;
  label: string;
  icon?: string;
  subcategories?: Subcategory[];
  documents?: DocumentLink[];
}

interface DocumentLink {
  id: string;
  title: string;
  path: string;
  isNew?: boolean;
  hasVideo?: boolean;
}
```

**Features**:
- Collapsible category sections
- Active state highlighting
- Search integration
- Mobile responsive
- Keyboard navigation
- Role-based visibility (for sales portal)

**Implementation**:
```typescript
@Component({
  selector: 'app-portal-navigation',
  templateUrl: './portal-navigation.component.html',
  styleUrls: ['./portal-navigation.component.scss']
})
export class PortalNavigationComponent implements OnInit {
  @Input() portalType: PortalType;
  @Output() documentSelected = new EventEmitter<string>();

  categories: Category[];
  expandedCategories: Set<string> = new Set();

  constructor(
    private documentService: DocumentService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadNavigation();
  }

  loadNavigation() {
    this.documentService.getNavigation(this.portalType)
      .subscribe(categories => {
        this.categories = categories;
      });
  }

  toggleCategory(categoryId: string) {
    if (this.expandedCategories.has(categoryId)) {
      this.expandedCategories.delete(categoryId);
    } else {
      this.expandedCategories.add(categoryId);
    }
  }

  navigateToDocument(path: string) {
    this.router.navigate(['/docs', path]);
    this.documentSelected.emit(path);
  }
}
```

---

#### 2. Search Component

**Location**: `apps/clinical-portal/src/app/components/document-search/`

**Purpose**: Unified search across all documentation

**Interface**:
```typescript
interface SearchQuery {
  query: string;
  portalType?: PortalType;
  filters?: SearchFilters;
  page?: number;
  pageSize?: number;
}

interface SearchFilters {
  category?: string[];
  difficulty?: string[];
  targetAudience?: string[];
  hasVideo?: boolean;
  lastUpdated?: DateRange;
}

interface SearchResult {
  documents: DocumentSearchResult[];
  totalCount: number;
  facets: Facet[];
  suggestions?: string[];
}

interface DocumentSearchResult {
  id: string;
  title: string;
  summary: string;
  path: string;
  portalType: PortalType;
  category: string;
  highlights?: string[];
  score: number;
}
```

**Features**:
- Real-time search (debounced 300ms)
- Autocomplete suggestions
- Faceted filtering
- Result highlighting
- "Did you mean?" suggestions
- Search history
- Keyboard shortcuts (Cmd+K / Ctrl+K)

**Implementation**:
```typescript
@Component({
  selector: 'app-document-search',
  templateUrl: './document-search.component.html',
  styleUrls: ['./document-search.component.scss']
})
export class DocumentSearchComponent implements OnInit {
  searchForm: FormGroup;
  searchResults$: Observable<SearchResult>;
  suggestions$: Observable<string[]>;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private searchService: SearchService
  ) {
    this.searchForm = this.fb.group({
      query: [''],
      filters: this.fb.group({
        category: [[]],
        difficulty: [[]],
        targetAudience: [[]],
        hasVideo: [null]
      })
    });
  }

  ngOnInit() {
    // Autocomplete
    this.suggestions$ = this.searchForm.get('query').valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      filter(query => query.length >= 3),
      switchMap(query => this.searchService.getAutocomplete(query))
    );

    // Search
    this.searchResults$ = this.searchForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      tap(() => this.loading = true),
      switchMap(formValue => this.searchService.search(formValue)),
      tap(() => this.loading = false)
    );
  }

  selectSuggestion(suggestion: string) {
    this.searchForm.patchValue({ query: suggestion });
  }
}
```

---

#### 3. Document Viewer Component

**Location**: `apps/clinical-portal/src/app/components/document-viewer/`

**Purpose**: Render markdown documents with rich features

**Features**:
- Markdown rendering (marked.js)
- Syntax highlighting (highlight.js)
- Table of contents generation
- Anchor link navigation
- Copy code button
- Print-friendly view
- Mobile responsive
- Accessibility (ARIA labels, keyboard nav)

**Implementation**:
```typescript
@Component({
  selector: 'app-document-viewer',
  templateUrl: './document-viewer.component.html',
  styleUrls: ['./document-viewer.component.scss']
})
export class DocumentViewerComponent implements OnInit {
  @Input() documentId: string;

  document: DocumentDetail;
  renderedContent: SafeHtml;
  tableOfContents: TocItem[];
  loading = true;

  constructor(
    private documentService: DocumentService,
    private sanitizer: DomSanitizer,
    private analyticsService: AnalyticsService
  ) {}

  ngOnInit() {
    this.loadDocument();
  }

  loadDocument() {
    this.documentService.getDocument(this.documentId)
      .subscribe(doc => {
        this.document = doc;
        this.renderedContent = this.renderMarkdown(doc.content);
        this.tableOfContents = this.generateToc(doc.content);
        this.loading = false;

        // Track view
        this.analyticsService.trackDocumentView(doc.id);
      });
  }

  renderMarkdown(markdown: string): SafeHtml {
    const renderer = new marked.Renderer();

    // Custom rendering for code blocks
    renderer.code = (code, language) => {
      const highlighted = hljs.highlight(code, { language }).value;
      return `
        <div class="code-block">
          <button class="copy-btn" onclick="copyCode(this)">Copy</button>
          <pre><code class="language-${language}">${highlighted}</code></pre>
        </div>
      `;
    };

    const html = marked(markdown, { renderer });
    return this.sanitizer.sanitize(SecurityContext.HTML, html);
  }

  generateToc(markdown: string): TocItem[] {
    // Extract headings and build TOC
    const headings = markdown.match(/^#{1,6}\s.+$/gm) || [];
    return headings.map(h => {
      const level = h.match(/^#+/)[0].length;
      const text = h.replace(/^#+\s/, '');
      const id = this.slugify(text);
      return { level, text, id };
    });
  }

  slugify(text: string): string {
    return text.toLowerCase()
      .replace(/[^\w\s-]/g, '')
      .replace(/\s+/g, '-');
  }
}
```

---

#### 4. Feedback Component

**Location**: `apps/clinical-portal/src/app/components/document-feedback/`

**Purpose**: Collect user feedback on documents

**Interface**:
```typescript
interface DocumentFeedback {
  documentId: string;
  rating: number; // 1-5
  helpful: boolean;
  comment?: string;
  userId?: string;
}
```

**Features**:
- Star rating (1-5)
- Thumbs up/down
- Optional comment field
- "Was this helpful?" prompt
- Thank you message
- Anonymous or authenticated

**Implementation**:
```typescript
@Component({
  selector: 'app-document-feedback',
  templateUrl: './document-feedback.component.html',
  styleUrls: ['./document-feedback.component.scss']
})
export class DocumentFeedbackComponent {
  @Input() documentId: string;

  rating: number = 0;
  helpful: boolean | null = null;
  comment: string = '';
  submitted = false;

  constructor(private feedbackService: FeedbackService) {}

  setRating(stars: number) {
    this.rating = stars;
  }

  setHelpful(isHelpful: boolean) {
    this.helpful = isHelpful;
  }

  submitFeedback() {
    const feedback: DocumentFeedback = {
      documentId: this.documentId,
      rating: this.rating,
      helpful: this.helpful,
      comment: this.comment
    };

    this.feedbackService.submitFeedback(feedback)
      .subscribe(() => {
        this.submitted = true;
        setTimeout(() => this.submitted = false, 3000);
      });
  }
}
```

---

### Backend Services

#### 1. Documentation Service

**Technology**: Spring Boot
**Base URL**: `/api/v1/documentation`

**Responsibilities**:
- Retrieve document metadata
- Serve markdown content
- Manage navigation hierarchies
- Track document views
- Handle document updates

**Endpoints**:

```java
@RestController
@RequestMapping("/api/v1/documentation")
public class DocumentationController {

    @Autowired
    private DocumentationService documentationService;

    // Get navigation for portal
    @GetMapping("/navigation/{portalType}")
    public ResponseEntity<NavigationResponse> getNavigation(
        @PathVariable PortalType portalType
    ) {
        return ResponseEntity.ok(
            documentationService.getNavigation(portalType)
        );
    }

    // Get document by ID
    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentDetail> getDocument(
        @PathVariable String id
    ) {
        DocumentDetail doc = documentationService.getDocument(id);
        documentationService.incrementViewCount(id);
        return ResponseEntity.ok(doc);
    }

    // Get document by path
    @GetMapping("/documents/by-path")
    public ResponseEntity<DocumentDetail> getDocumentByPath(
        @RequestParam String path
    ) {
        return ResponseEntity.ok(
            documentationService.getDocumentByPath(path)
        );
    }

    // Get related documents
    @GetMapping("/documents/{id}/related")
    public ResponseEntity<List<DocumentSummary>> getRelatedDocuments(
        @PathVariable String id
    ) {
        return ResponseEntity.ok(
            documentationService.getRelatedDocuments(id)
        );
    }

    // Get documents by category
    @GetMapping("/categories/{category}/documents")
    public ResponseEntity<List<DocumentSummary>> getDocumentsByCategory(
        @PathVariable String category,
        @RequestParam(required = false) PortalType portalType
    ) {
        return ResponseEntity.ok(
            documentationService.getDocumentsByCategory(category, portalType)
        );
    }
}
```

---

#### 2. Search Service

**Technology**: Spring Boot + Elasticsearch
**Base URL**: `/api/v1/search`

**Responsibilities**:
- Full-text search across documents
- Autocomplete suggestions
- Faceted search
- Search analytics tracking
- Query normalization and synonym expansion

**Endpoints**:

```java
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    // Main search endpoint
    @PostMapping("/documents")
    public ResponseEntity<SearchResponse> search(
        @RequestBody SearchRequest request
    ) {
        SearchResponse response = searchService.search(request);
        searchService.trackSearch(request.getQuery());
        return ResponseEntity.ok(response);
    }

    // Autocomplete suggestions
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(
        @RequestParam String query,
        @RequestParam(required = false) PortalType portalType
    ) {
        return ResponseEntity.ok(
            searchService.getAutocomplete(query, portalType)
        );
    }

    // Get available facets
    @GetMapping("/facets")
    public ResponseEntity<FacetsResponse> getFacets(
        @RequestParam(required = false) PortalType portalType
    ) {
        return ResponseEntity.ok(
            searchService.getFacets(portalType)
        );
    }

    // Popular searches
    @GetMapping("/popular")
    public ResponseEntity<List<PopularSearch>> getPopularSearches(
        @RequestParam(required = false) PortalType portalType,
        @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(
            searchService.getPopularSearches(portalType, limit)
        );
    }
}
```

**Search Service Implementation**:

```java
@Service
public class SearchService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private SynonymMapper synonymMapper;

    public SearchResponse search(SearchRequest request) {
        // Normalize and expand query
        String expandedQuery = synonymMapper.expand(request.getQuery());

        // Build Elasticsearch query
        SearchRequest esRequest = SearchRequest.of(s -> s
            .index("documentation")
            .query(q -> q
                .multiMatch(m -> m
                    .query(expandedQuery)
                    .fields("title^10", "summary^5", "tags^7", "content^2")
                    .fuzziness("AUTO")
                )
            )
            .highlight(h -> h
                .fields("title", f -> f)
                .fields("summary", f -> f)
                .fields("content", f -> f.fragmentSize(150).numberOfFragments(3))
            )
            .aggregations("category", a -> a.terms(t -> t.field("category")))
            .aggregations("difficulty", a -> a.terms(t -> t.field("difficulty")))
            .aggregations("targetAudience", a -> a.terms(t -> t.field("targetAudience")))
            .from(request.getPage() * request.getPageSize())
            .size(request.getPageSize())
        );

        // Execute search
        SearchResponse<DocumentSearchResult> esResponse =
            elasticsearchClient.search(esRequest, DocumentSearchResult.class);

        // Map to response
        return mapToSearchResponse(esResponse);
    }

    // ... other methods
}
```

---

#### 3. Analytics Service

**Technology**: Spring Boot + PostgreSQL
**Base URL**: `/api/v1/analytics`

**Responsibilities**:
- Track document views
- Track search queries
- Track user feedback
- Generate analytics reports
- Provide metrics dashboards

**Endpoints**:

```java
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    // Track document view
    @PostMapping("/views")
    public ResponseEntity<Void> trackView(
        @RequestBody ViewEvent event
    ) {
        analyticsService.trackView(event);
        return ResponseEntity.ok().build();
    }

    // Track search
    @PostMapping("/searches")
    public ResponseEntity<Void> trackSearch(
        @RequestBody SearchEvent event
    ) {
        analyticsService.trackSearch(event);
        return ResponseEntity.ok().build();
    }

    // Submit feedback
    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(
        @RequestBody FeedbackEvent event
    ) {
        analyticsService.submitFeedback(event);
        return ResponseEntity.ok().build();
    }

    // Get document metrics
    @GetMapping("/documents/{id}/metrics")
    public ResponseEntity<DocumentMetrics> getDocumentMetrics(
        @PathVariable String id
    ) {
        return ResponseEntity.ok(
            analyticsService.getDocumentMetrics(id)
        );
    }

    // Get portal metrics
    @GetMapping("/portals/{portalType}/metrics")
    public ResponseEntity<PortalMetrics> getPortalMetrics(
        @PathVariable PortalType portalType,
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(
            analyticsService.getPortalMetrics(portalType, startDate, endDate)
        );
    }
}
```

---

## Data Flow

### Flow 1: Document Retrieval

```
1. User navigates to document
   ↓
2. Frontend requests document by ID/path
   GET /api/v1/documentation/documents/{id}
   ↓
3. Documentation Service:
   a. Fetch metadata from PostgreSQL
   b. Fetch content from Git repository (or cache)
   c. Track view in analytics
   ↓
4. Return document + metadata to frontend
   ↓
5. Frontend renders markdown
   ↓
6. Frontend loads related documents
   GET /api/v1/documentation/documents/{id}/related
```

**Performance Optimization**:
- Cache frequently accessed documents in Redis (TTL: 1 hour)
- Lazy load related documents
- Progressive rendering for long documents

---

### Flow 2: Search

```
1. User types query in search box
   ↓
2. After 300ms debounce, frontend sends autocomplete request
   GET /api/v1/search/autocomplete?query={query}
   ↓
3. Search Service returns suggestions
   ↓
4. User selects suggestion or presses Enter
   ↓
5. Frontend sends full search request
   POST /api/v1/search/documents
   Body: { query, filters, page, pageSize }
   ↓
6. Search Service:
   a. Normalize query
   b. Expand synonyms
   c. Query Elasticsearch
   d. Calculate facets
   e. Track search
   ↓
7. Return search results + facets
   ↓
8. Frontend displays results
   ↓
9. User clicks result
   ↓
10. Track click-through in analytics
```

---

### Flow 3: Feedback Submission

```
1. User rates document or provides feedback
   ↓
2. Frontend sends feedback
   POST /api/v1/analytics/feedback
   Body: { documentId, rating, helpful, comment }
   ↓
3. Analytics Service:
   a. Store feedback in PostgreSQL
   b. Update document avg_rating
   c. Notify document owner (if comment)
   ↓
4. Return success
   ↓
5. Frontend shows thank you message
```

---

## API Specifications

### REST API Standards

**Base URL**: `https://api.healthdata-in-motion.com/v1`

**Authentication**: JWT Bearer Token (for internal/restricted portals)

**Request Headers**:
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
Accept: application/json
```

**Response Format**:
```json
{
  "data": { ... },
  "meta": {
    "timestamp": "2025-12-01T10:00:00Z",
    "requestId": "uuid"
  }
}
```

**Error Format**:
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable message",
    "details": { ... }
  },
  "meta": {
    "timestamp": "2025-12-01T10:00:00Z",
    "requestId": "uuid"
  }
}
```

**HTTP Status Codes**:
- 200: Success
- 201: Created
- 400: Bad Request
- 401: Unauthorized
- 403: Forbidden
- 404: Not Found
- 500: Internal Server Error

---

### Rate Limiting

**Limits**:
- Authenticated users: 100 requests/minute
- Unauthenticated: 20 requests/minute
- Search API: 30 searches/minute

**Headers**:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1638360000
```

---

## Search Architecture

### Elasticsearch Index Schema

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "title": {
        "type": "text",
        "analyzer": "english",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "summary": {
        "type": "text",
        "analyzer": "english"
      },
      "content": {
        "type": "text",
        "analyzer": "english"
      },
      "tags": {
        "type": "keyword"
      },
      "portalType": { "type": "keyword" },
      "category": { "type": "keyword" },
      "subcategory": { "type": "keyword" },
      "difficulty": { "type": "keyword" },
      "targetAudience": { "type": "keyword" },
      "lastUpdated": { "type": "date" },
      "viewCount": { "type": "integer" },
      "avgRating": { "type": "float" },
      "hasVideo": { "type": "boolean" }
    }
  },
  "settings": {
    "analysis": {
      "analyzer": {
        "english_synonyms": {
          "tokenizer": "standard",
          "filter": ["lowercase", "english_stop", "english_stemmer", "synonym_filter"]
        }
      },
      "filter": {
        "synonym_filter": {
          "type": "synonym",
          "synonyms_path": "synonyms.txt"
        }
      }
    }
  }
}
```

### Index Update Strategy

**Real-time Updates**: When document published/updated
**Bulk Reindex**: Weekly (Sunday 2 AM)
**Partial Updates**: For metrics (view count, rating)

---

## Deployment Architecture

### Environment Setup

**Development**:
- Local development with Docker Compose
- Hot reload for frontend (Angular)
- Local Elasticsearch instance
- PostgreSQL container
- Redis container

**Staging**:
- Cloud deployment (GCP/AWS/Azure)
- Mimics production setup
- Separate database
- Automated deployments from `develop` branch

**Production**:
- Multi-region cloud deployment
- Load balanced
- Auto-scaling enabled
- Separate database with backups
- CDN for static assets
- SSL/TLS enabled
- Deployments from `main` branch

---

### CI/CD Pipeline

```
┌──────────┐
│  Commit  │
└────┬─────┘
     │
     ▼
┌──────────────────────┐
│  Build & Test        │
│  • Install deps      │
│  • Lint markdown     │
│  • Validate metadata │
│  • Run unit tests    │
└────┬─────────────────┘
     │
     ▼
┌──────────────────────┐
│  Metadata Extraction │
│  • Parse YAML        │
│  • Calculate metrics │
│  • Validate schema   │
└────┬─────────────────┘
     │
     ▼
┌──────────────────────┐
│  Quality Checks      │
│  • Link checking     │
│  • Spell checking    │
│  • Accessibility     │
│  • SEO validation    │
└────┬─────────────────┘
     │
     ▼
┌──────────────────────┐
│  Build Frontend      │
│  • Angular build     │
│  • Optimize assets   │
│  • Generate bundles  │
└────┬─────────────────┘
     │
     ▼
┌──────────────────────┐
│  Deploy              │
│  • Update database   │
│  • Reindex search    │
│  • Deploy frontend   │
│  • Deploy backend    │
│  • Clear cache       │
└──────────────────────┘
```

---

## Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| Page Load Time | < 2s | Lighthouse |
| Time to Interactive | < 3s | Lighthouse |
| Search Response Time (p95) | < 200ms | APM |
| API Response Time (p95) | < 100ms | APM |
| Autocomplete Response (p95) | < 100ms | APM |
| Uptime | 99.9% | Monitoring |
| Error Rate | < 0.1% | Logs |
| Lighthouse Score | > 90 | CI/CD |
| Accessibility Score | 100 | pa11y |

---

## Security Considerations

**Authentication**: JWT tokens with 1-hour expiration
**Authorization**: Role-based access control (RBAC)
**Data Encryption**: TLS 1.3 in transit, AES-256 at rest
**CORS**: Configured for allowed origins only
**Rate Limiting**: Prevent abuse
**Input Validation**: Sanitize all user input
**XSS Protection**: Content Security Policy (CSP) headers
**SQL Injection**: Parameterized queries only
**Audit Logging**: Track all data access and modifications

---

## Monitoring & Observability

**Metrics**:
- Application metrics (Micrometer)
- Business metrics (custom)
- Infrastructure metrics (Cloud provider)

**Logging**:
- Centralized logging (ELK or Cloud Logging)
- Structured JSON logs
- Log levels: ERROR, WARN, INFO, DEBUG

**Alerting**:
- Uptime monitoring (PagerDuty/OpsGenie)
- Error rate thresholds
- Performance degradation
- Search failures

**Dashboards**:
- Application health
- API performance
- Search analytics
- User engagement
- Content health

---

## Document Control

**Version**: 1.0
**Status**: Final Specification
**Approved By**: Agent 1 - Documentation Architect
**Date**: December 1, 2025

**Next Steps**:
1. Review and approve technical architecture
2. Set up development environment
3. Implement frontend components
4. Implement backend services
5. Configure Elasticsearch
6. Set up CI/CD pipeline
7. Deploy to staging
8. Performance testing
9. Production deployment
