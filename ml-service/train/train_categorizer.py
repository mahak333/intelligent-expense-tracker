"""
Train the expense categorization ML model using Naive Bayes + TF-IDF
"""
import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
import joblib
import os

# Load dataset
df = pd.read_csv(os.path.join(os.path.dirname(__file__), 'dataset.csv'))

X = df['description']
y = df['category']

# Split
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Build pipeline: TF-IDF + Naive Bayes
pipeline = Pipeline([
    ('tfidf', TfidfVectorizer(ngram_range=(1, 2), max_features=5000)),
    ('clf', MultinomialNB(alpha=0.1))
])

# Train
pipeline.fit(X_train, y_train)

# Evaluate
y_pred = pipeline.predict(X_test)
print("=== Model Evaluation ===")
print(classification_report(y_test, y_pred))

# Save model
model_path = os.path.join(os.path.dirname(__file__), '..', 'categorizer_model.pkl')
joblib.dump(pipeline, model_path)
print(f"✅ Model saved to: {model_path}")