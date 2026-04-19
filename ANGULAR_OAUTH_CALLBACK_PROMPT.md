# Angular Google Calendar OAuth Callback Implementation Prompt

## Problem Statement
The backend Google Calendar OAuth integration is working correctly, but the **frontend popup window is stuck after successful authentication**. When Google redirects to the callback endpoint with the authorization code, the backend successfully processes it and returns a JSON response. However:

1. ❌ The popup window shows raw JSON instead of closing
2. ❌ The parent window doesn't know the OAuth was successful
3. ❌ The UI doesn't update to show "Google Calendar Connected"
4. ❌ The sync button isn't enabled
5. ❌ User experience is broken

## Backend Context (Already Implemented)

**OAuth Flow:**
1. Backend endpoint: `GET /api/itineraries/calendar/auth-url` - Returns Google OAuth URL with user ID encoded in state parameter
2. Google redirects to: `GET /api/itineraries/calendar/oauth/callback?code=xxx&state=userId`
3. Backend processes callback silently and returns JSON: `{"success":true,"message":"Google Calendar connected successfully!","userId":1,"isSynced":true,"calendarId":"primary","tokenExpiry":"2026-04-18T..."}`
4. Status check: `GET /api/itineraries/calendar/status` - Returns boolean (true = connected, false = not)
5. Sync: `POST /api/itineraries/calendar/sync` - Accepts `{itineraryId}`, syncs steps to calendar
6. Disconnect: `DELETE /api/itineraries/calendar/disconnect` - Removes connection

**Important:** The OAuth callback endpoint does NOT require JWT token because the authorization code IS the authentication.

## Requirements

### 1. Handle OAuth Popup Window Communication

Create an Angular service to:
- ✅ Open Google OAuth URL in a popup window
- ✅ Monitor when the popup receives the callback response
- ✅ Detect when callback succeeds or fails
- ✅ Close the popup automatically after callback
- ✅ Notify parent window of success/failure
- ✅ Handle user closes popup before completing auth
- ✅ Handle browser popup blockers gracefully

### 2. Update Planning Calendar Component

Modify the Planning Calendar component to:
- ✅ Display connection status (connected / not connected)
- ✅ Show connect button when not connected
- ✅ Show sync button when connected
- ✅ Show disconnect button when connected
- ✅ Handle loading states during OAuth flow
- ✅ Display success/error toasts/notifications
- ✅ Refresh calendar UI after successful connection

### 3. Handle Sync Flow

After successful OAuth connection:
- ✅ User can click "Sync to Google Calendar" button
- ✅ Frontend shows modal/dialog to select which itinerary to sync
- ✅ On selection, call `/api/itineraries/calendar/sync` with `{itineraryId}`
- ✅ Display loading indicator during sync
- ✅ Show success message: "Successfully synced X steps to Google Calendar!"
- ✅ Handle errors gracefully

## Implementation Details

### OAuth Callback Popup Strategy

**Recommended Approach:** Message-based communication using `window.postMessage()`

The popup will:
1. Reach the callback endpoint: `/api/itineraries/calendar/oauth/callback?code=xxx&state=userId`
2. Backend returns JSON response
3. Popup detects it's at the callback URL and extracts the response
4. Popup sends message to parent window with the result
5. Popup closes itself

### Service Architecture

**GoogleCalendarService** should:
- Handle all HTTP calls to Google Calendar endpoints
- Manage OAuth popup window reference
- Listen for messages from popup
- Handle popup lifecycle (open, close, errors)
- Return observables for components to subscribe to

**GoogleCalendarComponent** (or modal) should:
- Display connection UI
- Handle button clicks (Connect/Sync/Disconnect)
- Show modals for user selection
- Display success/error notifications

## Code Structure Needed

### 1. GoogleCalendarService (in services folder)

```typescript
// Methods needed:
- openGoogleCalendarAuth(): Observable<any>  // Opens popup and waits for callback
- getGoogleCalendarStatus(): Observable<boolean>  // GET /status
- syncItinerary(itineraryId: string): Observable<any>  // POST /sync
- disconnectGoogleCalendar(): Observable<any>  // DELETE /disconnect
- private handleOAuthCallback(): void  // Handles message from popup
```

### 2. GoogleCalendarComponent (or integrate into Planning Calendar)

```typescript
// Should handle:
- connectToGoogleCalendar() method
- syncToGoogleCalendar() method
- disconnectFromGoogleCalendar() method
- Template with Connect/Sync/Disconnect buttons
- Show/hide based on connection status
- Loading indicators
- Error handling
```

### 3. Popup Callback Handler

The callback response needs to be intercepted. Since the backend returns JSON, you need:
- A small handler that detects when popup is at callback URL
- Extract the JSON response
- Send it to parent window
- Close popup

This could be done via:
- **Option A:** Backend returns HTML page with JavaScript that posts message and closes
- **Option B:** Frontend intercepts the callback in popup using Angular's HttpInterceptor
- **Option C:** Backend redirects to a frontend route after callback completion

## Technical Requirements

### HTTP Requests Format

**GET Auth URL:**
```
GET http://localhost:8081/api/itineraries/calendar/auth-url
Authorization: Bearer {JWT_TOKEN}
Response: "https://accounts.google.com/o/oauth2/v2/auth?client_id=...&state=1..."
```

**GET Status:**
```
GET http://localhost:8081/api/itineraries/calendar/status
Authorization: Bearer {JWT_TOKEN}
Response: true or false
```

**POST Sync:**
```
POST http://localhost:8081/api/itineraries/calendar/sync
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
Body: {"itineraryId": "uuid-string"}
Response: {"success":true,"message":"Successfully synced 3 steps...","userId":1,...}
```

**DELETE Disconnect:**
```
DELETE http://localhost:8081/api/itineraries/calendar/disconnect
Authorization: Bearer {JWT_TOKEN}
Response: {"success":true,"message":"Google Calendar disconnected successfully","userId":1}
```

### Security Notes
- All endpoints except `/oauth/callback` require valid JWT token in Authorization header
- OAuth callback does NOT require JWT (no JWT in redirect from Google)
- User ID is determined from JWT token in other endpoints
- Store JWT token and include in all requests

## UX Flow

**Not Connected State:**
```
┌─────────────────────────────────────┐
│ Planning Calendar                   │
│                                     │
│ Status: Not Connected               │
│ [Connect to Google Calendar Button] │
└─────────────────────────────────────┘
```

**Connected State:**
```
┌─────────────────────────────────────┐
│ Planning Calendar                   │
│                                     │
│ Status: ✅ Connected to Google      │
│ [Sync to Google Calendar Button]    │
│ [Disconnect Button]                 │
└─────────────────────────────────────┘
```

**During OAuth:**
```
Popup opens: Google login screen
↓
User authorizes
↓
Google redirects to callback
↓
Popup shows success message briefly
↓
Popup closes automatically
↓
Main window shows "✅ Connected!" toast
↓
Buttons update to show Sync/Disconnect
```

## Error Handling Scenarios

1. **User closes popup before completing auth**
   - Popup reference lost
   - Show error: "Authorization cancelled"
   - Button remains in "Connect" state

2. **OAuth error from Google**
   - Backend returns `{"success":false,"message":"Authorization failed: access_denied"}`
   - Show error message to user
   - Keep button in "Connect" state

3. **Network error during callback**
   - Show error: "Failed to complete authorization"
   - Allow retry

4. **Sync fails (expired token, etc)**
   - Show error: "Sync failed: {error message}"
   - Offer to reconnect if token expired

## Integration Points

1. **Planning Calendar Component** - Where connect/sync/disconnect buttons live
2. **Services** - GoogleCalendarService for all API calls
3. **Auth Service** - To get current JWT token
4. **Toast/Notification Service** - To show success/error messages
5. **Itinerary Service** - To get list of itineraries for sync modal

## Key Implementation Notes

### Popup Message Communication Pattern

Parent window (main app):
```typescript
// Listen for message from popup
window.addEventListener('message', (event) => {
  if (event.origin !== window.location.origin) return;
  if (event.data.type === 'oauth-success') {
    // Handle success
    this.handleOAuthSuccess(event.data);
  } else if (event.data.type === 'oauth-error') {
    // Handle error
    this.handleOAuthError(event.data);
  }
});
```

Popup window (callback handler):
```typescript
// After backend returns response
window.opener.postMessage({
  type: 'oauth-success',
  data: {success: true, userId: 1, ...}
}, window.location.origin);
window.close();
```

### Sync Flow

After connection, when user clicks "Sync to Google Calendar":
1. Show modal with list of user's itineraries
2. User selects one itinerary
3. Call POST `/sync` with that itinerary ID
4. Show loading spinner
5. Display success: "Successfully synced 3 steps to Google Calendar!"
6. Close modal

## Backend Endpoints Summary

| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/auth-url` | GET | JWT | Get Google OAuth URL |
| `/oauth/callback` | GET | None | Google redirects here after user auth |
| `/status` | GET | JWT | Check if user connected to Google Calendar |
| `/sync` | POST | JWT | Sync selected itinerary to Google Calendar |
| `/disconnect` | DELETE | JWT | Remove Google Calendar connection |

## Success Criteria

✅ User clicks "Connect to Google Calendar" button
✅ OAuth popup opens with Google login
✅ User logs in and authorizes
✅ Popup closes automatically
✅ Main window shows "✅ Successfully connected!" toast
✅ UI updates to show Sync and Disconnect buttons
✅ User can select itinerary and click Sync
✅ Steps appear in user's Google Calendar
✅ User can disconnect calendar
✅ All errors are handled gracefully with user-friendly messages

## File Structure to Create/Modify

```
src/app/
  ├── services/
  │   └── google-calendar.service.ts (CREATE NEW)
  ├── components/
  │   └── planning-calendar/
  │       ├── planning-calendar.component.ts (MODIFY)
  │       ├── planning-calendar.component.html (MODIFY)
  │       ├── planning-calendar.component.scss (MODIFY)
  │       └── google-calendar-modal/ (CREATE NEW)
  │           ├── google-calendar-modal.component.ts
  │           ├── google-calendar-modal.component.html
  │           └── google-calendar-modal.component.scss
```

## Implementation Priority

1. **Phase 1:** Create GoogleCalendarService with OAuth popup handling
2. **Phase 2:** Add connection status UI to Planning Calendar
3. **Phase 3:** Add sync functionality with modal
4. **Phase 4:** Add disconnect functionality
5. **Phase 5:** Error handling and edge cases
6. **Phase 6:** UX polish (loading states, toast notifications)

---

## Questions to Clarify

Before implementing, consider:
1. Does your Planning Calendar component already exist? (Location in project?)
2. Are you using a Toast/Notification service? (Which library - Angular Material, ng-boostrap, custom?)
3. How does your app handle JWT tokens? (NgxAuthToken, localStorage, custom auth service?)
4. What's your itinerary model structure? (How to display in sync modal?)
5. Do you want modal or inline selection for choosing which itinerary to sync?

---

## Backend Already Done ✅

- Google Calendar entity, repository, and database schema
- OAuth token exchange and storage
- Google Calendar event creation and sync
- All 5 API endpoints fully implemented and tested
- Error handling and logging on backend
- Transactional operations for database integrity

## Frontend Still Needed ✅

Everything described above to handle the OAuth popup and sync workflow properly.
