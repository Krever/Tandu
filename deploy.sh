#!/usr/bin/env bash
set -euo pipefail

PROJECT_NAME="tandu"
BRANCH="${1:-main}"

cd "$(dirname "$0")"

echo "==> Building production bundle"
npm run build

echo "==> Deploying dist/ to Cloudflare Pages (project: $PROJECT_NAME, branch: $BRANCH)"
npx --yes wrangler@latest pages deploy dist \
  --project-name="$PROJECT_NAME" \
  --branch="$BRANCH"
