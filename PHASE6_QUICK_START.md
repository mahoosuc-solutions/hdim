# Phase 6: Advanced Features Quick Start Guide

Quick reference for using the 6 advanced services in your application.

---

## 1. Analytics Service

**Track events for backend collection**

```typescript
constructor(private analytics: AnalyticsService) {}

// Record an event
this.analytics.recordEvent('button_clicked', {
  buttonId: 'submit-btn',
  pageId: 'checkout'
}, 'engagement', traceId);

// Get current batch
const batch = this.analytics.getCurrentBatch();

// Listen to batches
this.analytics.batches$.subscribe(batch => {
  console.log('Flushed', batch.events.length, 'events');
});

// Configure
this.analytics.setConfig({
  batchSize: 20,
  flushInterval: 3000
});
```

---

## 2. Multi-Tenant Service

**Isolate data by tenant**

```typescript
constructor(private multiTenant: MultiTenantService) {}

// Set current tenant
this.multiTenant.setCurrentTenant('tenant-123');

// Filter data
const myData = this.multiTenant.filterByTenant(allData);
// Only returns items where tenantId === 'tenant-123'

// Per-tenant preferences
this.multiTenant.setTenantPreference('theme', 'dark');
this.multiTenant.setTenantPreference('language', 'en-US');

// Get preferences
const theme = this.multiTenant.getTenantPreference('theme');

// Listen for tenant changes
this.multiTenant.currentTenant$.subscribe(tenantId => {
  console.log('Switched to:', tenantId);
  // Reload data for new tenant
});
```

**Backend Query Pattern**:
```typescript
// Always include tenant filtering
@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId")
List<Patient> findAllForTenant(@Param("tenantId") String tenantId);
```

---

## 3. Error Recovery Service

**Retry operations and queue failed ones**

```typescript
constructor(private errorRecovery: ErrorRecoveryService) {}

// Execute with automatic retry
this.errorRecovery.executeWithRetry(
  () => this.http.post('/api/data', payload),
  'save_operation',
  { maxRetries: 3, initialDelay: 1000 }
).subscribe({
  next: (result) => console.log('Success:', result),
  error: (err) => console.error('Failed:', err)
});

// Queue operation for later (when offline)
const opId = this.errorRecovery.queueOperation(
  'save_user_data',
  () => this.http.post('/api/users', userData)
);

// Retry queued operations (on connection recovery)
this.websocket.connectionStatus$.subscribe(status => {
  if (status === 'connected') {
    this.errorRecovery.retryQueuedOperations();
  }
});

// Check queue status
const queueSize = this.errorRecovery.getQueueSize();
const operations = this.errorRecovery.getQueuedOperations();

// Error tracking
this.errorRecovery.errors$.subscribe(error => {
  if (error.severity === 'critical') {
    this.notifyOps(error);
  }
});
```

---

## 4. Feature Flag Service

**Control feature visibility and rollout**

```typescript
constructor(private featureFlags: FeatureFlagService) {}

// Simple toggle
if (this.featureFlags.isEnabled('dark-mode')) {
  this.applyDarkTheme();
}

// Percentage-based rollout (50% of users)
this.featureFlags.setFeatureFlagPercentage('new-ui', 50);
if (this.featureFlags.isEnabled('new-ui')) {
  this.loadNewUILayout();
}

// A/B testing with variants
const variant = this.featureFlags.getVariant(
  'checkout-flow',
  ['v1-simple', 'v2-advanced']
);
if (variant === 'v2-advanced') {
  this.showAdvancedCheckout();
}

// Listen for flag changes
this.featureFlags.featureFlags$.subscribe(flags => {
  console.log('Flags updated:', flags);
  this.updateFeatureUI();
});

// Admin: Set flags for testing
this.featureFlags.setFeatureFlag('maintenance-mode', true);
this.featureFlags.setFeatureFlagPercentage('beta-feature', 100); // Enable for all
```

---

## 5. Distributed Tracing Service

**Track requests end-to-end**

```typescript
constructor(
  private tracing: DistributedTracingService,
  private http: HttpClient
) {}

// Get trace headers for requests
const headers = this.tracing.getTraceHeaders();
// Returns: {
//   'x-correlation-id': '...',
//   'x-trace-id': '...',
//   'x-span-id': '...'
// }

// Add to HTTP request
const request = this.http.get('/api/data', {
  headers: this.tracing.getTraceHeaders()
});

// Start a span for important operations
ngOnInit() {
  const span = this.tracing.startSpan('component_init', {
    component: 'DashboardComponent'
  });

  // Do work...
  this.loadData();

  // Add events
  this.tracing.addSpanEvent('data_loaded', {
    itemCount: 100
  });

  // End span
  this.tracing.endSpan('completed');
}

// Get correlation ID for logging
const correlationId = this.tracing.getCurrentCorrelationId();
this.logger.info('Operation started', { correlationId });

// Get formatted trace for debugging
const traceLog = this.tracing.getFormattedTraceLog();
console.log(traceLog);
```

**Logged Output**:
```
Trace: 550e8400-e29b-41d4-a716-446655440000
Correlation: 6ba7b810-9dad-11d1-80b4-00c04fd430c8
Root Span: 550e8400-e29b
├─ [init-0001] component_init (156ms) - completed
│  └─ data_loaded
└─ [fetch-0001] fetch_users (45ms) - completed
```

---

## 6. Business Metrics Service

**Track user engagement and ROI**

```typescript
constructor(private businessMetrics: BusinessMetricsService) {}

// Track page visits
ngOnInit() {
  this.businessMetrics.recordPageView('dashboard');
}

// Track interactions
onButtonClick() {
  this.businessMetrics.recordInteraction('feature_button_clicked', {
    featureName: 'export-pdf',
    context: 'user-menu'
  });
}

// Track feature adoption
onFeatureUsed(featureName: string) {
  this.businessMetrics.recordFeatureAdoption(featureName);
}

// Track satisfaction
onSatisfactionSurvey(score: number) {
  this.businessMetrics.recordSatisfactionScore(score, 'new-feature');
}

// Calculate ROI
updateFeatureROI(featureName: string) {
  // User saves 5 minutes per week using feature
  // Company value is $200/hr (labor cost)
  // 5 min = 0.083 hr = $16.66 saved per user per week
  this.businessMetrics.updateROI(featureName, 5, 16.66);
}

// Get metrics
const engagement = this.businessMetrics.getEngagementMetrics();
console.log('Page Views:', engagement.pageViews);
console.log('Interactions:', engagement.interactionCount);
console.log('Time on Page:', engagement.timeOnPage, 'seconds');

const adoption = this.businessMetrics.getAdoptionMetrics('dark-mode');
console.log('Adoption Rate:', adoption.adoptionRate, '%');
console.log('Trend:', adoption.adoptionTrend);

const roi = this.businessMetrics.getROIMetrics('dark-mode');
console.log('Total Time Saved:', roi.totalTimeSaved, 'minutes');
console.log('Total Cost Saved:', roi.totalCostSaved, 'dollars');

const satisfaction = this.businessMetrics.getSatisfactionMetrics('dark-mode');
console.log('Average Score:', satisfaction.averageScore);
console.log('NPS:', satisfaction.nps);

// Listen for updates
this.businessMetrics.metrics$.subscribe(metrics => {
  this.updateDashboard(metrics);
});
```

---

## Common Integration Patterns

### Pattern 1: Track Event with Full Context

```typescript
// In a service method
async processPayment(amount: number) {
  const correlationId = this.tracing.getCurrentCorrelationId();
  const tenantId = this.multiTenant.getCurrentTenant();

  try {
    const result = await this.errorRecovery.executeWithRetry(
      () => this.http.post('/api/payments', { amount }),
      'process_payment'
    ).toPromise();

    this.analytics.recordEvent('payment_processed', {
      amount,
      status: 'success'
    }, 'business', correlationId);

    this.businessMetrics.recordInteraction('payment_submitted', {
      amount
    });

    return result;
  } catch (error) {
    this.analytics.recordEvent('payment_failed', {
      amount,
      error: error.message
    }, 'error', correlationId);
    throw error;
  }
}
```

### Pattern 2: Feature Flag with Analytics

```typescript
// Track which variant user sees
ngOnInit() {
  const variant = this.featureFlags.getVariant('checkout', ['v1', 'v2']);

  this.analytics.recordEvent('feature_variant_assigned', {
    feature: 'checkout',
    variant
  }, 'engagement');

  this.businessMetrics.recordFeatureAdoption('checkout-' + variant);
}
```

### Pattern 3: Tenant-Aware Error Handling

```typescript
// Ensure operations respect tenant boundaries
saveUserData(userId: string, data: any) {
  const tenantId = this.multiTenant.getCurrentTenant();

  return this.errorRecovery.executeWithRetry(
    () => this.http.post('/api/users', {
      userId,
      tenantId, // Always include tenant
      data
    }),
    'save_user_data'
  );
}
```

### Pattern 4: Monitor Feature Adoption & ROI

```typescript
// Track both technical metrics and business impact
onFeatureUsed(featureName: string, timeSavedMinutes: number) {
  // Technical tracking
  this.businessMetrics.recordFeatureAdoption(featureName);
  this.analytics.recordEvent('feature_used', {
    featureName,
    timeSaved: timeSavedMinutes
  }, 'engagement');

  // Business ROI
  const hourlyRate = 200; // $200/hr
  const costSaved = (timeSavedMinutes / 60) * hourlyRate;
  this.businessMetrics.updateROI(featureName, timeSavedMinutes, costSaved);
}
```

---

## Configuration Examples

### Development Environment
```typescript
// src/app/app.config.ts
export function setupServices(analyticsService: AnalyticsService,
                              errorRecoveryService: ErrorRecoveryService,
                              featureFlagService: FeatureFlagService) {
  // Strict thresholds for catching issues early
  errorRecoveryService.setDefaultRetryPolicy({
    maxRetries: 5,
    initialDelay: 500
  });

  // Collect all analytics events for debugging
  analyticsService.setConfig({
    batchSize: 5, // Flush frequently
    flushInterval: 1000, // 1 second
    enabled: true
  });

  // Test all feature flags during development
  featureFlagService.setFeatureFlag('debug-mode', true);
}
```

### Production Environment
```typescript
// Resilient defaults for production
errorRecoveryService.setDefaultRetryPolicy({
  maxRetries: 3,
  initialDelay: 1000,
  maxDelay: 30000,
  backoffMultiplier: 2
});

// Batch events for efficient backend communication
analyticsService.setConfig({
  batchSize: 50,
  flushInterval: 5000, // 5 seconds
  apiEndpoint: 'https://analytics.api.production.com/events',
  enabled: true
});
```

---

## Testing Services

### Mock Setup
```typescript
describe('MyComponent', () => {
  let component: MyComponent;
  let fixture: ComponentFixture<MyComponent>;
  let analyticsService: jasmine.SpyObj<AnalyticsService>;
  let multiTenantService: jasmine.SpyObj<MultiTenantService>;

  beforeEach(async () => {
    const analyticsSpy = jasmine.createSpyObj('AnalyticsService',
      ['recordEvent', 'flushBatch']);
    const multiTenantSpy = jasmine.createSpyObj('MultiTenantService',
      ['getCurrentTenant', 'filterByTenant']);

    await TestBed.configureTestingModule({
      declarations: [MyComponent],
      providers: [
        { provide: AnalyticsService, useValue: analyticsSpy },
        { provide: MultiTenantService, useValue: multiTenantSpy }
      ]
    }).compileComponents();

    analyticsService = TestBed.inject(AnalyticsService) as any;
    multiTenantService = TestBed.inject(MultiTenantService) as any;

    fixture = TestBed.createComponent(MyComponent);
    component = fixture.componentInstance;
  });

  it('should track button clicks', () => {
    fixture.debugElement.query(By.css('button')).nativeElement.click();
    expect(analyticsService.recordEvent).toHaveBeenCalledWith(
      jasmine.stringContaining('click'),
      jasmine.any(Object)
    );
  });
});
```

---

## Troubleshooting

### Analytics Events Not Appearing
- Check `enabled` config: `analyticsService.getConfig().enabled`
- Check batch queue: `analyticsService.getBatchQueue().length`
- Verify endpoint: `analyticsService.getConfig().apiEndpoint`
- Check network tab for POST requests

### Tenant Data Leaking
- Always filter through service: `multiTenantService.filterByTenant(data)`
- Verify tenant is set: `multiTenantService.getCurrentTenant()`
- Check localStorage: `localStorage.getItem('current_tenant')`

### Retries Not Happening
- Check error is retriable: `errorRecoveryService.isRetriable(error)`
- Verify policy: `errorRecoveryService.getDefaultRetryPolicy()`
- Check queue: `errorRecoveryService.getQueuedOperations()`
- Trigger retry: `errorRecoveryService.retryQueuedOperations()`

### Feature Flag Not Working
- Verify flag exists: `featureFlagService.getFlag('flag-name')`
- Check user ID: `featureFlagService.getUserId()`
- Clear cache: `sessionStorage.clear()`
- Reload page

---

## Performance Tips

1. **Batch Aggressively**: Increase `batchSize` in production (default: 10, set to 50+)
2. **Filter Early**: Use `MultiTenantService.filterByTenant()` to reduce data
3. **Reuse Trace IDs**: Pass `correlationId` through call chains
4. **Cache Feature Flags**: Results are cached per session
5. **Monitor Memory**: Use `BusinessMetricsService` to detect leaks

---

*Quick Start Guide - Phase 6 Advanced Features*
*For detailed docs, see PHASE6_ADVANCED_FEATURES_SUMMARY.md*
