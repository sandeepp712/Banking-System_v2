import {useState} from 'react';
import {useAuth} from '../../context/AuthContext.tsx';
import {Link, useNavigate} from 'react-router-dom';

export default function RegisterPage() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState('RETAIL_USER');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const {register, isLoading} = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e: React.SubmitEvent) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        try {
            await register(username, password, role);
            setSuccess('Registration successful! Please login.');
            setTimeout(() => navigate('/login'), 1500);
        } catch (err: any) {
            setError(err.message);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
            <div className="bg-white p-8 rounded-lg shadow-md w-96">
                <h1 className="text-2xl font-bold text-center mb-6">📝 Create Account</h1>

                {error && (
                    <div className="bg-red-100 text-red-700 p-3 rounded mb-4">{error}</div>
                )}
                {success && (
                    <div className="bg-green-100 text-green-700 p-3 rounded mb-4">{success}</div>
                )}

                <form onSubmit={handleSubmit}>
                    <input
                        type="text"
                        placeholder="Username"
                        className="w-full p-2 border rounded mb-3"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                    />
                    <input
                        type="password"
                        placeholder="Password (8+ chars, uppercase, number, special)"
                        className="w-full p-2 border rounded mb-3"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                    <select
                        className="w-full p-2 border rounded mb-4"
                        value={role}
                        onChange={(e) => setRole(e.target.value)}
                    >
                        <option value="RETAIL_USER">User</option>
                        <option value="ADMIN">Admin</option>
                    </select>
                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full bg-green-600 text-white p-2 rounded hover:bg-green-700 disabled:opacity-50"
                    >
                        {isLoading ? 'Creating...' : 'Register'}
                    </button>
                </form>

                <p className="text-center text-sm text-gray-600 mt-4">
                    Already have an account?{' '}
                    <Link to="/login" className="text-blue-600 hover:underline">
                        Login
                    </Link>
                </p>
            </div>
        </div>
    );
}