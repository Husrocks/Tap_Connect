import asyncio
from app.config import settings
from sqlalchemy.ext.asyncio import create_async_engine
from sqlalchemy import text

async def main():
    print(f"Connecting to database to add 'where_met' column...")
    engine = create_async_engine(settings.DATABASE_URL)
    async with engine.begin() as conn:
        # PostgreSQL supports ADD COLUMN IF NOT EXISTS in 9.6+
        await conn.execute(text("ALTER TABLE connections ADD COLUMN IF NOT EXISTS where_met VARCHAR(200);"))
        print("Column 'where_met' successfully added to 'connections' table (or already existed)!")
    await engine.dispose()

if __name__ == "__main__":
    asyncio.run(main())
