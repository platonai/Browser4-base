# Remote Server Configuration

This guide explains how to connect the Browser4 Python SDK to remote Browser4 servers instead of using the local Browser4Driver.

## Overview

While `Browser4Driver` is convenient for local development, production environments often require connecting to a centrally managed Browser4 server. The SDK supports remote connections via `PulsarClient`.

## Basic Remote Connection

### Simple Remote Connection

Connect to a remote server:

```python
from browser4 import PulsarClient, AgenticSession

# Connect to remote server
client = PulsarClient(base_url="http://remote-server.example.com:8182")

# Create session
session_id = client.create_session()
session = AgenticSession(client)

# Use the session
page = session.open("https://example.com")

# Cleanup
session.close()
client.close()
```

### With Authentication

If the server requires authentication:

```python
from browser4 import PulsarClient, AgenticSession

# Connect with authentication headers
client = PulsarClient(
    base_url="http://remote-server.example.com:8182",
    default_headers={
        "Authorization": "Bearer your-api-token",
        "X-API-Key": "your-api-key"
    }
)

session_id = client.create_session()
session = AgenticSession(client)

# Use authenticated session...
```

## PulsarClient Configuration

### Complete Configuration

```python
from browser4 import PulsarClient

client = PulsarClient(
    # Server URL
    base_url="http://remote-server.example.com:8182",
    
    # Request timeout (seconds)
    timeout=60.0,
    
    # Default headers for all requests
    default_headers={
        "Authorization": "Bearer your-token",
        "X-API-Key": "your-key",
        "User-Agent": "MyApp/1.0"
    }
)
```

### Timeout Configuration

Configure request timeouts for different operations:

```python
from browser4 import PulsarClient

# Short timeout for quick operations
client = PulsarClient(
    base_url="http://remote-server.example.com:8182",
    timeout=30.0  # 30 seconds
)

# Longer timeout for heavy scraping
client = PulsarClient(
    base_url="http://remote-server.example.com:8182",
    timeout=120.0  # 2 minutes
)
```

## Connection Patterns

### Reusable Client

Create a client factory for consistent configuration:

```python
from browser4 import PulsarClient
from typing import Optional

class Browser4ClientFactory:
    """Factory for creating configured Browser4 clients."""
    
    def __init__(
        self,
        base_url: str,
        api_token: Optional[str] = None,
        timeout: float = 60.0
    ):
        self.base_url = base_url
        self.api_token = api_token
        self.timeout = timeout
    
    def create_client(self) -> PulsarClient:
        """Create a new client with standard configuration."""
        headers = {}
        
        if self.api_token:
            headers["Authorization"] = f"Bearer {self.api_token}"
        
        return PulsarClient(
            base_url=self.base_url,
            timeout=self.timeout,
            default_headers=headers
        )

# Usage
factory = Browser4ClientFactory(
    base_url="http://remote-server.example.com:8182",
    api_token="your-api-token",
    timeout=60.0
)

# Create clients as needed
client1 = factory.create_client()
client2 = factory.create_client()
```

### Context Manager for Remote Sessions

Create a context manager for remote sessions:

```python
from contextlib import contextmanager
from browser4 import PulsarClient, AgenticSession

@contextmanager
def remote_session(server_url: str, api_token: str = None):
    """Context manager for remote Browser4 sessions."""
    
    headers = {}
    if api_token:
        headers["Authorization"] = f"Bearer {api_token}"
    
    client = PulsarClient(
        base_url=server_url,
        default_headers=headers
    )
    
    session = None
    
    try:
        session_id = client.create_session()
        session = AgenticSession(client)
        yield session
    finally:
        if session:
            session.close()
        client.close()

# Usage
with remote_session("http://remote.example.com:8182", "token") as session:
    page = session.open("https://example.com")
    # Session automatically cleaned up
```

## Multi-Server Configuration

### Load Balancing

Distribute requests across multiple servers:

```python
from browser4 import PulsarClient, AgenticSession
from typing import List
import random

class LoadBalancedClient:
    """Client that load balances across multiple servers."""
    
    def __init__(self, server_urls: List[str], api_token: str = None):
        self.server_urls = server_urls
        self.api_token = api_token
        self.clients = []
        
        for url in server_urls:
            headers = {}
            if api_token:
                headers["Authorization"] = f"Bearer {api_token}"
            
            client = PulsarClient(base_url=url, default_headers=headers)
            self.clients.append(client)
    
    def get_client(self) -> PulsarClient:
        """Get a random client for load balancing."""
        return random.choice(self.clients)
    
    def create_session(self) -> tuple[PulsarClient, AgenticSession]:
        """Create a session on a random server."""
        client = self.get_client()
        session_id = client.create_session()
        session = AgenticSession(client)
        return client, session
    
    def close_all(self):
        """Close all clients."""
        for client in self.clients:
            client.close()

# Usage
lb_client = LoadBalancedClient(
    server_urls=[
        "http://server1.example.com:8182",
        "http://server2.example.com:8182",
        "http://server3.example.com:8182",
    ],
    api_token="your-token"
)

try:
    # Create session on random server
    client, session = lb_client.create_session()
    
    page = session.open("https://example.com")
    
    session.close()
    
finally:
    lb_client.close_all()
```

### Failover Configuration

Implement failover for high availability:

```python
from browser4 import PulsarClient, AgenticSession
from typing import List, Optional
import requests

class FailoverClient:
    """Client with automatic failover to backup servers."""
    
    def __init__(self, server_urls: List[str], api_token: str = None):
        self.server_urls = server_urls
        self.api_token = api_token
        self.current_client: Optional[PulsarClient] = None
        self.current_index = 0
    
    def _create_client(self, url: str) -> PulsarClient:
        """Create a client for a specific server."""
        headers = {}
        if self.api_token:
            headers["Authorization"] = f"Bearer {self.api_token}"
        
        return PulsarClient(base_url=url, default_headers=headers)
    
    def _test_server(self, url: str) -> bool:
        """Test if a server is available."""
        try:
            response = requests.get(f"{url}/health", timeout=5)
            return response.status_code == 200
        except:
            return False
    
    def get_client(self) -> PulsarClient:
        """Get a working client, with failover."""
        # Try current client first
        if self.current_client:
            return self.current_client
        
        # Try each server until one works
        for i in range(len(self.server_urls)):
            index = (self.current_index + i) % len(self.server_urls)
            url = self.server_urls[index]
            
            if self._test_server(url):
                self.current_client = self._create_client(url)
                self.current_index = index
                print(f"Connected to server: {url}")
                return self.current_client
        
        raise ConnectionError("All servers are unavailable")
    
    def create_session(self) -> tuple[PulsarClient, AgenticSession]:
        """Create a session with failover."""
        client = self.get_client()
        
        try:
            session_id = client.create_session()
            session = AgenticSession(client)
            return client, session
        except Exception as e:
            # If session creation fails, try next server
            self.current_client = None
            print(f"Failed to create session: {e}. Trying next server...")
            return self.create_session()
    
    def close(self):
        """Close current client."""
        if self.current_client:
            self.current_client.close()
            self.current_client = None

# Usage
failover = FailoverClient(
    server_urls=[
        "http://primary.example.com:8182",
        "http://backup1.example.com:8182",
        "http://backup2.example.com:8182",
    ],
    api_token="your-token"
)

try:
    client, session = failover.create_session()
    
    page = session.open("https://example.com")
    
    session.close()
    
finally:
    failover.close()
```

## HTTPS and Security

### HTTPS Connection

Connect to servers using HTTPS:

```python
from browser4 import PulsarClient

# HTTPS connection
client = PulsarClient(
    base_url="https://secure-server.example.com:8443",
    default_headers={
        "Authorization": "Bearer your-token"
    }
)
```

### Custom SSL Verification

For self-signed certificates (development only):

```python
import requests
from browser4 import PulsarClient

# Note: The SDK uses httpx internally, which handles SSL verification
# For custom SSL handling, you might need to configure httpx settings

# For development with self-signed certs, you may need to:
# 1. Add the cert to your system's trusted certificates
# 2. Use a reverse proxy with valid certificates
# 3. Configure the server with proper SSL certificates
```

## Network Configuration

### Proxy Configuration

Configure proxy for remote connections:

```python
import os
from browser4 import PulsarClient

# Set proxy via environment variables
os.environ["HTTP_PROXY"] = "http://proxy.example.com:8080"
os.environ["HTTPS_PROXY"] = "http://proxy.example.com:8080"

# Create client (will use proxy)
client = PulsarClient(base_url="http://remote-server.example.com:8182")
```

### Connection Pooling

The SDK uses httpx which handles connection pooling automatically. Configure limits if needed:

```python
# The PulsarClient uses httpx with default connection pooling
# Connection pool limits are handled automatically
# For custom configuration, you may need to modify the PulsarClient implementation
```

## Health Checks

### Server Health Check

Check server health before connecting:

```python
import requests
from browser4 import PulsarClient, AgenticSession

def check_server_health(base_url: str) -> bool:
    """Check if Browser4 server is healthy."""
    try:
        response = requests.get(f"{base_url}/health", timeout=5)
        return response.status_code == 200
    except:
        return False

# Use health check
server_url = "http://remote-server.example.com:8182"

if check_server_health(server_url):
    client = PulsarClient(base_url=server_url)
    session_id = client.create_session()
    session = AgenticSession(client)
    # Use session...
    session.close()
    client.close()
else:
    print("Server is not available")
```

## Configuration from Environment

### Environment-Based Configuration

```python
import os
from browser4 import PulsarClient

# Read configuration from environment
server_url = os.getenv("BROWSER4_SERVER_URL", "http://localhost:8182")
api_token = os.getenv("BROWSER4_API_TOKEN")
timeout = float(os.getenv("BROWSER4_TIMEOUT", "60.0"))

# Create client with environment config
headers = {}
if api_token:
    headers["Authorization"] = f"Bearer {api_token}"

client = PulsarClient(
    base_url=server_url,
    timeout=timeout,
    default_headers=headers
)
```

### Configuration File

Load configuration from a file:

```python
import json
from pathlib import Path
from browser4 import PulsarClient

def load_config(config_path: str) -> dict:
    """Load configuration from JSON file."""
    with open(config_path, 'r') as f:
        return json.load(f)

# config.json:
# {
#   "server_url": "http://remote-server.example.com:8182",
#   "api_token": "your-token",
#   "timeout": 60.0
# }

config = load_config("config.json")

client = PulsarClient(
    base_url=config["server_url"],
    timeout=config.get("timeout", 60.0),
    default_headers={
        "Authorization": f"Bearer {config['api_token']}"
    }
)
```

## Best Practices

1. **Use Environment Variables**: Store server URLs and tokens in environment variables, not code

2. **Implement Health Checks**: Always check server health before creating sessions

3. **Handle Connection Errors**: Wrap remote operations in try-catch blocks

4. **Use Timeouts**: Configure appropriate timeouts for your use case

5. **Implement Retry Logic**: Add retry logic for transient network errors

6. **Close Connections**: Always close clients and sessions when done

7. **Use Failover**: Configure multiple servers for high availability

8. **Monitor Performance**: Track connection times and success rates

## Troubleshooting

### Connection Refused

```python
# Check if server is running
# Check firewall rules
# Verify server URL and port
# Test with curl: curl http://server:8182/health
```

### Timeout Errors

```python
# Increase timeout
client = PulsarClient(base_url=url, timeout=120.0)

# Check network latency
# Verify server has sufficient resources
```

### Authentication Failures

```python
# Verify token is correct
# Check token expiration
# Verify header format
# Check server logs for auth errors
```

## Next Steps

- [Browser4Driver Configuration](browser4-driver.md) - Local server configuration
- [Environment Variables](environment-variables.md) - Complete environment variable reference
- [Advanced Usage](../examples/advanced-usage.md) - Advanced connection patterns
