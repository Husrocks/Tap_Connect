import math
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.dependencies import get_db, get_current_user
from app.models.user import User
from app.schemas.discovery import LocationUpdate, NearbyUsersResponse, RadarUser

router = APIRouter(prefix="/discovery", tags=["discovery"])

RADAR_RADIUS_KM = 5.0  # Max distance to show on radar (5km)

def calculate_distance_km(lat1, lon1, lat2, lon2):
    R = 6371.0  # Earth radius in km
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat / 2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c

def calculate_bearing(lat1, lon1, lat2, lon2):
    lat1, lon1, lat2, lon2 = map(math.radians, [lat1, lon1, lat2, lon2])
    dlon = lon2 - lon1
    y = math.sin(dlon) * math.cos(lat2)
    x = math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(dlon)
    bearing = math.degrees(math.atan2(y, x))
    return (bearing + 360) % 360

@router.post("/update-location")
async def update_location(
    body: LocationUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Update current user's GPS coordinates and networking status."""
    current_user.latitude = body.latitude
    current_user.longitude = body.longitude
    current_user.is_networking_active = body.is_networking_active
    current_user.last_active = datetime.now(timezone.utc)
    
    await db.commit()
    return {"message": "Location updated successfully"}

@router.get("/nearby", response_model=NearbyUsersResponse)
async def get_nearby_users(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Find nearby active users and return their relative positions for the radar UI."""
    if current_user.latitude is None or current_user.longitude is None:
        raise HTTPException(status_code=400, detail="User location not set. Update location first.")

    # In a real production app, use PostGIS for efficient spatial queries.
    # For MVP, we fetch active users and filter in Python.
    result = await db.execute(
        select(User).where(
            User.is_networking_active == True,
            User.user_id != current_user.user_id
        )
    )
    all_active_users = result.scalars().all()
    
    # Dynamically filter out mock users User A and User B
    all_active_users = [
        u for u in all_active_users
        if u.full_name not in ["User A", "User B", "user_a", "user_b"]
    ]
    
    radar_users = []
    for user in all_active_users:
        if user.latitude is None or user.longitude is None:
            continue
            
        dist_km = calculate_distance_km(
            current_user.latitude, current_user.longitude,
            user.latitude, user.longitude
        )
        
        if dist_km <= RADAR_RADIUS_KM:
            angle = calculate_bearing(
                current_user.latitude, current_user.longitude,
                user.latitude, user.longitude
            )
            
            # Map distance to 0.0-1.0 fraction of radar radius
            distance_fraction = dist_km / RADAR_RADIUS_KM
            
            radar_users.append(RadarUser(
                user_id=user.user_id,
                full_name=user.full_name,
                role=user.role,
                profile_image_url=user.profile_image_url,
                distance=max(0.1, distance_fraction), # Min 0.1 so they aren't on top of the center
                angle=angle,
                actual_distance_km=round(dist_km, 2)
            ))
            
    return NearbyUsersResponse(users=radar_users)
