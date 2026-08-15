import {useState, useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {bankingApiClient} from '../../api/client';
import {fetchAccount} from '../../api/accountApi';
import type {Account} from "../../api/accountApi";

export default function TransferPage() {
    const [accounts, setAccounts] = useState<Account[]>([]);
    const [fromAccount, setFromAccount] = useState('');
    const [toAccount, setToAccount] = useState('');
    const [amount, setAmount] = useState('');
    const [currency, setCurrency] = useState('USD');
    const [loading, setLoading] = useState(false);
    const [status, setStatus] = useState<{ type: 'success' | 'error' | null; message: string }>({
        type: null,
        message: ''
    });
    const navigate = useNavigate();

    // Load accounts for the dropdown
    useEffect(() => {
        const loadAccounts = async () => {
            try {
                const data = await fetchAccount();
                setAccounts(data);
                if (data.length > 0) setFromAccount(data[0].accountNumber);
            } catch (error) {
                console.error('Failed to load accounts', error);
            }
        };
        loadAccounts();
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setStatus({type: null, message: ''});

        // 🔥 Generate a UNIQUE Idempotency Key per request!
        const idempotencyKey = crypto.randomUUID();

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

            // Optionally, clear the form or redirect after a delay
            setTimeout(() => {
                navigate('/history');
            }, 2000);

        } catch (error: any) {
            // Check if it's a duplicate transaction (409 Conflict)
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
                {/* From Account */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">From Account</label>
                    <select
                        className="w-full p-2 border rounded"
                        value={fromAccount}
                        onChange={(e) => setFromAccount(e.target.value)}
                        required
                    >
                        {accounts.map((acc) => (
                            <option key={acc.accountNumber} value={acc.accountNumber}>
                                {acc.accountNumber} (Balance: {acc.balance} {acc.currency})
                            </option>
                        ))}
                    </select>
                </div>

                {/* To Account */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">To Account</label>
                    <input
                        type="text"
                        placeholder="Account number"
                        className="w-full p-2 border rounded"
                        value={toAccount}
                        onChange={(e) => setToAccount(e.target.value)}
                        required
                    />
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

                {/* Currency */}
                {/*<div>*/}
                {/*    <label className="block text-sm font-medium text-gray-700 mb-1">Currency</label>*/}
                {/*    <select*/}
                {/*        className="w-full p-2 border rounded"*/}
                {/*        value={currency}*/}
                {/*        onChange={(e) => setCurrency(e.target.value)}*/}
                {/*    >*/}
                {/*        <option value="USD">USD</option>*/}
                {/*        <option value="EUR">EUR</option>*/}
                {/*        <option value="GBP">GBP</option>*/}
                {/*        <option value="INR">INR</option>*/}
                {/*    </select>*/}
                {/*</div>*/}

                <button
                    type="submit"
                    disabled={loading || accounts.length === 0}
                    className="w-full bg-green-600 text-white p-2 rounded hover:bg-green-700 disabled:opacity-50"
                >
                    {loading ? 'Processing...' : 'Send Money'}
                </button>
            </form>
        </div>
    );
}