import { Component, OnInit, OnDestroy, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort, Sort } from '@angular/material/sort';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { SelectionModel } from '@angular/cdk/collections';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

import { ToastService } from '../../services/toast.service';
import { DialogService } from '../../services/dialog.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { ConfirmationDialogComponent } from '../../shared/components/confirmation-dialog/confirmation-dialog.component';

import { AgentBuilderService } from './services/agent-builder.service';
import {
  AgentConfiguration,
  AgentStatus,
  ToolInfo,
  ProviderInfo,
} from './models/agent.model';

// Dialog imports (to be created)
import { CreateAgentDialogComponent } from './dialogs/create-agent-dialog.component';
import { TestAgentDialogComponent } from './dialogs/test-agent-dialog.component';

@Component({
  selector: 'app-agent-builder',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatDialogModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatTooltipModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    MatBadgeModule,
    MatCheckboxModule,
    MatDividerModule,
    PageHeaderComponent,
  ],
  templateUrl: './agent-builder.component.html',
  styleUrls: ['./agent-builder.component.scss'],
})
export class AgentBuilderComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private destroy$ = new Subject<void>();

  // Data
  agents: AgentConfiguration[] = [];
  dataSource = new MatTableDataSource<AgentConfiguration>([]);
  selection = new SelectionModel<AgentConfiguration>(true, []);
  availableTools: ToolInfo[] = [];
  providers: ProviderInfo[] = [];

  // Loading states
  loading = false;
  runtimeHealthy = true;

  // Pagination
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  // Filtering
  searchControl = new FormControl('');
  selectedStatus: AgentStatus | '' = '';
  selectedTab = 0;

  // Table columns
  displayedColumns: string[] = [
    'select',
    'name',
    'status',
    'modelProvider',
    'version',
    'updatedAt',
    'actions',
  ];

  // Status counts for tabs
  statusCounts = {
    all: 0,
    active: 0,
    draft: 0,
    deprecated: 0,
  };

  // Status styling
  statusConfig: Record<AgentStatus, { color: string; icon: string }> = {
    DRAFT: { color: 'accent', icon: 'edit' },
    TESTING: { color: 'primary', icon: 'science' },
    ACTIVE: { color: 'primary', icon: 'check_circle' },
    DEPRECATED: { color: 'warn', icon: 'warning' },
    ARCHIVED: { color: 'default', icon: 'archive' },
  };

  // Button loading states
  publishLoading = false;
  deleteLoading = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private agentService: AgentBuilderService,
    private toast: ToastService,
    private dialogService: DialogService
  ) {}

  ngOnInit(): void {
    this.loadAgents();
    this.loadMetadata();
    this.setupSearch();

    // Check if editing existing agent
    const agentId = this.route.snapshot.paramMap.get('id');
    if (agentId) {
      this.openEditDialog(agentId);
    }
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ============================================================================
  // DATA LOADING
  // ============================================================================

  loadAgents(): void {
    this.loading = true;
    const status = this.selectedStatus || undefined;

    this.agentService
      .listAgents(status, this.pageIndex, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          this.agents = page.content;
          this.dataSource.data = page.content;
          this.totalElements = page.totalElements;
          this.loading = false;
          this.updateStatusCounts();
        },
        error: (err) => {
          this.toast.error('Failed to load agents');
          console.error('Error loading agents:', err);
          this.loading = false;
        },
      });
  }

  loadMetadata(): void {
    // Load available tools
    this.agentService
      .getAvailableTools()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (tools) => (this.availableTools = tools),
        error: () => console.warn('Failed to load tools - using cached'),
      });

    // Load providers
    this.agentService
      .getSupportedProviders()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (providers) => (this.providers = providers),
        error: () => console.warn('Failed to load providers - using cached'),
      });

    // Check runtime health
    this.agentService
      .checkRuntimeHealth()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (health) => {
          this.runtimeHealthy = health['status'] === 'UP';
        },
        error: () => {
          this.runtimeHealthy = false;
        },
      });
  }

  setupSearch(): void {
    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((query) => {
        if (query && query.length >= 2) {
          this.searchAgents(query);
        } else {
          this.loadAgents();
        }
      });
  }

  searchAgents(query: string): void {
    this.loading = true;
    this.agentService
      .searchAgents(query, this.pageIndex, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          this.agents = page.content;
          this.dataSource.data = page.content;
          this.totalElements = page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.toast.error('Search failed');
          this.loading = false;
        },
      });
  }

  updateStatusCounts(): void {
    // For now, use simple counts - could be optimized with backend aggregation
    this.statusCounts.all = this.totalElements;
    this.statusCounts.active = this.agents.filter((a) => a.status === 'ACTIVE').length;
    this.statusCounts.draft = this.agents.filter((a) => a.status === 'DRAFT').length;
    this.statusCounts.deprecated = this.agents.filter(
      (a) => a.status === 'DEPRECATED'
    ).length;
  }

  // ============================================================================
  // TABLE ACTIONS
  // ============================================================================

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadAgents();
  }

  onSortChange(sort: Sort): void {
    // Client-side sorting is handled by MatSort
    // For server-side sorting, modify the API call
  }

  onTabChange(index: number): void {
    this.selectedTab = index;
    const statusMap: (AgentStatus | '')[] = ['', 'ACTIVE', 'DRAFT', 'DEPRECATED'];
    this.selectedStatus = statusMap[index];
    this.pageIndex = 0;
    this.loadAgents();
  }

  // Selection methods
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  toggleAllRows(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.dataSource.data.forEach((row) => this.selection.select(row));
    }
  }

  // ============================================================================
  // AGENT OPERATIONS
  // ============================================================================

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(CreateAgentDialogComponent, {
      width: '800px',
      maxWidth: '95vw',
      data: {
        tools: this.availableTools,
        providers: this.providers,
      },
      disableClose: true,
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadAgents();
        this.toast.success('Agent created successfully');
      }
    });
  }

  openEditDialog(agentId: string): void {
    this.agentService.getAgent(agentId).subscribe({
      next: (agent) => {
        const dialogRef = this.dialog.open(CreateAgentDialogComponent, {
          width: '800px',
          maxWidth: '95vw',
          data: {
            agent,
            tools: this.availableTools,
            providers: this.providers,
            isEdit: true,
          },
          disableClose: true,
        });

        dialogRef.afterClosed().subscribe((result) => {
          if (result) {
            this.loadAgents();
            this.toast.success('Agent updated successfully');
          }
        });
      },
      error: () => {
        this.toast.error('Failed to load agent');
        this.router.navigate(['/agent-builder']);
      },
    });
  }

  openTestDialog(agent: AgentConfiguration): void {
    this.dialog.open(TestAgentDialogComponent, {
      width: '900px',
      maxWidth: '95vw',
      height: '80vh',
      data: { agent },
    });
  }

  cloneAgent(agent: AgentConfiguration): void {
    const newName = `${agent.name} (Copy)`;
    this.agentService.cloneAgent(agent.id, newName).subscribe({
      next: () => {
        this.loadAgents();
        this.toast.success('Agent cloned successfully');
      },
      error: () => {
        this.toast.error('Failed to clone agent');
      },
    });
  }

  publishAgent(agent: AgentConfiguration): void {
    this.publishLoading = true;
    this.agentService.publishAgent(agent.id).subscribe({
      next: () => {
        this.loadAgents();
        this.toast.success('Agent published successfully');
        this.publishLoading = false;
      },
      error: () => {
        this.toast.error('Failed to publish agent');
        this.publishLoading = false;
      },
    });
  }

  deprecateAgent(agent: AgentConfiguration): void {
    this.agentService.deprecateAgent(agent.id).subscribe({
      next: () => {
        this.loadAgents();
        this.toast.success('Agent deprecated');
      },
      error: () => {
        this.toast.error('Failed to deprecate agent');
      },
    });
  }

  deleteAgent(agent: AgentConfiguration): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Agent',
        message: `Are you sure you want to delete "${agent.name}"? This action cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
        confirmColor: 'warn',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.deleteLoading = true;
        this.agentService.deleteAgent(agent.id).subscribe({
          next: () => {
            this.loadAgents();
            this.toast.success('Agent deleted');
            this.deleteLoading = false;
          },
          error: () => {
            this.toast.error('Failed to delete agent');
            this.deleteLoading = false;
          },
        });
      }
    });
  }

  viewVersions(agent: AgentConfiguration): void {
    // Navigate to versions view or open dialog
    this.router.navigate(['/agent-builder', agent.id], {
      queryParams: { tab: 'versions' },
    });
  }

  // ============================================================================
  // BULK OPERATIONS
  // ============================================================================

  publishSelected(): void {
    const selected = this.selection.selected.filter((a) => a.status === 'DRAFT');
    if (selected.length === 0) {
      this.toast.info('No draft agents selected for publishing');
      return;
    }

    // Publish sequentially (could be parallelized with forkJoin)
    this.publishLoading = true;
    let completed = 0;

    selected.forEach((agent) => {
      this.agentService.publishAgent(agent.id).subscribe({
        next: () => {
          completed++;
          if (completed === selected.length) {
            this.publishLoading = false;
            this.selection.clear();
            this.loadAgents();
            this.toast.success(`Published ${selected.length} agent(s)`);
          }
        },
        error: () => {
          completed++;
          if (completed === selected.length) {
            this.publishLoading = false;
            this.loadAgents();
          }
        },
      });
    });
  }

  deleteSelected(): void {
    const selected = this.selection.selected;
    if (selected.length === 0) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Agents',
        message: `Are you sure you want to delete ${selected.length} agent(s)? This action cannot be undone.`,
        confirmText: 'Delete All',
        cancelText: 'Cancel',
        confirmColor: 'warn',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.deleteLoading = true;
        let completed = 0;

        selected.forEach((agent) => {
          this.agentService.deleteAgent(agent.id).subscribe({
            complete: () => {
              completed++;
              if (completed === selected.length) {
                this.deleteLoading = false;
                this.selection.clear();
                this.loadAgents();
                this.toast.success(`Deleted ${selected.length} agent(s)`);
              }
            },
          });
        });
      }
    });
  }

  // ============================================================================
  // UTILITY METHODS
  // ============================================================================

  getStatusColor(status: AgentStatus): string {
    return this.statusConfig[status]?.color || 'default';
  }

  getStatusIcon(status: AgentStatus): string {
    return this.statusConfig[status]?.icon || 'help';
  }

  formatDate(dateString?: string): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }

  getProviderIcon(provider: string): string {
    const icons: Record<string, string> = {
      claude: 'smart_toy',
      'azure-openai': 'cloud',
      bedrock: 'dns',
    };
    return icons[provider] || 'psychology';
  }
}
