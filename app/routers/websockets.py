import uuid
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query, Depends, status
from typing import Optional
from app.services.websocket_manager import manager
from app.services.auth_service import decode_token

router = APIRouter(tags=["websockets"])

async def get_token(
    websocket: WebSocket,
    token: Optional[str] = Query(None),
):
    if token is None:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return None
    return token

@router.websocket("/ws/notifications")
async def websocket_endpoint(
    websocket: WebSocket,
    token: Optional[str] = Depends(get_token)
):
    if token is None:
        return
        
    user_id_str = decode_token(token)
    if not user_id_str:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return
        
    try:
        user_id = uuid.UUID(user_id_str)
    except ValueError:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return

    await manager.connect(websocket, user_id)
    
    # Send a welcome message to confirm connection
    await manager.send_personal_message(
        {"type": "connection_established", "message": "Successfully connected to real-time notifications."},
        user_id
    )

    try:
        while True:
            # We don't really expect the client to send messages here,
            # this is primarily for server-to-client push notifications.
            # But we must keep the socket open and listen for disconnects.
            data = await websocket.receive_text()
            # If client pings, we can pong, but Starlette handles standard ping/pong automatically.
            
    except WebSocketDisconnect:
        manager.disconnect(user_id)
