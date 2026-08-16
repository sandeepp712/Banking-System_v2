// src/features/dashboard/Dashboard.tsx
import {useAuth} from '../auth/AuthContext';
import {useEffect, useState} from 'react';
import {fetchAccount} from "../../api/accountApi.ts";

export default function Dashboard() {
    const {user} = useAuth();
    const [totalBalance, setTotalBalance] = useState<number>(0);
    const [accountCount, setAccountCount] = useState<number>(0);
    const [loading, setLoading] = useState<boolean>(false);

    useEffect(() => {
        const loadData = async () => {
            try {
                const accounts = await fetchAccount();
                setAccountCount(accounts.length);

                const total = accounts.reduce((sum, acc) => sum + acc.balance, 0);
                setTotalBalance(total);
            } catch (error) {
                console.log("Failed to load dashboard data", error);
            } finally {
                setLoading(false);
            }
        };
        loadData();
    }, []);

    return (
        <div className="space-y-6">
            <div className="bg-white p-6 rounded-lg shadow">
                <h2 className="text-2xl font-bold">Welcome back, {user?.username}! 👋</h2>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-blue-50 p-6 rounded-lg shadow text-center">
                    <p className="text-sm text-gray-600">Total Balance</p>
                    {loading ? (
                        <p className="text-2xl font-bold text-blue-600">Loading...</p>
                    ) : (
                        <p className="text-2xl font-bold text-blue-600">
                            {new Intl.NumberFormat('en-US', {style: 'currency', currency: 'USD'}).format(totalBalance)}
                        </p>
                    )}
                </div>
                <div className="bg-green-50 p-6 rounded-lg shadow text-center">
                    <p className="text-sm text-gray-600">Accounts</p>
                    <p className="text-2xl font-bold text-green-600">{accountCount}</p>
                </div>
                <div className="bg-purple-50 p-6 rounded-lg shadow text-center">
                    <p className="text-sm text-gray-600">Status</p>
                    <p className="text-2xl font-bold text-purple-600">✅ Active</p>
                </div>
            </div>
        </div>
    );
}