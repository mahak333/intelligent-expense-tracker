"""
Expense prediction using Linear Regression on time-series data
"""
import numpy as np
from sklearn.linear_model import LinearRegression

def predict_next_month(monthly_totals: list) -> dict:
    """
    Predict next month's spending using linear regression
    monthly_totals: list of (month_number, total_amount) tuples
    """
    if len(monthly_totals) < 2:
        return {
            "predicted_amount": 0.0,
            "confidence": "low",
            "message": "Need at least 2 months of data for prediction"
        }
    
    X = np.array([m[0] for m in monthly_totals]).reshape(-1, 1)
    y = np.array([m[1] for m in monthly_totals])
    
    model = LinearRegression()
    model.fit(X, y)
    
    next_month = X[-1][0] + 1
    predicted = model.predict([[next_month]])[0]
    predicted = max(0, predicted)  # No negative spending
    
    # Calculate R² score for confidence
    r2 = model.score(X, y)
    confidence = "high" if r2 > 0.7 else "medium" if r2 > 0.4 else "low"
    
    avg = np.mean(y)
    change_pct = ((predicted - avg) / avg * 100) if avg > 0 else 0
    
    direction = "increase" if change_pct > 0 else "decrease"
    
    return {
        "predicted_amount": round(float(predicted), 2),
        "average_spending": round(float(avg), 2),
        "change_percentage": round(float(abs(change_pct)), 1),
        "direction": direction,
        "confidence": confidence,
        "message": f"Predicted {direction} of {abs(change_pct):.1f}% compared to average"
    }