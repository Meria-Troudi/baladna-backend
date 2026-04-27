#!/usr/bin/env python3
"""
AI Training Data Generator for Baladna Itinerary Recommendation System

This script generates synthetic training data for the AI recommendation model.
It creates realistic itinerary datasets based on Tunisian locations and travel patterns.

Usage:
    python3 generate_training_data.py --output training_data.csv --count 1000
    
Requirements:
    - Python 3.7+
    - pandas
    - numpy
"""

import csv
import json
import random
from datetime import datetime, timedelta
from typing import List, Dict, Tuple
import argparse
import sys

# Tunisian locations and regions
TUNISIA_LOCATIONS = [
    "Tunis",
    "Sousse",
    "Sfax",
    "Djerba",
    "Kairouan",
    "Tozeur",
    "Nefta",
    "Gafsa",
    "Bizerte",
    "Hammamet",
    "Nabeul",
    "Monastir",
    "Mahdia",
    "Tataouine",
    "Gafsa",
    "Ksour",
    "Touzeur",
    "Kebili",
    "Douz",
    "Medenine"
]

# Budget ranges in TND (Tunisian Dinar)
BUDGET_RANGES = {
    "budget": (500, 3000),
    "moderate": (3000, 7000),
    "luxury": (7000, 20000)
}

class TrainingDataGenerator:
    """Generate synthetic training data for itinerary recommendations"""

    def __init__(self):
        self.records = []
        random.seed(42)  # For reproducibility

    def generate_budget(self, category: str = None) -> float:
        """Generate realistic budget"""
        if category is None:
            category = random.choice(list(BUDGET_RANGES.keys()))

        min_budget, max_budget = BUDGET_RANGES[category]
        return round(random.uniform(min_budget, max_budget), 2)

    def generate_duration(self) -> int:
        """Generate trip duration in days"""
        # Most trips are 3-14 days
        weights = [0.1, 0.15, 0.2, 0.25, 0.2, 0.1]  # 2-7 days distribution
        days = random.choices(range(2, 8), weights=weights, k=1)[0]
        return days * random.choice([1, 2])  # 2, 3, 4, 5, 6, 7, 8, 10, 12, 14 days

    def generate_location(self) -> str:
        """Generate location"""
        return random.choice(TUNISIA_LOCATIONS)

    def calculate_avg_daily_cost(self, budget: float, duration: int) -> float:
        """Calculate average daily cost"""
        return round(budget / duration, 2)

    def generate_num_steps(self) -> int:
        """Generate number of itinerary steps/activities"""
        # Most itineraries have 3-20 steps
        return random.randint(3, 20)

    def generate_num_collaborators(self) -> int:
        """Generate number of collaborators"""
        # Most itineraries have 1-8 collaborators
        weights = [0.3, 0.35, 0.2, 0.1, 0.04, 0.01]
        collaborators = random.choices(range(1, 7), weights=weights, k=1)[0]
        return collaborators

    def generate_num_expenses(self) -> int:
        """Generate number of expense records"""
        # More activities = more expenses typically
        return random.randint(5, 40)

    def calculate_rating(self, num_steps: int, num_collaborators: int, 
                        num_expenses: int) -> float:
        """Calculate rating based on activity completeness"""
        base_rating = 2.5

        # Reward having multiple activities
        if num_steps >= 10:
            base_rating += 1.2
        elif num_steps >= 7:
            base_rating += 0.8
        elif num_steps >= 5:
            base_rating += 0.4

        # Reward collaboration
        if num_collaborators >= 3:
            base_rating += 0.8
        elif num_collaborators >= 2:
            base_rating += 0.4

        # Reward expense tracking
        if num_expenses >= 20:
            base_rating += 0.5
        elif num_expenses >= 10:
            base_rating += 0.3

        # Add some randomness
        base_rating += random.uniform(-0.2, 0.3)

        # Cap at 5.0
        return round(min(base_rating, 5.0), 2)

    def normalize_value(self, value: float, min_val: float, max_val: float) -> float:
        """Normalize value to [0, 1]"""
        if max_val <= min_val:
            return 0.0
        normalized = (value - min_val) / (max_val - min_val)
        return round(max(0.0, min(1.0, normalized)), 4)

    def generate_record(self, itinerary_id: str = None) -> Dict:
        """Generate a single training record"""
        if itinerary_id is None:
            itinerary_id = f"itinerary_{len(self.records):06d}"

        budget = self.generate_budget()
        location = self.generate_location()
        duration = self.generate_duration()
        avg_daily_cost = self.calculate_avg_daily_cost(budget, duration)
        num_steps = self.generate_num_steps()
        num_collaborators = self.generate_num_collaborators()
        num_expenses = self.generate_num_expenses()
        rating = self.calculate_rating(num_steps, num_collaborators, num_expenses)

        return {
            "itinerary_id": itinerary_id,
            "location": location,
            "budget": budget,
            "duration_days": duration,
            "avg_daily_cost": avg_daily_cost,
            "num_steps": num_steps,
            "num_collaborators": num_collaborators,
            "num_expenses": num_expenses,
            "rating": rating
        }

    def generate_dataset(self, count: int) -> List[Dict]:
        """Generate synthetic dataset"""
        self.records = []
        for i in range(count):
            record = self.generate_record()
            self.records.append(record)

            if (i + 1) % 100 == 0:
                print(f"Generated {i + 1}/{count} records...", file=sys.stderr)

        return self.records

    def export_to_csv(self, filename: str):
        """Export records to CSV file"""
        if not self.records:
            print("No records to export. Generate data first.", file=sys.stderr)
            return

        with open(filename, 'w', newline='', encoding='utf-8') as csvfile:
            fieldnames = [
                'itinerary_id', 'location', 'budget', 'duration_days',
                'avg_daily_cost', 'num_steps', 'num_collaborators',
                'num_expenses', 'rating'
            ]

            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(self.records)

        print(f"Data exported to {filename}", file=sys.stderr)

    def export_to_json(self, filename: str):
        """Export records to JSON file"""
        if not self.records:
            print("No records to export. Generate data first.", file=sys.stderr)
            return

        with open(filename, 'w', encoding='utf-8') as jsonfile:
            json.dump(self.records, jsonfile, indent=2, ensure_ascii=False)

        print(f"Data exported to {filename}", file=sys.stderr)

    def print_statistics(self):
        """Print dataset statistics"""
        if not self.records:
            print("No records. Generate data first.", file=sys.stderr)
            return

        budgets = [r['budget'] for r in self.records]
        durations = [r['duration_days'] for r in self.records]
        ratings = [r['rating'] for r in self.records]

        print("\n=== Training Data Statistics ===")
        print(f"Total Records: {len(self.records)}")
        print(f"Budget Range: ${min(budgets):.2f} - ${max(budgets):.2f}")
        print(f"Average Budget: ${sum(budgets)/len(budgets):.2f}")
        print(f"Duration Range: {min(durations)} - {max(durations)} days")
        print(f"Average Duration: {sum(durations)/len(durations):.1f} days")
        print(f"Average Rating: {sum(ratings)/len(ratings):.2f}/5.0")
        print(f"Unique Locations: {len(set(r['location'] for r in self.records))}")

def main():
    parser = argparse.ArgumentParser(
        description='Generate synthetic training data for Baladna itinerary AI'
    )
    parser.add_argument(
        '--count', '-c',
        type=int,
        default=1000,
        help='Number of training records to generate (default: 1000)'
    )
    parser.add_argument(
        '--output', '-o',
        default='training_data.csv',
        help='Output CSV filename (default: training_data.csv)'
    )
    parser.add_argument(
        '--json',
        action='store_true',
        help='Also export to JSON format'
    )
    parser.add_argument(
        '--stats',
        action='store_true',
        help='Print statistics after generation'
    )

    args = parser.parse_args()

    print(f"Generating {args.count} training records...", file=sys.stderr)
    generator = TrainingDataGenerator()
    generator.generate_dataset(args.count)

    # Export to CSV
    generator.export_to_csv(args.output)

    # Export to JSON if requested
    if args.json:
        json_output = args.output.replace('.csv', '.json')
        generator.export_to_json(json_output)

    # Print statistics if requested
    if args.stats:
        generator.print_statistics()

    print("Done!", file=sys.stderr)

if __name__ == '__main__':
    main()
