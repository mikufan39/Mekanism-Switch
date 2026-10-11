package com.mikufan.meks.flight.math;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ExpressionParser extends Parser {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    /** Variable names already reported by the unknown-variable warning, to warn only once each. */
    private static final Set<String> WARNED_VARIABLES = ConcurrentHashMap.newKeySet();

    /**
     * Safety limits for user-supplied config formulas. Exceeding either one raises a
     * {@link RuntimeException}, which {@link #getCompiled()} already converts into the
     * configured default expression instead of crashing the client (a deeply nested formula
     * would otherwise kill the render thread with an uncatchable-at-callsite StackOverflowError).
     */
    private static final int MAX_INPUT_LENGTH = 1024;
    private static final int MAX_PARSE_DEPTH = 256;

    private Expression compiled;
    private RuntimeException error;
    private int depth;

    public static Expression parse(String string) {
        return new ExpressionParser(string).build();
    }

    public ExpressionParser(String string) {
        super(string);
    }

    public Expression getCompiled() {
        if (compiled == null && error == null) {
            try {
                return build();
            } catch (RuntimeException e) {
                error = e;
                return null;
            }
        }
        return compiled;
    }

    public Expression getCompiledOrDefaulting(double defaultValue) {
        return hasError() ? vars -> defaultValue : getCompiled();
    }

    public RuntimeException getError() {
        getCompiled();
        return error;
    }

    public boolean hasError() {
        getCompiled();
        return error != null;
    }

    public Expression build() {
        if (string.length() > MAX_INPUT_LENGTH) {
            throw new RuntimeException("Expression too long: " + string.length() + " > " + MAX_INPUT_LENGTH + " characters");
        }
        nextChar();
        var x = parseExpression();
        if (pos < string.length()) throw new RuntimeException("Unexpected character '" + ch + "' at position " + pos);
        compiled = x;
        return x;
    }

    private void enterRecursion() {
        if (++depth > MAX_PARSE_DEPTH) {
            throw new RuntimeException("Expression nested too deeply (max " + MAX_PARSE_DEPTH + ")");
        }
    }

    // Grammar:
    // expression = term | expression `+` term | expression `-` term
    // term = factor | term `*` factor | term `/` factor
    // factor = `+` factor | `-` factor | `(` expression `)` | number
    //        | functionName `(` expression `)` | functionName factor
    //        | factor `^` factor
    private Expression parseExpression() {
        enterRecursion();
        try {
            var x = parseTerm();
            while (true) {
                if (weat('+')) { // addition
                    var a = x;
                    var b = parseTerm();
                    x = vars -> a.eval(vars) + b.eval(vars);
                } else if (weat('-')) { // subtraction
                    var a = x;
                    var b = parseTerm();
                    x = vars -> a.eval(vars) - b.eval(vars);
                } else return x;
            }
        } finally {
            depth--;
        }
    }

    private Expression parseTerm() {
        var x = parseFactor();
        while (true) {
            if (weat('*')) { // multiplication
                var a = x;
                var b = parseFactor();
                x = vars -> a.eval(vars) * b.eval(vars);
            } else if (weat('/')) { // division
                var a = x;
                var b = parseFactor();
                x = vars -> a.eval(vars) / b.eval(vars);
            } else return x;
        }
    }

    private Expression parseFactor() {
        enterRecursion();
        try {
            return parseFactorInternal();
        } finally {
            depth--;
        }
    }

    private Expression parseFactorInternal() {
        if (weat('+')) { // unary plus
            var a = parseFactor();
            return vars -> +a.eval(vars);
        }
        if (weat('-')) { // unary minus
            var a = parseFactor();
            return vars -> -a.eval(vars);
        }
        Expression x;
        var startPos = pos;
        if (weat('(')) { // parentheses
            x = parseExpression();
            if (!weat(')')) throw new RuntimeException("Missing ')' at position " + pos);
        } else if (ch >= '0' && ch <= '9' || ch == '.') { // number literals
            while (ch >= '0' && ch <= '9' || ch == '.') nextChar();
            var a = Double.parseDouble(string.substring(startPos, pos));
            x = vars -> a;
        } else if (isVariableChar()) {
            while (isVariableChar()) nextChar();
            var name = string.substring(startPos, pos);
            List<Expression> args = new ArrayList<>();
            if (weat('(')) { // functions
                do {
                    args.add(parseExpression());
                } while (weat(','));
                if (!weat(')')) throw new RuntimeException("Missing ')' after argument to '" + name + "'");
                x = switch (args.size()) {
                    case 1 -> {
                        var a = args.get(0);
                        yield switch (name) {
                            case "sqrt" -> vars -> Math.sqrt(a.eval(vars));
                            case "sin" -> vars -> Math.sin(a.eval(vars));
                            case "cos" -> vars -> Math.cos(a.eval(vars));
                            case "tan" -> vars -> Math.tan(a.eval(vars));
                            case "asin" -> vars -> Math.asin(a.eval(vars));
                            case "acos" -> vars -> Math.acos(a.eval(vars));
                            case "atan" -> vars -> Math.atan(a.eval(vars));
                            case "abs" -> vars -> Math.abs(a.eval(vars));
                            case "ceil" -> vars -> Math.ceil(a.eval(vars));
                            case "floor" -> vars -> Math.floor(a.eval(vars));
                            case "log" -> vars -> Math.log(a.eval(vars));
                            case "round" -> vars -> Math.round(a.eval(vars));
                            // Bound is clamped so a bad config (0/negative/NaN) degrades to a
                            // constant instead of throwing IllegalArgumentException mid-render.
                            case "randint" -> vars -> RANDOM.nextInt(Math.max(1, (int) a.eval(vars)));
                            default ->
                                    throw new RuntimeException("Unknown function '" + name + "' for 1 arg at position " + (pos - name.length()));
                        };
                    }
                    case 2 -> {
                        var a = args.get(0);
                        var b = args.get(1);
                        yield switch (name) {
                            case "min" -> vars -> Math.min(a.eval(vars), b.eval(vars));
                            case "max" -> vars -> Math.max(a.eval(vars), b.eval(vars));
                            case "randint" -> vars -> {
                                var av = a.eval(vars);
                                return av + RANDOM.nextInt(Math.max(1, (int) (b.eval(vars) - av)));
                            };
                            default ->
                                    throw new RuntimeException("Unknown function '" + name + "' for 2 args at position " + (pos - name.length()));
                        };
                    }
                    default ->
                            throw new RuntimeException("Unknown function '" + name + "' for " + args.size() + " args at position " + (pos - name.length()));
                };
            } else { // constants
                var a = switch (name) {
                    case "PI" -> Math.PI;
                    case "E" -> Math.E;
                    case "TO_RAD" -> Math.PI / 180;
                    case "TO_DEG" -> 180 / Math.PI;
                    default ->
                            throw new RuntimeException("Unknown constant '" + name + "' at position " + (pos - name.length()));
                };
                x = vars -> a;
            }
        } else if (weat('$')) {
            while (isVariableChar()) nextChar();
            var variable = string.substring(startPos + 1, pos);
            x = vars -> {
                var value = vars.get(variable);
                if (value == null && WARNED_VARIABLES.add(variable)) {
                    LOGGER.warn("Unknown variable '{}' in flight formula \"{}\", using 0", variable, string);
                }
                return value != null ? value : 0;
            };
        } else {
            throw new RuntimeException("Unexpected '" + ch + "' at position " + pos);
        }
        if (weat('^')) { // exponentiation
            var a = x;
            var b = parseFactor();
            x = vars -> Math.pow(a.eval(vars), b.eval(vars));
        }
        return x;
    }
}