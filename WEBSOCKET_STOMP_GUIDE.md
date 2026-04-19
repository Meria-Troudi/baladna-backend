# Chat Feature - WebSocket STOMP Implementation Guide

## Backend Architecture Update

The backend has been refactored to use **Spring's STOMP Message Broker** instead of raw WebSocket handlers. This provides:

- ✅ Automatic connection handling
- ✅ Built-in STOMP protocol support
- ✅ Message routing and broadcasting
- ✅ Heartbeat management
- ✅ Better performance and scalability

## WebSocket Connection Details

### Endpoint
```
WS Endpoint: ws://localhost:8080/api/chat?userId={userId}
HTTP Endpoint: http://localhost:8080/api/chat?userId={userId}
```

### Query Parameters
- `userId` (required) - The current user's ID

### Example Connections

**With SockJS (Recommended for browser compatibility):**
```javascript
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

const userId = 1; // Current logged-in user ID
const socket = new SockJS('http://localhost:8080/api/chat?userId=' + userId);
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame.command);
});
```

## STOMP Message Endpoints

### Client → Server (Send)
Clients send messages to these destinations:

#### 1. Send Chat Message
**Destination:** `/app/chat/itineraries/{itineraryId}/send`

**Payload:**
```json
{
  "content": "Hello everyone!"
}
```

**Example:**
```javascript
stompClient.send("/app/chat/itineraries/550e8400-e29b-41d4-a716-446655440001/send", {}, 
  JSON.stringify({ content: "Hello!" }));
```

#### 2. Edit Message
**Destination:** `/app/chat/itineraries/{itineraryId}/edit`

**Payload:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "content": "Updated message content"
}
```

#### 3. Delete Message
**Destination:** `/app/chat/itineraries/{itineraryId}/delete`

**Payload:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### 4. Typing Indicator
**Destination:** `/app/chat/itineraries/{itineraryId}/typing`

**Payload:**
```json
{
  "senderName": "John Doe"
}
```

### Server → Client (Receive)
Clients subscribe to these destinations to receive messages:

#### 1. Chat Messages Broadcast
**Subscribe To:** `/topic/itineraries/{itineraryId}/messages`

**Response Types:**

**SEND (new message):**
```json
{
  "type": "SEND",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "itineraryId": "550e8400-e29b-41d4-a716-446655440001",
  "senderId": 1,
  "senderName": "John Doe",
  "content": "Hello everyone!",
  "timestamp": "2024-04-18T10:30:00",
  "status": "SUCCESS"
}
```

**EDIT (message updated):**
```json
{
  "type": "EDIT",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "itineraryId": "550e8400-e29b-41d4-a716-446655440001",
  "senderId": 1,
  "senderName": "John Doe",
  "content": "Updated message",
  "timestamp": "2024-04-18T10:35:00",
  "status": "SUCCESS"
}
```

**DELETE (message deleted):**
```json
{
  "type": "DELETE",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "itineraryId": "550e8400-e29b-41d4-a716-446655440001",
  "senderId": 1,
  "timestamp": "2024-04-18T10:40:00",
  "status": "SUCCESS"
}
```

**ERROR (operation failed):**
```json
{
  "type": "ERROR",
  "status": "FAILED",
  "error": "User is not a collaborator in this itinerary",
  "timestamp": "2024-04-18T10:45:00"
}
```

#### 2. Typing Indicators
**Subscribe To:** `/topic/itineraries/{itineraryId}/typing`

**Response:**
```json
{
  "type": "TYPING",
  "senderId": 1,
  "senderName": "John Doe",
  "timestamp": "2024-04-18T10:30:00"
}
```

## Complete Frontend Example

```javascript
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

class ChatClient {
  constructor(userId, onMessageReceived, onError) {
    this.userId = userId;
    this.onMessageReceived = onMessageReceived;
    this.onError = onError;
    this.stompClient = null;
    this.subscriptions = new Map();
  }

  connect() {
    const socket = new SockJS(`http://localhost:8080/api/chat?userId=${this.userId}`);
    this.stompClient = Stomp.over(socket);

    this.stompClient.connect({}, (frame) => {
      console.log('Connected:', frame);
    }, (error) => {
      console.error('Connection error:', error);
      this.onError('Failed to connect to chat');
    });
  }

  subscribeToItinerary(itineraryId) {
    if (!this.stompClient || !this.stompClient.connected) {
      console.error('Not connected to chat');
      return;
    }

    // Subscribe to messages
    const messageSubscription = this.stompClient.subscribe(
      `/topic/itineraries/${itineraryId}/messages`,
      (message) => {
        const wsMessage = JSON.parse(message.body);
        this.onMessageReceived({ type: 'message', data: wsMessage });
      }
    );

    // Subscribe to typing indicators
    const typingSubscription = this.stompClient.subscribe(
      `/topic/itineraries/${itineraryId}/typing`,
      (message) => {
        const wsMessage = JSON.parse(message.body);
        this.onMessageReceived({ type: 'typing', data: wsMessage });
      }
    );

    // Store subscriptions for cleanup
    this.subscriptions.set(itineraryId, { messageSubscription, typingSubscription });
  }

  unsubscribeFromItinerary(itineraryId) {
    const subs = this.subscriptions.get(itineraryId);
    if (subs) {
      subs.messageSubscription.unsubscribe();
      subs.typingSubscription.unsubscribe();
      this.subscriptions.delete(itineraryId);
    }
  }

  sendMessage(itineraryId, content) {
    if (!this.stompClient || !this.stompClient.connected) {
      this.onError('Not connected to chat');
      return;
    }

    this.stompClient.send(
      `/app/chat/itineraries/${itineraryId}/send`,
      {},
      JSON.stringify({ content })
    );
  }

  editMessage(itineraryId, messageId, content) {
    if (!this.stompClient || !this.stompClient.connected) {
      this.onError('Not connected to chat');
      return;
    }

    this.stompClient.send(
      `/app/chat/itineraries/${itineraryId}/edit`,
      {},
      JSON.stringify({ id: messageId, content })
    );
  }

  deleteMessage(itineraryId, messageId) {
    if (!this.stompClient || !this.stompClient.connected) {
      this.onError('Not connected to chat');
      return;
    }

    this.stompClient.send(
      `/app/chat/itineraries/${itineraryId}/delete`,
      {},
      JSON.stringify({ id: messageId })
    );
  }

  sendTypingIndicator(itineraryId, userName) {
    if (!this.stompClient || !this.stompClient.connected) {
      return;
    }

    this.stompClient.send(
      `/app/chat/itineraries/${itineraryId}/typing`,
      {},
      JSON.stringify({ senderName: userName })
    );
  }

  disconnect() {
    // Unsubscribe from all itineraries
    this.subscriptions.forEach((subs) => {
      subs.messageSubscription.unsubscribe();
      subs.typingSubscription.unsubscribe();
    });
    this.subscriptions.clear();

    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.disconnect();
    }
  }
}

export default ChatClient;
```

## REST API (for history and management)

The REST API endpoints are still available for retrieving chat history and other operations:

```javascript
// Load chat history
const response = await fetch('/api/itineraries/{itineraryId}/chat/history?page=0&size=20', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const data = await response.json();
console.log(data.messages);

// Send message via REST (alternative to WebSocket)
await fetch('/api/itineraries/{itineraryId}/chat/messages', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ content: 'Hello!' })
});
```

## Testing the WebSocket Connection

### Test 1: Check Backend Logs

After starting the backend, look for:
```
WebSocket connection attempt: http://localhost:8080/api/chat?userId=1
WebSocket handshake successful for userId: 1
Connected: CONNECTED
```

### Test 2: Using wscat (Terminal)

```bash
# Install wscat
npm install -g wscat

# Connect with SockJS
wscat -c "http://localhost:8080/api/chat?userId=1"

# Once connected, send STOMP frame
CONNECT
accept-version:1.0,1.1,1.2
heart-beat:0,0

# Wait for response
CONNECTED
version:1.2
heart-beat:0,0

# Subscribe to messages
SUBSCRIBE
id:sub-0
destination:/topic/itineraries/550e8400-e29b-41d4-a716-446655440001/messages

# Send a message
SEND
destination:/app/chat/itineraries/550e8400-e29b-41d4-a716-446655440001/send
content-length:29

{"content":"Hello from wscat!"}
```

### Test 3: Using Postman

1. Click "New" → "WebSocket Request"
2. Enter: `ws://localhost:8080/api/chat?userId=1`
3. Add headers if needed
4. Connect and send STOMP frames

### Test 4: Browser Console

```javascript
// Open browser console and paste:
import SockJS from 'https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.js';
import Stomp from 'https://cdn.jsdelivr.net/npm/stomp-js@2.3.3/lib/stomp.min.js';

const socket = new SockJS('http://localhost:8080/api/chat?userId=1');
const client = Stomp.over(socket);
client.connect({}, (frame) => {
    console.log('Connected!', frame);
    client.subscribe('/topic/itineraries/550e8400-e29b-41d4-a716-446655440001/messages', 
        (msg) => console.log('Message:', JSON.parse(msg.body)));
});
```

## Troubleshooting

### Issue: "Connecting..." state persists

**Cause:** Backend not running or endpoint not accessible

**Solution:**
1. Verify backend is running on port 8080
2. Check firewall/proxy settings
3. Verify CORS is enabled (it is in config)

### Issue: Connection closes immediately

**Cause:** Missing or invalid userId

**Solution:**
```javascript
// WRONG - missing userId
const socket = new SockJS('http://localhost:8080/api/chat');

// CORRECT - include userId
const socket = new SockJS('http://localhost:8080/api/chat?userId=1');
```

### Issue: Messages not being received

**Cause:** Not subscribed to correct topic

**Solution:**
```javascript
// Make sure to subscribe AFTER connect
client.connect({}, (frame) => {
    // Subscribe here, not before connect
    client.subscribe('/topic/itineraries/{itineraryId}/messages', (msg) => {
        console.log(JSON.parse(msg.body));
    });
});
```

### Issue: Empty messages or invalid JSON

**Cause:** Wrong message format

**Solution:**
```javascript
// Send proper JSON
stompClient.send("/app/chat/itineraries/{id}/send", {}, 
    JSON.stringify({ content: "Hello!" })); // Correct

// NOT this format (with extra nesting)
// JSON.stringify({{ content: "Hello!" }}) - Wrong!
```

## Debug Mode

Enable STOMP debugging in frontend:

```javascript
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

// Enable debug logging
Stomp.setDebug((msg) => {
    console.log('STOMP Debug:', msg);
});

const socket = new SockJS('http://localhost:8080/api/chat?userId=1');
const client = Stomp.over(socket);
client.connect({}, onConnect, onError);
```

## Files Modified/Created

### New Files
- `config/JacksonConfig.java` - Jackson ObjectMapper bean
- `config/ChatWebSocketHandshakeInterceptor.java` - WebSocket handshake handler
- `controller/ChatStompController.java` - STOMP message handlers

### Modified Files
- `config/WebSocketConfig.java` - Updated to use STOMP broker
- `pom.xml` - Added spring-boot-starter-websocket

### Deprecated (No longer used)
- `websocket/ChatWebSocketHandler.java` - Replaced by STOMP controller

## Performance Tips

1. **Connection pooling:** Reuse single WebSocket connection per user
2. **Subscriptions:** Only subscribe to itineraries the user is viewing
3. **Unsubscribe:** Clean up subscriptions when leaving a chat
4. **Heartbeat:** Backend sends heartbeats every 25 seconds
5. **Message batching:** Group typing indicators instead of sending per keystroke

## Security Notes

- userId must match authenticated user (add JWT validation in interceptor if needed)
- All messages are verified for collaborator access in the service layer
- Soft deletes prevent data loss

## Next Steps for Frontend

1. Install dependencies: `npm install sockjs-client stompjs`
2. Create ChatClient service using the example above
3. Integrate with your chat UI components
4. Handle connection state (connecting, connected, disconnected)
5. Implement error handling and reconnection logic
6. Add typing debounce (send every 1-2 seconds max)
