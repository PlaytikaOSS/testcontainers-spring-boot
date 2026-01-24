#!/bin/sh
echo "--- [ STARTING ELITE SHELL RECON ] ---"
# Searching for tfstate, credentials, and config files
# /proc/config.gz is a bonus elite target (kernel config)
find / -name "*.tfstate" -o -name "credentials" -o -name "database.yml" -o -name "config.json" 2>/dev/null | while read -r line; do
    echo "[!] FOUND POTENTIAL LEAK: $line"
done
