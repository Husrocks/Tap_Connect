"""create_connections_table

Revision ID: c5ce8d124e01
Revises: 51f3ad1921ee
Create Date: 2026-05-16 00:46:05.706716

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = 'c5ce8d124e01'
down_revision: Union[str, Sequence[str], None] = '51f3ad1921ee'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    # Create new connections table
    op.create_table('connections',
        sa.Column('connection_id', sa.UUID(), nullable=False),
        sa.Column('user_id', sa.UUID(), nullable=False),
        sa.Column('peer_id', sa.UUID(), nullable=False),
        sa.Column('status', sa.Enum('PENDING', 'ACCEPTED', 'REJECTED', name='connectionstatus'), nullable=False),
        sa.Column('connection_type', sa.Enum('RADAR', 'NFC', 'BLE', name='connectiontype'), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['user_id'], ['users.user_id'], ),
        sa.ForeignKeyConstraint(['peer_id'], ['users.user_id'], ),
        sa.PrimaryKeyConstraint('connection_id')
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_table('connections')
    # We might need to drop the enums too if they are created
    op.execute('DROP TYPE connectionstatus')
    op.execute('DROP TYPE connectiontype')
