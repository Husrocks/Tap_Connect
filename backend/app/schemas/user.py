import uuid
from datetime import datetime
from typing import Optional, List, Dict
from pydantic import BaseModel, EmailStr

class UserRegister(BaseModel):
    email: str
    password: str
    full_name: str

class UserLogin(BaseModel):
    email: str
    password: str

class UserRegisterResponse(BaseModel):
    user_id: uuid.UUID
    email: str
    full_name: str
    access_token: str
    token_type: str = "bearer"

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"

class UserProfileCreate(BaseModel):
    user_id: Optional[uuid.UUID] = None
    email: str
    full_name: str
    bio: Optional[str] = None
    role: Optional[str] = None
    organization: Optional[str] = None
    interests: List[str] = []
    social_links: Dict[str, str] = {}
    sharing_preferences: Optional[Dict[str, bool]] = None

class UserProfileUpdate(BaseModel):
    full_name: Optional[str] = None
    bio: Optional[str] = None
    role: Optional[str] = None
    organization: Optional[str] = None
    interests: Optional[List[str]] = None
    social_links: Optional[Dict[str, str]] = None
    sharing_preferences: Optional[Dict[str, bool]] = None

class UserProfileResponse(BaseModel):
    user_id: uuid.UUID
    email: str
    full_name: str
    bio: Optional[str] = None
    role: Optional[str] = None
    organization: Optional[str] = None
    interests: List[str] = []
    social_links: Dict[str, str] = {}
    sharing_preferences: Dict[str, bool] = {}
    profile_image_url: Optional[str] = None
    created_at: datetime
    last_active: datetime

    model_config = {"from_attributes": True}

class ProfileImageResponse(BaseModel):
    profile_image_url: str
