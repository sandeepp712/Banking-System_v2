import {useState, useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {bankingApiClient} from '../../api/client';
import {fetchAccount, fetchAllAccounts} from '../../api/accountApi';
import type {Account} from "../../api/accountApi";
import {generateIdempotencyKey} from "../../lib/utils/Fallbackfunction.ts";

export default function TransferPage() {
    const [userAccounts, setUserAccounts] = useState<Account[]>([]);   // For "From Account"
    const [allAccounts, setAllAccounts] = useState<Account[]>([]);     // For "To Account"
    const [fromAccount, setFromAccount] = useState('');
    const [toAccount, setToAccount] = useState('');
    const [amount, setAmount] = useState('');
    const [loading, setLoading] = useState(false);
    const [status, setStatus] = useState<{ type: 'success' | 'error' | null; message: string }>({
        type: null,
        message: ''
    });
    const navigate = useNavigate();

    // Load accounts on mount
    useEffect(() => {
        const loadAccounts = async () => {
            try {
                // 1. Load user's own accounts (for From dropdown)
                const userAccs = await fetchAccount();
                setUserAccounts(userAccs);
                if (userAccs.length > 0) setFromAccount(userAccs[0].accountNumber);

                // 2. Load ALL accounts (for To dropdown)
                const allAccs = await fetchAllAccounts();
                const userAccountsNumbers = userAccs.map(acc => acc.accountNumber);
                const otherAccounts = allAccs.filter(
                    acc => !userAccountsNumbers.includes(acc.accountNumber)
                );

                setAllAccounts(otherAccounts);
                if (otherAccounts.length > 0) {
                    setToAccount(otherAccounts[0].accountNumber);
                } else {
                    setToAccount('');
                }

            } catch (error) {
                console.error('Failed to load accounts', error );
            }
        };
        loadAccounts();
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setStatus({type: null, message: ''});

        const idempotencyKey = generateIdempotencyKey();

        try {
            const response = await bankingApiClient.post('/transfers', {
                fromAccountNumber: fromAccount,
                toAccountNumber: toAccount,
                amount: parseFloat(amount),
                idempotencyKey: idempotencyKey,
            });

            setStatus({
                type: 'success',
                message: `✅ Transfer successful! Transaction ID: ${response.data.transactionId}`
            });

            setTimeout(() => navigate('/history'), 2000);

        } catch (error: any) {
            if (error.response?.status === 409) {
                setStatus({
                    type: 'error',
                    message: '⚠️ Duplicate request detected! This transfer was already submitted.'
                });
            } else {
                setStatus({
                    type: 'error',
                    message: error.response?.data?.message || '❌ Transfer failed. Please try again.'
                });
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-xl font-bold mb-6">💸 Transfer Money</h2>

            {status.message && (
                <div
                    className={`p-4 rounded mb-4 ${status.type === 'success' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                    {status.message}
                </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
                {/* From Account (User's own accounts) */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">From Account</label>
                    <select
                        className="w-full p-2 border rounded"
                        value={fromAccount}
                        onChange={(e) => setFromAccount(e.target.value)}
                        required
                    >
                        {userAccounts.map((acc) => (
                            <option key={acc.accountNumber} value={acc.accountNumber}>
                                {acc.accountNumber} (Balance: {acc.balance} {acc.currency})
                            </option>
                        ))}
                    </select>
                </div>

                {/* To Account (All accounts) */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">To Account</label>
                    {allAccounts.length === 0 ? (
                        <div className="p-2 border rounded bg-gray-100 text-gray-500 text-sm">
                            No other accounts available to transfer to.
                        </div>
                    ) : (
                        <select
                            className="w-full p-2 border rounded"
                            value={toAccount}
                            onChange={(e) => setToAccount(e.target.value)}
                            required
                        >
                            {allAccounts.map((acc) => (
                                <option key={acc.accountNumber} value={acc.accountNumber}>
                                    {acc.accountNumber} ({acc.currency})
                                </option>
                            ))}
                        </select>
                    )}
                </div>

                {/* Amount */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Amount</label>
                    <input
                        type="number"
                        step="0.01"
                        placeholder="0.00"
                        className="w-full p-2 border rounded"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        required
                    />
                </div>

                <button
                    type="submit"
                    disabled={loading || userAccounts.length === 0 || allAccounts.length === 0}
                    className="w-full bg-green-600 text-white p-2 rounded hover:bg-green-700 disabled:opacity-50"
                >
                    {loading ? 'Processing...' : 'Send Money'}
                </button>
            </form>
        </div>
    );
}