import uuid
from pydantic import BaseModel, Field
from typing import List, Optional

class LocationUpdate(BaseModel):
    latitude: float = Field(..., ge=-90, le=90)
    longitude: float = Field(..., ge=-180, le=180)
    is_networking_active: bool = True

class RadarUser(BaseModel):
    user_id: uuid.UUID
    full_name: str
    role: Optional[str] = None
    profile_image_url: Optional[str] = None
    distance: float  # Fraction of radar radius (0.0 to 1.0)
    angle: float     # Angle in degrees (0 to 360)
    actual_distance_km: float

class NearbyUsersResponse(BaseModel):
    users: List[RadarUser]
