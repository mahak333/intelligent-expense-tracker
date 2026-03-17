"""
Anomaly detection using statistical methods (Z-score and IQR)
"""
import numpy as np
from typing import List, Dict

def detect_anomalies(expenses: List[Dict]) -> List[Dict]:
    """
    Detect anomalous expenses using Z-score method
    expenses: list of {id, amount, category, description, date}
    """
    if len(expenses) < 5:
        return []

    amounts = np.array([e['amount'] for e in expenses])
    mean = np.mean(amounts)
    std = np.std(amounts)

    if std == 0:
        return []

    anomalies = []
    for expense in expenses:
        z_score = abs((expense['amount'] - mean) / std)
        if z_score > 2.0:  # 2 standard deviations = anomaly
            severity = "high" if z_score > 3.0 else "medium"
            anomalies.append({
                "expense_id": expense.get('id'),
                "description": expense.get('description'),
                "amount": expense.get('amount'),
                "category": expense.get('category'),
                "z_score": round(float(z_score), 2),
                "severity": severity,
                "message": f"Unusual {expense.get('category', '')} expense: ₹{expense.get('amount')} "
                           f"({z_score:.1f}x above normal)"
            })

    return anomalies


def detect_category_anomalies(category_data: Dict[str, List[float]]) -> List[Dict]:
    """
    Compare current week vs average for each category
    """
    alerts = []
    for category, weekly_amounts in category_data.items():
        if len(weekly_amounts) < 3:
            continue
        avg = np.mean(weekly_amounts[:-1])  # historical avg
        current = weekly_amounts[-1]         # current week
        if avg > 0:
            pct_change = (current - avg) / avg * 100
            if pct_change > 30:
                alerts.append({
                    "category": category,
                    "current": round(current, 2),
                    "average": round(avg, 2),
                    "change_pct": round(pct_change, 1),
                    "message": f"You are spending {pct_change:.0f}% more on {category} this week"
                })
    return alerts