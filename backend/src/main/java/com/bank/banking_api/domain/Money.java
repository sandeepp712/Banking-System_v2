package com.bank.banking_api.domain;

import com.bank.banking_api.exception.CurrencyMismatchException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable Value Object representing a monetary amount.
 * WHY IMMUTABLE?
 * 1. Thread-safe: Multiple threads can read the same Money object without locks.
 * 2. No side effects: Passing Money to a method guarantees it won't be changed.
 * 3. HashCode stability: Can be safely used as a HashMap key.
 */

public final class Money{

    // fields are private and final
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount.setScale(2, RoundingMode.HALF_UP));
        this.currency = Objects.requireNonNull(currency);
    }


    /**
     * Factory method for convenience.
     * Note: We avoid double in the public API to prevent floating-point errors.
     */
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }


    public Money add(Money money) {
        checkCurrency(money);
        return new Money(amount.add(money.amount), currency);
    }

    public Money subtract(Money money) {
        checkCurrency(money);
        return new Money(amount.subtract(money.amount), currency);
    }

    public Money multiply(BigDecimal multiplier) {
        if(multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Multiplier cannot be null or zero");
        }
        return new Money(amount.multiply(multiplier), currency);
    }



    /**
     * Getters (return immutable types,so safe to expose
     */
    public BigDecimal getAmount(){
        return this.amount;
    }

    public Currency getCurrency(){
        return this.currency;
    }



    /**
    Helper methods
    */
    public void checkCurrency(Money money) {
        if(!this.currency.equals(money.currency)) {
            throw new CurrencyMismatchException("Currency mismatch");
        }
    }

    public int compareTo(Money money) {
        return this.amount.compareTo(money.amount);
    }

    public boolean isNegative(){
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isPositive(){
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero(){
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThan(Money money) {
        return this.amount.compareTo(money.amount) > 0;
    }

    public boolean isLessThan(Money money) {
        return this.amount.compareTo(money.amount) < 0;
    }


    /**
     * Standard object Methods
     */
    //Standard Object Methods
    @Override
    public String toString(){
        return currency.getSymbol()+" "+amount.toPlainString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public boolean equals(Object o){
        if(this==o) return true;
        if(!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0 && Objects.equals(currency, money.currency);
    }
}