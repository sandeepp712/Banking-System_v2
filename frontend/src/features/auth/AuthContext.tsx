import {createContext, useEffect, useState, ReactNode, useContext} from "react";
import {authApiClient} from "../../api/client.ts";

interface User {
    id: string;
    username: string;
    role: string;
}

interface AuthContext {
    user: User | null;
    login: (username: string, password: string) => Promise<void>;
    register: (username: string, password: string, role: string) => Promise<void>;
    logout: () => Promise<void>;
    isLoading: boolean;
}

const AuthContext = createContext<AuthContext | undefined>(undefined);

export const AuthProvider = ({children}: { children: ReactNode }) => {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);

    //Login
    const login = async (username: string, password: string) => {
        setIsLoading(true);
        try {
            // The backend set up the Httponly cookie automatically
            await authApiClient.post('/login', {username: username, password: password});

            //Since we can't read the cookie, we need a separate endpoint to fetch the user
            const userResponse = await authApiClient.get('/users/me');
            setUser(userResponse.data);
        } catch (error: any) {
            const message = error.response?.data?.message || "Login failed";
            throw new Error(message);
        } finally {
            setIsLoading(false);
        }
    };


    const register = async (username: string, password: string, role: string) => {
        setIsLoading(true);
        try {
            await authApiClient.post('/register', {username: username, password: password, role: role});
        } catch (error: any) {
            const message = error.response?.data?.message || "Register failed";
            throw new Error(message);
        } finally {
            setIsLoading(false);
        }
    };


    //Logout
    const logout = async () => {
        setIsLoading(true);
        try {
            await authApiClient.post('/logout');
        } catch (error: any) {
            const message = error.response?.data?.message || "Logout failed";
        } finally {
            setUser(null);
            setIsLoading(false);
        }
    };


    return (
        <AuthContext.Provider value={{user, login, register, logout, isLoading}}>
            {children}
        </AuthContext.Provider>
    );
};


export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) throw new Error("useAuth must be used within AuthProvider");
    return context;
}