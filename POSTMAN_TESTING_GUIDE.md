## **Google Calendar API - Postman Testing Guide**

### **Prerequisites**
1. Backend running on `http://localhost:8081`
2. Valid JWT token (login first to get token)
3. Postman installed or use Postman Web
4. Google OAuth credentials configured in `application.properties`

---

## **Step 1: Get Bearer Token (Login)**

**Endpoint:** `POST http://localhost:8081/api/auth/login`

**Request Body:**
```json
{
    "email": "user@example.com",
    "password": "password123"
}
```

**Response:**
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "username": "user@example.com",
    "roles": ["ROLE_USER"]
}
```

**Save the token value for use in all subsequent requests!**

---

## **Step 2: Check Calendar Connection Status**

**Endpoint:** `GET http://localhost:8081/api/itineraries/calendar/status`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Response:**
```json
false
```
*(User has not connected yet)*

---

## **Step 3: Get Google Authorization URL**

**Endpoint:** `GET http://localhost:8081/api/itineraries/calendar/auth-url`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Response:**
```json
"https://accounts.google.com/o/oauth2/v2/auth?client_id=YOUR_CLIENT_ID&scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fcalendar&redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Fapi%2Fitineraries%2Fcalendar%2Foauth%2Fcallback&response_type=code&access_type=offline"
```

**Action:**
1. Copy the URL from response
2. Open it in a browser
3. Login with your Google account
4. Click "Allow" to authorize
5. **Google redirects back with authorization code in URL like:**
   ```
   http://localhost:8081/api/itineraries/calendar/oauth/callback?code=4/0AY0e-g5XyZ...&scope=https://www.googleapis.com/auth/calendar
   ```
6. **Copy the `code` parameter value** (everything after `code=`)

---

## **Step 4: Handle OAuth Callback (Store Credentials)**

**Endpoint:** `POST http://localhost:8081/api/itineraries/calendar/oauth/callback`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Request Body:**
```json
{
    "authorizationCode": "4/0AY0e-g5XyZ1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t1u2v3w4x5y6z7"
}
```

**Response (201 Created):**
```json
{
    "userId": 1,
    "isSynced": false,
    "calendarId": "primary",
    "tokenExpiry": "2024-04-17T15:30:00",
    "message": "Google Calendar connected successfully!"
}
```

---

## **Step 5: Verify Connection Status**

**Endpoint:** `GET http://localhost:8081/api/itineraries/calendar/status`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Response:**
```json
true
```
*(Now connected!)*

---

## **Step 6: Get User's Itineraries (Get Itinerary ID)**

**Endpoint:** `GET http://localhost:8081/api/itineraries/my`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Response:**
```json
[
    {
        "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        "title": "Tunisia Trip 2024",
        "destinationRegion": "Tunis",
        "startDate": "2024-05-01T08:00:00",
        "endDate": "2024-05-10T18:00:00",
        "status": "PLANNING",
        "visibility": "PRIVATE",
        "estimatedBudget": 5000,
        "steps": [
            {
                "id": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
                "title": "Flight to Tunis",
                "plannedDate": "2024-05-01T08:00:00",
                "serviceType": "TRANSPORT",
                "notes": "Direct flight from CDG",
                "position": 1,
                "estimatedCost": 200,
                "latitude": 36.7372,
                "longitude": 10.2274,
                "createdAt": "2024-04-15T10:30:00",
                "updatedAt": "2024-04-15T10:30:00"
            },
            {
                "id": "b2c3d4e5-f6g7-48h9-i0j1-k2l3m4n5o6p7",
                "title": "Hotel Diar Leila & Spa",
                "plannedDate": "2024-05-01T15:00:00",
                "serviceType": "ACCOMMODATION",
                "notes": "5-star hotel in Tunis Medina",
                "position": 2,
                "estimatedCost": 300,
                "latitude": 36.7965,
                "longitude": 10.1697,
                "createdAt": "2024-04-15T10:35:00",
                "updatedAt": "2024-04-15T10:35:00"
            }
        ],
        "collaborators": []
    }
]
```

**Copy the itinerary `id` value for the next step!**

---

## **Step 7: Sync Itinerary to Google Calendar**

**Endpoint:** `POST http://localhost:8081/api/itineraries/calendar/sync`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Request Body:**
```json
{
    "itineraryId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

**Response (200 OK):**
```json
{
    "userId": 1,
    "isSynced": true,
    "calendarId": "primary",
    "message": "Successfully synced 2 steps to Google Calendar!"
}
```

**Check your Google Calendar - you should see:**
- ✅ "Flight to Tunis" on May 1, 2024 at 8:00 AM
- ✅ "Hotel Diar Leila & Spa" on May 1, 2024 at 3:00 PM

---

## **Step 8: Disconnect Google Calendar**

**Endpoint:** `DELETE http://localhost:8081/api/itineraries/calendar/disconnect`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**No Request Body needed**

**Response (200 OK):**
```json
"Google Calendar disconnected successfully"
```

---

## **Error Scenarios & Responses**

### **1. User Not Connected to Google Calendar**
**Endpoint:** `POST /api/itineraries/calendar/sync`

**Response (400 Bad Request):**
```json
{
    "timestamp": "2024-04-17T14:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "User has not connected Google Calendar. Please authenticate first.",
    "path": "/api/itineraries/calendar/sync"
}
```

### **2. Expired Authorization Code**
**Endpoint:** `POST /api/itineraries/calendar/oauth/callback`

**Request Body with expired code:**
```json
{
    "authorizationCode": "4/0AY0e-g5XyZ1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t1u2v3w4x5y6z7"
}
```

**Response (400 Bad Request):**
```json
{
    "timestamp": "2024-04-17T14:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Failed to exchange authorization code",
    "path": "/api/itineraries/calendar/oauth/callback"
}
```
*(Code expires after ~10 minutes)*

### **3. Invalid JWT Token**
Any endpoint without valid JWT

**Response (401 Unauthorized):**
```json
{
    "timestamp": "2024-04-17T14:30:00",
    "status": 401,
    "error": "Unauthorized",
    "message": "Unauthorized",
    "path": "/api/itineraries/calendar/status"
}
```

### **4. Itinerary Not Found**
**Endpoint:** `POST /api/itineraries/calendar/sync`

**Request Body with invalid ID:**
```json
{
    "itineraryId": "00000000-0000-0000-0000-000000000000"
}
```

**Response (400 Bad Request):**
```json
{
    "timestamp": "2024-04-17T14:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Itinerary not found",
    "path": "/api/itineraries/calendar/sync"
}
```

---

## **Postman Collection JSON (Import this)**

Save this as `google-calendar-api.postman_collection.json` and import into Postman:

```json
{
    "info": {
        "name": "Google Calendar API Integration",
        "description": "Test collection for Google Calendar sync feature",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": [
        {
            "name": "1. Login (Get Token)",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"email\": \"user@example.com\",\n    \"password\": \"password123\"\n}"
                },
                "url": {
                    "raw": "http://localhost:8081/api/auth/login",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8081",
                    "path": ["api", "auth", "login"]
                }
            }
        },
        {
            "name": "2. Check Calendar Status",
            "request": {
                "method": "GET",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{token}}",
                        "type": "text"
                    },
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "url": {
                    "raw": "http://localhost:8081/api/itineraries/calendar/status",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8081",
                    "path": ["api", "itineraries", "calendar", "status"]
                }
            }
        },
        {
            "name": "3. Get Auth URL",
            "request": {
                "method": "GET",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{token}}",
                        "type": "text"
                    },
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "url": {
                    "raw": "http://localhost:8081/api/itineraries/calendar/auth-url",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8081",
                    "path": ["api", "itineraries", "calendar", "auth-url"]
                }
            }
        },
        {
            "name": "4. OAuth Callback",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{token}}",
                        "type": "text"
                    },
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"authorizationCode\": \"PASTE_CODE_HERE\"\n}"
                },
                "url": {
                    "raw": "http://localhost:8081/api/itineraries/calendar/oauth/callback",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8081",
                    "path": ["api", "itineraries", "calendar", "oauth", "callback"]
                }
            }
        },
        {
            "name": "5. Get My Itineraries",
            "request": {
                "method": "GET",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{token}}",
                        "type": "text"
                    },
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "url": {
                    "raw": "http://localhost:8081/api/itineraries/my",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8081",
                    "path": ["api", "itineraries", "my"]
                }
            }
        },
        {
            "name": "6. Sync to Google Calendar",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{token}}",
                        "type": "text"
                    },
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"itineraryId\": \"f47ac10b-58cc-4372-a567-0e02b2c3d479\"\n}"
                },
                "url": {
                    "raw": "http://localhost:8081/api/itineraries/calendar/sync",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8081",
                    "path": ["api", "itineraries", "calendar", "sync"]
                }
            }
        },
        {
            "name": "7. Disconnect Calendar",
            "request": {
                "method": "DELETE",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{token}}",
                        "type": "text"
                    },
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    }
                ],
                "url": {
                    "raw": "http://localhost:8081/api/itineraries/calendar/disconnect",
                    "protocol": "http",
                    "host": ["localhost"],
                    "port": "8081",
                    "path": ["api", "itineraries", "calendar", "disconnect"]
                }
            }
        }
    ],
    "variable": [
        {
            "key": "token",
            "value": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "type": "string"
        }
    ]
}
```

---

## **Quick Test Flow**

1. **Step 1:** Run endpoint `1. Login` → Copy token from response
2. **Step 2:** Set `{{token}}` variable in Postman with the copied token
3. **Step 3:** Run `2. Check Calendar Status` → Should return `false`
4. **Step 4:** Run `3. Get Auth URL` → Copy the returned URL
5. **Step 5:** Open URL in browser, authorize with Google, copy authorization code from redirect URL
6. **Step 6:** Paste code in `4. OAuth Callback` request body, run it
7. **Step 7:** Run `2. Check Calendar Status` again → Should return `true`
8. **Step 8:** Run `5. Get My Itineraries` → Copy an itinerary ID
9. **Step 9:** Paste itinerary ID in `6. Sync to Google Calendar` body, run it
10. **Step 10:** Check your Google Calendar - events should appear! ✅

---

## **Important Notes**

- **Authorization Code expires in ~10 minutes** - Must be used quickly
- **Token format:** Copy only the token string, NOT including the quote marks
- **Replace placeholders:** 
  - `PASTE_CODE_HERE` → Actual code from Google redirect
  - `f47ac10b-58cc-4372-a567-0e02b2c3d479` → Your actual itinerary ID
- **All timestamps are in ISO 8601 format:** `2024-04-17T14:30:00`
