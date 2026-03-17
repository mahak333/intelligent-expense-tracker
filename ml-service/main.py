"""
FastAPI ML Service for Intelligent Expense Tracker
Endpoints: category prediction, anomaly detection, prediction, OCR
"""
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
import base64
import io
import os

from models.categorizer import predict_category
from models.predictor import predict_next_month
from models.anomaly_detector import detect_anomalies, detect_category_anomalies

app = FastAPI(
    title="Expense Tracker ML Service",
    description="ML APIs for intelligent expense categorization, prediction, and anomaly detection",
    version="1.0.0"
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ===== Request/Response Models =====

class CategoryRequest(BaseModel):
    description: str

class ExpenseItem(BaseModel):
    id: Optional[int] = None
    amount: float
    category: str
    description: str
    date: Optional[str] = None

class MonthlyData(BaseModel):
    month: int
    total: float

class PredictionRequest(BaseModel):
    monthly_data: List[MonthlyData]

class ReceiptRequest(BaseModel):
    image: str  # base64 encoded


# ===== Health Check =====

@app.get("/")
def health():
    return {"status": "ML Service is running", "version": "1.0.0"}

@app.get("/health")
def health_check():
    return {"status": "healthy"}


# ===== Category Prediction =====

@app.post("/predict/category")
def categorize_expense(request: CategoryRequest):
    """Predict expense category from description using ML"""
    if not request.description:
        raise HTTPException(status_code=400, detail="Description is required")
    
    category = predict_category(request.description)
    return {
        "description": request.description,
        "category": category
    }


# ===== Expense Prediction =====

@app.post("/predict/expenses")
def predict_expenses(request: PredictionRequest):
    """Predict next month's expenses using linear regression"""
    monthly_tuples = [(m.month, m.total) for m in request.monthly_data]
    result = predict_next_month(monthly_tuples)
    return result


# ===== Anomaly Detection =====

@app.post("/detect/anomalies")
def anomaly_detection(expenses: List[ExpenseItem]):
    """Detect anomalous expenses using Z-score"""
    expense_dicts = [e.dict() for e in expenses]
    anomalies = detect_anomalies(expense_dicts)
    return {"anomalies": anomalies, "count": len(anomalies)}


# ===== OCR Receipt Scanner =====

@app.post("/ocr/receipt")
def scan_receipt(request: ReceiptRequest):
    """
    Extract data from receipt image using OCR (Tesseract)
    Returns: amount, date, items
    """
    try:
        import pytesseract
        from PIL import Image
        
        # Decode base64 image
        image_data = base64.b64decode(request.image)
        image = Image.open(io.BytesIO(image_data))
        
        # Extract text via OCR
        text = pytesseract.image_to_string(image)
        
        # Parse extracted text
        result = parse_receipt_text(text)
        result["raw_text"] = text
        return result
        
    except ImportError:
        return {
            "error": "Tesseract not installed",
            "message": "Install tesseract-ocr on your system",
            "raw_text": "",
            "amount": None,
            "date": None,
            "items": []
        }
    except Exception as e:
        return {
            "error": str(e),
            "raw_text": "",
            "amount": None,
            "date": None,
            "items": []
        }


def parse_receipt_text(text: str) -> dict:
    """Parse OCR text to extract amount, date, items"""
    import re
    
    lines = text.split('\n')
    amount = None
    date = None
    items = []
    
    # Extract total amount (look for TOTAL, AMOUNT patterns)
    for line in lines:
        total_match = re.search(r'(?:total|amount|grand total)[:\s]*[₹$]?\s*(\d+\.?\d*)', 
                                line, re.IGNORECASE)
        if total_match:
            amount = float(total_match.group(1))
            break
    
    # Fallback: find largest number
    if not amount:
        amounts = re.findall(r'[₹$]?\s*(\d{2,6}\.?\d{0,2})', text)
        if amounts:
            amount = max(float(a) for a in amounts)
    
    # Extract date
    date_patterns = [
        r'\d{2}[/-]\d{2}[/-]\d{4}',
        r'\d{4}[/-]\d{2}[/-]\d{2}',
        r'\d{2}\s+\w+\s+\d{4}'
    ]
    for pattern in date_patterns:
        match = re.search(pattern, text)
        if match:
            date = match.group()
            break
    
    # Extract items (lines with price pattern)
    for line in lines:
        if re.search(r'\d+\.?\d*', line) and len(line.strip()) > 3:
            if not any(skip in line.lower() for skip in ['total', 'tax', 'gst', 'subtotal']):
                items.append(line.strip())
    
    return {
        "amount": amount,
        "date": date,
        "items": items[:10]  # Limit to 10 items
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)