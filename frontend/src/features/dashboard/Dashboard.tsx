// src/features/dashboard/Dashboard.tsx
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.tsx';
import { fetchAccount } from '../../api/accountApi';
import { fetchTransaction } from '../../api/accountApi';
import type {Account,Transaction} from "../../api/accountApi";

export default function Dashboard() {
    const { user } = useAuth();
    const navigate = useNavigate();
    const [accounts, setAccounts] = useState<Account[]>([]);
    const [recentTxs, setRecentTxs] = useState<Transaction[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const loadDashboardData = async () => {
            // console.log('🔵 Dashboard: Starting to load data...');
            try {
                // console.log('🟢 Dashboard: Fetching accounts...');
                const accData = await fetchAccount();
                // console.log('✅ Dashboard: Accounts loaded:', accData);
                setAccounts(accData);

                // console.log('🟢 Dashboard: Fetching transactions...');
                const txData = await fetchTransaction();
                // console.log('✅ Dashboard: Transactions loaded:', txData);
                setRecentTxs(txData.slice(0, 4));
            } catch (err) {
                // console.error('❌ Dashboard: Error loading data:', err);
                setError(err instanceof Error ? err.message : 'Failed to load dashboard data');
            } finally {
                // console.log('🟢 Dashboard: Setting loading to false');
                setLoading(false);
            }
        };
        loadDashboardData();
    }, []);

    // console.log('🔄 Dashboard: Rendering... loading:', loading, 'accounts:', accounts.length, 'error:', error);

    if (error) {
        return (
            <div className="p-6 bg-red-50 border border-red-200 rounded-lg">
                <h3 className="text-red-800 font-bold">Error loading dashboard</h3>
                <p className="text-red-600">{error}</p>
                <button
                    onClick={() => window.location.reload()}
                    className="mt-2 px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                >
                    Retry
                </button>
            </div>
        );
    }

    // If loading, show a spinner
    if (loading) {
        return (
            <div className="p-6 text-center">
                <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                <p className="mt-2 text-gray-500">Loading dashboard...</p>
            </div>
        );
    }

    // If no accounts, show a message
    if (accounts.length === 0) {
        return (
            <div className="p-6 bg-yellow-50 border border-yellow-200 rounded-lg">
                <h3 className="text-yellow-800 font-bold">No accounts found</h3>
                <p className="text-yellow-600">You don't have any accounts yet.</p>
            </div>
        );
    }

    // Calculate total balance
    const totalBalance = accounts.reduce((sum, acc) => sum + (acc.balance || 0), 0);

    return (
        <div className="space-y-6">
            {/* Welcome Section */}
            <div className="bg-white p-6 rounded-lg shadow">
                <h2 className="text-2xl font-bold">Welcome back, {user?.username}! 👋</h2>
                <p className="text-gray-600">Here's a quick overview of your finances.</p>
            </div>

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-blue-50 p-6 rounded-lg shadow text-center">
                    <p className="text-sm text-gray-600">Total Balance</p>
                    <p className="text-2xl font-bold text-blue-600">
                        {new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(totalBalance)}
                    </p>
                </div>
                <div className="bg-green-50 p-6 rounded-lg shadow text-center">
                    <p className="text-sm text-gray-600">Accounts</p>
                    <p className="text-2xl font-bold text-green-600">{accounts.length}</p>
                </div>
                <div className="bg-purple-50 p-6 rounded-lg shadow text-center">
                    <p className="text-sm text-gray-600">Recent Transactions</p>
                    <p className="text-2xl font-bold text-purple-600">{recentTxs.length}</p>
                </div>
            </div>

            {/* Quick Actions */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <button
                    onClick={() => navigate('/transfer')}
                    className="bg-blue-600 text-white p-4 rounded-lg shadow hover:bg-blue-700 transition flex items-center justify-center gap-2"
                >
                    💸 Send Money
                </button>
                <button
                    onClick={() => navigate('/accounts')}
                    className="bg-gray-800 text-white p-4 rounded-lg shadow hover:bg-gray-900 transition flex items-center justify-center gap-2"
                >
                    💳 View Accounts
                </button>
                <button
                    onClick={() => navigate('/history')}
                    className="bg-green-700 text-white p-4 rounded-lg shadow hover:bg-green-800 transition flex items-center justify-center gap-2"
                >
                    📜 View History
                </button>
            </div>

            {/* Accounts Mini-List */}
            <div className="bg-white p-6 rounded-lg shadow">
                <div className="flex justify-between items-center mb-4">
                    <h3 className="text-lg font-semibold">Your Accounts</h3>
                    <button
                        onClick={() => navigate('/accounts')}
                        className="text-sm text-blue-600 hover:underline"
                    >
                        View All →
                    </button>
                </div>
                <div className="divide-y">
                    {accounts.slice(0, 3).map((acc) => (
                        <div key={acc.accountNumber} className="py-2 flex justify-between">
                            <span className="font-mono">{acc.accountNumber}</span>
                            <span className="font-medium">
                                {new Intl.NumberFormat('en-US', { style: 'currency', currency: acc.currency || 'USD' }).format(acc.balance)}
                            </span>
                        </div>
                    ))}
                </div>
            </div>

            {/* Recent Activity */}
            <div className="bg-white p-6 rounded-lg shadow">
                <div className="flex justify-between items-center mb-4">
                    <h3 className="text-lg font-semibold">Recent Activity</h3>
                    <button
                        onClick={() => navigate('/history')}
                        className="text-sm text-blue-600 hover:underline"
                    >
                        See All →
                    </button>
                </div>
                {recentTxs.length === 0 ? (
                    <p className="text-gray-500">No recent transactions. <button onClick={() => navigate('/transfer')} className="text-blue-600 hover:underline">Send money now</button></p>
                ) : (
                    <div className="divide-y">
                        {recentTxs.map((tx) => (
                            <div key={tx.transactionId} className="py-2 flex justify-between items-center">
                                <div>
                                    <p className="text-sm">
                                        <span className="font-mono">{tx.fromAccount}</span>
                                        {' → '}
                                        <span className="font-mono">{tx.toAccount}</span>
                                    </p>
                                    <p className="text-xs text-gray-400">
                                        {new Date(tx.createdAt).toLocaleDateString()}
                                    </p>
                                </div>
                                <div className="text-right">
                                    <p className="font-medium">
                                        {new Intl.NumberFormat('en-US', {
                                            style: 'currency',
                                            currency: tx.amount?.currency || 'USD'
                                        }).format(tx.amount?.amount || 0)}
                                    </p>
                                    <span className={`text-xs px-2 py-0.5 rounded ${
                                        tx.status === 'COMMITTED'
                                            ? 'bg-green-100 text-green-800'
                                            : 'bg-yellow-100 text-yellow-800'
                                    }`}>
                                        {tx.status}
                                    </span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}