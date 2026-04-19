# Itinerary Chat Feature - Backend Implementation Guide

## Overview

This document describes the complete chat feature implementation for collaborators in itineraries. The feature supports both REST API endpoints for retrieving chat history and WebSocket connections for real-time messaging.

## Architecture

### Components Created

1. **ChatMessage Entity** - Stores chat messages in the database
2. **DTOs** - Data transfer objects for requests/responses
3. **ChatMessageRepository** - Database access layer
4. **ChatMessageService** - Business logic layer
5. **ChatController** - REST API endpoints
6. **ChatWebSocketHandler** - Real-time WebSocket messaging
7. **WebSocketConfig** - WebSocket configuration

## Database Schema

### ChatMessage Table

```sql
CREATE TABLE chat_message (
    id BINARY(16) PRIMARY KEY,
    itinerary_id BINARY(16) NOT NULL,
    sender_id BIGINT NOT NULL,
    content LONGTEXT NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (itinerary_id) REFERENCES itinerary(id),
    INDEX idx_itinerary_id (itinerary_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_created_at (created_at)
);
```

## API Endpoints

### REST API

All REST endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <token>
```

#### 1. Send a Message
**Endpoint:** `POST /api/itineraries/{itineraryId}/chat/messages`

**Request:**
```json
{
  "content": "This is a chat message"
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "itineraryId": "550e8400-e29b-41d4-a716-446655440001",
  "senderId": 1,
  "senderName": "John Doe",
  "content": "This is a chat message",
  "isDeleted": false,
  "createdAt": "2024-04-18T10:30:00",
  "updatedAt": "2024-04-18T10:30:00"
}
```

#### 2. Get Chat History (Paginated)
**Endpoint:** `GET /api/itineraries/{itineraryId}/chat/history?page=0&size=20`

**Query Parameters:**
- `page` (optional, default: 0) - Page number (0-indexed)
- `size` (optional, default: 20) - Number of messages per page

**Response:** `200 OK`
```json
{
  "itineraryId": "550e8400-e29b-41d4-a716-446655440001",
  "messages": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "itineraryId": "550e8400-e29b-41d4-a716-446655440001",
      "senderId": 1,
      "senderName": "John Doe",
      "content": "First message",
      "isDeleted": false,
      "createdAt": "2024-04-18T10:30:00",
      "updatedAt": "2024-04-18T10:30:00"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "itineraryId": "550e8400-e29b-41d4-a716-446655440001",
      "senderId": 2,
      "senderName": "Jane Smith",
      "content": "Second message",
      "isDeleted": false,
      "createdAt": "2024-04-18T10:35:00",
      "updatedAt": "2024-04-18T10:35:00"
    }
  ],
  "totalMessages": 2,
  "page": 0,
  "size": 20
}
```

#### 3. Get Specific Message
**Endpoint:** `GET /api/itineraries/{itineraryId}/chat/messages/{messageId}`

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "itineraryId": "550e8400-e29b-41d4-a716-446655440001",
  "senderId": 1,
  "senderName": "John Doe",
  "content": "This is a chat message",
  "isDeleted": false,
  "createdAt": "2024-04-18T10:30:00",
  "updatedAt": "2024-04-18T10:30:00"
}
```

#### 4. Update a Message
**Endpoint:** `PUT /api/itineraries/{itineraryId}/chat/messages/{messageId}`

**Request:**
```json
{
  "content": "Updated message content"
}
```

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "itineraryId": "550e8400-e29b-41d4-a716-446655440001",
  "senderId": 1,
  "senderName": "John Doe",
  "content": "Updated message content",
  "isDeleted": false,
  "createdAt": "2024-04-18T10:30:00",
  "updatedAt": "2024-04-18T10:35:00"
}
```

**Restrictions:**
- Only the message sender can update the message

#### 5. Delete a Message
**Endpoint:** `DELETE /api/itineraries/{itineraryId}/chat/messages/{messageId}`

**Response:** `204 No Content`

**Restrictions:**
- Only the sender or itinerary owner can delete
- Deletion is soft (message marked as deleted, not removed from DB)

#### 6. Get Latest Message
**Endpoint:** `GET /api/itineraries/{itineraryId}/chat/latest`

**Response:** `200 OK` or `204 No Content` (if no messages)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "itineraryId": "550e8400-e29b-41d4-a716-446655440001",
  "senderId": 2,
  "senderName": "Jane Smith",
  "content": "Latest message",
  "isDeleted": false,
  "createdAt": "2024-04-18T10:35:00",
  "updatedAt": "2024-04-18T10:35:00"
}
```

#### 7. Get Message Count
**Endpoint:** `GET /api/itineraries/{itineraryId}/chat/count`

**Response:** `200 OK`
```json
42
```

### WebSocket API

#### Connection
**Endpoint:** `ws://localhost:8080/api/chat/itineraries/{itineraryId}?userId={userId}`

**Example with SockJS:**
```javascript
const itineraryId = "550e8400-e29b-41d4-a716-446655440001";
const userId = 1;
const socket = new SockJS(`/api/chat/itineraries/${itineraryId}?userId=${userId}`);
const stompClient = Stomp.over(socket);
```

#### Message Types

##### 1. Send a Message
```json
{
  "type": "SEND",
  "content": "Real-time message"
}
```

**Broadcast Response (to all users in itinerary):**
```json
{
  "type": "SEND",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "itineraryId": "550e8400-e29b-41d4-a716-446655440001",
  "senderId": 1,
  "senderName": "John Doe",
  "content": "Real-time message",
  "timestamp": "2024-04-18T10:30:00",
  "status": "SUCCESS"
}
```

##### 2. Edit a Message
```json
{
  "type": "EDIT",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "content": "Updated message"
}
```

**Broadcast Response:**
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

##### 3. Delete a Message
```json
{
  "type": "DELETE",
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Broadcast Response:**
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

##### 4. User Joined (System Message)
```json
{
  "type": "USER_JOINED",
  "senderId": 1,
  "senderName": "John Doe",
  "timestamp": "2024-04-18T10:30:00"
}
```

##### 5. User Left (System Message)
```json
{
  "type": "USER_LEFT",
  "senderId": 1,
  "timestamp": "2024-04-18T10:45:00"
}
```

##### 6. Typing Status
**Send to indicate user is typing:**
```json
{
  "type": "TYPING",
  "senderName": "John Doe"
}
```

**Broadcast to others (except sender):**
```json
{
  "type": "TYPING",
  "senderId": 1,
  "senderName": "John Doe",
  "timestamp": "2024-04-18T10:30:00"
}
```

##### 7. Error Message
```json
{
  "type": "ERROR",
  "status": "FAILED",
  "error": "User is not a collaborator in this itinerary"
}
```

## Security & Authorization

### Access Control

1. **Collaborator Verification**: Only users who are collaborators (or the owner) of an itinerary can send messages or access the chat
2. **Message Update Restriction**: Only the message sender can edit their own messages
3. **Message Deletion**: Only the sender or itinerary owner can delete messages
4. **JWT Authentication**: All HTTP endpoints require valid JWT token

### Best Practices

- Messages are soft-deleted (marked as deleted, not removed)
- User IDs are extracted from the security context
- WebSocket connections validate user authorization
- Error messages are descriptive but don't expose sensitive information

## Implementation Details

### ChatMessage Entity Features

- **UUID Primary Key**: Uses UUID for scalability
- **Timestamps**: Automatic `createdAt` and `updatedAt`
- **Soft Delete**: Messages marked with `isDeleted` flag
- **Indexed Columns**: Efficient queries on `itinerary_id`, `sender_id`, and `created_at`

### ChatMessageService Key Methods

```java
// Send a message
ChatMessageResponse sendMessage(UUID itineraryId, Long senderId, ChatMessageRequest request)

// Get paginated chat history
ChatHistoryResponse getChatHistory(UUID itineraryId, Integer page, Integer size)

// Get specific message
ChatMessageResponse getMessageById(UUID messageId)

// Update message
ChatMessageResponse updateMessage(UUID messageId, Long senderId, ChatMessageRequest request)

// Delete message (soft)
void deleteMessage(UUID messageId, Long senderId, UUID itineraryId)

// Get latest message
ChatMessageResponse getLatestMessage(UUID itineraryId)

// Get message count
Long getMessageCount(UUID itineraryId)
```

### WebSocket Session Management

- **Concurrent Maps**: Thread-safe session management
- **Per-Itinerary Sessions**: Sessions grouped by itinerary ID
- **User Tracking**: Each session linked to specific user
- **Automatic Cleanup**: Disconnected sessions automatically removed
- **Broadcast Mechanism**: Messages efficiently broadcast to all connected users

## Configuration

### Maven Dependencies Added

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### WebSocket Configuration

Located in: `config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/api/chat/itineraries/{itineraryId}")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

## Frontend Integration Examples

### REST API Usage (JavaScript/Fetch)

```javascript
// Get chat history
fetch('/api/itineraries/550e8400-e29b-41d4-a716-446655440001/chat/history?page=0&size=20', {
  headers: { 'Authorization': `Bearer ${token}` }
})
.then(r => r.json())
.then(data => console.log(data.messages));

// Send message
fetch('/api/itineraries/550e8400-e29b-41d4-a716-446655440001/chat/messages', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ content: 'Hello!' })
})
.then(r => r.json())
.then(data => console.log('Message sent:', data));
```

### WebSocket Usage (with SockJS + Stomp)

```javascript
// Install: npm install sockjs-client stompjs

import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

const itineraryId = '550e8400-e29b-41d4-a716-446655440001';
const userId = 1;

// Connect
const socket = new SockJS(`http://localhost:8080/api/chat/itineraries/${itineraryId}?userId=${userId}`);
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  console.log('Connected to chat');
  
  // Send message
  stompClient.send('/queue/chat', {}, JSON.stringify({
    type: 'SEND',
    content: 'Hello from WebSocket!'
  }));
  
  // Receive messages
  stompClient.subscribe('/topic/chat', (message) => {
    const wsMessage = JSON.parse(message.body);
    console.log('Received:', wsMessage);
  });
});

// Disconnect
stompClient.disconnect(() => {
  console.log('Disconnected from chat');
});
```

## Testing Guide

### Testing with Postman

1. **Create an Itinerary** (GET /api/itineraries)
2. **Add a Collaborator** (POST /api/itineraries/{id}/collaborators/invite)
3. **Send a Message**
   - Endpoint: POST `/api/itineraries/{id}/chat/messages`
   - Body: `{"content": "Test message"}`
4. **Get Chat History**
   - Endpoint: GET `/api/itineraries/{id}/chat/history?page=0&size=10`
5. **Update Message**
   - Endpoint: PUT `/api/itineraries/{id}/chat/messages/{messageId}`
6. **Delete Message**
   - Endpoint: DELETE `/api/itineraries/{id}/chat/messages/{messageId}`

### WebSocket Testing

Use tools like:
- **wscat**: `npm install -g wscat`
- **WebSocket Echo Client**: Browser extensions
- **Postman**: WebSocket request support

```bash
wscat -c "ws://localhost:8080/api/chat/itineraries/550e8400-e29b-41d4-a716-446655440001?userId=1"

# Send message
{ "type": "SEND", "content": "Test message" }
```

## Error Handling

### Common Error Scenarios

| Error | HTTP Status | Description |
|-------|------------|-------------|
| Itinerary not found | 400 | Invalid itinerary ID |
| User not collaborator | 400 | User is not part of itinerary |
| Message not found | 400 | Invalid message ID |
| Unauthorized update | 400 | Only sender can update |
| Unauthorized delete | 400 | Only sender/owner can delete |
| Invalid input | 400 | Message content is blank |

## Performance Considerations

1. **Pagination**: Chat history is paginated to handle large message volumes
2. **Indexing**: Database indexes on `itinerary_id`, `sender_id`, and `created_at`
3. **Soft Deletes**: Improves query performance vs hard deletes
4. **Session Management**: In-memory maps for efficient real-time delivery
5. **Connection Pooling**: WebSocket connections reused via SockJS

## Future Enhancements

1. **Message Search**: Full-text search across messages
2. **File Attachments**: Support for uploading/sharing files
3. **Message Reactions**: Emojis and reactions
4. **Read Receipts**: Track when messages are read
5. **Message Threads**: Reply to specific messages
6. **User Mentions**: @mention collaborators
7. **Message Encryption**: End-to-end encryption for privacy
8. **Message Pinning**: Pin important messages
9. **Chat History Export**: Export conversations
10. **Moderation Tools**: Spam/abuse prevention

## Troubleshooting

### WebSocket Connection Issues

1. **Connection Timeout**: Ensure userId is passed in query parameter
2. **403 Forbidden**: Check JWT token validity
3. **User not collaborator**: Add user as collaborator first
4. **Message delivery failed**: Check network connectivity

### Performance Issues

1. **Large history queries**: Use smaller page sizes
2. **Many concurrent connections**: Monitor server resources
3. **Slow message delivery**: Check database performance

## Files Created/Modified

### New Files
- `src/main/java/.../itinerary/entity/ChatMessage.java`
- `src/main/java/.../itinerary/dto/ChatMessageRequest.java`
- `src/main/java/.../itinerary/dto/ChatMessageResponse.java`
- `src/main/java/.../itinerary/dto/ChatHistoryResponse.java`
- `src/main/java/.../itinerary/dto/ChatWebSocketMessage.java`
- `src/main/java/.../itinerary/repository/ChatMessageRepository.java`
- `src/main/java/.../itinerary/service/ChatMessageService.java`
- `src/main/java/.../itinerary/controller/ChatController.java`
- `src/main/java/.../itinerary/websocket/ChatWebSocketHandler.java`
- `src/main/java/.../config/WebSocketConfig.java`

### Modified Files
- `pom.xml` - Added spring-boot-starter-websocket dependency

## Support & Maintenance

For issues or questions about the chat feature implementation, refer to the code comments and this guide. The implementation follows Spring Boot best practices and is compatible with Spring Boot 4.0.2.
