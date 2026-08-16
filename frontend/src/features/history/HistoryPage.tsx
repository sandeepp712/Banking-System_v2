import { useEffect, useState } from 'react';
import { fetchTransaction } from '../../api/accountApi';
import type {Transaction} from "../../api/accountApi";

export default function HistoryPage() {
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const loadHistory = async () => {
            try {
                const data = await fetchTransaction();
                setTransactions(data);
            } catch (err) {
                setError('Failed to load transaction history');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        loadHistory();
    }, []);

    if (loading) return <div className="text-gray-500">Loading history...</div>;
    if (error) return <div className="text-red-500">{error}</div>;

    return (
        <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-xl font-bold mb-4">📜 Transaction History</h2>
            {transactions.length === 0 ? (
                <p className="text-gray-500">No transactions yet.</p>
            ) : (
                <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-gray-50 text-gray-700 uppercase">
                        <tr>
                            <th className="p-3">Date</th>
                            <th className="p-3">From</th>
                            <th className="p-3">To</th>
                            <th className="p-3 text-right">Amount</th>
                            <th className="p-3 text-center">Status</th>
                        </tr>
                        </thead>
                        <tbody>
                        {transactions.map((tx) => (
                            <tr key={tx.transactionId} className="border-b hover:bg-gray-50">
                                <td className="p-3">
                                    {new Date(tx.createdAt).toLocaleDateString()}
                                    <br />
                                    <span className="text-xs text-gray-400">
                                            {new Date(tx.createdAt).toLocaleTimeString()}
                                        </span>
                                </td>
                                <td className="p-3 font-mono text-sm">{tx.fromAccount}</td>
                                <td className="p-3 font-mono text-sm">{tx.toAccount}</td>
                                <td className="p-3 text-right font-medium">
                                    {new Intl.NumberFormat('en-US', {
                                        style: 'currency',
                                        currency: tx.amount.currency,
                                    }).format(tx.amount.amount)}
                                </td>
                                <td className="p-3 text-center">
                                        <span
                                            className={`px-2 py-1 rounded text-xs font-semibold ${
                                                tx.status === 'COMMITTED'
                                                    ? 'bg-green-100 text-green-800'
                                                    : tx.status === 'PENDING'
                                                        ? 'bg-yellow-100 text-yellow-800'
                                                        : 'bg-red-100 text-red-800'
                                            }`}
                                        >
                                            {tx.status}
                                        </span>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}