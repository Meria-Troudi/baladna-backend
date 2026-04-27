# Test Scenarios & Validation Guide

This document provides comprehensive test scenarios for the AI Recommendation System.

## Pre-Test Setup

### 1. Start Application
```bash
mvn spring-boot:run
# or
./mvnw spring-boot:run
```

### 2. Generate Training Data
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/train/generate
```

**Expected Response:**
```json
{
  "success": true,
  "recordsGenerated": <number_of_itineraries>,
  "message": "Training data generated for X itineraries"
}
```

### 3. Verify Statistics
```bash
curl -X GET http://localhost:8081/api/itinerary/recommendations/train/statistics
```

Should show training records count > 0.

## Test Scenarios

### ✅ TEST 1: Basic Search Without Filters

**Request:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "limit": 3
  }'
```

**Expected Result:**
- ✓ HTTP 200 OK
- ✓ `"success": true`
- ✓ `"count": 3` (or less if not enough data)
- ✓ Each recommendation has: `itineraryId`, `title`, `budget`, `similarityScore`

---

### ✅ TEST 2: Search by Budget Only

**Request:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "maxBudget": 3000,
    "limit": 5
  }'
```

**Expected Result:**
- ✓ HTTP 200 OK
- ✓ All returned recommendations have `budget` <= 3000
- ✓ Multiple results if budget range is reasonable

**Validation Queries:**
```bash
# All returned budgets should be ≤ 3000
for rec in recommendations: assert rec.budget <= 3000
```

---

### ✅ TEST 3: Search by Location

**Request:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "location": "Sousse",
    "limit": 5
  }'
```

**Expected Result:**
- ✓ HTTP 200 OK
- ✓ Results contain "Sousse" in destination (unless fuzzy matching)
- ✓ Similarity scores should be reasonable

---

### ✅ TEST 4: Search by Duration

**Request:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "minDuration": 3,
    "maxDuration": 7,
    "limit": 5
  }'
```

**Expected Result:**
- ✓ HTTP 200 OK
- ✓ All returned: 3 ≤ `durationDays` ≤ 7

---

### ✅ TEST 5: Combined Filters

**Request:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "maxBudget": 5000,
    "location": "Sousse",
    "minDuration": 3,
    "maxDuration": 7,
    "limit": 5
  }'
```

**Expected Result:**
- ✓ HTTP 200 OK
- ✓ All results satisfy ALL criteria:
  - budget ≤ 5000
  - location contains "Sousse"
  - 3 ≤ duration ≤ 7

---

### ✅ TEST 6: Filter by Rating

**Request:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "minRating": 4.0,
    "limit": 5
  }'
```

**Expected Result:**
- ✓ HTTP 200 OK
- ✓ All returned: `rating` >= 4.0
- ✓ Fewer results than without filter (more restrictive)

---

### ✅ TEST 7: Exact Location Match

**Request:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "location": "Sousse",
    "exactLocationMatch": true,
    "limit": 10
  }'
```

**Expected Result:**
- ✓ HTTP 200 OK
- ✓ Only exact "Sousse" matches returned
- ✓ Case-insensitive matching

**vs. Fuzzy Match:**
```bash
# Fuzzy (default)
"location": "Sousse",
"exactLocationMatch": false

# Should return more results than exact match
```

---

### ✅ TEST 8: Similar Recommendations

**First: Get an itinerary ID**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{"limit": 1}'
```

**Save the `itineraryId` from response, then use it:**
```bash
curl -X GET "http://localhost:8081/api/itinerary/recommendations/similar/{ITINERARY_ID}?limit=5"
```

**Expected Result:**
- ✓ HTTP 200 OK
- ✓ `"count": 5` (or less if not enough similar)
- ✓ Original itinerary NOT in results
- ✓ Similar location/budget as original

---

### ✅ TEST 9: Similarity Score Validation

**Request:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "maxBudget": 5000,
    "location": "Sousse",
    "limit": 5
  }'
```

**Validation:**
```javascript
// Check all similarity scores are between 0 and 1
for (let rec of recommendations) {
  assert(rec.similarityScore >= 0);
  assert(rec.similarityScore <= 1);
}

// Verify results are sorted by similarity (highest first)
for (let i = 0; i < recommendations.length - 1; i++) {
  assert(recommendations[i].similarityScore >= 
         recommendations[i + 1].similarityScore);
}
```

---

### ✅ TEST 10: Limit Parameter

**Request with limit=1:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{"limit": 1}'
```

**Expected:** `"count": 1`

**Request with limit=50:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{"limit": 50}'
```

**Expected:** `"count": min(50, total_records)`

---

### ✅ TEST 11: Empty Results

**Request with impossible filters:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "maxBudget": 100,
    "location": "NonexistentPlace"
  }'
```

**Expected Result:**
- ✓ HTTP 200 OK
- ✓ `"success": true`
- ✓ `"count": 0`
- ✓ `"recommendations": []`
- ✓ Helpful message in response

---

### ✅ TEST 12: Invalid Request Parameters

**Bad Request - negative budget:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "maxBudget": -5000
  }'
```

**Expected:** HTTP 400 (Bad Request) or handled gracefully

---

### ✅ TEST 13: Training Data Generation

**Generate from existing:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/train/generate
```

**Expected:**
- ✓ HTTP 200 OK
- ✓ `"recordsGenerated": <number>`
- ✓ Can be called multiple times (updates existing)

---

### ✅ TEST 14: Synthetic Data Generation

**Generate 100 synthetic records:**
```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/train/synthetic?count=100
```

**Expected:**
- ✓ HTTP 200 OK
- ✓ `"recordsGenerated": 100`
- ✓ New records appear in statistics

---

### ✅ TEST 15: Data Normalization

**Before and after check:**
```bash
# 1. Generate data
curl -X POST http://localhost:8081/api/itinerary/recommendations/train/generate

# 2. Normalize
curl -X POST http://localhost:8081/api/itinerary/recommendations/train/normalize

# 3. Export and verify
curl -X GET http://localhost:8081/api/itinerary/recommendations/train/export
```

**Expected:**
- ✓ HTTP 200 OK
- ✓ Normalized values are between 0 and 1
- ✓ CSV shows normalized columns populated

---

### ✅ TEST 16: Export Training Data

```bash
curl -X GET http://localhost:8081/api/itinerary/recommendations/train/export \
  -o training_data.csv
```

**Validation:**
```bash
# Check CSV format
head training_data.csv

# Should have headers:
# itinerary_id,location,budget,duration_days,avg_daily_cost,num_steps,...

# Validate CSV can be imported
wc -l training_data.csv  # Should have multiple records
```

---

### ✅ TEST 17: Statistics Endpoint

```bash
curl -X GET http://localhost:8081/api/itinerary/recommendations/train/statistics
```

**Expected:**
```json
{
  "success": true,
  "statistics": "=== Training Data Statistics ===\n
    Total Records: XXX\n
    Average Budget: XXX\n
    Average Duration: XXX days\n
    Average Rating: XXX/5.0\n
    Unique Locations: XXX\n"
}
```

---

### ✅ TEST 18: Response Time Check

Use Apache Bench or curl with timing:

```bash
curl -w "\nTotal time: %{time_total}s\n" \
  -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{"maxBudget": 5000, "limit": 5}'
```

**Expected:**
- ✓ First call: 50-200ms (depending on data size)
- ✓ Subsequent calls: <50ms (cached)

---

### ✅ TEST 19: Concurrency Test

Run multiple requests simultaneously:

```bash
# Terminal 1
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -d '{"maxBudget": 5000, "limit": 5}' &

# Terminal 2
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -d '{"location": "Sousse", "limit": 5}' &

# Terminal 3
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -d '{"minDuration": 5, "limit": 5}' &

wait
```

**Expected:** All succeed without errors

---

### ✅ TEST 20: Error Handling

**Test various error conditions:**

```bash
# Missing required connection
systemctl stop mysql  # Stop database

curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -d '{"limit": 5}'

# Expected: HTTP 500 with error message
```

## Performance Tests

### Load Test - 100 concurrent requests

Using Apache Bench:
```bash
ab -n 100 -c 10 -p request.json -T application/json \
  http://localhost:8081/api/itinerary/recommendations/search
```

**Expected:**
- ✓ Response time: < 500ms avg
- ✓ 0 failures
- ✓ Requests/sec: > 20

### Data Size Test

1. Generate 10,000 synthetic records
2. Run search test
3. Monitor memory usage
4. Check response time increases are acceptable

---

## Integration Tests

### Test with Frontend

```typescript
// Angular/TypeScript test
it('should get recommendations', (done) => {
  service.searchRecommendations({
    maxBudget: 5000,
    location: 'Sousse',
    limit: 5
  }).subscribe(response => {
    expect(response.success).toBe(true);
    expect(response.recommendations.length).toBeGreaterThan(0);
    done();
  });
});
```

---

## Acceptance Criteria

| Criterion | Status |
|-----------|--------|
| API returns recommendations with correct filters | ✓ |
| Similarity scores between 0 and 1 | ✓ |
| Results sorted by similarity (highest first) | ✓ |
| Response time < 200ms for 1000 records | ✓ |
| Handles empty results gracefully | ✓ |
| Training data can be generated | ✓ |
| Synthetic data generation works | ✓ |
| Export to CSV works | ✓ |
| Statistics accurate | ✓ |
| Normalization produces 0-1 range | ✓ |

---

## Checklist for Full Validation

- [ ] All 20 test scenarios pass
- [ ] Response times acceptable
- [ ] Error handling works correctly
- [ ] Database migration successful
- [ ] Configuration loaded properly
- [ ] Security endpoints protected
- [ ] Frontend integration complete
- [ ] Documentation accurate
- [ ] Code reviewed for quality
- [ ] Performance baseline established

---

## Debugging Tips

### Enable SQL Logging
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### Check Logs
```bash
tail -f application.log | grep recommendation
```

### Verify Database
```sql
SELECT COUNT(*) FROM training_dataset;
SELECT * FROM training_dataset LIMIT 5;
```

### Test Python Script
```bash
python3 generate_training_data.py --count 10 --stats
```

---

## Test Report Template

```
Test Date: YYYY-MM-DD
Tester: Name
Status: [PASS/FAIL]

Passed Tests: XX/20
Failed Tests: XX/20

Issues Found:
1. [Issue description]

Performance Notes:
- Avg Response Time: XXms
- Memory Usage: XXmb

Recommendations:
1. [Recommendation]

Signature: _______
```
