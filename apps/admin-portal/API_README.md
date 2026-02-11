# Investor Dashboard API

Standalone TypeScript API for the Investor Dashboard, designed for Vercel deployment alongside the Angular frontend.

## Architecture

```
admin-portal/
├── api/                    # Vercel Serverless Functions
│   ├── auth/
│   │   ├── login.ts        # POST /api/auth/login
│   │   ├── refresh.ts      # POST /api/auth/refresh
│   │   └── me.ts           # GET /api/auth/me
│   ├── tasks/
│   │   ├── index.ts        # GET/POST /api/tasks
│   │   └── [id].ts         # GET/PUT/PATCH/DELETE /api/tasks/:id
│   ├── contacts/
│   │   ├── index.ts        # GET/POST /api/contacts
│   │   └── [id].ts         # GET/PUT/PATCH/DELETE /api/contacts/:id
│   ├── activities/
│   │   ├── index.ts        # GET/POST /api/activities
│   │   └── [id].ts         # GET/PUT/DELETE /api/activities/:id
│   └── dashboard/
│       └── stats.ts        # GET /api/dashboard/stats
├── lib/                    # Shared utilities
│   ├── db.ts               # Prisma client singleton
│   ├── auth.ts             # JWT generation/verification
│   ├── middleware.ts       # Auth, CORS, rate limiting
│   └── types.ts            # TypeScript interfaces
├── prisma/
│   ├── schema.prisma       # Database schema
│   └── seed.ts             # Initial data seed
├── vercel.json             # Combined frontend + API config
└── api-package.json        # API-specific dependencies
```

## Setup

### 1. Install Dependencies

The API dependencies are installed automatically by Vercel during deployment. For local development:

```bash
cd apps/admin-portal
npm install @prisma/client @vercel/postgres bcryptjs jsonwebtoken
npm install -D prisma @types/bcryptjs @types/jsonwebtoken
```

### 2. Configure Environment

Copy `.env.example` to `.env.local`:

```bash
cp .env.example .env.local
```

Set the required variables:
- `POSTGRES_PRISMA_URL` - Vercel Postgres connection string
- `POSTGRES_URL_NON_POOLING` - Direct connection (for migrations)
- `JWT_SECRET` - 256-bit secret for JWT signing

### 3. Create Vercel Postgres Database

Via Vercel Dashboard:
1. Go to your project → Storage → Create Database
2. Select Postgres
3. Environment variables are automatically added

Or via CLI:
```bash
vercel storage create postgres investor-db
vercel env pull .env.local
```

### 4. Run Database Migrations

```bash
# Generate Prisma client
npx prisma generate

# Push schema to database
npx prisma db push

# Seed initial data
npx ts-node prisma/seed.ts
```

## Local Development

Start the Vercel dev server (runs both frontend and API):

```bash
vercel dev
```

Or with the Angular dev server (separate terminals):

```bash
# Terminal 1: Angular frontend
npx nx serve admin-portal

# Terminal 2: API (Vercel dev)
cd apps/admin-portal && vercel dev --listen 3000
```

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login with email/password |
| POST | `/api/auth/refresh` | Refresh access token |
| GET | `/api/auth/me` | Get current user info |

### Tasks

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tasks` | List tasks (filter: status, category, week) |
| POST | `/api/tasks` | Create new task |
| GET | `/api/tasks/:id` | Get single task |
| PUT | `/api/tasks/:id` | Update task |
| PATCH | `/api/tasks/:id?status=X` | Update task status |
| DELETE | `/api/tasks/:id` | Delete task |

### Contacts

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/contacts` | List contacts (filter: category, status, tier) |
| POST | `/api/contacts` | Create new contact |
| GET | `/api/contacts/:id` | Get contact with activities |
| PUT | `/api/contacts/:id` | Update contact |
| PATCH | `/api/contacts/:id?status=X` | Update contact status |
| DELETE | `/api/contacts/:id` | Delete contact |

### Activities

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/activities` | List activities (filter: contactId) |
| POST | `/api/activities` | Log new activity |
| GET | `/api/activities/:id` | Get single activity |
| PUT | `/api/activities/:id` | Update activity |
| DELETE | `/api/activities/:id` | Delete activity |

### Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard/stats` | Aggregated dashboard statistics |

## Authentication

All endpoints except `/api/auth/login` and `/api/auth/refresh` require a valid JWT.

Include the token in the Authorization header:
```
Authorization: Bearer <access_token>
```

### Login Example

```bash
curl -X POST https://your-app.vercel.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"aaron@mahoosuc.solutions","password":"investor2026!"}'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "email": "aaron@mahoosuc.solutions",
    "firstName": "Aaron",
    "lastName": "Wilder",
    "role": "ADMIN"
  }
}
```

## Deployment

Deploy to Vercel:

```bash
# From project root
cd apps/admin-portal
vercel --prod
```

Or connect your GitHub repository for automatic deployments.

## Cost Estimate

| Resource | Monthly | Annual |
|----------|---------|--------|
| Vercel Pro (optional) | $20 | $240 |
| Vercel Postgres | $0-15 | $0-180 |
| **Total** | **$20-35** | **$240-420** |

**Savings vs Java Backend: 85-95%**

## Troubleshooting

### "Module not found" errors
Run `npx prisma generate` to generate the Prisma client.

### Database connection errors
Ensure `POSTGRES_PRISMA_URL` is set correctly. For local dev, run `vercel env pull .env.local`.

### CORS errors
The API automatically handles CORS for allowed origins. Check `lib/middleware.ts` for the whitelist.

### Rate limiting
Login is rate-limited to 5 attempts per 15 minutes per IP. Wait or use a different IP.
