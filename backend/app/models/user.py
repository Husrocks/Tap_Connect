import uuid
from datetime import datetime, timezone
from sqlalchemy import String, Text, ARRAY, DateTime, Float
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import Mapped, mapped_column
from app.database import Base

class User(Base):
    __tablename__ = "users"

    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    email: Mapped[str] = mapped_column(String(255), unique=True, nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    full_name: Mapped[str] = mapped_column(String(255), nullable=False)
    
    # Profile fields (Phase 2)
    bio: Mapped[str | None] = mapped_column(Text, nullable=True)
    role: Mapped[str | None] = mapped_column(String(100), nullable=True)
    organization: Mapped[str | None] = mapped_column(String(150), nullable=True)
    interests: Mapped[list] = mapped_column(ARRAY(Text), default=list)
    social_links: Mapped[dict] = mapped_column(JSONB, default=dict)
    sharing_preferences: Mapped[dict] = mapped_column(JSONB, default=lambda: {"share_email": False, "nfc_enabled": True, "ble_enabled": True})
    profile_image_url: Mapped[str | None] = mapped_column(Text, nullable=True)
    
    # Location fields (Phase 3)
    latitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    longitude: Mapped[float | None] = mapped_column(Float, nullable=True)
    is_networking_active: Mapped[bool] = mapped_column(default=False)
    
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    last_active: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
