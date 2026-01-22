# Password Standardization Recommendation

## Current State
A review of the repository reveals inconsistent password usage for demo environments:
- `docker-compose.demo.yml` uses `demo_password_2024`.
- `backend/modules/services/quality-measure-service/src/main/resources/application-demo.yml` was upgraded to `demo_password_2024`.
- `demo/docker-compose.demo.yml` and `demo/init-infrastructure.sh` still default to `demo_password_123`.
- Documentation (`DEMO_IMPLEMENTATION_AND_SERVICE_TIERS.md`) references `demo_password_123`.

## Issues caused by Inconsistency
- **Connection Failures**: Services configured with the new password fail to connect to databases initialized with the old password (as seen with `quality-measure-service`).
- **Confusion**: Developers may not know which password to use for manual database access.

## Recommendation

### 1. Adopt `demo_password_2024` as the Standard
Move all demo configurations to `demo_password_2024` to ensure consistency with the root `docker-compose.demo.yml`.

### 2. Update Documentation
Update all markdown files to reference the new password standard.

### 3. Update Scripts
Update `demo/*.sh` scripts to default to `demo_password_2024`.

### 4. Centralize Configuration
Where possible, rely on the `POSTGRES_PASSWORD` environment variable rather than hardcoding values in `application.yml` files.

## Updated Plan
1.  [x] Update `quality-measure-service` configuration (Completed).
2.  [ ] Update `demo/docker-compose.demo.yml` and `demo/.env`.
3.  [ ] Update `demo/init-infrastructure.sh` and `demo/init-demo-users.sh`.
4.  [ ] grep for `demo_password_123` and replace with `demo_password_2024`.
