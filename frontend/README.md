# 🏦 Banking System - Frontend

This is the **React + TypeScript** frontend for the Multithreaded Banking API. It provides a secure, responsive dashboard for users to manage their accounts, transfer money, and view transaction history—all while communicating with the Spring Boot backend via JWT-authenticated `HttpOnly` cookies.

## ✨ Key Features

- **🔐 Secure Authentication**: Login/Register flow using JWT stored in `HttpOnly` cookies (XSS-safe).
- **📊 Dashboard Overview**: Quick glance at total balance, account list, and recent transactions.
- **💸 Idempotent Transfers**: Frontend automatically generates a unique `idempotencyKey` (`crypto.randomUUID()`) to prevent double-debiting on network retries.
- **📜 Transaction History**: Complete audit trail of all sent/received funds.
- **🎨 Modern UI**: Built with Tailwind CSS for a clean, responsive banking interface.
- **⚡ Performance**: Blazing fast development with Vite.

## 🛠️ Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Core** | React 18, TypeScript |
| **Build Tool** | Vite |
| **Styling** | Tailwind CSS |
| **HTTP Client** | Axios (with interceptors for 401 handling) |
| **Routing** | React Router DOM (v6) |
| **State Management** | React Context (Auth state) |

## 🚀 Getting Started

### Prerequisites
- **Node.js** (v18 or higher)
- **npm** (v9 or higher)
- The **Backend API** must be running locally (or accessible via network).

### 1. Clone the Repository (if not already done)
```bash
1. git clone https://github.com/sandeepp712/Banking-System_v2.git
2. cd Banking-System_v2/frontend

3. npm install
4. npm run dev
5. npm run build
```


### Frontend Directory Structure

```text
frontend/
├── src/
│   ├── api/                      # Axios clients & API fetch functions
│   │   ├── client.ts             # (authApiClient, bankingApiClient with interceptors)
│   │   └── accountApi.ts         # fetchAccounts, fetchTransactions
│   │
│   ├── features/                 # Feature-based modules (Scalable architecture)
│   │   ├── auth/                 # Login, Register, AuthContext
│   │   ├── accounts/             # Accounts list page
│   │   ├── dashboard/            # Main dashboard (stats, quick actions)
│   │   ├── transfers/            # Transfer form (with idempotency)
│   │   ├── history/              # Transaction history table
│   │   └── layout/               # Shell (Sidebar + Header + Outlet)
│   │
│   ├── App.tsx                   # Routes & Providers
│   └── main.tsx                  # React entry point (StrictMode)
│
├── index.html                    # Vite entry HTML
├── tailwind.config.js            # Tailwind CSS configuration
└── package.json                  # Dependencies & scripts
```

### 🤝 Contributing
```bash
  Fork the repository.
  Create a feature branch (git switch -c feature/amazing-feature).
  Commit your changes (git commit -m 'Add some amazing feature').
  Push to the branch (git push origin feature/amazing-feature).
  Open a Pull Request
```