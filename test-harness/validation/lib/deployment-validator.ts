import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

export class DeploymentValidator {
  async checkDockerServices(): Promise<Record<string, string>> {
    try {
      const { stdout } = await execAsync('docker ps --format "{{.Names}}\t{{.State}}"');

      const services: Record<string, string> = {};
      const lines = stdout.trim().split('\n');

      for (const line of lines) {
        const [name, state] = line.split('\t');
        services[name] = state;
      }

      return services;
    } catch (error: any) {
      console.error('Error checking Docker services:', error.message);
      return {};
    }
  }

  async checkServiceHealth(serviceName: string): Promise<boolean> {
    try {
      const { stdout } = await execAsync(`docker inspect --format='{{.State.Health.Status}}' ${serviceName}`);
      return stdout.trim() === 'healthy';
    } catch {
      return false;
    }
  }

  async getServiceLogs(serviceName: string, lines: number = 100): Promise<string> {
    try {
      const { stdout } = await execAsync(`docker logs --tail ${lines} ${serviceName}`);
      return stdout;
    } catch (error: any) {
      return `Error retrieving logs: ${error.message}`;
    }
  }
}
