import axios from "axios";

// ==========================================
// 1. AUTH CLIENT (For Login, Register, Logout)
// ==========================================
export const authApiClient = axios.create({
    baseURL: "http://localhost:8080/api/v1/auth/",
    headers: {'Content-Type': 'application/json'},
    withCredentials: true,
});

// ==========================================
// 2. BANKING CLIENT (For Accounts, Transfers)
// ==========================================
export const bankingApiClient = axios.create({
    baseURL: "http://localhost:8080/api/v1/",
    headers: {'Content-Type': 'application/json'},
    withCredentials: true,
});

// Single flight silent refresh logic
let isRefreshing = false;
let failedQueue: Array<{
    resolve: (value?: unknown) => void;
    reject: (value?: unknown) => void;
}> = [];

const processQueue = (error:any)=>{
    failedQueue.forEach(item=>{
        if(error){
            item.reject(error);
        }else{
            item.resolve();
        }
    });
    failedQueue =[];
};

const creatSilentRefreshInterceptor = (client:any)=>{
    client.interceptors.request.use(
        (response:any)=> response,
        async (error:any)=>{
            const originalRequest = error.config;

            // if 401 and we haven't retried this specific request yet
            if(error.response?.status===401 && !originalRequest._retry){

                //if the refresh or login endpoint itself fails, force logout
                if(originalRequest.url?.includes('/refresh') || originalRequest.url?.includes('/login')){
                    window.location.href="/login";
                    return Promise.reject(error);
                }

                if(isRefreshing){
                    return new Promise((resolve,reject)=>{
                        failedQueue.push({resolve,reject});
                    }).then(()=>{
                        return client(originalRequest);
                    }).catch(error=>{Promise.reject(error)});
                }

                originalRequest._retry = true;
                isRefreshing = true;

                try{
                    await authApiClient.post("/refresh");

                    processQueue(null);

                    return client(originalRequest);
                }catch (refreshError){
                    processQueue(refreshError);
                    window.location.href="/login";
                    return Promise.reject(refreshError);
                }finally{
                    isRefreshing = false;
                }
            }
            return Promise.reject(error);
        }
    );
};

creatSilentRefreshInterceptor(authApiClient);
creatSilentRefreshInterceptor(bankingApiClient);