import httpx
import os

BASE_URL = "http://localhost:8000"

def test_phase5():
    print("--- Phase 5: Media & Storage (Cloudinary) ---")
    
    # 1. Setup User
    user_data = {
        "email": "test_phase5@example.com",
        "password": "password",
        "full_name": "Test Image User"
    }
    
    httpx.post(f"{BASE_URL}/auth/register", json=user_data, timeout=30.0)
    login_resp = httpx.post(f"{BASE_URL}/auth/login", json={"email": user_data["email"], "password": user_data["password"]}, timeout=30.0)
    token = login_resp.json().get("access_token")
    headers = {"Authorization": f"Bearer {token}"}
    
    # Check if Cloudinary is configured
    from app.config import settings
    if not settings.CLOUDINARY_CLOUD_NAME or settings.CLOUDINARY_CLOUD_NAME == "your_cloud_name":
        print("\nSKIPPING TEST: Cloudinary credentials are not set in .env")
        print("Please add your Cloudinary Cloud Name, API Key, and API Secret to the .env file.")
        print("Then, try testing manually via Swagger UI (http://localhost:8000/docs).")
        return
        
    print("\nAttempting to upload an image...")
    # Create a dummy image file for testing
    dummy_image_path = "test_image.jpg"
    with open(dummy_image_path, "wb") as f:
        f.write(b"fake_image_data")
        
    print("Note: Automated test uses a fake image file which Cloudinary might reject.")
    print("It is highly recommended to test this manually via Swagger UI with a real image.")
    
    try:
        with open(dummy_image_path, "rb") as f:
            files = {"file": ("test_image.jpg", f, "image/jpeg")}
            response = httpx.post(f"{BASE_URL}/users/me/profile-image", files=files, headers=headers, timeout=60.0)
            
        print(f"Status: {response.status_code}")
        if response.status_code == 200:
            print(f"Uploaded Image URL: {response.json().get('profile_image_url')}")
            print("\nSUCCESS: Phase 5 Image Upload Verified!")
        else:
            print(f"Response: {response.text}")
            print("\nFAILURE or REJECTED BY CLOUDINARY: Check response details above.")
    finally:
        if os.path.exists(dummy_image_path):
            os.remove(dummy_image_path)

if __name__ == "__main__":
    test_phase5()
