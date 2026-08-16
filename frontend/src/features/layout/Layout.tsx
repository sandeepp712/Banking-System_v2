// src/features/layout/Layout.tsx
import {Outlet, Link, useNavigate} from 'react-router-dom';
import {useAuth} from '../auth/AuthContext';

export default function Layout() {
    const {user, logout} = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        await logout();
        navigate('/login');
    };

    return (
        <div className="flex h-screen bg-gray-100">
            {/* Sidebar */}
            <div className="w-64 bg-white shadow-md flex flex-col">
                <div className="p-4 border-b">
                    <h1 className="text-xl font-bold text-blue-600">🏦 MyBank</h1>
                    <p className="text-sm text-gray-500">{user?.username}</p>
                </div>
                <nav className="flex-1 p-4 space-y-2">
                    <Link to="/dashboard" className="block p-2 hover:bg-blue-50 rounded">
                        📊 Dashboard
                    </Link>
                    <Link to="/accounts" className="block p-2 hover:bg-blue-50 rounded">
                        💳 Accounts
                    </Link>
                    <Link to="/transfer" className="block p-2 hover:bg-blue-50 rounded">
                        💸 Transfer
                    </Link>
                    <Link to="/history" className="block p-2 hover:bg-blue-50 rounded">
                        📜 History
                    </Link>
                </nav>
                <div className="p-4 border-t">
                    <button
                        onClick={handleLogout}
                        className="w-full text-left text-red-600 hover:bg-red-50 p-2 rounded"
                    >
                        🚪 Logout
                    </button>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 overflow-y-auto p-6">
                <Outlet/> {/* This is where the current page (Dashboard, Transfer, etc.) renders */}
            </div>
        </div>
    );
}