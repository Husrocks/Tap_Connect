from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, or_, and_
from typing import List
from app.dependencies import get_db, get_current_user
from app.models.user import User
from app.models.connection import Connection, ConnectionStatus, ConnectionType
from app.schemas.connection import ConnectionRequest, ConnectionResponse, ConnectionAction
from app.services.websocket_manager import manager

router = APIRouter(prefix="/connections", tags=["connections"])

import uuid

async def _build_connection_response(conn: Connection, current_user_id: uuid.UUID, db: AsyncSession) -> ConnectionResponse:
    """Helper to dynamically populate peer profiles on Connection DTOs."""
    # The peer is the user who is NOT current_user_id
    peer_id = conn.peer_id if conn.user_id == current_user_id else conn.user_id
    
    # Query database for peer profile details
    peer_result = await db.execute(select(User).where(User.user_id == peer_id))
    peer_user = peer_result.scalar_one_or_none()
    
    response_dto = ConnectionResponse.model_validate(conn)
    if peer_user:
        response_dto.peer_name = peer_user.full_name
        response_dto.peer_role = peer_user.role
        response_dto.peer_organization = peer_user.organization
        response_dto.peer_image_url = peer_user.profile_image_url
        
    return response_dto

@router.post("/request", response_model=ConnectionResponse, status_code=status.HTTP_201_CREATED)
async def send_connection_request(
    body: ConnectionRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Send a connection request to another user."""
    if body.peer_id == current_user.user_id:
        raise HTTPException(status_code=400, detail="You cannot connect with yourself")

    # Check if user exists
    result = await db.execute(select(User).where(User.user_id == body.peer_id))
    if not result.scalar_one_or_none():
        raise HTTPException(status_code=404, detail="Peer user not found")

    # Check if existing connection
    result = await db.execute(
        select(Connection).where(
            or_(
                and_(Connection.user_id == current_user.user_id, Connection.peer_id == body.peer_id),
                and_(Connection.user_id == body.peer_id, Connection.peer_id == current_user.user_id)
            )
        )
    )
    existing = result.scalar_one_or_none()
    if existing:
        return await _build_connection_response(existing, current_user.user_id, db)

    # Map raw input string to valid database ConnectionType enum
    raw_type = (body.connection_type or "radar").lower()
    if "nfc" in raw_type:
        mapped_type = ConnectionType.NFC
    elif "ble" in raw_type:
        mapped_type = ConnectionType.BLE
    else:
        mapped_type = ConnectionType.RADAR

    new_conn = Connection(
        user_id=current_user.user_id,
        peer_id=body.peer_id,
        status=ConnectionStatus.PENDING,
        connection_type=mapped_type,
        where_met=body.where_met
    )
    db.add(new_conn)
    await db.commit()
    await db.refresh(new_conn)
    
    # Push real-time notification to the receiver
    await manager.send_personal_message(
        {
            "type": "new_connection_request",
            "message": f"You have a new connection request from {current_user.full_name}",
            "connection_id": str(new_conn.connection_id),
            "sender_id": str(current_user.user_id),
            "sender_name": current_user.full_name
        },
        body.peer_id
    )
    
    return await _build_connection_response(new_conn, current_user.user_id, db)

@router.post("/accept", response_model=ConnectionResponse)
async def accept_connection(
    body: ConnectionAction,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Accept a pending connection request."""
    result = await db.execute(
        select(Connection).where(
            Connection.connection_id == body.connection_id,
            Connection.peer_id == current_user.user_id,
            Connection.status == ConnectionStatus.PENDING
        )
    )
    conn = result.scalar_one_or_none()
    if not conn:
        raise HTTPException(status_code=404, detail="Pending connection request not found")

    conn.status = ConnectionStatus.ACCEPTED
    
    # Fetch original sender profile to generate personalized icebreaker
    sender_result = await db.execute(select(User).where(User.user_id == conn.user_id))
    sender_user = sender_result.scalar_one_or_none()
    
    if sender_user:
        try:
            from app.services.ai_service import ai_service
            summary = await ai_service.generate_profile_match_summary(
                user_name=sender_user.full_name,
                user_role=sender_user.role or "Professional",
                user_org=sender_user.organization or "TapConnect",
                user_interests=sender_user.interests or [],
                peer_name=current_user.full_name,
                peer_role=current_user.role or "Professional",
                peer_org=current_user.organization or "TapConnect",
                peer_interests=current_user.interests or []
            )
            conn.ai_summary = summary
        except Exception as ai_err:
            print(f"Error generating profile summary with Groq: {ai_err}")
            conn.ai_summary = f"Ask {current_user.full_name} about their professional background and share your goals."
    else:
        conn.ai_summary = f"Ask {current_user.full_name} about their professional background and share your goals."

    await db.commit()
    await db.refresh(conn)
    
    # Push real-time notification to the original sender
    await manager.send_personal_message(
        {
            "type": "request_accepted",
            "message": f"{current_user.full_name} accepted your connection request!",
            "connection_id": str(conn.connection_id),
            "accepter_id": str(current_user.user_id),
            "accepter_name": current_user.full_name
        },
        conn.user_id
    )
    
    return await _build_connection_response(conn, current_user.user_id, db)

@router.post("/decline", response_model=ConnectionResponse)
async def decline_connection(
    body: ConnectionAction,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Decline a pending connection request."""
    result = await db.execute(
        select(Connection).where(
            Connection.connection_id == body.connection_id,
            Connection.peer_id == current_user.user_id,
            Connection.status == ConnectionStatus.PENDING
        )
    )
    conn = result.scalar_one_or_none()
    if not conn:
        raise HTTPException(status_code=404, detail="Pending connection request not found")

    conn.status = ConnectionStatus.REJECTED
    await db.commit()
    await db.refresh(conn)
    return await _build_connection_response(conn, current_user.user_id, db)

@router.get("/", response_model=List[ConnectionResponse])
async def list_connections(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """List all connections (friends) for the current user."""
    result = await db.execute(
        select(Connection).where(
            or_(Connection.user_id == current_user.user_id, Connection.peer_id == current_user.user_id),
            Connection.status == ConnectionStatus.ACCEPTED
        )
    )
    connections = result.scalars().all()
    return [await _build_connection_response(c, current_user.user_id, db) for c in connections]

@router.get("/pending", response_model=List[ConnectionResponse])
async def list_pending_requests(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """List all incoming pending connection requests."""
    result = await db.execute(
        select(Connection).where(
            Connection.peer_id == current_user.user_id,
            Connection.status == ConnectionStatus.PENDING
        )
    )
    connections = result.scalars().all()
    return [await _build_connection_response(c, current_user.user_id, db) for c in connections]
