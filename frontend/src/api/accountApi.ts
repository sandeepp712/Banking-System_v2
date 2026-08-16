import {bankingApiClient} from "./client.ts";

export interface Account {
    accountNumber: string;
    balance: number;
    currency: string;
}

export interface Transaction {
    transactionId: string;
    fromAccountId: string;
    toAccountId: string;
    amount: { amount: number, currency: string };
    status: string;
    createdAt: string;
    updatedAt: string | null;
}

export const fetchAccount = async (): Promise<Account[]> => {
    const response = await bankingApiClient.get('accounts/account');
    return response.data;
}

export const fetchTransaction = async (): Promise<Transaction[]> => {
    const response = await bankingApiClient.get('/transactions');
    return response.data;
}