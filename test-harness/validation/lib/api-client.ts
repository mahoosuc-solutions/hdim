import axios, { AxiosInstance, AxiosResponse } from 'axios';

export class APIClient {
  private client: AxiosInstance;
  private tenantId: string | null = null;

  constructor(baseURL: string) {
    this.client = axios.create({
      baseURL,
      validateStatus: () => true, // Don't throw on any status code
      timeout: 30000, // 30 second timeout
    });
  }

  setTenantContext(tenantId: string): void {
    this.tenantId = tenantId;
  }

  private getHeaders(): any {
    const headers: any = {
      'Content-Type': 'application/json',
    };

    if (this.tenantId) {
      headers['X-Tenant-ID'] = this.tenantId;
    }

    return headers;
  }

  async get(path: string, params?: any): Promise<AxiosResponse> {
    try {
      return await this.client.get(path, {
        headers: this.getHeaders(),
        params,
      });
    } catch (error: any) {
      // Return error as response for consistent handling
      return {
        status: error.response?.status || 500,
        statusText: error.response?.statusText || 'Internal Server Error',
        data: error.response?.data || { error: error.message },
        headers: error.response?.headers || {},
        config: error.config,
      } as AxiosResponse;
    }
  }

  async post(path: string, data: any): Promise<AxiosResponse> {
    try {
      return await this.client.post(path, data, {
        headers: this.getHeaders(),
      });
    } catch (error: any) {
      return {
        status: error.response?.status || 500,
        statusText: error.response?.statusText || 'Internal Server Error',
        data: error.response?.data || { error: error.message },
        headers: error.response?.headers || {},
        config: error.config,
      } as AxiosResponse;
    }
  }

  async put(path: string, data: any): Promise<AxiosResponse> {
    try {
      return await this.client.put(path, data, {
        headers: this.getHeaders(),
      });
    } catch (error: any) {
      return {
        status: error.response?.status || 500,
        statusText: error.response?.statusText || 'Internal Server Error',
        data: error.response?.data || { error: error.message },
        headers: error.response?.headers || {},
        config: error.config,
      } as AxiosResponse;
    }
  }

  async delete(path: string): Promise<AxiosResponse> {
    try {
      return await this.client.delete(path, {
        headers: this.getHeaders(),
      });
    } catch (error: any) {
      return {
        status: error.response?.status || 500,
        statusText: error.response?.statusText || 'Internal Server Error',
        data: error.response?.data || { error: error.message },
        headers: error.response?.headers || {},
        config: error.config,
      } as AxiosResponse;
    }
  }

  async createQualityMeasure(tenantId: string, measure: any): Promise<any> {
    this.setTenantContext(tenantId);
    const response = await this.post('/api/v1/quality-measures', measure);
    return response.data;
  }

  async checkHealth(): Promise<boolean> {
    try {
      const response = await this.get('/health');
      return response.status === 200;
    } catch {
      return false;
    }
  }
}
