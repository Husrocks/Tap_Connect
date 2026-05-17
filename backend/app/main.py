from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text
from app.database import engine
from app.routers import auth, users, discovery, connections, websockets

app = FastAPI(title="TapConnect API", version="1.0.0")

@app.on_event("startup")
async def startup_event():
    async with engine.begin() as conn:
        try:
            # Automatic schema sync for the new ai_summary field
            await conn.execute(text("ALTER TABLE connections ADD COLUMN IF NOT EXISTS ai_summary VARCHAR(500);"))
        except Exception as e:
            print(f"Startup DDL Migration Error: {e}")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(users.router)
app.include_router(discovery.router)
app.include_router(connections.router)
app.include_router(websockets.router)

@app.get("/")
async def root():
    return {"message": "TapConnect Backend is running! Phase 5 (Media & Storage) active."}
