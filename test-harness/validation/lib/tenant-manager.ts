import { DatabaseHelper } from './database-helper';
import { randomUUID } from 'crypto';

export interface TenantConfig {
  name: string;
  type: 'hospital' | 'clinic' | 'payer' | 'provider_practice';
  config?: any;
}

export class TenantManager {
  private dbHelper: DatabaseHelper;
  private testTenantIds: Set<string>;

  constructor(dbHelper?: DatabaseHelper) {
    this.dbHelper = dbHelper || new DatabaseHelper({
      host: process.env.DB_HOST || 'localhost',
      port: parseInt(process.env.DB_PORT || '5432'),
      database: process.env.DB_NAME || 'hdim',
      user: process.env.DB_USER || 'hdim_user',
      password: process.env.DB_PASSWORD || '',
    });
    this.testTenantIds = new Set();
  }

  async createTestTenant(config: TenantConfig): Promise<any> {
    await this.dbHelper.connect();

    const tenantId = randomUUID();
    const tenant = {
      id: tenantId,
      name: config.name,
      type: config.type,
      config: config.config || {},
      created_at: new Date(),
      updated_at: new Date(),
    };

    // Check if tenants table exists
    const tableExists = await this.dbHelper.tableExists('tenants');

    if (tableExists) {
      await this.dbHelper.queryAsSystem(
        `INSERT INTO tenants (id, name, type, config, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, $6)`,
        [tenant.id, tenant.name, tenant.type, JSON.stringify(tenant.config), tenant.created_at, tenant.updated_at]
      );
    } else {
      // If tenants table doesn't exist, just track the tenant in memory
      console.warn('Warning: tenants table does not exist. Tenant will be tracked in memory only.');
    }

    this.testTenantIds.add(tenantId);
    return tenant;
  }

  async listTenants(): Promise<any[]> {
    await this.dbHelper.connect();

    const tableExists = await this.dbHelper.tableExists('tenants');

    if (!tableExists) {
      return [];
    }

    const result = await this.dbHelper.queryAsSystem(
      'SELECT * FROM tenants ORDER BY created_at DESC'
    );

    return result.rows;
  }

  async deleteTestTenant(tenantId: string): Promise<void> {
    await this.dbHelper.connect();

    const tableExists = await this.dbHelper.tableExists('tenants');

    if (tableExists) {
      // Delete all tenant data (cascading deletes should handle related records)
      await this.dbHelper.queryAsSystem(
        'DELETE FROM tenants WHERE id = $1',
        [tenantId]
      );
    }

    this.testTenantIds.delete(tenantId);
  }

  async deleteTenant(tenantId: string): Promise<void> {
    return this.deleteTestTenant(tenantId);
  }

  async cleanupAllTestTenants(): Promise<void> {
    for (const tenantId of this.testTenantIds) {
      await this.deleteTestTenant(tenantId);
    }
  }
}
