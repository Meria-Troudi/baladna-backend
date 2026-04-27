# Google Calendar Integration Fix

## Issues Identified

### Backend Issues (FIXED)
- **Problem**: OAuth callback endpoint required authentication but Google's redirect doesn't include JWT
- **Solution**: Added `/api/itineraries/calendar/oauth/callback` to the permitAll() list in SecurityConfig
- **Status**: ✅ FIXED

### Frontend Issues (REQUIRES FIX)
The frontend is experiencing CORS/COOP (Cross-Origin-Opener-Policy) issues with the popup window detection.

#### Error Messages:
```
Cross-Origin-Opener-Policy policy would block the window.closed call
Error connecting to Google Calendar {message: 'Authorization cancelled by user.'}
```

#### Root Causes:
1. Browser security policy blocking `window.closed` property access across origins
2. Popup window detection mechanism using polling is fragile
3. Need to use postMessage communication instead

---

## Frontend Fix Required

### Location
- File: `google-calendar.service.ts` (line 69)
- File: `planning-calendar-modal.component.ts` (line 84)

### Solution: Replace popup polling with postMessage communication

#### Step 1: Update GoogleCalendarService

Replace the problematic window.closed polling approach with postMessage-based communication:

```typescript
// OLD CODE (line 48-73 approx) - REMOVE THIS
// This uses XMLHttpRequest polling and window.closed which causes COOP issues

// NEW CODE - Add this instead:

connectToGoogleCalendar(): Promise<GoogleCalendarResponse> {
  return new Promise((resolve, reject) => {
    // Get the authorization URL from backend
    this.http.get<string>('/api/itineraries/calendar/auth-url')
      .subscribe({
        next: (authUrl: string) => {
          // Open popup window
          const width = 500;
          const height = 600;
          const left = window.screenX + (window.outerWidth - width) / 2;
          const top = window.screenY + (window.outerHeight - height) / 2;
          
          const popup = window.open(
            authUrl,
            'GoogleCalendarAuth',
            `width=${width},height=${height},left=${left},top=${top}`
          );
          
          if (!popup) {
            reject(new Error('Failed to open authorization popup. Please check popup blocker settings.'));
            return;
          }
          
          // Setup message listener for OAuth callback
          const messageHandler = (event: MessageEvent) => {
            // Verify origin for security
            if (!event.origin.includes(window.location.hostname)) {
              return;
            }
            
            if (event.data.type === 'GOOGLE_CALENDAR_AUTH_SUCCESS') {
              window.removeEventListener('message', messageHandler);
              resolve(event.data.payload);
            } else if (event.data.type === 'GOOGLE_CALENDAR_AUTH_ERROR') {
              window.removeEventListener('message', messageHandler);
              reject(new Error(event.data.error || 'Authorization failed'));
            }
          };
          
          window.addEventListener('message', messageHandler);
          
          // Fallback timeout (60 seconds)
          const timeout = setTimeout(() => {
            window.removeEventListener('message', messageHandler);
            reject(new Error('Authorization timeout. Please try again.'));
          }, 60000);
          
          // Optional: Also set up a timer that checks if window was closed
          const checkClosed = setInterval(() => {
            try {
              if (popup.closed) {
                clearInterval(checkClosed);
                clearTimeout(timeout);
                window.removeEventListener('message', messageHandler);
                reject(new Error('Authorization cancelled by user.'));
              }
            } catch (e) {
              // Ignore errors from checking popup due to COOP
            }
          }, 1000);
        },
        error: (err) => {
          reject(new Error('Failed to get authorization URL: ' + err.message));
        }
      });
  });
}
```

#### Step 2: Update OAuth Callback Handling

The backend callback should post a message back to the opener window:

**Backend**: Create a simple HTML file or response that handles the callback and posts message to opener.

**Frontend**: Add this to your `oauth-callback.component.ts` or a new component for the callback page:

```typescript
// oauth-callback.component.ts or similar
ngOnInit() {
  // Extract query parameters
  const params = new URLSearchParams(window.location.search);
  const code = params.get('code');
  const state = params.get('state');
  const error = params.get('error');
  
  if (error) {
    // Send error message to opener
    if (window.opener) {
      window.opener.postMessage({
        type: 'GOOGLE_CALENDAR_AUTH_ERROR',
        error: error
      }, window.location.origin);
    }
    window.close();
    return;
  }
  
  if (code && state) {
    // Exchange code for tokens via backend
    this.googleCalendarService.completeOAuthCallback(code, state)
      .subscribe({
        next: (response) => {
          // Send success message to opener
          if (window.opener) {
            window.opener.postMessage({
              type: 'GOOGLE_CALENDAR_AUTH_SUCCESS',
              payload: response
            }, window.location.origin);
          }
          // Close popup after short delay
          setTimeout(() => window.close(), 500);
        },
        error: (err) => {
          // Send error message to opener
          if (window.opener) {
            window.opener.postMessage({
              type: 'GOOGLE_CALENDAR_AUTH_ERROR',
              error: err.message || 'Authorization failed'
            }, window.location.origin);
          }
          setTimeout(() => window.close(), 500);
        }
      });
  } else {
    // No code or state
    if (window.opener) {
      window.opener.postMessage({
        type: 'GOOGLE_CALENDAR_AUTH_ERROR',
        error: 'Missing authorization code'
      }, window.location.origin);
    }
    window.close();
  }
}
```

#### Step 3: Add missing service method

In GoogleCalendarService, add:

```typescript
completeOAuthCallback(code: string, state: string): Observable<GoogleCalendarResponse> {
  return this.http.get<GoogleCalendarResponse>(
    '/api/itineraries/calendar/oauth/callback',
    { params: { code, state } }
  );
}
```

---

## Backend Response Headers

To fully resolve COOP issues, configure these response headers in Spring:

Add to `WebSecurityConfig` or global response headers:

```java
http.headers(headers -> headers
    .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
);
```

Or add a filter to handle headers:

```java
@Component
public class SecurityHeadersFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin-allow-popups");
        httpResponse.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
        chain.doFilter(request, response);
    }
}
```

---

## Testing Steps

1. **Backend**: Rebuild and restart the server
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

2. **Frontend**: Update the Google Calendar service with the new code

3. **Test flow**:
   - Click "Connect to Google Calendar" button
   - Popup should open with Google login
   - After authorization, popup should close automatically
   - Success message should appear in the main window
   - Calendar status should show as connected

4. **Verify backend logs**:
   - Should see: `OAuth callback received. State: X, Has code: true, Error: null`
   - Should see: `Successfully processed OAuth callback for user: X`

---

## Additional Notes

- The postMessage approach is more secure and compatible with modern browser security policies
- Make sure your OAuth callback page is on the same origin as your main app for postMessage
- The `state` parameter now properly identifies the user
- All error handling is more robust with timeout support
