package by.vsu.enums;

import by.vsu.records.Money;

public enum Converter {
    TO_DOLLARS {
        @Override
        public Money convert(Money money) {
            return money.currency().equals(Currency.USD) ? money : new Money(money.amount() / exRate, Currency.USD);
        }
    },
    TO_RUBLES {
        @Override
        public Money convert(Money money) {
            return money.currency().equals(Currency.RUB) ? money : new Money(money.amount() * exRate, Currency.RUB);
        }
    };

    private static final double exRate = 76d;

    public abstract Money convert(Money money);
}
