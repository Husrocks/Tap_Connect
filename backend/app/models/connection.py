import uuid
from datetime import datetime, timezone
from enum import Enum
from sqlalchemy import String, DateTime, ForeignKey, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column
from app.database import Base

class ConnectionStatus(str, Enum):
    PENDING = "pending"
    ACCEPTED = "accepted"
    REJECTED = "rejected"

class ConnectionType(str, Enum):
    RADAR = "radar"
    NFC = "nfc"
    BLE = "ble"

class Connection(Base):
    __tablename__ = "connections"

    connection_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.user_id"), nullable=False)
    peer_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.user_id"), nullable=False)
    
    status: Mapped[ConnectionStatus] = mapped_column(SQLEnum(ConnectionStatus), default=ConnectionStatus.PENDING)
    connection_type: Mapped[ConnectionType] = mapped_column(SQLEnum(ConnectionType), default=ConnectionType.RADAR)
    
    # AI generated icebreaker/matching summary (Phase 6/Groq)
    ai_summary: Mapped[str | None] = mapped_column(String(500), nullable=True)
    
    # Information about where the users met
    where_met: Mapped[str | None] = mapped_column(String(200), nullable=True)
    
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), onupdate=lambda: datetime.now(timezone.utc))
