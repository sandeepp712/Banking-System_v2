import {useState} from "react";

export  interface TransactionModalProps {
    isOpen: boolean,
    onClose: () => void,
    onConfirm: (amount: number) => Promise<void>,
    title: string,
    actionLabel: string,
    accountNumber: string
}

export default function TransactionModal({
                                             isOpen,
                                             onClose,
                                             onConfirm,
                                             title,
                                             actionLabel,
                                             accountNumber,
                                         }: TransactionModalProps) {
    const [amount, setAmount] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    if (!isOpen) return null;

    const handleSubmit = async (e: React.SubmitEvent) => {
        e.preventDefault();
        setLoading(true);
        setError("");
        try {
            await onConfirm(parseFloat(amount));
            setAmount("");
            onClose();
        } catch (err: any) {
            setError(err.response?.data?.message || "Transaction failed");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white p-6 rounded-lg shadow-lg w-96">
                <h3 className="text-xl font-bold mb-2">{title}</h3>
                <p className="text-sm text-gray-500 mb-4">Account: {accountNumber}</p>

                {error && (
                    <div className="bg-red-100 text-red-700 p-2 rounded mb-3">{error}</div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Amount
                        </label>
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
                    <div className="flex gap-2">
                        <button
                            type="submit"
                            disabled={loading}
                            className={`flex-1 p-2 rounded text-white ${
                                actionLabel === "Deposit"
                                    ? "bg-green-600 hover:bg-green-700"
                                    : "bg-red-600 hover:bg-red-700"
                            } disabled:opacity-50`}
                        >
                            {loading ? "Processing..." : actionLabel}
                        </button>
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 bg-gray-300 text-gray-700 p-2 rounded hover:bg-gray-400"
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}