package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.expression.*;
import io.github.poshjosh.ratelimiter.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Function;

final class WebExpressionMatcher
        implements ExpressionMatcher<HttpServletRequest>, WebExpressionKey,
        ExpressionParser<HttpServletRequest, Object>, ExpressionResolver<Object> {

    private static final Function<String, Object> TO_LOCALE = value ->
            Locale.forLanguageTag(value.replace('_', '-'));
    private static final Function<String, Object> IDENTITY = value -> value;

    private final ExpressionMatcher<HttpServletRequest> delegate;

    WebExpressionMatcher() {
        delegate = ExpressionMatchers.ofParseAhead(
                this, this, Expressions.of(ATTRIBUTE + " = 0"));
    }

    /**
     * Parse an expression into a state that is easily resolved to true or false.
     * Examples of input and output expressions:
     *
     * <pre>
     * web.request.header[Content-Type] = text/plain
     * [CONTENT_TYPE_OF_REQUEST]        = text/plain
     *
     * web.request.header[Content-Type] !=              (match if Content-Type has a value)
     * [CONTENT_TYPE_OF_REQUEST]        != ''
     *
     * web.request.locale = [en_US | en_UK]             (match either locales)
     * [en_US | en_UK] contains [LOCALE_FROM_REQUEST]
     *
     * web.request.user.role = GUEST                    (match if the user role is GUEST)
     * GUEST = GUEST  (if user is in role GUEST)
     * null  = GUEST  (if user is not in role GUEST)
     * </pre>
     *
     * @param request The web request
     * @param expression The expression to be parsed
     * @return A parsed version of the expression
     * @see #parseLeft(HttpServletRequest, Expression)
     * @see #parseRight(Expression)
     */
    @Override
    public Expression<Object> parse(
            HttpServletRequest request, Expression<String> expression) {
        return ExpressionParser.super.parse(request, expression);
    }

    @Override
    public String match(HttpServletRequest toMatch) {
        return delegate.match(toMatch);
    }

    @Override
    public ExpressionMatcher<HttpServletRequest> matcher(Expression<String> expression) {
        return delegate.matcher(expression);
    }

    @Override
    public boolean isSupported(Expression<String> expression) {
        if (!isSupported(expression.getOperator())) {
            return false;
        }
        return WebExpressionKey.isKey(expression.requireLeft());
    }

    @Override
    public boolean isSupported(Operator operator) {
        return operator.equalsIgnoreNegation(Operator.EQUALS);
    }

    @Override
    public Object parseLeft(HttpServletRequest request, Expression<String> expression) {
        final String left = expression.requireLeft();
        final String name = Expressions.getTextInSquareBracketsOrNull(left);
        final boolean keyValueType = WebExpressionKey.isKeyValueType(left);
        if (keyValueType && name == null) {
            throw Checks.notSupported(this, expression);
        }
        final String key = name != null ? left.substring(0, left.indexOf('[')) : left;
        switch(key) {
            case ATTRIBUTE: return request.getAttribute(name);
            case AUTH_SCHEME: return DefaultRequestInfo.authScheme(request, "");
            case COOKIE:
                return DefaultRequestInfo.cookies(request).stream()
                    .filter(c -> Objects.equals(name, c.getName()))
                    .map(Cookie::getValue)
                    .findAny().orElse(null);
            case HEADER: return DefaultRequestInfo.headers(request, name);
            case PARAMETER: return DefaultRequestInfo.parameters(request, name);
            case IP:
            case REMOTE_ADDRESS: return DefaultRequestInfo.remoteAddr(request, "");
            case LOCALE: return DefaultRequestInfo.locales(request);
            case USER_ROLE: return parseLeftForRole(request, expression);
            case USER_PRINCIPAL: return request.getUserPrincipal().getName();
            case REQUEST_URI: return request.getRequestURI();
            case SESSION_ID: return DefaultRequestInfo.sessionId(request, "");
            default: throw Checks.notSupported(this, key);
        }
    }

    @Override
    public Object parseRight(Expression<String> expression) {
        final String right = expression.getRightOrDefault(null);
        if (!StringUtils.hasText(right)) {
            return null;
        }
        final String left = expression.requireLeft();
        if (left.startsWith(LOCALE)) {
            return toCollection(right, TO_LOCALE);
        }
        if (right.indexOf('[') == 0) {
            return toCollection(right, IDENTITY);
        }
        return right;
    }

    @Override
    public boolean resolve(Object left, Operator operator, Object right) {
        final boolean result = resolve(left, right);
        return operator.isNegation() != result;
    }

    private Object parseLeftForRole(HttpServletRequest request, Expression<String> expression) {
        final Object right = parseRight(expression); // Oh no! we are repeating this
        if (right instanceof Composite) {
            final Composite composite = (Composite)right;
            if (composite.operator == io.github.poshjosh.ratelimiter.util.Operator.AND) {
                throw Checks.notSupported(this, composite.operator
                        + " for " + WebExpressionKey.USER_ROLE + " values");
            }
            final String [] roles = (String[])composite.values;
            return Arrays.stream(roles).anyMatch(request::isUserInRole) ? right : null;
        }
        final String role = right == null ? null : right.toString();
        return request.isUserInRole(role) ? right : null;
    }

    private boolean resolve(Object fromWebRequest, Object fromExpression) {
        if (fromExpression instanceof Composite) {
            final Composite composite = (Composite)fromExpression;
            final Object [] inputArr = composite.values;
            if (fromWebRequest instanceof List) {
                final List<Object> fromReqList = (List)fromWebRequest;
                return MatchUtils.matchesList(composite.operator, fromReqList, inputArr);
            }
            return MatchUtils.matchesValue(composite.operator, fromWebRequest, inputArr);
        }
        if (fromWebRequest instanceof List) {
            final List<?> fromReqList = (List<?>)fromWebRequest;
            return fromReqList.size() == 1 && Objects.equals(fromReqList.get(0), fromExpression);
        }
        return Objects.equals(fromWebRequest, fromExpression);
    }

    private Composite toCollection(String rhsText, Function<String, Object> mapper) {
        final boolean orList = isOrList(rhsText);
        final io.github.poshjosh.ratelimiter.util.Operator operator = orList ?
                io.github.poshjosh.ratelimiter.util.Operator.OR :
                io.github.poshjosh.ratelimiter.util.Operator.AND;
        final String arraySeparatorRegex = orList ? "\\|" : operator.getSymbol();
        final String [] parts = withoutBrackets(rhsText).split(arraySeparatorRegex);
        final Object [] array =  Arrays.stream(parts)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(mapper)
                .toArray();
        return new Composite(array, operator);
    }

    private boolean isOrList(String rhsText) {
        if (rhsText.contains(io.github.poshjosh.ratelimiter.util.Operator.OR.getSymbol())) {
            return true;
        }
        if (rhsText.contains(io.github.poshjosh.ratelimiter.util.Operator.AND.getSymbol())) {
            return false;
        }
        throw Checks.notSupported(this, "Right hand side text: " + rhsText);
    }

    private String withoutBrackets(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.charAt(0) == '[') {
            value = value.substring(1);
        }
        if (value.charAt(value.length() - 1) == ']') {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static final class Composite{
        private final Object [] values;
        private final io.github.poshjosh.ratelimiter.util.Operator operator;
        private Composite(Object[] values, io.github.poshjosh.ratelimiter.util.Operator operator) {
            this.values = Objects.requireNonNull(values);
            this.operator = Objects.requireNonNull(operator);
        }
        @Override public String toString() {
            return Arrays.toString(values);
        }
    }

    @Override
    public String toString() {
        return "WebExpressionMatcher{delegate=" + delegate + '}';
    }
}
