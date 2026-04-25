import threading
import uvicorn
from fastapi import FastAPI
from app.consumer import start_consumer

app = FastAPI(title="CodeMind AI Service")

@app.get("/health")
def health():
    return {"status": "up"}

@app.on_event("startup")
def startup_event():
    thread = threading.Thread(target=start_consumer, daemon=True)
    thread.start()
    print("Kafka consumer started in background thread")

if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8083, reload=False)