# Console.log Migration Guide

Generated: $(date)

## Files Requiring Migration


### apps/clinical-portal/src/app/interceptors/error.interceptor.ts

**Console statements:** 2

**Occurrences:**
```
103:        console.warn(
183:        console.error('HTTP Error:', {
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/demo-mode/services/demo-mode.service.ts

**Console statements:** 8

**Occurrences:**
```
197:    console.log('[Demo Mode] Enabled');
212:    console.log('[Demo Mode] Disabled');
294:          console.warn('[Demo Mode] Failed to parse storyboard message', err);
344:      console.log('[Demo Mode] Demo backend not available - using local-only mode');
388:      console.warn('[Demo Mode] Could not load status:', err);
415:      console.error('[Demo Mode] Could not load scenarios:', err);
617:    console.log('[Demo Mode] Recording started');
633:    console.log(`[Demo Mode] Recording stopped. Duration: ${this.formattedRecordingTime()}`);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/demo-mode/components/demo-control-bar/demo-control-bar.component.ts

**Console statements:** 6

**Occurrences:**
```
423:      console.error('Failed to load scenario:', err);
432:        console.error('Failed to reset demo:', err);
452:      console.error('Failed to cancel scenario load:', err);
463:      console.error('Failed to stop demo session:', err);
472:        console.error('Failed to reset current tenant:', err);
484:        console.error('Failed to create snapshot:', err);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/shared/services/user-role.service.ts

**Console statements:** 2

**Occurrences:**
```
166:      console.error('Error saving user role:', err);
180:      console.error('Error loading user role:', err);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/shared/components/loading-spinner/loading-spinner.component.ts

**Console statements:** 1

**Occurrences:**
```
118:      console.warn('LoadingSpinner: diameter should be between 20 and 200 pixels');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/shared/components/global-search/global-search.component.ts

**Console statements:** 4

**Occurrences:**
```
220:        console.error('Error searching patients:', err);
243:        console.error('Error searching measures:', err);
368:      console.error('Error loading recent searches:', err);
389:      console.error('Error saving recent search:', err);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/shared/components/form-field/form-field.component.ts

**Console statements:** 1

**Occurrences:**
```
158:      console.error('FormFieldComponent: control input is required');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/shared/components/risk-trend-chart/risk-trend-chart.component.ts

**Console statements:** 2

**Occurrences:**
```
535:          console.error('Error loading risk assessment data:', error);
556:          console.error('Error loading health score data:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/shared/components/batch-calculation/batch-calculation.component.ts

**Console statements:** 4

**Occurrences:**
```
83:          console.error('Failed to start batch calculation:', err);
110:          console.error('Error polling job status:', err);
137:          console.error('Failed to cancel job:', err);
170:          console.error('Failed to load job history:', err);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/dialogs/sdoh-referral-dialog/sdoh-referral-dialog.component.ts

**Console statements:** 2

**Occurrences:**
```
415:          console.error('Referral submission error:', error);
436:          console.error('Draft save error:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/dialogs/evaluation-details-dialog/evaluation-details-dialog.component.ts

**Console statements:** 1

**Occurrences:**
```
232:    console.log('Export to PDF functionality would be implemented here');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/dialogs/batch-evaluation-dialog/batch-evaluation-dialog.component.ts

**Console statements:** 4

**Occurrences:**
```
445:          console.error('Failed to load measures:', err);
469:          console.error('Failed to load patients:', err);
563:          console.error('Failed to create schedule:', err);
674:          console.error('Failed to detect care gaps:', err);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/visualization/angular/quality-constellation.component.ts

**Console statements:** 4

**Occurrences:**
```
149:          console.warn('Quality results API unavailable:', error.message);
179:            console.log(`Loaded ${qualityResults.length} quality results from API for ${patients.length} patients`);
206:          console.error('Error loading data:', error);
253:    console.log(`Generated ${this.qualityResults.length} mock quality results for ${patients.length} patients`);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/visualization/angular/visualization-layout.component.ts

**Console statements:** 8

**Occurrences:**
```
74:    console.log('Visualization Layout initialized');
85:    console.log('Reset camera requested');
94:    console.log('Fullscreen toggle requested');
102:    console.log('Screenshot capture requested');
125:      console.error('Screenshot capture failed:', error);
134:    console.log('FPS display toggle:', enabled);
143:    console.log('VR mode requested');
151:    console.log('Visualization mode changed:', modeId);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/visualization/angular/live-batch-monitor.component.ts

**Console statements:** 12

**Occurrences:**
```
119:        console.log('WebSocket status:', status);
127:        console.log('Batch monitor state:', state);
155:    console.log('Visualization ready. Waiting for user to start batch evaluation.');
232:    console.log('Batch progress update:', event);
376:      console.error('No library selected');
381:      console.warn('Batch evaluation already in progress');
385:    console.log('Starting batch evaluation...', {
404:        console.log('Batch progress received:', progress);
409:        console.error('Batch evaluation error:', error);
413:        console.log('Batch evaluation completed');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/visualization/scenes/quality-constellation.scene.ts

**Console statements:** 2

**Occurrences:**
```
120:    console.log(`Initializing Quality Constellation with ${qualityResults.length} results`);
135:    console.log(`Created ${this.patientPoints.length} patient points`);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/dialog.service.ts

**Console statements:** 2

**Occurrences:**
```
220:    console.error('Error Details:', errorInfo);
230:    console.log('Opening help for topic:', topic);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/api.service.ts

**Console statements:** 3

**Occurrences:**
```
273:    console.log(`[API Service] ${method} ${url}`, response);
282:    console.error(`[API Service] ${method} ${url} - Error:`, {
296:    console.log(`[API Service] Retry attempt ${attempt}/${maxRetries} after ${delay}ms`);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/guided-tour.service.ts

**Console statements:** 2

**Occurrences:**
```
312:      console.warn(`Tour "${tourId}" not found or has no steps`);
466:      console.warn(`Tour step target not found: ${step.targetSelector}`);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/risk-assessment.service.ts

**Console statements:** 5

**Occurrences:**
```
42:        console.error(
66:        console.error(
94:        console.error(
118:        console.error(
136:        console.error('Error fetching population statistics:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/medication/medication.service.ts

**Console statements:** 1

**Occurrences:**
```
1150:    console.error(`[MedicationService] Error in ${context}:`, error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/care-plan/care-plan.service.ts

**Console statements:** 1

**Occurrences:**
```
1067:    console.error(`[CarePlanService] Error in ${context}:`, error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/nurse-workflow/nurse-workflow.service.ts

**Console statements:** 1

**Occurrences:**
```
806:    console.error(`[NurseWorkflowService] Error in ${context}:`, error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/scheduled-evaluation.service.ts

**Console statements:** 6

**Occurrences:**
```
78:      console.error('Failed to load scheduled evaluations:', error);
90:      console.error('Failed to save scheduled evaluations:', error);
106:      console.error('Failed to load execution history:', error);
120:      console.error('Failed to save execution history:', error);
150:        console.log(`[ScheduledEvaluationService] Executing schedule: ${schedule.name}`);
487:      console.warn(
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/medication-adherence.service.ts

**Console statements:** 4

**Occurrences:**
```
87:          console.error('Error fetching active medications:', error);
184:        console.error('Error calculating overall adherence:', error);
273:        console.error('Error getting problematic medications:', error);
310:          console.error('Error fetching medication dispenses:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/offline/network-status.service.ts

**Console statements:** 1

**Occurrences:**
```
204:        console.log(
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/offline/offline-data-cache.service.ts

**Console statements:** 2

**Occurrences:**
```
80:            console.log(`Serving stale cache for ${id} (offline)`);
159:          console.log('Server unavailable, saving locally');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/offline/sync-queue.service.ts

**Console statements:** 4

**Occurrences:**
```
347:      console.warn(`Validation error for sync item ${item.id}, removing from queue`);
405:      console.error(`Max retries reached for sync item ${item.id}`, item);
426:        console.log('Network restored - starting auto-sync');
428:          console.log('Auto-sync completed:', result);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/offline/offline-storage.service.ts

**Console statements:** 12

**Occurrences:**
```
65:      console.warn('IndexedDB not supported - offline mode disabled');
72:      console.error('IndexedDB error:', event);
79:      console.log('IndexedDB initialized successfully');
156:        console.error(`Error getting item from ${storeName}:`, error);
181:        console.error(`Error getting all items from ${storeName}:`, error);
211:        console.error(`Error getting items by index from ${storeName}:`, error);
236:        console.error(`Error putting item in ${storeName}:`, error);
274:        console.error(`Error putting items in ${storeName}:`, error);
299:        console.error(`Error deleting item from ${storeName}:`, error);
324:        console.error(`Error clearing ${storeName}:`, error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/report-export.service.ts

**Console statements:** 1

**Occurrences:**
```
475:      console.error('Failed to open print window. Please allow popups.');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/measure-favorites.service.ts

**Console statements:** 3

**Occurrences:**
```
65:      console.error('Error loading measure favorites from storage:', error);
78:      console.error('Error saving measure favorites:', error);
89:      console.error('Error saving recent measures:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/patient-deduplication.service.ts

**Console statements:** 2

**Occurrences:**
```
414:          console.log(`Linked duplicate: ${duplicateId} -> master: ${masterId} (score: ${matchScore})`);
422:    console.log(`Detection complete: ${mastersCreated} masters, ${duplicatesLinked} duplicates linked`);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/recent-patients.service.ts

**Console statements:** 2

**Occurrences:**
```
165:      console.error('[RecentPatientsService] Error loading from storage:', error);
178:      console.error('[RecentPatientsService] Error saving to storage:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/audit.service.ts

**Console statements:** 2

**Occurrences:**
```
83:        console.warn('[AuditService] Failed to send audit batch, storing locally:', error);
173:        console.warn('[AuditService] Failed to log immediate event:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/batch-monitor.service.ts

**Console statements:** 7

**Occurrences:**
```
52: *   console.log('Batch progress:', progress);
87:    console.log('Starting batch evaluation with config:', config);
95:        console.log(`Loaded ${patients.length} patients`);
108:        console.log('Batch evaluation submitted:', batchResponse);
136:          console.error('Batch evaluation error:', error);
205:      console.log('Connecting to WebSocket...');
214:    console.log('Stopping batch monitoring');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/report-builder.service.ts

**Console statements:** 3

**Occurrences:**
```
600:      console.error('Error loading reports from storage:', e);
611:      console.error('Error saving reports to storage:', e);
622:      console.error('Error saving history to storage:', e);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/filter-persistence.service.ts

**Console statements:** 4

**Occurrences:**
```
27:      console.warn('Failed to save filters to localStorage:', error);
57:      console.warn('Failed to load filters from localStorage:', error);
70:      console.warn('Failed to clear filters from localStorage:', error);
86:      console.warn('Failed to clear all filters from localStorage:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/fhir.service.ts

**Console statements:** 6

**Occurrences:**
```
59:        console.error(`Error fetching observations for patient ${patientId}:`, error);
96:        console.error(`Error fetching conditions for patient ${patientId}:`, error);
133:        console.error(`Error fetching medication requests for patient ${patientId}:`, error);
170:        console.error(`Error fetching procedures for patient ${patientId}:`, error);
185:        console.error('Error importing FHIR resources:', error);
208:        console.error(`Error exporting patient ${patientId} as bundle:`, error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/services/evaluation-data-flow.service.ts

**Console statements:** 5

**Occurrences:**
```
29:        console.log('[DataFlow] WebSocket connected');
54:          console.error('[DataFlow] Error parsing WebSocket message:', error);
59:        console.error('[DataFlow] WebSocket error:', error);
64:        console.log('[DataFlow] WebSocket closed');
68:      console.error('[DataFlow] Failed to connect WebSocket:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/guards/permission.guard.ts

**Console statements:** 1

**Occurrences:**
```
53:    console.warn(
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/guards/auth.guard.ts

**Console statements:** 1

**Occurrences:**
```
43:    console.warn(`User not authenticated. Redirecting to login. Return URL: ${returnUrl}`);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/guards/role.guard.ts

**Console statements:** 1

**Occurrences:**
```
53:    console.warn(
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/guards/dev.guard.ts

**Console statements:** 1

**Occurrences:**
```
42:    console.warn('DevGuard: Production mode - access denied');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/agent-builder/agent-builder.component.ts

**Console statements:** 3

**Occurrences:**
```
184:          console.error('Error loading agents:', err);
197:        error: () => console.warn('Failed to load tools - using cached'),
206:        error: () => console.warn('Failed to load providers - using cached'),
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/agent-builder/services/agent-builder.service.ts

**Console statements:** 1

**Occurrences:**
```
375:    console.error('AgentBuilderService error:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/measure-builder/dialogs/test-preview-dialog.component.ts

**Console statements:** 2

**Occurrences:**
```
509:          console.warn('Test evaluation timed out after', EVALUATION_TIMEOUT_MS / 1000, 'seconds');
512:          console.warn('Test API unavailable, using sample data:', err);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/measure-builder/dialogs/version-history-dialog.component.ts

**Console statements:** 3

**Occurrences:**
```
883:          console.error('Failed to create version:', err);
915:          console.error('Failed to publish version:', err);
947:          console.error('Failed to retire version:', err);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/measure-builder/config/production-monitoring.config.ts

**Console statements:** 4

**Occurrences:**
```
118:    console.error('🔴 CRITICAL ALERT:', alert.metric, alert.currentValue);
133:      }).catch(err => console.error('Failed to send alert:', err));
144:    console.warn('🟡 WARNING:', alert.metric, alert.currentValue);
160:    console.log('ℹ️ INFO:', alert.metric, alert.currentValue);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/measure-builder/components/visual-algorithm-builder/visual-algorithm-builder.component.ts

**Console statements:** 1

**Occurrences:**
```
455:      console.log('Edit block:', this.contextMenu.blockId);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/patients/patients.component.ts

**Console statements:** 12

**Occurrences:**
```
251:          console.error('[ERR-5001] Error loading patients:', err);
613:          console.error('Error loading patient details:', err);
644:          console.error('Error loading patient evaluations:', err);
1045:              console.error(`[${appError.code}] Error deleting patient ${patient.fullName}:`, err);
1083:      console.log(
1093:      console.log(
1130:          console.error('Error calculating deduplication statistics:', err);
1142:    console.log(`Starting duplicate detection for ${this.patients.length} patients...`);
1150:        console.log('Detection result:', result);
1161:        console.log('Updated stats:', this.deduplicationStats);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/evaluations/evaluations.component.ts

**Console statements:** 13

**Occurrences:**
```
197:        console.warn('[WARN] Local measures unavailable, falling back to HEDIS registry:', error);
203:            console.error('[ERR-5002] Error loading measures:', hedisError);
212:      console.log(`[INFO] Loaded ${measures.length} measures:`, measures.map(m => m.name).join(', '));
319:        console.error('[ERR-5001] Error loading patients:', error);
408:        console.warn('[WARN] Unable to save default evaluation preset:', error);
424:        console.warn('[WARN] Unable to clear default evaluation preset:', error);
516:        console.warn('[WARN] Unable to load default evaluation preset:', error);
594:        console.error(`[${this.evaluationErrorDetails.code}] CQL evaluation error:`, error);
611:        console.log(`[INFO] CQL evaluation result:`, evaluation);
628:        console.error(`[${this.evaluationErrorDetails.code}] Local evaluation error:`, error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/quality-measures/quality-measures.component.ts

**Console statements:** 2

**Occurrences:**
```
268:        console.error('Failed to load care gap statistics:', error);
289:        console.error('Failed to load patient count:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/results/results.component.ts

**Console statements:** 8

**Occurrences:**
```
257:          console.error('Error loading results:', error);
432:          console.error('Error loading patient:', err);
1010:          console.error('QRDA export failed:', err);
1038:          console.error('Error polling QRDA job:', err);
1064:          console.error('Failed to download QRDA export:', err);
1086:          console.error('Failed to cancel QRDA export:', err);
1193:    console.log(`Executing action ${actionType} on result ${result.id}`);
1329:          console.error('Error loading patient:', err);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/patient-health-overview/patient-health-overview.component.ts

**Console statements:** 2

**Occurrences:**
```
106:        console.error('Error loading health overview:', err);
323:    console.log('Alert dismissed with audit record:', dismissalRecord);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/qa-audit-dashboard/qa-audit-dashboard.component.ts

**Console statements:** 10

**Occurrences:**
```
89:    console.log('Loading QA review queue...');
97:    console.log('Loading QA metrics...');
105:    console.log('Loading trend data...');
132:    console.log('Approving decision:', this.currentReviewDecision.eventId, 'Notes:', this.reviewNotes);
149:    console.log('Rejecting decision:', this.currentReviewDecision.eventId, 'Notes:', this.reviewNotes);
166:    console.log('Flagging decision for escalation:', this.currentReviewDecision.eventId, 'Notes:', this.reviewNotes);
181:    console.log('Marking as false positive:', decision.eventId);
192:    console.log('Marking as false negative:', decision.eventId);
218:    console.log('Applying filters:', {
232:    console.log('Exporting QA report...');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/pre-visit-planning/pre-visit-planning.component.ts

**Console statements:** 2

**Occurrences:**
```
159:          console.error('Failed to load patients:', err);
296:    console.log('Export PDF - would generate PDF with', this.patients().length, 'patients');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/risk-stratification/risk-stratification.component.ts

**Console statements:** 2

**Occurrences:**
```
445:    console.log('Schedule visit for:', patient.patientName);
450:    console.log('Initiate outreach for:', patient.patientName);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/clinical-audit-dashboard/clinical-audit-dashboard.component.ts

**Console statements:** 7

**Occurrences:**
```
99:    console.log('Loading clinical decisions...');
107:    console.log('Loading clinical metrics...');
122:    console.log('Applying filters:', {
147:    console.log('Accepting recommendation:', this.selectedDecision.eventId, 'Notes:', this.clinicalNotes);
161:    console.log('Rejecting recommendation:', this.selectedDecision.eventId, 'Clinical rationale:', this.clinicalNotes);
175:    console.log('Modifying recommendation:', this.selectedDecision.eventId, 'Modifications:', this.clinicalNotes);
202:    console.log('Exporting clinical audit report...');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/mpi-audit-dashboard/mpi-audit-dashboard.component.ts

**Console statements:** 7

**Occurrences:**
```
93:    console.log('Loading MPI audit events...');
101:    console.log('Loading MPI metrics...');
116:    console.log('Applying filters:', {
144:    console.log('Validating merge:', mergeEvent.eventId);
153:      console.log('Rolling back merge:', mergeEvent.eventId);
162:    console.log('Resolving data quality issue:', issue.issueId);
170:    console.log('Exporting MPI audit report...');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/settings/mfa-settings.component.ts

**Console statements:** 5

**Occurrences:**
```
510:          console.error('Failed to load MFA status:', error);
530:          console.error('Failed to start MFA setup:', error);
558:          console.error('Failed to confirm MFA setup:', error);
591:          console.error('Failed to regenerate recovery codes:', error);
617:          console.error('Failed to disable MFA:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/reports/reports.component.ts

**Console statements:** 1

**Occurrences:**
```
2698:          console.error(`Error deleting report ${report.reportName}:`, err);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/dashboard/dashboard.component.ts

**Console statements:** 3

**Occurrences:**
```
330:      console.error('Error loading role component:', error);
369:        console.error('Error loading critical dashboard data:', error);
398:        console.error('Error loading evaluations:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/dashboard/provider-dashboard/provider-dashboard.component.ts

**Console statements:** 6

**Occurrences:**
```
610:        console.warn('Failed to load care gaps from API', error);
812:        console.warn('Failed to load quality measures from API, using fallback data', error);
926:        console.warn('Failed to load pending results from API', error);
1224:    console.log(`Care gap closed in ${result.closureTimeMs}ms via ${result.action}`);
1238:    console.log('Reviewing result:', result.resultType);
1753:      console.warn('Failed to save section preferences:', e);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/dashboard/ma-dashboard/ma-dashboard.component.ts

**Console statements:** 4

**Occurrences:**
```
429:          console.error('Error rescheduling appointment:', error);
617:          console.error('Error loading care gaps:', error);
656:                  console.error('Error closing care gap:', error);
701:                  console.error('Error assigning intervention:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/care-gaps/care-gap-manager.component.ts

**Console statements:** 7

**Occurrences:**
```
762:    console.log('Submitting intervention:', intervention);
765:    console.log(`Intervention created for ${this.selectedGap.patientName}`);
807:    console.log('Submitting closure:', closure);
810:    console.log(`Care gap closed for ${this.selectedGap.patientName}`);
860:    console.log('Bulk closing gaps:', selectedGaps);
869:    console.log(`${selectedGaps.length} care gap(s) closed successfully`);
1206:    console.log('Closure metric tracked:', {
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/measure-comparison/measure-comparison.component.ts

**Console statements:** 4

**Occurrences:**
```
1423:    console.log('Showing overlap patients');
1430:    console.log(`Showing overlap between ${measure1.code} and ${measure2.code}`);
1444:    console.log('Exporting to PDF...');
1452:    console.log('Exporting to PNG...');
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/pages/testing-dashboard/testing-dashboard.component.ts

**Console statements:** 6

**Occurrences:**
```
143:      console.error('Failed to load scenarios:', error);
185:      console.error('Failed to check service health:', error);
378:      console.warn('Failed to load test results from storage:', error);
390:      console.warn('Failed to save test results to storage:', error);
433:      console.error('Failed to export test results:', error);
472:      console.error('Failed to export test results as CSV:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/components/dialogs/patient-selection-dialog.component.ts

**Console statements:** 1

**Occurrences:**
```
430:        console.error('Error loading patients:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/components/dialogs/report-detail-dialog.component.ts

**Console statements:** 5

**Occurrences:**
```
811:      console.error('Error parsing report data:', error);
838:          console.log('CSV export successful');
841:          console.error('Error exporting to CSV:', error);
854:          console.log('Excel export successful');
857:          console.error('Error exporting to Excel:', error);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/components/dialogs/provider-leaderboard-dialog.component.ts

**Console statements:** 4

**Occurrences:**
```
1194:    console.log('View provider details:', provider);
1202:    console.log('Compare with org:', provider);
1209:    console.log('View patient panel:', provider);
1216:    console.log('View care gaps:', provider);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---

### apps/clinical-portal/src/app/components/ai-audit-dashboard/ai-audit-dashboard.component.ts

**Console statements:** 7

**Occurrences:**
```
73:    console.log('Loading real-time audit events...');
127:    console.log('Loading analytics...');
168:      console.log('Executing NLQ:', this.naturalLanguageQuery);
183:      console.error('NLQ error:', error);
232:    console.log('View decision details:', decision);
240:    console.log('View audit trail:', correlationId);
248:    console.log('Replay decision:', decision);
```

**Migration steps:**
1. Add `import { LoggerService } from '../../services/logger.service';`
2. Add to constructor: `private loggerService: LoggerService`
3. Create contextual logger: `private logger = this.loggerService.withContext('ComponentName');`
4. Replace:
   - `console.log(...)` → `this.logger.info(...)`
   - `console.error(...)` → `this.logger.error(...)`
   - `console.warn(...)` → `this.logger.warn(...)`
   - `console.debug(...)` → `this.logger.debug(...)`

---
