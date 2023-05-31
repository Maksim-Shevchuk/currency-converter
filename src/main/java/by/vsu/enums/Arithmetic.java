package by.vsu.enums;

import by.vsu.records.Money;

import java.util.InputMismatchException;

public enum Arithmetic {
    PLUS {
        @Override
        public Money action(Money a, Money b) throws InputMismatchException {
            if (!a.currency().equals(b.currency())) {
                throw new InputMismatchException();
            }
            return new Money(a.amount() + b.amount(), a.currency());
        }
    },
    MINUS {
        @Override
        public Money action(Money a, Money b) throws InputMismatchException {
            if (!a.currency().equals(b.currency())) {
                throw new InputMismatchException();
            }
            return new Money(b.amount() - a.amount(), a.currency());
        }
    };

    public abstract Money action(Money a, Money b) throws InputMismatchException;
}
