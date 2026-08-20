// src/features/accounts/AccountsPage.tsx
import {useEffect, useState} from "react";
import {fetchAccount, deposit, withdraw} from "../../api/accountApi";
import type {Account} from "../../api/accountApi";
import CreateAccountForm from "./CreateAccountForm";
import TransactionModal from "./TransactionModal.tsx";


export default function AccountsPage() {
    const [accounts, setAccounts] = useState<Account[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string>("");
    const [showCreateForm, setShowCreateForm] = useState<boolean>(false);
    const [modalOpen, setModalOpen] = useState<boolean>(false);
    const [modalType, setModalType] = useState<"deposit" | "withdraw">("deposit");
    const [selectedAccount, setSelectedAccount] = useState<Account | null>(null);

    // Fetch accounts on page load
    const loadAccounts = async () => {
        setLoading(true);
        try {
            const data = await fetchAccount();
            setAccounts(data);
        } catch (err: any) {
            setError("Failed to load accounts");
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadAccounts();
    }, []);

    const openModal = (type: "deposit" | "withdraw", account: Account) => {
        setModalType(type);
        setSelectedAccount(account);
        setModalOpen(true);
    }

    const handleTransaction = async (amount: number) => {
        if (!selectedAccount) return;

        const idemoptencyKey = crypto.randomUUID();

        if (modalType === "deposit") {
            await deposit(selectedAccount.accountNumber, amount, idemoptencyKey);
        } else {
            await withdraw(selectedAccount.accountNumber, amount, idemoptencyKey);
        }
    }

    // Handle account creation success
    const handleAccountCreated = () => {
        setShowCreateForm(false);
        loadAccounts(); // Refresh the list
    };

    if (loading) return <div className="text-gray-500">Loading accounts...</div>;
    if (error) return <div className="text-red-500">{error}</div>;

    return (
        <div className="bg-white p-6 rounded-lg shadow">
            <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-bold">Your Accounts</h2>
                <button
                    onClick={() => setShowCreateForm(!showCreateForm)}
                    className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition"
                >
                    {showCreateForm ? "Cancel" : "+ Create Account"}
                </button>
            </div>

            {showCreateForm && (
                <CreateAccountForm
                    onSuccess={handleAccountCreated}
                    onCancel={() => setShowCreateForm(false)}
                />
            )}

            {accounts.length === 0 && !showCreateForm ? (
                <p className="text-gray-500 text-center py-8">
                    You have no accounts yet. Click "Create Account" to get started.
                </p>
            ) : (
                <div className="space-y-4">
                    {accounts.map((acc) => (
                        <div
                            key={acc.accountNumber}
                            className="flex justify-between items-center border-b pb-3"
                        >
                            <div>
                                <p className="font-medium">{acc.accountNumber}</p>
                                <p className="text-sm text-gray-500">{acc.currency}</p>
                            </div>
                            <p className="text-lg font-semibold">
                                {new Intl.NumberFormat("en-US", {
                                    style: "currency",
                                    currency: acc.currency,
                                }).format(acc.balance)}
                            </p>
                            <div className="flex gap-2">
                                <button
                                    onClick={() => openModal("deposit", acc)}
                                    className="bg-green-600 text-white px-3 py-1 rounded text-sm hover:bg-green-700"
                                >
                                    Deposit
                                </button>
                                <button
                                    onClick={() => openModal("withdraw", acc)}
                                    className="bg-red-600 text-white px-3 py-1 rounded text-sm hover:bg-red-700"
                                >
                                    Withdraw
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {selectedAccount && (
                <TransactionModal
                    isOpen={modalOpen}
                    onClose={() => setModalOpen(false)}
                    onConfirm={handleTransaction}
                    title={`${modalType === "deposit" ? "💰 Deposit" : "💳 Withdraw"} Money`}
                    actionLabel={modalType === "deposit" ? "Deposit" : "Withdraw"}
                    accountNumber={selectedAccount.accountNumber}
                />
            )}
        </div>
    );
}