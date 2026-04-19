# Google Calendar Integration Setup Guide

## Overview
This guide walks you through setting up Google Calendar API integration for syncing itinerary steps to users' Google Calendars.

## Prerequisites
- Google Account
- Google Cloud Project with Calendar API enabled
- OAuth 2.0 credentials (OAuth client ID and secret)

## Step 1: Create Google Cloud Project & Get Credentials

### 1.1 Create a Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Note your **Project ID**

### 1.2 Enable Google Calendar API
1. In Google Cloud Console, go to **APIs & Services** > **Library**
2. Search for "Google Calendar API"
3. Click on it and select **Enable**

### 1.3 Create OAuth 2.0 Credentials
1. Go to **APIs & Services** > **Credentials**
2. Click **Create Credentials** > **OAuth client ID**
3. Select **Web application**
4. Add Authorized redirect URIs:
   - Development: `http://localhost:8081/api/itineraries/calendar/oauth/callback`
   - Production: `https://yourdomain.com/api/itineraries/calendar/oauth/callback`
5. Copy your **Client ID** and **Client Secret**

## Step 2: Configure Backend

### 2.1 Add Application Properties
Update `src/main/resources/application.properties`:

```properties
### GOOGLE CALENDAR INTEGRATION ###
google.calendar.client-id=YOUR_CLIENT_ID
google.calendar.client-secret=YOUR_CLIENT_SECRET
google.calendar.redirect-uri=http://localhost:8081/api/itineraries/calendar/oauth/callback
```

Replace:
- `YOUR_CLIENT_ID` with your Google OAuth Client ID
- `YOUR_CLIENT_SECRET` with your Google OAuth Client Secret

### 2.2 Build the Project
```bash
mvn clean install
mvn spring-boot:run
```

## Step 3: Frontend Integration

### 3.1 Get Authorization URL
The user clicks a "Connect Google Calendar" button which calls:
```
GET /api/itineraries/calendar/auth-url
```
This returns the Google OAuth authorization URL. Redirect user to this URL.

### 3.2 Handle OAuth Callback
After user authorizes, Google redirects with an `authorization_code`. Send it to backend:
```
POST /api/itineraries/calendar/oauth/callback
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "authorizationCode": "authorization_code_from_google"
}
```

**Response:**
```json
{
  "userId": 1,
  "isSynced": true,
  "calendarId": "primary",
  "tokenExpiry": "2024-04-17T14:30:00",
  "message": "Google Calendar connected successfully!"
}
```

### 3.3 Check Calendar Connection Status
```
GET /api/itineraries/calendar/status
Authorization: Bearer {jwt_token}
```

Returns: `true` or `false`

## Step 4: Sync Itinerary Steps

### 4.1 Sync Steps to Google Calendar
```
POST /api/itineraries/calendar/sync
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "itineraryId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
```json
{
  "userId": 1,
  "isSynced": true,
  "calendarId": "primary",
  "message": "Successfully synced 5 steps to Google Calendar!"
}
```

### 4.2 Disconnect Google Calendar
```
DELETE /api/itineraries/calendar/disconnect
Authorization: Bearer {jwt_token}
```

## Database Schema

### GoogleCalendarCredential Table
```
google_calendar_credentials:
├── id (BIGINT, PK)
├── user_id (BIGINT, UNIQUE FK)
├── access_token (LONGTEXT)
├── refresh_token (LONGTEXT)
├── token_expiry (DATETIME)
├── calendar_id (VARCHAR)
├── is_synced (BOOLEAN)
├── created_at (DATETIME)
└── updated_at (DATETIME)
```

## Data Mapping: ItineraryStep → Google Calendar Event

| ItineraryStep Field | Google Calendar Event Field |
|---|---|
| `title` | `summary` |
| `notes` | `description` |
| `plannedDate` | Start/End `dateTime` |
| `latitude`, `longitude` | `location` |

## Error Handling

### Common Errors

| Error | Cause | Solution |
|---|---|---|
| `401 Unauthorized` | Invalid JWT token | Ensure valid JWT in header |
| "User has not connected Google Calendar" | No credentials stored | User must authenticate first |
| "Google Calendar token expired" | Token expired | User must reconnect |
| `INVALID_GRANT` | Invalid authorization code | Code expired (valid ~10 minutes) |

## Security Notes

⚠️ **Important:**
- Store Client ID and Client Secret in environment variables (do NOT commit to Git)
- Access tokens are encrypted and stored per user
- Delete credentials when user disconnects calendar
- Always validate JWT token before operations

### Environment Variables (Optional)
Instead of application.properties, use environment variables:
```bash
export GOOGLE_CALENDAR_CLIENT_ID=xxxx
export GOOGLE_CALENDAR_CLIENT_SECRET=xxxx
export GOOGLE_CALENDAR_REDIRECT_URI=xxxx
```

## Testing

### Manual Test Flow
1. Get auth URL: `GET /api/itineraries/calendar/auth-url`
2. Visit returned URL in browser
3. Authorize the application
4. Google redirects with authorization code
5. Extract code and send to `/calendar/oauth/callback`
6. Verify response with `isSynced: true`
7. Create/get an itinerary and sync: `POST /calendar/sync` with itineraryId
8. Check your Google Calendar for the synced events

## Troubleshooting

### Token Errors
- If getting `INVALID_GRANT`, ensure authorization code is fresh (< 10 min old)
- Codes can only be used once

### Missing Events in Calendar
- Verify `plannedDate` is set on all steps
- Check that itinerary ID is correct
- Ensure user has synced permission on their Google Calendar

### Configuration Issues
- Verify redirect URI matches exactly: `http://localhost:8081/api/itineraries/calendar/oauth/callback` (port must match)
- Check Client ID and Secret are correct in application.properties

## Next Steps

1. **Implement Frontend OAuth Flow:** Use Google Sign-In libraries for your framework
2. **Add Refresh Token Logic:** Handle token expiry and auto-refresh
3. **Event Deletion Sync:** Sync when user deletes/modifies itinerary steps
4. **Calendar Customization:** Allow users to select which calendar to sync to
5. **Bidirectional Sync:** Sync calendar events back to itinerary

## References
- [Google Calendar API Docs](https://developers.google.com/calendar/api/guides/overview)
- [Google Calendar Java Client Library](https://developers.google.com/resources/api-libraries/documentation/calendar/v3/java/latest/)
- [OAuth 2.0 Flow](https://developers.google.com/identity/protocols/oauth2)
