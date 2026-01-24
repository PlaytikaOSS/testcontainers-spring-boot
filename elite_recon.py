import os
import glob

def hunt_sensitive_files():
    # Elite patterns: search for state files, keys, and cloud configs
    patterns = [
        "**/*.tfstate", 
        "**/.aws/credentials", 
        "**/database.yml", 
        "**/config.json"
    ]
    
    print("--- [ STARTING ELITE FILE RECON ] ---")
    for pattern in patterns:
        for filepath in glob.iglob('/' + pattern, recursive=True):
            if os.path.isfile(filepath):
                print(f"[!] FOUND POTENTIAL LEAK: {filepath}")

if __name__ == "__main__":
    hunt_sensitive_files()
