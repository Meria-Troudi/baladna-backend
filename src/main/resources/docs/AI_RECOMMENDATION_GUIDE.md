# AI Itinerary Recommendation System

## Overview

This AI-powered recommendation system recommends travel itineraries based on user preferences including:
- **Budget constraints** - Find itineraries within your price range
- **Location preferences** - Discover trips to your desired destinations
- **Duration** - Filter by trip length
- **Complexity** - Find itineraries matching your activity level

The system uses **k-nearest neighbors (k-NN) collaborative filtering** to find the most similar itineraries to your criteria.

## Architecture

### Key Components

1. **TrainingDataset Entity** - Stores extracted features from itineraries for ML model
2. **ItineraryRecommendationService** - Core recommendation engine using k-NN algorithm
3. **TrainingDataGeneratorService** - Extracts features from itineraries and generates datasets
4. **ItineraryRecommendationController** - REST API endpoints for recommendations
5. **RecommendationUtils** - Helper utilities for ML calculations

## Features

### 1. Smart Recommendations
- Uses cosine similarity to find similar itineraries
- Considers multiple factors: budget, location, duration, complexity
- Boosts recommendations with high ratings
- Returns similarity scores for transparency

### 2. Training Data Management
- Automatic extraction of features from existing itineraries
- Normalization of data for ML models
- Support for synthetic data generation for testing
- CSV export for external analysis

### 3. Multiple Recommendation Modes
- **Search-based**: Find recommendations matching specific criteria
- **Similarity-based**: Find itineraries similar to a specific trip
- **Related**: Get alternatives to a trip you're viewing

## API Endpoints

### Search for Recommendations
```
POST /api/itinerary/recommendations/search
Content-Type: application/json

{
  "maxBudget": 5000,
  "location": "Sousse",
  "limit": 10,
  "minDuration": 3,
  "maxDuration": 14,
  "kNeighbors": 5,
  "minRating": 3.5,
  "exactLocationMatch": false
}

Response:
{
  "success": true,
  "count": 5,
  "recommendations": [
    {
      "itineraryId": "uuid",
      "title": "Beach Getaway",
      "destination": "Sousse",
      "budget": 4500,
      "durationDays": 7,
      "avgDailyCost": 642.86,
      "similarityScore": 0.8421,
      "rating": 4.5,
      "recommendationReason": "Highly relevant itinerary..."
    }
  ]
}
```

### Get Similar Recommendations
```
GET /api/itinerary/recommendations/similar/{itineraryId}?limit=5

Response:
{
  "success": true,
  "count": 4,
  "recommendations": [...]
}
```

### Training Data Management

#### Generate Training Data
```
POST /api/itinerary/recommendations/train/generate

Response:
{
  "success": true,
  "recordsGenerated": 45,
  "message": "Training data generated for 45 itineraries"
}
```

#### Generate Synthetic Data
```
POST /api/itinerary/recommendations/train/synthetic?count=1000

Response:
{
  "success": true,
  "recordsGenerated": 1000,
  "message": "Synthetic training data generated"
}
```

#### Export Training Data as CSV
```
GET /api/itinerary/recommendations/train/export

Returns: CSV file with training data
```

#### Get Statistics
```
GET /api/itinerary/recommendations/train/statistics

Response:
{
  "success": true,
  "statistics": "=== Training Data Statistics ===\nTotal Records: 45\n..."
}
```

#### Normalize Data
```
POST /api/itinerary/recommendations/train/normalize

Response:
{
  "success": true,
  "message": "Training data normalized successfully"
}
```

## Quick Start

### 1. Setup Database
Ensure the `TrainingDataset` table is created (via migration).

### 2. Generate Initial Training Data
```bash
curl -X POST http://localhost:8080/api/itinerary/recommendations/train/generate
```

### 3. Search for Recommendations
```bash
curl -X POST http://localhost:8080/api/itinerary/recommendations/search \
  -H "Content-Type: application/json" \
  -d '{
    "maxBudget": 5000,
    "location": "Sousse",
    "limit": 5
  }'
```

## Algorithm Details

### Cosine Similarity
The system uses **cosine similarity** to measure how similar two itineraries are:

```
similarity = (A·B) / (||A|| ||B||)
```

Where vectors contain normalized features:
- Budget (0.0 to 1.0)
- Duration (0.0 to 1.0)
- Complexity (0.0 to 1.0)

### Feature Normalization
Features are normalized to [0, 1] range:
```
normalized = (value - min) / (max - min)
```

### Rating Boost
Final scores are adjusted based on itinerary ratings:
```
final_score = similarity * 0.8 + (rating / 5.0) * 0.2
```

## Python Dataset Generator

### Installation
```bash
pip install numpy pandas
```

### Usage
```bash
# Generate 1000 records
python3 src/main/resources/scripts/generate_training_data.py \
  --count 1000 \
  --output training_data.csv \
  --stats

# Export to both CSV and JSON
python3 src/main/resources/scripts/generate_training_data.py \
  --count 500 \
  --output data.csv \
  --json \
  --stats
```

### Output Formats

**CSV Format:**
```
itinerary_id,location,budget,duration_days,avg_daily_cost,num_steps,num_collaborators,num_expenses,rating
itinerary_000000,Sousse,4500.00,7,642.86,12,4,18,4.42
```

**JSON Format:**
```json
[
  {
    "itinerary_id": "itinerary_000000",
    "location": "Sousse",
    "budget": 4500.00,
    "duration_days": 7,
    "avg_daily_cost": 642.86,
    "num_steps": 12,
    "num_collaborators": 4,
    "num_expenses": 18,
    "rating": 4.42
  }
]
```

## Configuration

### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| maxBudget | BigDecimal | No | Maximum budget for recommendations |
| location | String | No | Preferred destination |
| limit | Integer | No | Number of recommendations (default: 5) |
| minDuration | Integer | No | Minimum trip duration in days |
| maxDuration | Integer | No | Maximum trip duration in days |
| kNeighbors | Integer | No | k value for k-NN (default: 5) |
| minRating | BigDecimal | No | Minimum rating filter (0.0-5.0) |
| exactLocationMatch | Boolean | No | Require exact location match (default: false) |

## Performance Optimization

### Current Implementation
- **Time Complexity**: O(n * m) where n = candidates, m = features
- **Space Complexity**: O(n) for storing similarity scores

### Optimization Strategies
1. **Caching**: Cache frequently searched locations/budgets
2. **Indexing**: Database indexes on location and budget columns
3. **Pre-computed Similarities**: Pre-compute and cache similarity matrices
4. **Batch Processing**: Generate training data in batches

## Future Enhancements

1. **Advanced ML Models**
   - Neural networks for deeper pattern recognition
   - Matrix factorization for collaborative filtering
   - Deep learning with embedding layers

2. **Hybrid Recommendations**
   - Content-based + Collaborative filtering
   - User behavior tracking
   - Personalized ratings

3. **Real-time Learning**
   - Update model with user interactions
   - A/B testing framework
   - Feedback loop integration

4. **Explainability**
   - Why recommendations are made
   - Feature importance scores
   - Recommendation confidence levels

## Troubleshooting

### No Recommendations Found
- Ensure training data exists: `GET /api/itinerary/recommendations/train/statistics`
- Check if filters are too restrictive
- Try without exact location match: set `"exactLocationMatch": false`

### Poor Recommendation Quality
- Generate more training data: increase itinerary pool
- Adjust weights in `ItineraryRecommendationService`
- Check rating calculation in `TrainingDataGeneratorService`

### Performance Issues
- Limit results with smaller `limit` parameter
- Use exact location match when possible
- Consider caching recommendations

## Security Notes

⚠️ **Training data generation endpoints should be protected**:
- `/train/generate` - Requires admin role
- `/train/synthetic` - Requires admin role
- `/train/normalize` - Requires admin role

Use Spring Security to restrict access to these endpoints.

## Contributing

To improve the recommendation engine:

1. Adjust similarity weight in `ItineraryRecommendationService.calculateSimilarityScores()`
2. Modify rating calculation in `TrainingDataGeneratorService.calculateRating()`
3. Add new features to `TrainingDataset` entity
4. Update Python generator script accordingly

## License

This module is part of the Baladna Backend project.
