import { Pool, PoolClient, QueryResult } from 'pg';

export interface DatabaseConfig {
  host: string;
  port: number;
  database: string;
  user: string;
  password: string;
}

export class DatabaseHelper {
  private pool: Pool | null = null;
  private config: DatabaseConfig;

  constructor(config: DatabaseConfig) {
    this.config = config;
  }

  async connect(): Promise<void> {
    if (!this.pool) {
      this.pool = new Pool(this.config);
    }
  }

  async disconnect(): Promise<void> {
    if (this.pool) {
      await this.pool.end();
      this.pool = null;
    }
  }

  async checkHealth(): Promise<any> {
    const startTime = Date.now();
    const result = await this.queryAsSystem('SELECT version() as version');
    const endTime = Date.now();

    return {
      connected: true,
      responseTime: endTime - startTime,
      version: result.rows[0].version,
    };
  }

  async getSchemas(): Promise<string[]> {
    const result = await this.queryAsSystem(
      `SELECT schema_name FROM information_schema.schemata
       WHERE schema_name NOT IN ('pg_catalog', 'information_schema')`
    );

    return result.rows.map((r: any) => r.schema_name);
  }

  async getTableColumns(tableName: string): Promise<any[]> {
    const [schema, table] = tableName.includes('.')
      ? tableName.split('.')
      : ['public', tableName];

    const result = await this.queryAsSystem(
      `SELECT column_name, data_type, is_nullable
       FROM information_schema.columns
       WHERE table_schema = $1 AND table_name = $2`,
      [schema, table]
    );

    return result.rows;
  }

  async getForeignKeyConstraints(): Promise<any[]> {
    const result = await this.queryAsSystem(`
      SELECT
        tc.table_schema || '.' || tc.table_name as table_name,
        kcu.column_name,
        ccu.table_schema || '.' || ccu.table_name AS foreign_table_name,
        ccu.column_name AS foreign_column_name
      FROM information_schema.table_constraints AS tc
      JOIN information_schema.key_column_usage AS kcu
        ON tc.constraint_name = kcu.constraint_name
        AND tc.table_schema = kcu.table_schema
      JOIN information_schema.constraint_column_usage AS ccu
        ON ccu.constraint_name = tc.constraint_name
        AND ccu.table_schema = tc.table_schema
      WHERE tc.constraint_type = 'FOREIGN KEY'
    `);

    return result.rows;
  }

  async getRLSPolicies(): Promise<any[]> {
    const result = await this.queryAsSystem(
      `SELECT schemaname, tablename, policyname, permissive, cmd
       FROM pg_policies
       ORDER BY tablename, policyname`
    );

    return result.rows;
  }

  async getIndexes(): Promise<any[]> {
    const result = await this.queryAsSystem(
      `SELECT schemaname, tablename, indexname, indexdef
       FROM pg_indexes
       WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
       ORDER BY tablename, indexname`
    );

    return result.rows;
  }

  async getJSONBIndexes(): Promise<any[]> {
    const result = await this.queryAsSystem(
      `SELECT schemaname, tablename, indexname, indexdef
       FROM pg_indexes
       WHERE indexdef LIKE '%gin%'
       ORDER BY tablename, indexname`
    );

    return result.rows;
  }

  async queryAsSystem(query: string, params: any[] = []): Promise<QueryResult> {
    if (!this.pool) {
      await this.connect();
    }

    return this.pool!.query(query, params);
  }

  async queryAsTenant(tenantId: string, query: string, params: any[] = []): Promise<QueryResult> {
    if (!this.pool) {
      await this.connect();
    }

    const client: PoolClient = await this.pool!.connect();
    try {
      // Set tenant context for row-level security
      await client.query('SET app.current_tenant_id = $1', [tenantId]);
      return await client.query(query, params);
    } finally {
      client.release();
    }
  }

  async insertAsTenant(tenantId: string, table: string, data: any): Promise<QueryResult> {
    const columns = Object.keys(data);
    const values = Object.values(data);
    const placeholders = values.map((_, i) => `$${i + 1}`).join(', ');

    const query = `INSERT INTO ${table} (${columns.join(', ')}) VALUES (${placeholders}) RETURNING *`;

    return this.queryAsTenant(tenantId, query, values);
  }

  async getAppliedMigrations(): Promise<any[]> {
    try {
      const result = await this.queryAsSystem(
        `SELECT * FROM schema_migrations ORDER BY version`
      );
      return result.rows;
    } catch (error: any) {
      // If schema_migrations table doesn't exist, return empty array
      if (error.code === '42P01') {
        return [];
      }
      throw error;
    }
  }

  async tableExists(tableName: string): Promise<boolean> {
    const [schema, table] = tableName.includes('.')
      ? tableName.split('.')
      : ['public', tableName];

    const result = await this.queryAsSystem(
      `SELECT EXISTS (
        SELECT FROM information_schema.tables
        WHERE table_schema = $1 AND table_name = $2
      ) as exists`,
      [schema, table]
    );

    return result.rows[0].exists;
  }
}
