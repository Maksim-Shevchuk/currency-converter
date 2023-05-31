package by.vsu.logic;

import by.vsu.enums.Bracket;
import by.vsu.enums.Converter;
import by.vsu.enums.Currency;
import by.vsu.exceptions.EmptyExpressionException;
import by.vsu.records.Money;
import by.vsu.enums.Arithmetic;

import java.util.InputMismatchException;
import java.util.Objects;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputParser {
    private String expression = null;

    // Стек операций
    private final Stack<Object> operationStack = new Stack<>();
    // Стек с денежными значениями
    private final Stack<Money> moneyStack = new Stack<>();

    // Чтение вводных данных
    public String read() throws InputMismatchException {
        Scanner scanner = new Scanner(System.in);

        // Замена нежелательных символов, которые могут помешать запарсить значения
        String expression = scanner.nextLine()
                .replace(" ", "")
                .replace(",", ".")
                .replace("р", "p");

        // Проверка правильности введенных данных через регулярное выражение
        // Данное выражение позволяет вводить строки, содержащие из текста только toRubles и toDollars, а также скобки и непосредственно денежные значения
        // С большего фильтрует введенное выражение, но полностью не спасает от неверного ввода, в таких случаях выбрасываются исключения
        Matcher matcher = Pattern.compile("^(((toDollars|toRubles)\\()*?\\$?\\d+(\\.\\d{1,2})?p?[-+)]*)+").matcher(expression);
        if (!matcher.matches()) {
            throw new InputMismatchException();
        }

        this.expression = expression;
        return expression;
    }

    // Метод, который парсит и рассчитывает всё веденное пользователем
    public String calculate() throws EmptyExpressionException {
        // Если выражение пустое, вызвать исключение
        if (expression.isBlank()) {
            throw new EmptyExpressionException();
        }

        // На всякий случай очистить стеки, если в них что-то осталось
        operationStack.clear();
        moneyStack.clear();

        // Пока не закончится выражения и стек операций не будет пустой
        while (!expression.isEmpty() || !operationStack.isEmpty()) {
            // В случае если парсить уже нечего, то отработать по оставшимся операциям
            Object parseResult = expression.isEmpty() ? operationStack.peek() : parseNext();

            // Если вернувшийся объект является арифметическим оператором
            if (parseResult instanceof Arithmetic) {
                // Пока стек операций не будет пустым, либо последний элемент в стеке не будет арифметической операцией, выполнять расчёт
                while (!operationStack.isEmpty() && operationStack.peek() instanceof Arithmetic) {
                    Arithmetic operator = (Arithmetic) operationStack.pop();
                    moneyStack.push(operator.action(moneyStack.pop(), moneyStack.pop()));
                }
                // После выполнения предыдущего условия положить операцию в стек, либо если выражение уже пустое, то пропустить этот шаг
                if (!expression.isEmpty()) {
                    operationStack.push(parseResult);
                }
            }

            // Если вернувшийся объект является скобкой
            else if (parseResult instanceof Bracket) {
                // Если скобка открывающая, то просто положить в стек
                if (Objects.equals(parseResult, Bracket.OPEN)) {
                    operationStack.push(parseResult);
                } else if (Objects.equals(parseResult, Bracket.CLOSE) && operationStack.contains(Bracket.OPEN)) {
                    // Если же скобка закрывающая, то выполнить все промежуточные операции, пока не доберёмся до открывающей, чтобы они "ликвидировали" друг друга
                    while (!operationStack.peek().equals(Bracket.OPEN)) {
                        Arithmetic operator = (Arithmetic) operationStack.pop();
                        moneyStack.push(operator.action(moneyStack.pop(), moneyStack.pop()));
                    }
                    operationStack.pop();

                    // Если перед открывающей скобкой стоял конвертер, то выполнить конвертацию
                    if (!operationStack.isEmpty() && operationStack.peek() instanceof Converter) {
                        Converter converter = (Converter) operationStack.pop();
                        moneyStack.push(converter.convert(moneyStack.pop()));
                    }
                } else {
                    throw new InputMismatchException();
                }
            }

            // Если вернувшийся объект является конвертером, просто положить его в стек
            else if (parseResult instanceof Converter) {
                operationStack.push(parseResult);
            }

            // Если вернувшийся объект является деньгами, точно так же положить в стек
            else if (parseResult instanceof Money) {
                moneyStack.push((Money) parseResult);
            }
        }
        // Возврат результата в формате строки, округлённой до сотых
        Money result = moneyStack.pop();
        return result.toString();
    }

    // Парсит новую операцию, в случае неверной записи выдаёт исключение
    public Object parseNext() {

        // Определение конвертаций
        if (expression.startsWith("toRubles")) {
            this.expression = expression.replaceFirst("toRubles", "");
            return Converter.TO_RUBLES;
        }
        if (expression.startsWith("toDollars")) {
            this.expression = expression.replaceFirst("toDollars", "");
            return Converter.TO_DOLLARS;
        }

        // Определение скобок
        if (expression.startsWith("(")) {
            this.expression = expression.replaceFirst("\\(", "");
            return Bracket.OPEN;
        }
        if (expression.startsWith(")")) {
            this.expression = expression.replaceFirst("\\)", "");
            return Bracket.CLOSE;
        }

        // Определение арифметических операций
        if (expression.startsWith("+")) {
            this.expression = expression.replaceFirst("\\+", "");
            return Arithmetic.PLUS;
        }
        if (expression.startsWith("-")) {
            this.expression = expression.replaceFirst("-", "");
            return Arithmetic.MINUS;
        }

        // Определение валют
        Matcher rubleMatcher = Pattern.compile("^\\d+(\\.\\d{1,2})?p").matcher(expression);
        if (rubleMatcher.find()) {
            double amount = Double.parseDouble(rubleMatcher.group().replaceAll("p", ""));
            this.expression = expression.replaceFirst(rubleMatcher.pattern().pattern(), "");
            return new Money(amount, Currency.RUB);
        }
        Matcher dollarMatcher = Pattern.compile("^\\$\\d+(\\.\\d{1,2})?").matcher(expression);
        if (dollarMatcher.find()) {
            double amount = Double.parseDouble(dollarMatcher.group().replaceAll("\\$", ""));
            this.expression = expression.replaceFirst(dollarMatcher.pattern().pattern(), "");
            return new Money(amount, Currency.USD);
        } else {
            throw new InputMismatchException();
        }
    }
}