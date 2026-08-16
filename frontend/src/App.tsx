import { JSX, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, useAuth } from "./features/auth/AuthContext.tsx";
import LoginPage from "./features/auth/LoginPage.tsx";
import RegisterPage from "./features/auth/RegisterPage.tsx";
import Dashboard from "./features/dashboard/Dashboard.tsx";
import './App.css'
import Layout from "./features/layout/Layout.tsx";
import AccountsPage from "./features/accounts/AccountsPage.tsx";
import TransferPage from "./features/transfers/TransferPage.tsx";
import HistoryPage from "./features/history/HistoryPage.tsx";

const ProtectedRoutes = ({ children }: { children: JSX.Element }) => {
    const { user } = useAuth();
    return user ? children : <Navigate to="/login" replace />;
}

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    <Route path='/login' element={<LoginPage />} />
                    <Route path='/register' element={<RegisterPage />} />
                    <Route
                        path='/dashboard'
                        element={
                            <ProtectedRoutes>
                                <Layout/>
                            </ProtectedRoutes>
                        }
                    />
                    <Route path="dashboard" element={<Dashboard/>}/>
                    <Route path="accounts" element={<AccountsPage />} />
                    <Route path="transfer" element={<TransferPage />} />
                    <Route path="history" element={<HistoryPage />} />
                    <Route path="/" element={<Navigate to='/dashboard' replace />} />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    )
}

export default App