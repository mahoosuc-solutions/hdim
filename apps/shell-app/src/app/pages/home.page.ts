import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'health-platform-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="home-container">
      <div class="hero">
        <h1>Welcome to Health Data Platform</h1>
        <p class="subtitle">Modular Micro Frontend Architecture</p>
      </div>

      <div class="features">
        <div class="feature-card">
          <h2>🏥 Patient Management</h2>
          <p>Comprehensive patient data management with real-time updates</p>
          <a routerLink="/mfePatients" class="btn-primary">Access Patients Module</a>
        </div>

        <div class="feature-card">
          <h2>📊 Analytics</h2>
          <p>Advanced analytics and reporting capabilities</p>
          <button class="btn-secondary" disabled>Coming Soon</button>
        </div>

        <div class="feature-card">
          <h2>🔒 Security</h2>
          <p>HIPAA-compliant security with role-based access control</p>
          <button class="btn-secondary" disabled>Coming Soon</button>
        </div>
      </div>

      <div class="architecture-info">
        <h2>Micro Frontend Architecture</h2>
        <ul>
          <li>✅ Module Federation for dynamic loading</li>
          <li>✅ Shared libraries for common functionality</li>
          <li>✅ Independent deployment of each module</li>
          <li>✅ Centralized authentication and tenant management</li>
          <li>🔄 Incremental migration from monolith</li>
        </ul>
      </div>
    </div>
  `,
  styles: [`
    .home-container {
      padding: 2rem 0;
    }

    .hero {
      text-align: center;
      margin-bottom: 3rem;
      padding: 3rem 2rem;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-radius: 8px;
    }

    .hero h1 {
      font-size: 2.5rem;
      margin: 0 0 1rem 0;
    }

    .subtitle {
      font-size: 1.25rem;
      opacity: 0.9;
      margin: 0;
    }

    .features {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 2rem;
      margin-bottom: 3rem;
    }

    .feature-card {
      background: white;
      padding: 2rem;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .feature-card h2 {
      margin: 0;
      font-size: 1.5rem;
      color: #333;
    }

    .feature-card p {
      margin: 0;
      color: #666;
      flex: 1;
    }

    .btn-primary, .btn-secondary {
      padding: 0.75rem 1.5rem;
      border-radius: 4px;
      border: none;
      font-size: 1rem;
      cursor: pointer;
      text-decoration: none;
      display: inline-block;
      text-align: center;
      transition: all 0.2s;
    }

    .btn-primary {
      background: #1976d2;
      color: white;
    }

    .btn-primary:hover {
      background: #1565c0;
    }

    .btn-secondary {
      background: #e0e0e0;
      color: #999;
      cursor: not-allowed;
    }

    .architecture-info {
      background: #f5f5f5;
      padding: 2rem;
      border-radius: 8px;
      border-left: 4px solid #1976d2;
    }

    .architecture-info h2 {
      margin-top: 0;
      color: #1976d2;
    }

    .architecture-info ul {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .architecture-info li {
      padding: 0.5rem 0;
      font-size: 1.1rem;
    }
  `]
})
export class HomePage {}
