import socket

def scan_internal():
    # Kubernetes default internal service range
    base_ip = "10.244.0." 
    print(f"--- [ STARTING NETWORK SCAN ON {base_ip}0/24 ] ---")
    
    for i in range(1, 255):
        ip = base_ip + str(i)
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(0.01) # Ultra-fast elite scanning
        result = sock.connect_ex((ip, 80)) # Looking for HTTP services
        if result == 0:
            print(f"[!] DISCOVERED: Internal Web Service at {ip}:80")
        sock.close()

if __name__ == "__main__":
    scan_internal()
