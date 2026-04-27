# AI Recommendation System - Usage Examples

This document provides practical examples of how to use the AI Itinerary Recommendation System.

## Setup

### 1. Generate Training Data from Existing Itineraries

First, extract features from your existing itineraries:

```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/train/generate
```

**Response:**
```json
{
  "success": true,
  "recordsGenerated": 45,
  "message": "Training data generated for 45 itineraries"
}
```

### 2. (Optional) Generate Synthetic Training Data

If you need more training data for testing:

```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/train/synthetic?count=1000
```

This generates 1000 synthetic itineraries with realistic data based on Tunisian locations.

### 3. Normalize the Training Data

Prepare the data for ML processing:

```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/train/normalize
```

## Search Examples

### Example 1: Budget Travel to Sousse

Find affordable trips to Sousse (3-7 days, max 3000 TND):

```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "maxBudget": 3000,
    "location": "Sousse",
    "limit": 5,
    "minDuration": 3,
    "maxDuration": 7,
    "exactLocationMatch": true
  }'
```

**Sample Response:**
```json
{
  "success": true,
  "count": 3,
  "recommendations": [
    {
      "itineraryId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "Sousse Summer Escape",
      "description": "Beautiful beach town with great nightlife",
      "destination": "Sousse",
      "budget": 2800.00,
      "durationDays": 5,
      "startDate": "2024-06-15",
      "endDate": "2024-06-20",
      "numSteps": 8,
      "avgDailyCost": 560.00,
      "similarityScore": 0.9200,
      "rating": 4.50,
      "numCollaborators": 3,
      "recommendationReason": "Highly relevant itinerary. Budget: 2800.0. Located in your preferred region. Includes 8 activities."
    },
    {
      "itineraryId": "660e8400-e29b-41d4-a716-446655440001",
      "title": "Sousse Beach & Culture Mix",
      "destination": "Sousse",
      "budget": 2950.00,
      "durationDays": 6,
      "startDate": "2024-07-01",
      "endDate": "2024-07-07",
      "numSteps": 10,
      "avgDailyCost": 491.67,
      "similarityScore": 0.8750,
      "rating": 4.25,
      "numCollaborators": 4,
      "recommendationReason": "Highly relevant itinerary. Budget: 2950.0. Located in your preferred region. Includes 10 activities."
    }
  ]
}
```

### Example 2: Luxury Desert Adventure

Find luxury trips to Tozeur (8-14 days, 10000-15000 TND):

```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "maxBudget": 15000,
    "location": "Tozeur",
    "limit": 3,
    "minDuration": 8,
    "maxDuration": 14
  }'
```

### Example 3: Flexible Budget, Specific Location

Find anything in Djerba under 5000 TND (any duration):

```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "maxBudget": 5000,
    "location": "Djerba",
    "limit": 10
  }'
```

### Example 4: High-Quality Itineraries Only

Only show highly-rated trips:

```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "maxBudget": 8000,
    "location": "Hammamet",
    "limit": 5,
    "minRating": 4.0
  }'
```

### Example 5: Short & Affordable

Quick weekends trips under 1500 TND for 2-3 days:

```bash
curl -X POST http://localhost:8081/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "maxBudget": 1500,
    "minDuration": 2,
    "maxDuration": 3,
    "limit": 5
  }'
```

## Similarity Search Examples

### Find Similar Itineraries

Get itineraries similar to a specific trip:

```bash
# Get 5 similar recommendations to itinerary ID 550e8400-e29b-41d4-a716-446655440000
curl -X GET "http://localhost:8081/api/itinerary/recommendations/similar/550e8400-e29b-41d4-a716-446655440000?limit=5"
```

**Response:**
```json
{
  "success": true,
  "count": 5,
  "recommendations": [
    {
      "itineraryId": "550e8400-e29b-41d4-a716-446655440001",
      "title": "Similar Sousse Trip",
      "destination": "Sousse",
      "budget": 2900.00,
      "durationDays": 5,
      "similarityScore": 0.8900,
      ...
    }
  ]
}
```

## Data Management Examples

### Check Statistics

```bash
curl -X GET http://localhost:8081/api/itinerary/recommendations/train/statistics
```

**Response:**
```json
{
  "success": true,
  "statistics": "=== Training Data Statistics ===\nTotal Records: 1045\nAverage Budget: 4567.89\nAverage Duration: 6.2 days\nAverage Rating: 4.15/5.0\nUnique Locations: 15\n"
}
```

### Export Training Data

```bash
# Export as CSV
curl -X GET http://localhost:8081/api/itinerary/recommendations/train/export \
  -o training_data.csv

# Then use with Python/Excel for further analysis
```

## Python Script Usage

### Generate Standalone Dataset

Generate your own training dataset using the Python script:

```bash
# Generate 2000 records
python3 src/main/resources/scripts/generate_training_data.py \
  --count 2000 \
  --output my_training_data.csv \
  --stats

# Output:
# Generating my_training_data.csv
# === Training Data Statistics ===
# Total Records: 2000
# Budget Range: $521.43 - $19876.54
# Average Budget: $8234.56
# ...
```

### Export to JSON

```bash
python3 src/main/resources/scripts/generate_training_data.py \
  --count 500 \
  --output training_data \
  --json \
  --stats
```

This creates:
- `training_data.csv`
- `training_data.json`

## Integration Examples

### In Frontend (JavaScript/Angular)

```typescript
// Angular service for recommendations
import { HttpClient } from '@angular/common/http';

@Injectable()
export class RecommendationService {
  private baseUrl = '/api/itinerary/recommendations';

  constructor(private http: HttpClient) {}

  // Search recommendations
  searchRecommendations(criteria: RecommendationRequest) {
    return this.http.post(`${this.baseUrl}/search`, criteria);
  }

  // Get similar recommendations
  getSimilarRecommendations(itineraryId: string, limit: number = 5) {
    return this.http.get(
      `${this.baseUrl}/similar/${itineraryId}?limit=${limit}`
    );
  }
}

// Usage in component
this.recommendationService.searchRecommendations({
  maxBudget: 5000,
  location: 'Sousse',
  minDuration: 3,
  maxDuration: 7,
  limit: 5
}).subscribe(response => {
  this.recommendations = response.recommendations;
  this.displayRecommendations();
});
```

### In Java/Spring

```java
@Service
public class TravelPlannerService {
  
  @Autowired
  private ItineraryRecommendationService recommendationService;

  public void suggestTripForUser(User user, TravelPreferences prefs) {
    RecommendationRequest request = RecommendationRequest.builder()
      .maxBudget(prefs.getMaxBudget())
      .location(prefs.getPreferredLocation())
      .minDuration(prefs.getMinDays())
      .maxDuration(prefs.getMaxDays())
      .limit(10)
      .build();

    List<RecommendationResponse> suggestions = 
      recommendationService.getRecommendations(request);

    // Send recommendations to user
    suggestions.forEach(rec -> sendNotification(user, rec));
  }
}
```

## Performance Tips

1. **Limit Requests**: Start with smaller limits (5-10) for faster responses
2. **Use Location Filter**: Filtering by location reduces computation
3. **Cache Results**: The system caches recommendations when enabled
4. **Pre-Generate Data**: Generate training data during off-peak hours
5. **Index Database**: Ensure database has indexes on frequently queried columns

## Troubleshooting

### Problem: "No recommendations found"
**Solution**: 
- Ensure training data exists: `GET /api/itinerary/recommendations/train/statistics`
- Try relaxing filters (remove minRating, use fuzzy location matching)
- Set `exactLocationMatch: false`

### Problem: "Slow response times"
**Solution**:
- Reduce the `limit` parameter
- Disable exact location matching
- Clear cache: restart the application

### Problem: "Low similarity scores"
**Solution**:
- Check feature weights in application.properties
- Regenerate training data with more itineraries
- Adjust min similarity threshold

## API Response Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 400 | Bad request (invalid parameters) |
| 500 | Server error |
| 503 | Service temporarily unavailable |

## Next Steps

- Monitor recommendation quality with user feedback
- Implement user rating system for recommendations
- Add recommendation analytics dashboard
- Consider advanced ML models for better accuracy
