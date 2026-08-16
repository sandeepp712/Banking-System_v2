import axios from "axios";

// ==========================================
// 1. AUTH CLIENT (For Login, Register, Logout)
// ==========================================
export const authApiClient = axios.create({
    baseURL: "http://localhost:8080/api/v1/auth/",
    headers: { 'Content-Type': 'application/json' },
    withCredentials: true,
});

// ==========================================
// 2. BANKING CLIENT (For Accounts, Transfers)
// ==========================================
export const bankingApiClient = axios.create({
    baseURL: "http://localhost:8080/api/v1/",
    headers: { 'Content-Type': 'application/json' },
    withCredentials: true,
});

// Response interceptor: Redirect to login on 401
const responseInterceptor = (error: any) => {
    if (error.response?.status === 401) {
        window.location.href = "/login";
    }
    return Promise.reject(error);
};

authApiClient.interceptors.response.use((res) => res, responseInterceptor);
bankingApiClient.interceptors.response.use((res) => res, responseInterceptor);