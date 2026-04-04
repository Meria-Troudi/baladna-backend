# Event API Postman Collection Examples

## Base URL
```
http://localhost:8080
```

## Headers
```
Content-Type: application/json
```

---

## 1. Event Category Endpoints

### GET All Event Categories
```http
GET /api/events/event-category/list
```

**Response:** Array of EventCategory objects

### GET Event Category by ID
```http
GET /api/events/event-category/get/1
```

**Response:** Single EventCategory object

### POST Add Event Category
```http
POST /api/events/event-category/add
```

**Request Body:**
```json
{
  "name": "Sports",
  "description": "Sports and fitness events",
  "imageUrl": "https://example.com/sports.jpg",
  "icon": "sports-icon",
  "isActive": true
}
```

**Response:** Created EventCategory object

### PUT Update Event Category
```http
PUT /api/events/event-category/update
```

**Request Body:**
```json
{
  "id": 1,
  "name": "Sports & Fitness",
  "description": "Sports and fitness events updated",
  "imageUrl": "https://example.com/sports-updated.jpg",
  "icon": "sports-icon-updated",
  "isActive": true
}
```

**Response:** Updated EventCategory object

### DELETE Event Category
```http
DELETE /api/events/event-category/delete/1
```

**Response:** 204 No Content

---

## 2. Event Endpoints

### GET All Events
```http
GET /api/events/event/list
```

**Response:** Array of Event objects

### GET Event by ID
```http
GET /api/events/event/get/1
```

**Response:** Single Event object

### POST Add Event
```http
POST /api/events/event/add
```

**Request Body:**
```json
{
  "title": "Summer Basketball Tournament",
  "description": "Join us for an exciting basketball tournament this summer",
  "categoryId": 1,
  "startAt": "2024-07-15T10:00:00",
  "endAt": "2024-07-15T18:00:00",
  "location": "Sports Complex, Downtown",
  "latitude": 36.8065,
  "longitude": 10.1815,
  "capacity": 100,
  "price": 25.50,
  "createdByUserId": 1
}
```

**Response:** Created Event object

### PUT Update Event
```http
PUT /api/events/event/update
```

**Request Body:**
```json
{
  "id": 1,
  "title": "Summer Basketball Tournament Updated",
  "description": "Join us for an exciting basketball tournament this summer - Updated",
  "categoryId": 1,
  "startAt": "2024-07-15T10:00:00",
  "endAt": "2024-07-15T18:00:00",
  "location": "Sports Complex, Downtown",
  "latitude": 36.8065,
  "longitude": 10.1815,
  "capacity": 120,
  "price": 30.00,
  "createdByUserId": 1
}
```

**Response:** Updated Event object

### DELETE Event
```http
DELETE /api/events/event/delete/1
```

**Response:** 204 No Content

---

## 3. Event Comment Endpoints

### GET All Event Comments
```http
GET /api/events/event-comment/list
```

**Response:** Array of EventComment objects

### GET Event Comment by ID
```http
GET /api/events/event-comment/get/1
```

**Response:** Single EventComment object

### POST Add Event Comment
```http
POST /api/events/event-comment/add
```

**Request Body:**
```json
{
  "eventId": 1,
  "userId": 1,
  "content": "This event looks amazing! Can't wait to attend."
}
```

**Response:** Created EventComment object

### PUT Update Event Comment
```http
PUT /api/events/event-comment/update
```

**Request Body:**
```json
{
  "id": 1,
  "eventId": 1,
  "userId": 1,
  "content": "This event looks amazing! Can't wait to attend. Updated comment."
}
```

**Response:** Updated EventComment object

### DELETE Event Comment
```http
DELETE /api/events/event-comment/delete/1
```

**Response:** 204 No Content

---

## 4. Event Reservation Endpoints

### GET All Event Reservations
```http
GET /api/events/event-reservation/list
```

**Response:** Array of EventReservation objects

### GET Event Reservation by ID
```http
GET /api/events/event-reservation/get/1
```

**Response:** Single EventReservation object

### POST Add Event Reservation
```http
POST /api/events/event-reservation/add
```

**Request Body:**
```json
{
  "eventId": 1,
  "userId": 1,
  "personsCount": 2,
  "totalPrice": 51.00,
  "status": "CONFIRMED",
  "paymentStatus": "PAID"
}
```

**Response:** Created EventReservation object

### PUT Update Event Reservation
```http
PUT /api/events/event-reservation/update
```

**Request Body:**
```json
{
  "id": 1,
  "eventId": 1,
  "userId": 1,
  "personsCount": 3,
  "totalPrice": 76.50,
  "status": "CONFIRMED",
  "paymentStatus": "PAID"
}
```

**Response:** Updated EventReservation object

### DELETE Event Reservation
```http
DELETE /api/events/event-reservation/delete/1
```

**Response:** 204 No Content

---

## 5. Event Review Endpoints

### GET All Event Reviews
```http
GET /api/events/event-review/list
```

**Response:** Array of EventReview objects

### GET Event Review by ID
```http
GET /api/events/event-review/get/1
```

**Response:** Single EventReview object

### POST Add Event Review
```http
POST /api/events/event-review/add
```

**Request Body:**
```json
{
  "eventId": 1,
  "userId": 1,
  "reservationId": 1,
  "rating": 5,
  "comment": "Excellent event! Well organized and great atmosphere."
}
```

**Response:** Created EventReview object

### PUT Update Event Review
```http
PUT /api/events/event-review/update
```

**Request Body:**
```json
{
  "id": 1,
  "eventId": 1,
  "userId": 1,
  "reservationId": 1,
  "rating": 4,
  "comment": "Great event! Well organized and good atmosphere."
}
```

**Response:** Updated EventReview object

### DELETE Event Review
```http
DELETE /api/events/event-review/delete/1
```

**Response:** 204 No Content

---

## 6. Event Media Endpoints

### GET All Event Media
```http
GET /api/events/event-media/list
```

**Response:** Array of EventMedia objects

### GET Event Media by ID
```http
GET /api/events/event-media/get/1
```

**Response:** Single EventMedia object

### POST Add Event Media
```http
POST /api/events/event-media/add
```

**Request Body:**
```json
{
  "eventId": 1,
  "mediaUrl": "https://example.com/event-image.jpg",
  "mediaType": "IMAGE",
  "caption": "Event banner image",
  "isCover": true
}
```

**Response:** Created EventMedia object

### PUT Update Event Media
```http
PUT /api/events/event-media/update
```

**Request Body:**
```json
{
  "id": 1,
  "eventId": 1,
  "mediaUrl": "https://example.com/event-image-updated.jpg",
  "mediaType": "IMAGE",
  "caption": "Event banner image updated",
  "isCover": true
}
```

**Response:** Updated EventMedia object

### DELETE Event Media
```http
DELETE /api/events/event-media/delete/1
```

**Response:** 204 No Content

---

## 7. Event Forum Post Endpoints

### GET All Event Forum Posts
```http
GET /api/events/event-forum-post/list
```

**Response:** Array of EventForumPost objects

### GET Event Forum Post by ID
```http
GET /api/events/event-forum-post/get/1
```

**Response:** Single EventForumPost object

### POST Add Event Forum Post
```http
POST /api/events/event-forum-post/add
```

**Request Body:**
```json
{
  "eventId": 1,
  "userId": 1,
  "content": "Looking for 2 more players for basketball tournament. Contact me if interested!"
}
```

**Response:** Created EventForumPost object

### PUT Update Event Forum Post
```http
PUT /api/events/event-forum-post/update
```

**Request Body:**
```json
{
  "id": 1,
  "eventId": 1,
  "userId": 1,
  "content": "Looking for 2 more players for basketball tournament. Contact me if interested! Updated post."
}
```

**Response:** Updated EventForumPost object

### DELETE Event Forum Post
```http
DELETE /api/events/event-forum-post/delete/1
```

**Response:** 204 No Content

---

## 8. Event Forum Notification Endpoints

### GET All Event Forum Notifications
```http
GET /api/events/event-forum-notification/list
```

**Response:** Array of EventForumNotification objects

### GET Event Forum Notification by ID
```http
GET /api/events/event-forum-notification/get/1
```

**Response:** Single EventForumNotification object

### POST Add Event Forum Notification
```http
POST /api/events/event-forum-notification/add
```

**Request Body:**
```json
{
  "postId": 1,
  "recipientUserId": 1,
  "type": "NEW_REPLY",
  "message": "Someone replied to your forum post"
}
```

**Response:** Created EventForumNotification object

### PUT Update Event Forum Notification
```http
PUT /api/events/event-forum-notification/update
```

**Request Body:**
```json
{
  "id": 1,
  "postId": 1,
  "recipientUserId": 1,
  "type": "NEW_REPLY",
  "message": "Someone replied to your forum post",
  "isRead": true
}
```

**Response:** Updated EventForumNotification object

### DELETE Event Forum Notification
```http
DELETE /api/events/event-forum-notification/delete/1
```

**Response:** 204 No Content

---

## Usage Notes

1. **Authentication**: Add appropriate authentication headers if your API requires authentication
2. **Date Format**: Use ISO 8601 format for datetime fields (YYYY-MM-DDTHH:mm:ss)
3. **ID References**: Make sure referenced IDs (categoryId, userId, eventId, etc.) exist in the database
4. **Status Values**: Use appropriate status values based on your enum definitions
5. **Media Types**: For EventMedia, use appropriate media types like "IMAGE", "VIDEO", etc.
6. **Payment Status**: For reservations, use payment statuses like "PAID", "PENDING", "FAILED"
7. **Notification Types**: For forum notifications, use types like "NEW_REPLY", "NEW_POST", "MENTION"

## Common Response Formats

### Success Response (GET)
```json
{
  "id": 1,
  "title": "Sample Event",
  "description": "Event description",
  // ... other fields
}
```

### Success Response (POST/PUT)
```json
{
  "id": 1,
  "title": "Created/Updated Event",
  "description": "Event description",
  // ... other fields
}
```

### Error Response
```json
{
  "timestamp": "2024-06-01T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation error details",
  "path": "/api/events/event/add"
}
```
