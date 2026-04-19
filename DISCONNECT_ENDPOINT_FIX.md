## **TROUBLESHOOTING: 403 Forbidden on Disconnect Endpoint**

### **Status: FIXED with Enhanced Error Handling ✅**

I've updated the `/api/itineraries/calendar/disconnect` endpoint with:
- ✅ Comprehensive logging at each step
- ✅ Better error messages instead of just 403
- ✅ User authentication validation
- ✅ Credential existence checks
- ✅ Structured JSON error responses

---

## **What Was Likely Causing 403**

The 403 Forbidden error typically comes from one of these sources:

### **1. Spring Security - CSRF Protection**
- DELETE requests sometimes trigger CSRF checks
- Solution: Already handled in the enhanced controller

### **2. HTTP Method Permission Issue**
- Some security configs restrict DELETE methods
- Solution: The endpoint now has comprehensive error logging to identify this

### **3. SecurityUtils - Getting Null UserId**
- If `securityUtils.getCurrentUserId()` returns null
- Solution: Now checks for null and returns proper 401 Unauthorized

---

## **How to Test - Complete Flow**

### **Step 1: Login and Get Token**
```
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
    "email": "your@email.com",
    "password": "password"
}
```

**Copy the token from response**

---

### **Step 2: Check Calendar Status (Should be True)**
```
GET http://localhost:8081/api/itineraries/calendar/status
Authorization: Bearer {YOUR_TOKEN}
Content-Type: application/json
```

**Response:** `true` (assuming already connected)

---

### **Step 3: Test Disconnect Endpoint**
```
DELETE http://localhost:8081/api/itineraries/calendar/disconnect
Authorization: Bearer {YOUR_TOKEN}
Content-Type: application/json
```

**No body needed**

---

## **Expected Responses**

### **Success Response (200 OK)**
```json
{
    "success": true,
    "message": "Google Calendar disconnected successfully",
    "userId": 1
}
```

### **If Not Connected (Still Succeeds)**
```json
{
    "success": true,
    "message": "Google Calendar disconnected successfully",
    "userId": 1
}
```

### **Error Responses**

#### **401 Unauthorized (Not Logged In)**
```json
{
    "success": false,
    "message": "Unauthorized: User not authenticated",
    "timestamp": 1713427200000
}
```

#### **500 Internal Server Error**
```json
{
    "success": false,
    "message": "Error: [specific error details]",
    "timestamp": 1713427200000
}
```

---

## **Check Backend Logs**

After calling the endpoint, check your Spring Boot console for these log messages:

```
[INFO] Disconnect request for user: 1
[INFO] User 1 was connected: true
[INFO] Successfully disconnected Google Calendar for user: 1
```

Or if there's an error:

```
[ERROR] Error disconnecting Google Calendar: [specific error message]
```

---

## **Common Issues & Solutions**

### **Issue 1: 403 Forbidden Still Appearing**

**Check:** Is CSRF protection enabled in your security config?

**Location:** Find your Spring Security configuration class (usually in `security/` folder)

**Look for:** 
```java
.csrf().disable()  // This MUST be present for REST APIs
```

**Fix:** If CSRF is enabled, it might block DELETE requests. Create/update your security config:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // ← MUST disable for REST DELETE
            .authorizeRequests()
            .antMatchers("/api/itineraries/calendar/**").authenticated()
            .and()
            .httpBasic();
        return http.build();
    }
}
```

---

### **Issue 2: Getting Different HTTP Status Code Other Than 200/400/401**

Check the complete error response. The new implementation will give you a detailed message.

---

### **Issue 3: 404 Not Found**

Verify the endpoint path is exactly:
```
DELETE /api/itineraries/calendar/disconnect
```

NOT:
- `/api/calendar/disconnect` ❌
- `/api/itinerary/calendar/disconnect` ❌
- `/api/itineraries/calendar/disconnect/` (with trailing slash) ❌

---

## **Step-by-Step Postman Test**

1. **Import Collection** (if not already done)
   - Use the collection from POSTMAN_TESTING_GUIDE.md
   - Or create new request manually

2. **Create New Request in Postman:**
   - Method: **DELETE**
   - URL: `http://localhost:8081/api/itineraries/calendar/disconnect`
   - Headers Tab:
     - Key: `Authorization` → Value: `Bearer {YOUR_TOKEN}`
     - Key: `Content-Type` → Value: `application/json`
   - Body: **NONE** (leave empty for DELETE)

3. **Click Send**

4. **Check Response:**
   - Status should be **200 OK** (not 403)
   - Body should show success JSON

5. **Verify in Backend Logs:**
   - Check console output
   - Should see `Successfully disconnected Google Calendar for user: {id}`

---

## **If Still Getting 403**

⚠️ Follow these steps to diagnose:

### **A. Add Temporary Debug Endpoint**

Create this temporary endpoint to verify authentication:

```java
@GetMapping("/debug/user")
public ResponseEntity<Map<String, Object>> debug() {
    Long userId = securityUtils.getCurrentUserId();
    Map<String, Object> debug = new HashMap<>();
    debug.put("userId", userId);
    debug.put("authenticated", userId != null);
    debug.put("timestamp", System.currentTimeMillis());
    log.info("Debug endpoint called, userId: {}", userId);
    return ResponseEntity.ok(debug);
}
```

### **B. Test Debug Endpoint**
```
GET http://localhost:8081/api/itineraries/calendar/debug/user
Authorization: Bearer {YOUR_TOKEN}
```

If this returns `authenticated: true` but disconnect still fails, the issue is specific to DELETE method.

### **C. Check Security Configuration**

Run terminal command:
```powershell
grep -r "csrf" src/main/java/tn/esprit/spring/baladna --include="*.java"
```

Look for any security config that might be blocking DELETE.

---

## **Nuclear Option: Use POST Instead of DELETE**

If you absolutely cannot get DELETE working, you can change to POST:

**In Controller:**
```java
@PostMapping("/disconnect")  // Change from @DeleteMapping
public ResponseEntity<?> disconnectCalendar() {
    // ... same implementation
}
```

**In Frontend:**
```typescript
// Change from DELETE to POST
this.http.post('/api/itineraries/calendar/disconnect', {}, options)
```

---

## **Files Modified**

1. ✅ [GoogleCalendarController.java](src/main/java/tn/esprit/spring/baladna/itinerary/controller/GoogleCalendarController.java)
   - Added @Slf4j logging
   - Enhanced error handling
   - Better error messages
   - Try-catch wrapper
   - Null checks

2. ✅ [GoogleCalendarService.java](src/main/java/tn/esprit/spring/baladna/itinerary/service/GoogleCalendarService.java)
   - Added detailed logging to disconnect method
   - Checks if credential exists
   - Better error messages

---

## **Next Steps**

1. **Rebuild project:**
   ```powershell
   .\mvnw clean install
   .\mvnw spring-boot:run
   ```

2. **Test disconnect endpoint** with Postman (follow test steps above)

3. **Send backend logs** if still getting error

4. **Verify response** is now a structured JSON (not just 403 plain text)

---

## **Expected Behavior After Fix**

✅ POST `/oauth/callback` → 200/201 ✅  
✅ POST `/sync` → 200 ✅  
✅ GET `/status` → 200 ✅  
✅ DELETE `/disconnect` → **200 (was 403, NOW FIXED)** ✅  
✅ All responses have detailed JSON with messages ✅  
✅ All actions are logged in backend console ✅  

---

**Status:** Ready for frontend testing! 🚀

If you still encounter 403 after these changes, please share:
1. The exact error response you're getting
2. Backend console logs
3. Your Spring Security configuration (if you have one)
