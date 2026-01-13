# Server-Side Event Handlers

This document describes the ServerSideEventHandlers mechanism for broadcasting page lifecycle events from the server to clients via Server-Sent Events (SSE).

## Overview

The ServerSideEventHandlers system provides a way to capture and stream events that occur during web page processing. Events are captured at various stages of the page lifecycle (crawl, load, browse phases) and can be streamed to clients through the REST API.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          Client (Browser/CLI)                            │
│                                                                           │
│  POST /api/commands  ────────────►  GET /api/commands/{id}/stream       │
│       (async)                              │                             │
│                                            │ SSE Stream                   │
│                                            ▼                             │
│                    ┌───────────────────────────────────┐                │
│                    │   data: {"event":"onWillLoad"...} │                │
│                    │   data: {"event":"onFetched"...}  │                │
│                    │   data: {"event":"onParsed"...}   │                │
│                    └───────────────────────────────────┘                │
└─────────────────────────────────────────────────────────────────────────┘
                                     ▲
                                     │ SSE
─────────────────────────────────────┼─────────────────────────────────────
                                     │
┌────────────────────────────────────┴─────────────────────────────────────┐
│                         Server (pulsar-rest)                              │
│                                                                           │
│  CommandController  ──►  CommandService.executeCommand()                 │
│                              │                                            │
│                              ├─► Create DefaultServerSideEventHandlers   │
│                              ├─► Set GlobalEventHandlers.serverSide...   │
│                              ├─► executeCommandStepByStep()              │
│                              │                                            │
│                              └─► commandStatusFlow()                     │
│                                    │                                      │
│                                    ├─► Poll CommandStatus                │
│                                    ├─► Collect ServerSideEventHandlers   │
│                                    └─► Emit updated status via SSE       │
└───────────────────────────────────────────────────────────────────────────┘
                                     ▲
                                     │ Events
─────────────────────────────────────┼─────────────────────────────────────
                                     │
┌────────────────────────────────────┴─────────────────────────────────────┐
│                    Core Processing (pulsar-skeleton)                      │
│                                                                           │
│  Page Lifecycle Events:                                                  │
│                                                                           │
│  AbstractTaskRunner                                                      │
│    └─► onWillLoad(url) ──────┬──────┐                                   │
│    └─► onLoaded(url, page) ──┤      │                                   │
│                               │      │                                   │
│  LoadComponent                │      │                                   │
│    └─► onWillLoad(url) ───────┤      │                                   │
│    └─► onLoaded(page) ────────┤      │                                   │
│                               │      │                                   │
│  FetchComponent               │      │                                   │
│    └─► onWillFetch(page) ─────┤      │                                   │
│    └─► onFetched(page) ───────┤      │                                   │
│                               │      │                                   │
│  PageParser                   │      │                                   │
│    └─► onWillParse(page) ─────┤      │                                   │
│    └─► onParsed(page) ────────┤      │                                   │
│                               │      │                                   │
│  PrimerHtmlParser             │      │                                   │
│    └─► onWillParseHTML... ────┤      │                                   │
│    └─► onHTMLDocumentParsed ──┤      │                                   │
│                               │      │                                   │
│                               ▼      ▼                                   │
│             GlobalEventHandlers.serverSideEventHandlers                  │
│                               │                                           │
│                               ▼                                           │
│                   DefaultServerSideEventHandlers                         │
│                               │                                           │
│                               └─► eventFlow (SharedFlow)                 │
│                                      │                                    │
│                                      └─► ServerSideEvent                 │
│                                            (eventType, phase, url...)    │
└───────────────────────────────────────────────────────────────────────────┘
```

## Architecture

### Components

1. **ServerSideEventHandlers Interface** (`pulsar-core/pulsar-skeleton`)
   - Defines the contract for capturing and broadcasting events
   - Provides methods for different event phases: crawl, load, browse
   - Exposes a reactive `eventFlow` for subscribing to events

2. **DefaultServerSideEventHandlers** (`pulsar-core/pulsar-skeleton`)
   - Default implementation using Kotlin coroutines and Flow
   - Uses `MutableSharedFlow` for event broadcasting
   - Configured with replay=0, extraBufferCapacity=64

3. **GlobalEventHandlers Integration** (`pulsar-core/pulsar-skeleton`)
   - Added `serverSideEventHandlers` property to `GlobalEventHandlers`
   - All existing event firing points now forward to ServerSideEventHandlers

4. **CommandService Integration** (`pulsar-rest`)
   - Creates ServerSideEventHandlers instance per command execution
   - Wires it to GlobalEventHandlers during command execution
   - Merges events into the command status flow

5. **REST API Endpoint**
   - `GET /api/commands/{id}/stream` - SSE endpoint for streaming command events
   - Returns `ServerSentEvent<CommandStatus>` stream

## Event Flow

```
Page Processing
  ↓
Event Fired (e.g., onWillLoad, onFetched, onHTMLDocumentParsed)
  ↓
GlobalEventHandlers.serverSideEventHandlers.onXxxEvent()
  ↓
ServerSideEvent emitted to SharedFlow
  ↓
CommandService.commandStatusFlow() collects events
  ↓
CommandStatus.event and message updated
  ↓
SSE stream sends updated CommandStatus to client
  ↓
Client receives event notification
```

## Event Types and Phases

### Crawl Phase Events
- `onWillLoad` - URL is about to be loaded
- `onLoaded` - URL has been loaded

### Load Phase Events
- `onWillLoad` - Page is about to be loaded
- `onWillFetch` - Page is about to be fetched
- `onFetched` - Page has been fetched
- `onWillParse` - Page is about to be parsed
- `onWillParseHTMLDocument` - HTML document is about to be parsed
- `onHTMLDocumentParsed` - HTML document has been parsed
- `onParsed` - Page parsing is complete
- `onLoaded` - Page is fully loaded

### Browse Phase Events
(Browser automation events can be added similarly)

## Usage Example

### Server-Side (Kotlin)

```kotlin
// Events are automatically captured when GlobalEventHandlers.serverSideEventHandlers is set
val handlers = DefaultServerSideEventHandlers()
GlobalEventHandlers.serverSideEventHandlers = handlers

// Subscribe to events
handlers.eventFlow.collect { event ->
    println("Event: ${event.eventType} at ${event.url}")
}
```

### Client-Side (JavaScript)

```javascript
// Stream events for a command
const eventSource = new EventSource('/api/commands/' + commandId + '/stream');

eventSource.onmessage = (event) => {
    const status = JSON.parse(event.data);
    console.log('Event:', status.event, 'State:', status.state);
    
    if (status.state === 'done') {
        eventSource.close();
    }
};
```

### Client-Side (curl)

```bash
# Stream events
curl -N -H "Accept: text/event-stream" \
  http://localhost:8182/api/commands/{id}/stream
```

## Implementation Details

### Event Data Structure

```kotlin
data class ServerSideEvent(
    val eventType: String,      // e.g., "onWillLoad", "onFetched"
    val eventPhase: String,      // e.g., "crawl", "load", "browse"
    val url: String?,            // URL being processed
    val message: String?,        // Optional message
    val timestamp: Instant,      // When the event occurred
    val metadata: Map<String, Any?>  // Additional metadata
)
```

### CommandStatus Updates

When an event is received:
1. `CommandStatus.event` is set to the event type
2. `CommandStatus.message` is appended with the event type
3. `CommandStatus.lastModifiedTime` is updated
4. The updated status is emitted to SSE subscribers

### Thread Safety

- ServerSideEventHandlers are created per-command execution
- Each command has its own isolated event stream
- GlobalEventHandlers is temporarily set during command execution
- Previous handlers are restored after command completes

## Event Locations

Events are fired from the following components:

1. **AbstractTaskRunner** - Crawl phase events (onWillLoad, onLoaded)
2. **LoadComponent** - Load phase entry events (onWillLoad, onLoaded)
3. **FetchComponent** - Fetch events (onWillFetch, onFetched)
4. **PageParser** - Parse events (onWillParse, onParsed)
5. **PrimerHtmlParser** - HTML parsing events (onWillParseHTMLDocument, onHTMLDocumentParsed)

## Testing

Tests are available in:
- `pulsar-core/pulsar-skeleton/src/test/.../ServerSideEventHandlersTest.kt` - Unit tests
- `pulsar-rest/src/test/.../CommandControllerSSETest.kt` - Integration tests

## Future Enhancements

Potential improvements:
1. Add browser phase events (onBrowserLaunched, onNavigated, etc.)
2. Support event filtering by type or phase
3. Add event history/replay capability
4. Provide event statistics and monitoring
5. Support multiple simultaneous subscribers per command
