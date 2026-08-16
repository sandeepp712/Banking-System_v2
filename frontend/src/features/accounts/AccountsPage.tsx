import {useEffect, useState} from "react";
import {fetchAccount} from "../../api/accountApi.ts";
import type {Account} from "../../api/accountApi.ts";

export default function AccountsPage() {
    const [accounts, setAccounts] = useState<Account[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState('');

    useEffect(() => {
        const loadAccounts = async () => {
            try {
                const data = await fetchAccount();
                setAccounts(data);
            } catch (err: any) {
                setError('Failed to load accounts');
                console.log(err);
            } finally {
                setLoading(false);
            }
        };
        loadAccounts();
    }, []);

    if (loading) return <div className='text-gray-500'> Loading accounts</div>
    if (error) return <div className='text-red-500'>{error}</div>

    return (
        <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-xl font-bold mb-4">Your Accounts</h2>
            {accounts.length === 0 ? (
                <p className="text-gray-500">You have no accounts yet.</p>
            ) : (
                <div className="space-y-4">
                    {accounts.map((acc) => (
                        <div key={acc.accountNumber} className="flex justify-between items-center border-b pb-3">
                            <div>
                                <p className="font-medium">{acc.accountNumber}</p>
                                <p className="text-sm text-gray-500">{acc.currency}</p>
                            </div>
                            <p className="text-lg font-semibold">
                                {new Intl.NumberFormat('en-US', {
                                    style: 'currency',
                                    currency: acc.currency
                                }).format(acc.balance)}
                            </p>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}