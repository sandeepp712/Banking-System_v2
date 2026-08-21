// Add this helper function at the top of your file or in a utils folder
export const generateIdempotencyKey = (): string => {
    // 1. Try the modern secure way first (works on localhost or HTTPS)
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        return crypto.randomUUID();
    }

    // 2. Fallback for plain HTTP environments (like your EC2 IP)
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
    });
};