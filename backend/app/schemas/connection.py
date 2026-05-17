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
    ai_summary: Optional[str] = None
    where_met: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    
    # Dynamic peer details populated at API level
    peer_name: Optional[str] = None
    peer_role: Optional[str] = None
    peer_organization: Optional[str] = None
    peer_image_url: Optional[str] = None

    class Config:
        from_attributes = True

class ConnectionList(BaseModel):
    connections: List[ConnectionResponse]
