from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import auth, users, discovery, connections, websockets

app = FastAPI(title="TapConnect API", version="1.0.0")

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
