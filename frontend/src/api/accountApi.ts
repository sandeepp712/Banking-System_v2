import {bankingApiClient} from "./client.ts";

export interface Account {
    accountNumber: string;
    balance: number;
    currency: string;
}

export interface Transaction {
    transactionId: string;
    fromAccount: string;
    toAccount: string;
    amount: { amount: number, currency: string };
    status: string;
    createdAt: string;
    updatedAt: string | null;
}

export interface CreateAccountRequest {
    accountNumber: string;
    amount: number;
}


export const fetchAccount = async (): Promise<Account[]> => {
    const response = await bankingApiClient.get('accounts/account');
    return response.data;
}

export const fetchTransaction = async (): Promise<Transaction[]> => {
    const response = await bankingApiClient.get('/transactions');
    return response.data;
}

export const createAccount = async (data: CreateAccountRequest): Promise<Account> => {
    const response = await bankingApiClient.post("/accounts", data);
    return response.data;
};

// src/api/accountApi.ts
export const fetchAllAccounts = async (): Promise<Account[]> => {
    const response = await bankingApiClient.get("/accounts/all");
    return response.data;
};


// src/api/accountApi.ts
export const deposit = async (
    accountNumber: string,
    amount: number,
    idempotencyKey: string
): Promise<Transaction> => {
    const response = await bankingApiClient.post(`/accounts/${accountNumber}/deposit`, {
        amount,
        idempotencyKey,
    });
    return response.data;
};

export const withdraw = async (
    accountNumber: string,
    amount: number,
    idempotencyKey: string
): Promise<Transaction> => {
    const response = await bankingApiClient.post(`/accounts/${accountNumber}/withdraw`, {
        amount,
        idempotencyKey,
    });
    return response.data;
};