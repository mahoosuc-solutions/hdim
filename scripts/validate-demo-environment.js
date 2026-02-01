/**
 * Demo Environment Validation Script
 * Validates all services are healthy and demo data is properly seeded
 */

const http = require('http');
const crypto = require('crypto');
const { execSync } = require('child_process');

const CONFIG = {
  timeout: 10000,
  retries: 3,
  retryDelay: 2000,
};

const TENANT_ID = process.env.TENANT_ID || 'acme-health';
const DEMO_USER_ID = process.env.AUTH_USER_ID || '550e8400-e29b-41d4-a716-446655440010';
const DEMO_USERNAME = process.env.AUTH_USERNAME || 'demo_admin@hdim.ai';
const DEMO_ROLES = process.env.AUTH_ROLES || 'ADMIN,EVALUATOR';
const SIGNING_SECRET = process.env.GATEWAY_AUTH_SIGNING_SECRET || '2J3YcbuHgPp0xyMq3GHYJ/MNolqB+Hvp4j/fsA6LQYM=';
const ENABLE_DEMO_SEEDING = process.env.ENABLE_DEMO_SEEDING === 'true';

function buildAuthHeaders() {
  const validatedTs = Math.floor(Date.now() / 1000);
  const hmac = crypto
    .createHmac('sha256', SIGNING_SECRET)
    .update(`${DEMO_USER_ID}:${validatedTs}`)
    .digest('base64');
  return {
    'X-Tenant-ID': TENANT_ID,
    'X-Auth-User-Id': DEMO_USER_ID,
    'X-Auth-Username': DEMO_USERNAME,
    'X-Auth-Roles': DEMO_ROLES,
    'X-Auth-Tenant-Ids': TENANT_ID,
    'X-Auth-Validated': `gateway-${validatedTs}-${hmac}`,
  };
}

// Service definitions
const SERVICES = {
  infrastructure: [
    { name: 'PostgreSQL', check: () => checkDockerHealth('hdim-demo-postgres', 'pg_isready -U healthdata') },
    { name: 'Redis', check: () => checkDockerHealth('hdim-demo-redis', 'redis-cli ping') },
    { name: 'Kafka', check: () => checkDockerHealth('hdim-demo-kafka', 'kafka-broker-api-versions --bootstrap-server localhost:29092') },
    { name: 'Jaeger', check: () => checkHttpHealth('http://localhost:16686') },
  ],
  backend: [
    { name: 'FHIR Service', check: () => checkHttpHealth('http://localhost:8085/fhir/actuator/health') },
    { name: 'CQL Engine', check: () => checkHttpHealth('http://localhost:8081/cql-engine/actuator/health') },
    { name: 'Patient Service', check: () => checkHttpHealth('http://localhost:8084/patient/actuator/health') },
    { name: 'Quality Measure', check: () => checkHttpHealth('http://localhost:8087/quality-measure/actuator/health') },
    { name: 'Care Gap Service', check: () => checkHttpHealth('http://localhost:8086/care-gap/actuator/health') },
    { name: 'Event Processing', check: () => checkHttpHealth('http://localhost:8083/events/actuator/health') },
  ],
  gateway: [
    { name: 'Gateway Edge', check: () => checkHttpHealth('http://localhost:18080/actuator/health') },
    { name: 'Gateway Admin', check: () => checkDockerHealth('hdim-demo-gateway-admin', 'wget --spider -q http://localhost:8080/actuator/health') },
  ],
  frontend: [
    { name: 'Clinical Portal', check: () => checkHttpHealth('http://localhost:4200') },
  ],
  demo: [
    { name: 'Demo Seeding Service', check: () => checkHttpHealth('http://localhost:8098/demo/actuator/health') },
  ],
};

// Demo data validation
const DATA_VALIDATION = [
  {
    name: 'Patients',
    check: async () => {
      try {
        const response = await httpRequest(
          'http://localhost:8084/patient/api/v1/patients?page=0&size=10',
          { headers: buildAuthHeaders() }
        );
        const data = JSON.parse(response);
        return data.content && data.content.length > 0;
      } catch (e) {
        return false;
      }
    },
  },
  {
    name: 'Care Gaps',
    check: async () => {
      try {
        const response = await httpRequest(
          'http://localhost:8086/care-gap/api/v1/care-gaps?page=0&size=10',
          { headers: buildAuthHeaders() }
        );
        const data = JSON.parse(response);
        return data.content && data.content.length > 0;
      } catch (e) {
        return false;
      }
    },
  },
  {
    name: 'Quality Measures',
    check: async () => {
      try {
        const response = await httpRequest(
          'http://localhost:8087/quality-measure/measures/local',
          { headers: buildAuthHeaders() }
        );
        const data = JSON.parse(response);
        return Array.isArray(data) && data.length > 0;
      } catch (e) {
        return false;
      }
    },
  },
  {
    name: 'FHIR Resources',
    check: async () => {
      try {
        const response = await httpRequest(
          'http://localhost:8085/fhir/Patient?_count=10',
          { headers: buildAuthHeaders() }
        );
        // FHIR Bundle response
        return response.includes('"resourceType":"Bundle"') && response.includes('"resourceType":"Patient"');
      } catch (e) {
        return false;
      }
    },
  },
];

// Utility functions
function log(message, type = 'info') {
  const colors = {
    info: '\x1b[34m',
    success: '\x1b[32m',
    error: '\x1b[31m',
    warning: '\x1b[33m',
  };
  const reset = '\x1b[0m';
  const icon = type === 'success' ? '✓' : type === 'error' ? '✗' : type === 'warning' ? '⚠' : 'ℹ';
  console.log(`${colors[type]}${icon}${reset} ${message}`);
}

function checkDockerHealth(container, command) {
  try {
    execSync(`docker exec ${container} ${command}`, { 
      encoding: 'utf8',
      timeout: 5000,
      stdio: 'pipe'
    });
    return true;
  } catch (e) {
    return false;
  }
}

function checkHttpHealth(url) {
  return new Promise((resolve) => {
    const req = http.get(url, { timeout: CONFIG.timeout }, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        if (res.statusCode === 200) {
          try {
            const json = JSON.parse(data);
            resolve(json.status === 'UP' || json.status === 'healthy');
          } catch (e) {
            resolve(res.statusCode === 200);
          }
        } else {
          resolve(false);
        }
      });
    });
    
    req.on('error', () => resolve(false));
    req.on('timeout', () => {
      req.destroy();
      resolve(false);
    });
  });
}

function httpRequest(url, options = {}) {
  return new Promise((resolve, reject) => {
    const requestOptions = {
      method: options.method || 'GET',
      headers: options.headers || {},
      timeout: CONFIG.timeout,
    };
    const req = http.request(url, requestOptions, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(data);
        } else {
          reject(new Error(`HTTP ${res.statusCode}`));
        }
      });
    });
    
    req.on('error', reject);
    req.on('timeout', () => {
      req.destroy();
      reject(new Error('Timeout'));
    });
    req.end();
  });
}

async function checkService(service) {
  for (let i = 0; i < CONFIG.retries; i++) {
    const result = await service.check();
    if (result) {
      return true;
    }
    if (i < CONFIG.retries - 1) {
      await new Promise(resolve => setTimeout(resolve, CONFIG.retryDelay));
    }
  }
  return false;
}

async function validateServices() {
  log('\n========================================', 'info');
  log('HDIM Demo Environment Validation', 'info');
  log('========================================\n', 'info');

  let allPassed = true;

  // Infrastructure Services
  log('Infrastructure Services:', 'info');
  for (const service of SERVICES.infrastructure) {
    const passed = await checkService(service);
    if (passed) {
      log(`  ${service.name}`, 'success');
    } else {
      log(`  ${service.name}`, 'error');
      allPassed = false;
    }
  }

  // Backend Services
  log('\nBackend Services:', 'info');
  for (const service of SERVICES.backend) {
    const passed = await checkService(service);
    if (passed) {
      log(`  ${service.name}`, 'success');
    } else {
      log(`  ${service.name}`, 'error');
      allPassed = false;
    }
  }

  // Gateway Services
  log('\nGateway Services:', 'info');
  for (const service of SERVICES.gateway) {
    const passed = await checkService(service);
    if (passed) {
      log(`  ${service.name}`, 'success');
    } else {
      log(`  ${service.name}`, 'error');
      allPassed = false;
    }
  }

  // Frontend
  log('\nFrontend:', 'info');
  for (const service of SERVICES.frontend) {
    const passed = await checkService(service);
    if (passed) {
      log(`  ${service.name}`, 'success');
    } else {
      log(`  ${service.name}`, 'error');
      allPassed = false;
    }
  }

  // Demo Services
  log('\nDemo Services:', 'info');
  for (const service of SERVICES.demo) {
    const passed = await checkService(service);
    if (passed) {
      log(`  ${service.name}`, 'success');
    } else {
      const level = ENABLE_DEMO_SEEDING ? 'error' : 'warning';
      log(`  ${service.name}`, level);
      if (ENABLE_DEMO_SEEDING) {
        allPassed = false;
      }
    }
  }

  return allPassed;
}

async function validateDemoData() {
  log('\n========================================', 'info');
  log('Demo Data Validation', 'info');
  log('========================================\n', 'info');

  let allPassed = true;

  for (const validation of DATA_VALIDATION) {
    const passed = await validation.check();
    if (passed) {
      log(`  ${validation.name} - Data present`, 'success');
    } else {
      log(`  ${validation.name} - No data found`, 'error');
      allPassed = false;
    }
  }

  return allPassed;
}

async function seedDemoData() {
  if (!ENABLE_DEMO_SEEDING) {
    log('Demo seeding disabled (set ENABLE_DEMO_SEEDING=true to enable).', 'warning');
    return true;
  }

  log('\n========================================', 'info');
  log('Seeding Demo Data', 'info');
  log('========================================\n', 'info');

  try {
    const response = await httpRequest(
      'http://localhost:8098/demo/api/v1/demo/scenarios/hedis-evaluation',
      { method: 'POST', headers: buildAuthHeaders() }
    );
    const data = JSON.parse(response);
    
    if (data.status === 'success' || data.message) {
      log('Demo scenario loaded successfully', 'success');
      if (data.patientsCreated) {
        log(`  Patients created: ${data.patientsCreated}`, 'info');
      }
      if (data.careGapsCreated) {
        log(`  Care gaps created: ${data.careGapsCreated}`, 'info');
      }
      return true;
    } else {
      log('Demo scenario may already be loaded', 'warning');
      return true;
    }
  } catch (e) {
    log(`Failed to seed demo data: ${e.message}`, 'error');
    return false;
  }
}

async function main() {
  // Step 1: Validate services
  const servicesValid = await validateServices();
  
  if (!servicesValid) {
    log('\n❌ Some services are not healthy. Please check service logs.', 'error');
    process.exit(1);
  }

  // Step 2: Seed demo data
  const seeded = await seedDemoData();
  if (!seeded) {
    log('\n⚠️  Demo data seeding failed, but continuing...', 'warning');
  }

  // Step 3: Wait for data to propagate
  log('\nWaiting for data to propagate (10 seconds)...', 'info');
  await new Promise(resolve => setTimeout(resolve, 10000));

  // Step 4: Validate demo data
  const dataValid = await validateDemoData();

  log('\n========================================', 'info');
  if (servicesValid && dataValid) {
    log('✅ Environment validation complete!', 'success');
    log('All services are healthy and demo data is present.', 'success');
    log('Ready for screenshot capture.', 'success');
    process.exit(0);
  } else {
    log('⚠️  Environment validation completed with warnings', 'warning');
    if (!dataValid) {
      log('Demo data may not be fully seeded. Screenshots may show empty pages.', 'warning');
    }
    process.exit(0); // Continue anyway
  }
}

main().catch(error => {
  log(`Fatal error: ${error.message}`, 'error');
  process.exit(1);
});
