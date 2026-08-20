import type { JSX } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext.tsx";
import LoginPage from "./features/auth/LoginPage.tsx";
import RegisterPage from "./features/auth/RegisterPage.tsx";
import Layout from "./features/layout/Layout.tsx";
import Dashboard from "./features/dashboard/Dashboard.tsx";
import AccountsPage from "./features/accounts/AccountsPage.tsx";
import TransferPage from "./features/transfers/TransferPage.tsx";
import HistoryPage from "./features/history/HistoryPage.tsx";
import './App.css';

const ProtectedRoutes = ({ children }: { children: JSX.Element }) => {
    const { user } = useAuth();
    return user ? children : <Navigate to="/login" replace />;
}

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    {/* Public Routes */}
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />

                    {/* Protected Routes (wrapped in Layout) */}
                    <Route
                        path="/"
                        element={
                            <ProtectedRoutes>
                                <Layout />
                            </ProtectedRoutes>
                        }
                    >
                        {/* ✅ Nested routes render inside Layout's <Outlet /> */}
                        <Route index element={<Navigate to="/dashboard" replace />} />
                        <Route path="dashboard" element={<Dashboard />} />
                        <Route path="accounts" element={<AccountsPage />} />
                        <Route path="transfer" element={<TransferPage />} />
                        <Route path="history" element={<HistoryPage />} />
                    </Route>
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}

export default App;