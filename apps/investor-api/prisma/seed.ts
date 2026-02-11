/**
 * Database Seed Script
 *
 * Seeds the investor dashboard with:
 * - Admin users (aaron@mahoosuc.solutions)
 * - Initial 23 investor tasks (from Liquibase data)
 *
 * Run with: npx tsx prisma/seed.ts
 */

import { config } from 'dotenv';
import { PrismaClient } from '@prisma/client';
import { PrismaPg } from '@prisma/adapter-pg';
import bcrypt from 'bcryptjs';

// Load environment variables
config({ path: '.env.local' });
config({ path: '.env' });

// Create PostgreSQL adapter for Prisma 7
const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL });
const prisma = new PrismaClient({ adapter });

async function main() {
  console.log('🌱 Starting database seed...\n');

  // ============================================
  // Seed Users
  // ============================================
  console.log('Creating users...');

  const passwordHash = await bcrypt.hash('investor2026!', 10);
  console.log('  Generated hash:', passwordHash);

  const adminUser = await prisma.user.upsert({
    where: { email: 'aaron@mahoosuc.solutions' },
    update: {
      passwordHash, // Update password hash to ensure bcryptjs compatibility
    },
    create: {
      email: 'aaron@mahoosuc.solutions',
      passwordHash,
      firstName: 'Aaron',
      lastName: 'Wilder',
      role: 'ADMIN',
      active: true,
    },
  });

  console.log(`  ✓ Created admin user: ${adminUser.email}`);

  // ============================================
  // Seed Tasks (from Liquibase initial data)
  // ============================================
  console.log('\nCreating investor tasks...');

  const tasks = [
    // Week 1 Tasks
    {
      subject: 'Create 3-year financial model',
      description: 'ARR projections, SaaS metrics, cash flow',
      status: 'completed',
      category: 'Financial',
      week: 1,
      deliverable: 'docs/investor/financial-model.md',
      sortOrder: 1,
    },
    {
      subject: 'Verify demo stack runs smoothly',
      description: 'Demo verification, nginx fix',
      status: 'completed',
      category: 'Technical',
      week: 1,
      sortOrder: 2,
    },
    {
      subject: 'Draft 4 LinkedIn posts',
      description: 'Thought leadership content',
      status: 'completed',
      category: 'Marketing',
      week: 1,
      deliverable: 'docs/investor/linkedin-posts.md',
      sortOrder: 3,
    },
    {
      subject: 'Request 5 warm intros',
      description: 'Use outreach-templates.md',
      status: 'in_progress',
      category: 'Admin',
      week: 1,
      sortOrder: 4,
    },
    {
      subject: 'Search 2nd-degree VC connections',
      description: 'LinkedIn search for mutual connections',
      status: 'in_progress',
      category: 'Admin',
      week: 1,
      sortOrder: 5,
    },
    {
      subject: 'Build investor spreadsheet (50 VCs)',
      description: 'Healthcare VCs with intro paths',
      status: 'completed',
      category: 'Legal',
      week: 1,
      deliverable: 'docs/investor/investor-target-list.md',
      sortOrder: 6,
    },
    {
      subject: 'Send first 20 LinkedIn requests',
      description: 'Quality leaders #1-20',
      status: 'in_progress',
      category: 'Admin',
      week: 1,
      sortOrder: 7,
    },
    {
      subject: 'Draft customer outreach templates',
      description: 'Cold/warm email templates',
      status: 'completed',
      category: 'Marketing',
      week: 1,
      deliverable: 'docs/investor/outreach-templates.md',
      sortOrder: 8,
    },
    {
      subject: 'Find 100 quality leader profiles',
      description: 'LinkedIn contacts at target orgs',
      status: 'completed',
      category: 'Marketing',
      week: 1,
      deliverable: 'docs/investor/quality-leader-profiles.md',
      sortOrder: 9,
    },
    {
      subject: 'Build customer target list (50)',
      description: 'Health systems and ACOs',
      status: 'completed',
      category: 'Marketing',
      week: 1,
      deliverable: 'docs/investor/customer-target-list.md',
      sortOrder: 10,
    },
    {
      subject: 'Clean up cap table document',
      description: 'Ownership tracking template',
      status: 'completed',
      category: 'Legal',
      week: 1,
      deliverable: 'docs/investor/cap-table.md',
      sortOrder: 11,
    },

    // Week 2 Tasks
    {
      subject: 'Create angel outreach list',
      description: '15 angels with personalized angles',
      status: 'completed',
      category: 'Legal',
      week: 2,
      deliverable: 'docs/investor/angel-outreach-list.md',
      sortOrder: 1,
    },
    {
      subject: 'Create monthly investor update template',
      description: 'Email template with metrics',
      status: 'completed',
      category: 'Legal',
      week: 2,
      deliverable: 'docs/investor/monthly-update-template.md',
      sortOrder: 2,
    },
    {
      subject: 'Research healthcare conferences',
      description: 'Events in next 60 days',
      status: 'completed',
      category: 'Governance',
      week: 2,
      deliverable: 'docs/investor/healthcare-conferences.md',
      sortOrder: 3,
    },
    {
      subject: 'Create accelerator application list',
      description: 'YC, Techstars, Rock Health',
      status: 'completed',
      category: 'Governance',
      week: 2,
      deliverable: 'docs/investor/accelerator-applications.md',
      sortOrder: 4,
    },
    {
      subject: 'Identify healthcare conferences to attend',
      description: 'Register for HIMSS',
      status: 'pending',
      category: 'Admin',
      week: 2,
      sortOrder: 5,
    },
    {
      subject: 'Start Y Combinator S26 application',
      description: 'Begin at ycombinator.com/apply',
      status: 'pending',
      category: 'Governance',
      week: 2,
      sortOrder: 6,
    },

    // Week 3 Tasks
    {
      subject: 'Schedule 3 customer discovery calls',
      description: 'Quality leaders from target list',
      status: 'pending',
      category: 'Marketing',
      week: 3,
      sortOrder: 1,
    },
    {
      subject: 'Send follow-up LinkedIn messages',
      description: 'Week 1 connections who accepted',
      status: 'pending',
      category: 'Admin',
      week: 3,
      sortOrder: 2,
    },
    {
      subject: 'Draft pitch deck outline',
      description: '12-slide investor presentation',
      status: 'pending',
      category: 'Legal',
      week: 3,
      sortOrder: 3,
    },

    // Week 4 Tasks
    {
      subject: 'Complete pitch deck draft',
      description: 'Full deck with design',
      status: 'pending',
      category: 'Legal',
      week: 4,
      deliverable: 'docs/investor/pitch-deck.md',
      sortOrder: 1,
    },
    {
      subject: 'Request feedback on pitch deck',
      description: 'Send to 3 trusted advisors',
      status: 'pending',
      category: 'Admin',
      week: 4,
      sortOrder: 2,
    },
    {
      subject: 'Schedule first investor meetings',
      description: 'Target 2-3 meetings from warm intros',
      status: 'pending',
      category: 'Admin',
      week: 4,
      sortOrder: 3,
    },
  ];

  let createdCount = 0;
  for (const task of tasks) {
    // Check if task already exists by subject
    const existing = await prisma.task.findFirst({
      where: { subject: task.subject },
    });

    if (!existing) {
      await prisma.task.create({ data: task });
      createdCount++;
    }
  }

  console.log(`  ✓ Created ${createdCount} new tasks (${tasks.length - createdCount} already existed)`);

  // ============================================
  // Summary
  // ============================================
  const [userCount, taskCount] = await Promise.all([
    prisma.user.count(),
    prisma.task.count(),
  ]);

  console.log('\n📊 Database summary:');
  console.log(`  Users: ${userCount}`);
  console.log(`  Tasks: ${taskCount}`);
  console.log('\n✅ Seed completed successfully!');
}

main()
  .catch((e) => {
    console.error('❌ Seed failed:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
