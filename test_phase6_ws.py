import asyncio
import httpx
import websockets
import json

BASE_URL = "http://localhost:8000"
WS_URL = "ws://localhost:8000/ws/notifications"

async def test_phase6():
    print("--- Phase 6: Real-Time WebSockets ---")
    
    # 1. Setup 2 Users
    users = [
        {"email": "ws_user_1@example.com", "password": "password", "full_name": "WS User 1"},
        {"email": "ws_user_2@example.com", "password": "password", "full_name": "WS User 2"}
    ]
    
    tokens = []
    user_ids = []
    
    async with httpx.AsyncClient() as client:
        for u in users:
            resp = await client.post(f"{BASE_URL}/auth/register", json=u, timeout=30.0)
            if resp.status_code == 201:
                user_ids.append(resp.json()["user_id"])
            else:
                # Login and fetch profile to get ID
                login_resp = await client.post(f"{BASE_URL}/auth/login", json={"email": u["email"], "password": u["password"]}, timeout=30.0)
                token = login_resp.json()['access_token']
                profile_resp = await client.get(f"{BASE_URL}/users/me", headers={"Authorization": f"Bearer {token}"}, timeout=30.0)
                user_ids.append(profile_resp.json()["user_id"])
                
            login_resp = await client.post(f"{BASE_URL}/auth/login", json={"email": u["email"], "password": u["password"]}, timeout=30.0)
            tokens.append(login_resp.json()["access_token"])
            
    print(f"Users setup complete. Tokens acquired.")

    # 2. Connect User 2 to WebSocket
    print("Connecting User 2 to WebSocket...")
    try:
        async with websockets.connect(f"{WS_URL}?token={tokens[1]}") as ws2:
            welcome_msg = await ws2.recv()
            print(f"User 2 connected: {welcome_msg}")
            
            # Now, have User 1 send a request via REST
            print("User 1 sending connection request via REST...")
            async with httpx.AsyncClient() as client:
                headers_1 = {"Authorization": f"Bearer {tokens[0]}"}
                response = await client.post(f"{BASE_URL}/connections/request", 
                                     json={"peer_id": user_ids[1], "connection_type": "radar"},
                                     headers=headers_1, timeout=30.0)
                print(f"Request sent. Status: {response.status_code}")
                
            # Listen for the WebSocket message on User 2
            try:
                push_msg = await asyncio.wait_for(ws2.recv(), timeout=2.0)
                push_data = json.loads(push_msg)
                print(f"User 2 received real-time push: {push_data['type']}")
                if push_data['type'] == 'new_connection_request':
                    print("\nSUCCESS: Phase 6 WebSockets Verified!")
            except asyncio.TimeoutError:
                print("\nFAILURE: User 2 did not receive WebSocket push in time.")
                
    except Exception as e:
        print(f"\nFAILURE: WebSocket error: {e}")

if __name__ == "__main__":
    asyncio.run(test_phase6())
