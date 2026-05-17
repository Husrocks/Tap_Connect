import cloudinary
import cloudinary.uploader
from fastapi import UploadFile, HTTPException
from app.config import settings

# Configure Cloudinary
cloudinary.config(
    cloud_name=settings.CLOUDINARY_CLOUD_NAME,
    api_key=settings.CLOUDINARY_API_KEY,
    api_secret=settings.CLOUDINARY_API_SECRET
)

async def upload_image_to_cloudinary(file: UploadFile, folder: str = "tapconnect/profiles") -> str:
    """
    Uploads an image file to Cloudinary and returns the secure URL.
    Falls back to a premium branded initial avatar if credentials are not configured.
    """
    is_placeholder = (
        not settings.CLOUDINARY_CLOUD_NAME 
        or "your" in settings.CLOUDINARY_CLOUD_NAME.lower()
        or settings.CLOUDINARY_CLOUD_NAME == "your_cloud_name"
    )
    
    if is_placeholder:
        # Graceful fallback: DiceBear initials avatar matching AccentIndigo theme!
        import random
        random_id = random.randint(1000, 9999)
        return f"https://api.dicebear.com/7.x/initials/png?seed=TC{random_id}&backgroundColor=6366f1,4f46e5"
         
    try:
        # Cloudinary uploader doesn't natively support FastAPI's async SpooledTemporaryFile well, 
        # so we read it into memory first. Be careful with very large files.
        contents = await file.read()
        
        result = cloudinary.uploader.upload(
            contents,
            folder=folder,
            resource_type="image"
        )
        return result.get("secure_url")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to upload image: {str(e)}")
