"""
Expense categorizer using trained Naive Bayes model
"""
import joblib
import os

MODEL_PATH = os.path.join(os.path.dirname(__file__), '..', 'categorizer_model.pkl')

_model = None

def load_model():
    global _model
    if _model is None and os.path.exists(MODEL_PATH):
        _model = joblib.load(MODEL_PATH)
    return _model

def predict_category(description: str) -> str:
    """Predict expense category from description text"""
    model = load_model()
    if model is None:
        return rule_based_fallback(description)
    
    prediction = model.predict([description.lower()])
    return prediction[0]

def rule_based_fallback(description: str) -> str:
    """Fallback when model is not available"""
    desc = description.lower()
    rules = {
        "Food": ["pizza", "burger", "food", "cafe", "coffee", "restaurant", "zomato", "swiggy", "lunch", "dinner"],
        "Travel": ["uber", "ola", "flight", "hotel", "taxi", "bus", "train", "petrol", "fuel"],
        "Shopping": ["amazon", "flipkart", "clothes", "shoes", "mall", "shopping"],
        "Bills": ["electricity", "water", "wifi", "internet", "recharge", "broadband"],
        "Health": ["doctor", "medicine", "pharmacy", "hospital", "gym", "health"],
        "Entertainment": ["netflix", "spotify", "movie", "game", "entertainment"]
    }
    for category, keywords in rules.items():
        if any(kw in desc for kw in keywords):
            return category
    return "Other"