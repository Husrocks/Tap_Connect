import uuid
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel
from app.models.connection import ConnectionStatus, ConnectionType

class ConnectionRequest(BaseModel):
    peer_id: uuid.UUID
    connection_type: str = "radar"
    where_met: Optional[str] = None

class ConnectionAction(BaseModel):
    connection_id: uuid.UUID

class ConnectionResponse(BaseModel):
    connection_id: uuid.UUID
    user_id: uuid.UUID
    peer_id: uuid.UUID
    status: ConnectionStatus
    connection_type: ConnectionType
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True

class ConnectionList(BaseModel):
    connections: List[ConnectionResponse]
