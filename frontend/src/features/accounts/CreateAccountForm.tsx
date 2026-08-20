// src/features/accounts/CreateAccountForm.tsx
import {useState} from "react";
import {createAccount} from "../../api/accountApi";

interface CreateAccountFormProps {
    onSuccess: () => void;
    onCancel: () => void;
}

export default function CreateAccountForm({onSuccess, onCancel}: CreateAccountFormProps) {
    const [accountNumber, setAccountNumber] = useState("");
    const [initialBalance, setInitialBalance] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError("");

        try {
            await createAccount({
                accountNumber,
                amount: parseFloat(initialBalance),
            });
            onSuccess(); // Refresh the account list
        } catch (err: any) {
            setError(err.response?.data?.message || "Failed to create account");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="bg-gray-50 p-4 rounded-lg border mb-4">
            <h3 className="text-lg font-semibold mb-3">Create New Account</h3>
            {error && (
                <div className="bg-red-100 text-red-700 p-2 rounded mb-3">
                    {error}
                </div>
            )}
            <form onSubmit={handleSubmit} className="space-y-3">
                <input
                    type="text"
                    placeholder="Account Number (e.g., ACC-123)"
                    className="w-full p-2 border rounded"
                    value={accountNumber}
                    onChange={(e) => setAccountNumber(e.target.value)}
                    required
                />
                <input
                    type="number"
                    step="0.01"
                    placeholder="Initial Balance (e.g., 100.00)"
                    className="w-full p-2 border rounded"
                    value={initialBalance}
                    onChange={(e) => setInitialBalance(e.target.value)}
                />
                <div className="flex gap-2">
                    <button
                        type="submit"
                        disabled={loading}
                        className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 disabled:opacity-50"
                    >
                        {loading ? "Creating..." : "Create Account"}
                    </button>
                    <button
                        type="button"
                        onClick={onCancel}
                        className="bg-gray-300 text-gray-700 px-4 py-2 rounded hover:bg-gray-400"
                    >
                        Cancel
                    </button>
                </div>
            </form>
        </div>
    );
}