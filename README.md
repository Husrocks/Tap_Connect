# 📲 TapConnect — Proximity-Based, AI-Powered Professional Networking

TapConnect is a futuristic, real-time professional networking platform designed to eliminate the friction of networking in physical spaces (conferences, meetups, co-working offices). By pairing local **Bluetooth Low Energy (BLE)** scanning with a robust **FastAPI backend** and a **Dual-Model LLaMA AI matching engine**, TapConnect automatically discovers nearby professionals, highlights mutual interests, and provides instant, context-aware icebreakers to kickstart organic, real-world conversations.

---

## 🏗️ Platform Architecture

TapConnect is built as a modular multi-tier platform combining a native Android client with a container-ready async Python server.

```mermaid
graph TD
    subgraph "Mobile Client (Kotlin / Compose)"
        MC[Jetpack Compose UI]
        BLE_A[BLE Advertiser]
        BLE_S[BLE Scanner]
        RF[Retrofit REST Client]
        WS_C[WebSocket Client]
    end

    subgraph "Network Layer"
        BLE_NET((Bluetooth LE Proximity))
        HTTP((HTTP REST API))
        WS_NET((WebSockets))
    end

    subgraph "Backend System (FastAPI)"
        FA[FastAPI Server]
        DB[(PostgreSQL / SQLite)]
        AI[Groq AI Service]
        CL[Cloudinary Storage]
    end

    %% Proximity Flow
    BLE_A <--> BLE_NET
    BLE_S <--> BLE_NET

    %% Frontend to Backend Connection
    RF <--> HTTP <--> FA
    WS_C <--> WS_NET <--> FA

    %% Backend integrations
    FA <--> DB
    FA <--> AI
    FA <--> CL
```

---

## ⚡ Key Features

* **Real-Time Proximity Scanning (BLE)**: Active advertisement and scanning using Bluetooth Low Energy. Discovers nearby TapConnect users in a 10–30 meter radius without needing active GPS coordinates.
* **Dual-Model LLaMA Matchmaking (via Groq)**:
  * 🎛️ **`llama-3.1-8b-instant`**: Blazing-fast, ultra-low latency generation of short, one-sentence conversation starters (icebreakers) as soon as two devices cross paths.
  * 🧠 **`llama-3.3-70b-versatile`**: Deep semantic analysis of user profiles (roles, bios, organizations, and interests) to generate highly polished, recruiter-grade professional matching summaries.
* **Bi-directional WebSockets**: Implements instant state synchronization. When you "Tap" to connect with a nearby professional, they receive a real-time connection request notification on their screen instantly.
* **Media & Storage (Phase 5)**: Cloudinary integration for smooth, CDN-optimized user profile picture uploads.
* **Premium UI/UX Design**: Built with Jetpack Compose featuring cohesive custom theme palettes, micro-interactions, responsive list animations, and clean navigation flows.

---

## 🛠️ Technology Stack

| Layer | Technologies Used |
|---|---|
| **Mobile Frontend** | Kotlin, Jetpack Compose, Coroutines/Flow, Retrofit, OkHttp, WebSockets, BLE API |
| **Backend API** | Python 3.10+, FastAPI (Asynchronous), SQLAlchemy (Async Engine), Pydantic v2 |
| **Database** | PostgreSQL (or SQLite locally), Alembic (Schema Migrations) |
| **AI Infrastructure** | Groq API (LLaMA-3.1-8B-Instant & LLaMA-3.3-70B-Versatile) |
| **Media Hosting** | Cloudinary CDN Service |

---

## 📂 Codebase Directory Map

We maintain a clean separation of concerns, keeping the client and server fully independent:

```text
Tap_Connect/
├── app/                      # Native Android Client (Kotlin / Compose)
│   ├── src/main/
│   │   ├── java/com/tapconnect/
│   │   │   ├── data/         # API Clients, BLE Scanner/Advertiser, WebSockets
│   │   │   ├── ui/           # Jetpack Compose Screens, ViewModels, Theme
│   │   │   └── TapConnectApp # Application Class
│   │   └── res/              # Vector Icons, Layout resources, Launch logo
│   └── build.gradle          # Android Module build config
├── backend/                  # Async FastAPI Server (Python)
│   ├── app/
│   │   ├── models/           # SQLAlchemy DB Models (Users, Connections)
│   │   ├── routers/          # API Endpoints (Auth, Discovery, WebSockets)
│   │   ├── schemas/          # Pydantic v2 Data Validation
│   │   ├── services/         # Business Logic (Groq AI, Cloudinary Storage)
│   │   └── main.py           # Application Entry Point
│   ├── alembic/              # Database Schema Migrations version history
│   ├── alembic.ini           # Migrations Configuration
│   ├── requirements.txt      # Python Package Dependencies
│   └── .env.example          # Environment variables template
├── build.gradle              # Multi-project Root Gradle configuration
├── settings.gradle           # Gradle Module definitions
└── .gitignore                # Optimized Git rules (IDE, Secrets, Builds ignored)
```

---

## 🚀 Local Development Setup

### 1. Backend Server Setup

Navigate into the `backend/` directory:
```bash
cd backend
```

#### Step 1.1: Virtual Environment
Create and activate a Python virtual environment:
```bash
# Windows
python -m venv .venv
.venv\Scripts\activate

# macOS / Linux
python3 -m venv .venv
source .venv/bin/activate
```

#### Step 1.2: Install Dependencies
```bash
pip install -r requirements.txt
```

#### Step 1.3: Configure Environments
Create a `.env` file inside the `backend/` folder based on `.env.example`:
```env
DATABASE_URL=postgresql+asyncpg://postgres:yourpassword@localhost:5432/tapconnect
SECRET_KEY=generate-a-long-random-string-here
ACCESS_TOKEN_EXPIRE_MINUTES=10080

# Cloudinary Details
CLOUDINARY_CLOUD_NAME=your_cloudinary_cloud_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret

# AI Models Detail
GROQ_API_KEY=your_groq_api_key
```

#### Step 1.4: Database Migrations
Generate and run database tables using Alembic:
```bash
alembic upgrade head
```

#### Step 1.5: Run the Server
Launch the development server via Uvicorn:
```bash
uvicorn app.main:app --reload
```
The interactive documentation will be available locally at **http://127.0.0.1:8000/docs**.

---

### 2. Native Android Client Setup

#### Step 2.1: Open Project
1. Launch **Android Studio**.
2. Select **Open an existing project** and choose the root `Tap_Connect` folder.
3. Allow Gradle to automatically sync dependencies.

#### Step 2.2: API Server Configuration
Set up your local or production server endpoint in the `RetrofitClient` and `WebSocketManager` source files located in `app/src/main/java/com/tapconnect/data/`:
* **HTTP Endpoint**: Change to your backend server IP (e.g., `http://10.0.2.2:8000/` for emulator or `http://<your-machine-ip>:8000/` for a physical device).
* **WebSocket Endpoint**: `ws://<your-machine-ip>:8000/ws`

#### Step 2.3: Device & Permissions
* A **physical Android device** is strongly recommended for testing BLE. Android Emulators do not fully support Bluetooth scanning/advertising emulation.
* Enable **Bluetooth** and **Location Services** (required by Android's BLE framework for scanning nearby devices).
* Run the app via Android Studio onto your connected device.

---

## 🔒 Security Best Practices

1. **Never Commit Secret Keys**: Always load `DATABASE_URL`, `SECRET_KEY`, `GROQ_API_KEY`, and Cloudinary variables via the `.env` configuration file, which is actively ignored by `.gitignore`.
2. **API Authentication**: All communication to protected endpoints (connections, profile management, websocket sessions) is secured via standard JWT Bearer tokens.
3. **Bluetooth Compliance**: Respects standard Android runtime permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `ACCESS_FINE_LOCATION`) ensuring complete user compliance.
