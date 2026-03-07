# History Cleanup Plan (Large Objects)

**Date:** 2026-03-07  
**Target Repo:** `mahoosuc-solutions/hdim`

## 1) Why cleanup is needed

Migration push succeeded, but GitHub flagged large files in history.

Top large blobs found:

- `backend/modules/services/cms-connector-service/target/cms-connector-service-1.0.0.jar` (~61.7 MB)
- `backend/.gradle/8.11.1/executionHistory/executionHistory.bin` (~28.4 MB)
- Multiple media files under `landing-page-v0/public/videos/*.mp4` (~16-23 MB each)

## 2) Recommended cleanup policy

### Required removal (build artifacts)

Remove build artifacts from *history*:

- `**/target/**`
- `backend/.gradle/**`
- `**/build/**` (optional; evaluate before use)

### Optional optimization (media)

For `landing-page-v0/public/videos/*.mp4`:

- Option A: keep in git (simpler, larger repo)
- Option B: migrate to Git LFS (leaner clone/pull footprint)

## 3) Safe execution sequence

1. Mirror-clone the destination repo to a temp path.
2. Rewrite history locally.
3. Validate rewritten history and size.
4. Force-push with lease.
5. Communicate reset instructions to collaborators.

## 4) Commands (build artifact removal)

```bash
git clone --mirror https://github.com/mahoosuc-solutions/hdim.git /tmp/hdim-clean.git
cd /tmp/hdim-clean.git

# Requires git-filter-repo installed
git filter-repo \
  --path-glob '**/target/**' \
  --path 'backend/.gradle' \
  --invert-paths

git push --force --all
git push --force --tags
```

## 5) Optional commands (LFS for videos)

```bash
git clone https://github.com/mahoosuc-solutions/hdim.git /tmp/hdim-lfs
cd /tmp/hdim-lfs

git lfs install
git lfs track 'landing-page-v0/public/videos/*.mp4'
git add .gitattributes
git commit -m 'Track landing-page videos in Git LFS'
git push
```

## 6) Post-cleanup validation

- `git rev-list --objects --all | git cat-file --batch-check ...` shows no
  `target/` or `.gradle` blobs.
- GitHub no longer reports large-file warnings for removed artifacts.
- `.gitignore` still includes `target/` and `**/target/` (already applied).

