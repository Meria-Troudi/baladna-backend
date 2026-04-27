# WebSocket JWT Authentication Fix - Frontend Update Required

## Backend Changes Summary

The backend WebSocket authentication has been completely refactored to:

✅ **Extract and validate JWT tokens** from query parameter or Authorization header  
✅ **Verify user is a collaborator** in the itinerary (or owner)  
✅ **Check permissions** before allowing WebSocket connection  
✅ **Reject with 403** if user lacks permissions  
✅ **Return detailed error logs** for debugging  

## Updated WebSocket Connection Requirements

### **IMPORTANT: New Endpoint Format**

**Old (❌ NOT WORKING):**
```
ws://localhost:8081/api/chat?userId=1
```

**NEW (✅ CORRECT):**
```
ws://localhost:8081/api/chat?itineraryId={uuid}&token={jwt_token}
```

### Connection Parameters Required

| Parameter | Type | Required | Example |
|-----------|------|----------|---------|
| `itineraryId` | UUID | ✅ Yes | `1c2d858f-24ba-4a7b-b0bf-dfcaaa3a0fdc` |
| `token` | JWT String | ✅ Yes | `eyJhbGciOiJIUzI1NiIs...` |

### Query Parameter Format

```javascript
const itineraryId = "1c2d858f-24ba-4a7b-b0bf-dfcaaa3a0fdc";
const token = "your_jwt_token_here";
const wsUrl = `http://localhost:8081/api/chat?itineraryId=${itineraryId}&token=${token}`;
```

## Frontend Implementation (Updated)

### 1. Get JWT Token

```javascript
// From your AuthService
const token = AuthService.getAccessToken(); // Retrieved from localStorage
```

### 2. Connect to WebSocket

```javascript
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

const connectToChat = (itineraryId) => {
    const token = AuthService.getAccessToken();
    
    if (!token) {
        console.error('No authentication token found');
        return;
    }

    // Build connection URL with both itineraryId and token
    const wsUrl = `http://localhost:8081/api/chat?itineraryId=${itineraryId}&token=${token}`;
    
    const socket = new SockJS(wsUrl);
    const stompClient = Stomp.over(socket);

    stompClient.connect({}, 
        (frame) => {
            console.log('✅ Connected to chat:', frame);
            
            // Subscribe to message channel
            stompClient.subscribe(`/topic/itineraries/${itineraryId}/messages`, 
                (message) => {
                    const wsMessage = JSON.parse(message.body);
                    handleIncomingMessage(wsMessage);
                });

            // Subscribe to typing indicators
            stompClient.subscribe(`/topic/itineraries/${itineraryId}/typing`,
                (message) => {
                    const typingMessage = JSON.parse(message.body);
                    handleTypingIndicator(typingMessage);
                });
        },
        (error) => {
            console.error('❌ Connection error:', error);
            if (error.includes('403')) {
                console.error('Access Denied - You are not a collaborator in this itinerary');
            }
        }
    );

    return stompClient;
};
```

### 3. Send Messages

```javascript
const sendMessage = (stompClient, itineraryId, content) => {
    if (!stompClient || !stompClient.connected) {
        console.error('Not connected to chat');
        return;
    }

    stompClient.send(
        `/app/chat/itineraries/${itineraryId}/send`,
        {},
        JSON.stringify({ content })
    );
};
```

### 4. Edit Message

```javascript
const editMessage = (stompClient, itineraryId, messageId, newContent) => {
    stompClient.send(
        `/app/chat/itineraries/${itineraryId}/edit`,
        {},
        JSON.stringify({ 
            id: messageId, 
            content: newContent 
        })
    );
};
```

### 5. Delete Message

```javascript
const deleteMessage = (stompClient, itineraryId, messageId) => {
    stompClient.send(
        `/app/chat/itineraries/${itineraryId}/delete`,
        {},
        JSON.stringify({ id: messageId })
    );
};
```

### 6. Send Typing Indicator

```javascript
const sendTypingIndicator = (stompClient, itineraryId) => {
    stompClient.send(
        `/app/chat/itineraries/${itineraryId}/typing`,
        {},
        JSON.stringify({ senderName: "Current User" })
    );
};
```

## Complete React Hook Example

```javascript
import { useEffect, useState } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { AuthService } from './services/AuthService';

export const useChatConnection = (itineraryId) => {
    const [stompClient, setStompClient] = useState(null);
    const [connected, setConnected] = useState(false);
    const [messages, setMessages] = useState([]);
    const [error, setError] = useState(null);
    const [typingUsers, setTypingUsers] = useState(new Set());

    useEffect(() => {
        if (!itineraryId) return;

        const connectToChat = () => {
            try {
                const token = AuthService.getAccessToken();
                if (!token) {
                    setError('No authentication token found');
                    return;
                }

                const wsUrl = `http://localhost:8081/api/chat?itineraryId=${itineraryId}&token=${token}`;
                const socket = new SockJS(wsUrl);
                const client = Stomp.over(socket);

                // Enable debug mode
                client.debug = (str) => console.log('[STOMP]', str);

                client.connect(
                    {},
                    (frame) => {
                        console.log('✅ Connected:', frame);
                        setConnected(true);
                        setError(null);

                        // Subscribe to messages
                        client.subscribe(
                            `/topic/itineraries/${itineraryId}/messages`,
                            (msg) => {
                                const wsMessage = JSON.parse(msg.body);
                                
                                if (wsMessage.type === 'ERROR') {
                                    setError(wsMessage.error);
                                } else if (wsMessage.type === 'SEND') {
                                    setMessages(prev => [...prev, wsMessage]);
                                } else if (wsMessage.type === 'EDIT') {
                                    setMessages(prev => prev.map(m => 
                                        m.id === wsMessage.id ? wsMessage : m
                                    ));
                                } else if (wsMessage.type === 'DELETE') {
                                    setMessages(prev => prev.filter(m => m.id !== wsMessage.id));
                                }
                            }
                        );

                        // Subscribe to typing indicators
                        client.subscribe(
                            `/topic/itineraries/${itineraryId}/typing`,
                            (msg) => {
                                const { senderId } = JSON.parse(msg.body);
                                setTypingUsers(prev => {
                                    const newSet = new Set(prev);
                                    newSet.add(senderId);
                                    setTimeout(() => {
                                        setTypingUsers(prev => {
                                            const updated = new Set(prev);
                                            updated.delete(senderId);
                                            return updated;
                                        });
                                    }, 2000);
                                    return newSet;
                                });
                            }
                        );

                        setStompClient(client);
                    },
                    (error) => {
                        console.error('❌ Connection error:', error);
                        
                        if (error.includes('403')) {
                            setError('Access Denied: You are not a collaborator in this itinerary');
                        } else if (error.includes('401')) {
                            setError('Unauthorized: Invalid or expired token');
                        } else {
                            setError('Connection failed. Please try again.');
                        }
                        
                        setConnected(false);
                        
                        // Retry connection after 3 seconds
                        setTimeout(connectToChat, 3000);
                    }
                );
            } catch (err) {
                console.error('Error connecting to chat:', err);
                setError(err.message);
                setConnected(false);
            }
        };

        connectToChat();

        return () => {
            if (stompClient && stompClient.connected) {
                stompClient.disconnect();
            }
        };
    }, [itineraryId]);

    const sendMessage = (content) => {
        if (!stompClient || !stompClient.connected) {
            setError('Not connected to chat');
            return;
        }

        stompClient.send(
            `/app/chat/itineraries/${itineraryId}/send`,
            {},
            JSON.stringify({ content })
        );
    };

    const editMessage = (messageId, newContent) => {
        stompClient.send(
            `/app/chat/itineraries/${itineraryId}/edit`,
            {},
            JSON.stringify({ id: messageId, content: newContent })
        );
    };

    const deleteMessage = (messageId) => {
        stompClient.send(
            `/app/chat/itineraries/${itineraryId}/delete`,
            {},
            JSON.stringify({ id: messageId })
        );
    };

    const sendTypingIndicator = () => {
        if (stompClient && stompClient.connected) {
            stompClient.send(
                `/app/chat/itineraries/${itineraryId}/typing`,
                {},
                JSON.stringify({ senderName: 'User' })
            );
        }
    };

    return {
        connected,
        messages,
        error,
        typingUsers,
        sendMessage,
        editMessage,
        deleteMessage,
        sendTypingIndicator
    };
};
```

## Backend Error Responses (403 Forbidden)

The backend will now return **403 Forbidden** in these cases:

| Reason | Backend Log | Frontend Action |
|--------|-------------|-----------------|
| JWT token missing | "JWT token not provided" | Get token from AuthService |
| Invalid JWT token | "Invalid JWT token" | Refresh/re-authenticate |
| User not collaborator | "User {} is not a collaborator in itinerary {}" | Add user as collaborator first |
| Itinerary doesn't exist | "Itinerary not found" | Verify itinerary ID |
| Invalid itinerary ID format | "Invalid itineraryId format" | Ensure UUID format |

## Testing Checklist

- [ ] User is logged in (token available)
- [ ] User is added as collaborator in the itinerary
- [ ] `itineraryId` is a valid UUID format
- [ ] `token` is not expired
- [ ] Both `itineraryId` and `token` are passed as query parameters
- [ ] Frontend receives "Connected" message
- [ ] Messages appear in real-time
- [ ] Typing indicators work
- [ ] Edit/Delete operations broadcast to all users

## Debug the Connection

Add this to your frontend:

```javascript
// Enable detailed STOMP logging
Stomp.setDebug((msg) => console.log('[STOMP DEBUG]', msg));

// Log connection URL (without exposing token in console)
const wsUrl = `http://localhost:8081/api/chat?itineraryId=${itineraryId}&token=***`;
console.log('Connecting to:', wsUrl);
```

## Common Issues & Fixes

### Issue: 403 Forbidden

**Cause:** User is not a collaborator
```javascript
// ❌ Fix in backend first:
// 1. Add user as collaborator in itinerary
// 2. Or ensure user is the itinerary owner
```

### Issue: "Invalid JWT"

**Cause:** Expired or malformed token
```javascript
// ✅ Fix:
const token = AuthService.getAccessToken(); // Might need refresh
if (!token) {
    AuthService.login(); // Re-authenticate
}
```

### Issue: "itineraryId undefined"

**Cause:** Not extracting ID from URL
```javascript
// ✅ Fix: Get from URL params
import { useParams } from 'react-router-dom';
const { itineraryId } = useParams();
```

### Issue: Connection closes immediately

**Cause:** Backend requires both parameters
```javascript
// ✅ Correct URL format:
`http://localhost:8081/api/chat?itineraryId=123e4567-e89b-12d3-a456-426614174000&token=eyJhbGc...`

// ❌ Wrong (missing one parameter):
`http://localhost:8081/api/chat?userId=1`
```

## Backend Validation Logic

The backend now validates in this order:

1. ✅ Extract JWT from `?token=` or `Authorization: Bearer`
2. ✅ Validate JWT signature and expiration
3. ✅ Extract `userId` from JWT claims
4. ✅ Extract `itineraryId` from query parameter
5. ✅ Check itinerary exists
6. ✅ Check user is collaborator OR owner
7. ✅ Store both `userId` and `itineraryId` in session
8. ✅ Allow connection

## Migration from Old Endpoint

**Before (Old):**
```javascript
const socket = new SockJS(`http://localhost:8081/api/chat?userId=${userId}`);
```

**After (New):**
```javascript
const token = AuthService.getAccessToken();
const socket = new SockJS(`http://localhost:8081/api/chat?itineraryId=${itineraryId}&token=${token}`);
```

---

**Update your frontend code with these changes and the connection should work!** 🚀
