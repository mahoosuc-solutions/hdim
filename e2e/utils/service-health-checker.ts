/**
 * Service Health Checker Utility
 * 
 * Provides flexible service health checking with multiple fallback strategies
 * and graceful degradation for test execution.
 */

export interface ServiceHealthResult {
  service: string;
  healthy: boolean;
  url: string;
  status?: number;
  error?: string;
  responseTime?: number;
}

export interface ServiceHealthSummary {
  allHealthy: boolean;
  services: ServiceHealthResult[];
  healthyCount: number;
  totalCount: number;
}

export class ServiceHealthChecker {
  private readonly timeout: number;
  private readonly retries: number;
  private readonly retryDelay: number;

  constructor(options: {
    timeout?: number;
    retries?: number;
    retryDelay?: number;
  } = {}) {
    this.timeout = options.timeout || 10000;
    this.retries = options.retries || 3;
    this.retryDelay = options.retryDelay || 1000;
  }

  async checkService(
    serviceName: string,
    urls: string[],
    options: { required?: boolean } = {}
  ): Promise<ServiceHealthResult> {
    for (const url of urls) {
      for (let attempt = 0; attempt < this.retries; attempt++) {
        const startTime = Date.now();
        try {
          const controller = new AbortController();
          const timeoutId = setTimeout(() => controller.abort(), this.timeout);

          const response = await fetch(url, {
            method: 'GET',
            signal: controller.signal,
            headers: { 'Accept': 'application/json' },
          });

          clearTimeout(timeoutId);
          const responseTime = Date.now() - startTime;

          if (response.ok || response.status === 404) {
            return {
              service: serviceName,
              healthy: true,
              url,
              status: response.status,
              responseTime,
            };
          }
        } catch (error: any) {
          if (attempt < this.retries - 1) {
            await this.delay(this.retryDelay);
            continue;
          }
          if (url === urls[urls.length - 1] && attempt === this.retries - 1) {
            return {
              service: serviceName,
              healthy: false,
              url,
              error: error.message || 'Service not responding',
            };
          }
        }
      }
    }
    return {
      service: serviceName,
      healthy: false,
      url: urls[0],
      error: 'All URLs failed',
    };
  }

  async checkServices(
    services: Array<{ name: string; urls: string[]; required?: boolean }>
  ): Promise<ServiceHealthSummary> {
    const checks = services.map(service =>
      this.checkService(service.name, service.urls, { required: service.required })
    );
    const results = await Promise.all(checks);
    const healthyCount = results.filter(r => r.healthy).length;
    const requiredServices = services.filter(s => s.required !== false);
    const requiredHealthy = results
      .filter(r => requiredServices.some(s => s.name === r.service && s.required !== false))
      .every(r => r.healthy);

    return {
      allHealthy: requiredHealthy,
      services: results,
      healthyCount,
      totalCount: results.length,
    };
  }

  async checkHdimServices(baseUrls: {
    gateway?: string;
    patient?: string;
    qualityMeasure?: string;
    fhir?: string;
    portal?: string;
  }): Promise<ServiceHealthSummary> {
    const services = [
      {
        name: 'Gateway',
        urls: [
          `${baseUrls.gateway || 'http://localhost:8080'}/actuator/health`,
          `${baseUrls.gateway || 'http://localhost:8080'}/health`,
        ],
        required: false,
      },
      {
        name: 'Patient Service',
        urls: [
          `${baseUrls.patient || 'http://localhost:8084'}/patient/actuator/health`,
          `${baseUrls.patient || 'http://localhost:8084'}/actuator/health`,
          `${baseUrls.patient || 'http://localhost:8084'}/health`,
        ],
        required: false,
      },
      {
        name: 'Quality Measure Service',
        urls: [
          `${baseUrls.qualityMeasure || 'http://localhost:8087'}/quality-measure/actuator/health`,
          `${baseUrls.qualityMeasure || 'http://localhost:8087'}/actuator/health`,
          `${baseUrls.qualityMeasure || 'http://localhost:8087'}/health`,
        ],
        required: false,
      },
      {
        name: 'FHIR Service',
        urls: [
          `${baseUrls.fhir || 'http://localhost:8085'}/fhir/metadata`,
          `${baseUrls.fhir || 'http://localhost:8085'}/metadata`,
        ],
        required: false,
      },
      {
        name: 'Clinical Portal',
        urls: [
          `${baseUrls.portal || 'http://localhost:4200'}`,
          `${baseUrls.portal || 'http://localhost:4200'}/index.html`,
        ],
        required: false,
      },
    ];
    return this.checkServices(services);
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
