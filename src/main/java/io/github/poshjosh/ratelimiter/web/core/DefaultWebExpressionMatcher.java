package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.expression.*;
import io.github.poshjosh.ratelimiter.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class DefaultWebExpressionMatcher implements
        WebExpressionMatcher,
        ExpressionParser<HttpServletRequest, Object>,
        ExpressionResolver<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultWebExpressionMatcher.class);

    private enum Type{OBJ_RHS, NON_OBJ_RHS, NON_OBJ_RHS__PAIR_TYPE}
    private final ExpressionMatcher<HttpServletRequest, Object> delegate;

    private final Transformer<Locale> stringToLocaleConverter;
    private final Transformer<String> noopConverter;

    DefaultWebExpressionMatcher() {
        // With parseAtMatchTime we don't have to implement parseLeft(..) or parseRight(..)
        delegate = ExpressionMatcher.ofParseAtMatchTime(
                this, this, Expression.of(ATTRIBUTE + "=0"));
        stringToLocaleConverter = new StringToLocaleTransformer();
        noopConverter = new NoopTransformer();
    }

    @Override
    public Object parseLeft(HttpServletRequest context, Expression<String> expression) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Object parseRight(Expression<String> expression) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public String match(HttpServletRequest request) {
        return delegate.match(request);
    }

    @Override
    public ExpressionMatcher<HttpServletRequest, Object> matcher(Expression<String> expression) {
        return delegate.matcher(expression);
    }

    @Override
    public boolean isSupported(Expression<String> expression) {
        if (!isSupported(expression.getOperator())) {
            return false;
        }
        if (!WebExpressionKey.isKey(expression.requireLeft())){
            return false;
        }
        validate(expression);
        return true;
    }

    @Override
    public boolean isSupported(Operator operator) {
        return operator.equalsIgnoreNegation(Operator.EQUALS);
    }

    @Override
    public boolean resolve(Object left, Operator operator, Object right) {
        final boolean result = resolve(left, right);
        return operator.isNegation() != result;
    }

    /**
     * Parse an expression into a state that is easily resolved to true or false.
     *
     * Examples:
     *
     * <pre>
     * web.request.header={Content-Type=text/plain} means: match if Content-Type = text/plain
     * We resolve the above to expression: text/plain=[CONTENT_TYPE_OF_REQUEST]
     *
     * web.request.header=Content-Type              means: match if Content-Type has a value
     * We resolve the above to expression: Content-Type!=''  (i.e not equals)
     *
     * web.request.locale=[en_US|en_UK]             means: match either locales
     * We resolve the above to expression: [en_US|en_UK]=[LOCALES_FROM_REQUEST]
     *
     * web.request.user.role=GUEST                  means: match if the user role is GUEST
     * We resolve the above as follows:
     *  - If the user is in role GUEST: GUEST=GUEST
     *  - If the user is not in role GUEST: GUEST=''
     * </pre>
     *
     * @param context The web request
     * @param expression The expression to be parsed
     * @return A parsed version of the expression
     */
    @Override
    public Expression<Object> parse(HttpServletRequest context, Expression<String> expression) {
        if (!isSupported(expression)) {
            throw Checks.notSupported(this, expression);
        }
        final Type type = getType(expression);
        final String key = expression.requireLeft();
        final String name;
        final Expression<Object> result;
        if (Type.OBJ_RHS.equals(type)) {
            final Expression<String> rhs = expression.requireRightAsExpression();
            name = requireName(rhs.requireLeft(), rhs);
            final Object fromWebRequest = getValue(context, key, name);
            final Object fromExpression;
            if (COOKIE.equals(key)) {
                fromExpression = hasValue(fromWebRequest, key) ? fromWebRequest : "";
            } else {
                fromExpression = splitIntoArrayIfNeed(
                        rhs.requireLeft(), rhs.getRightOrDefault(null), 
                        fromWebRequest, getTransformer(key));
            }
            result = expression.with(fromWebRequest, fromExpression);
        } else {
            if(Type.NON_OBJ_RHS__PAIR_TYPE.equals(type)) {
                name = requireName(expression.getRightOrDefault(null), expression);
                final Object fromWebRequest = getValue(context, key, name);
                final boolean hasValue = hasValue(fromWebRequest, key);
                if (COOKIE.equals(key)) {
                    final Object fromExpression = hasValue ? fromWebRequest : "";
                    result = expression.with(fromWebRequest, fromExpression);
                } else {
                    if (!hasValue) {
                        result = Expression.FALSE;
                    } else {
                        // web.request.header=Content-Type  means: If the content type has a value
                        // We resolve the above to expression: Content-Type!=''  (i.e not equals)
                        //
                        final Object fromExpression = splitIntoArrayIfNeed(
                                name, fromWebRequest, getTransformer(key));
                        result = expression.with(fromWebRequest, fromExpression).flipOperator();
                    }
                }
            } else {
                name = "";
                final Object fromWebRequest = getValue(context, key, name);
                final Object fromExpression = splitIntoArrayIfNeed(
                        "", expression.getRightOrDefault(null), 
                        fromWebRequest, getTransformer(key));
                result = expression.with(fromWebRequest, fromExpression);
            }
        }
        LOG.trace("Type: {}, key: {}, name: {}, output: {}, input: {}",
                type, key, name, result, expression);
        return result;
    }

    private boolean resolve(Object fromWebRequest, Object fromExpression) {
        fromWebRequest = convertArrayToListIfNeed(fromWebRequest); // Support for arrays
        if (fromExpression instanceof Composite) {
            Composite composite = (Composite)fromExpression;
            Object [] inputArr = composite.values;
            if (fromWebRequest instanceof List) {
                List fromReqList = (List)fromWebRequest;
                return MatchUtils.matches(composite.operator, fromReqList, inputArr);
            }
            return MatchUtils.matches(composite.operator, fromWebRequest, inputArr);
        }
        if (fromWebRequest instanceof List) {
            List fromReqList = (List)fromWebRequest;
            return fromReqList.size() == 1 ? Objects.equals(fromReqList.get(0), fromExpression) : false;
        }
        return Objects.equals(fromWebRequest, fromExpression);
    }

    private void validate(Expression<String> expression) {
        final Type type = getType(expression);
        final String rhs = expression.getRightOrDefault(null);
        if (!Type.OBJ_RHS.equals(type)) {
            if(Type.NON_OBJ_RHS__PAIR_TYPE.equals(type) && (!StringUtils.hasText(rhs))) {
                throw Checks.notSupported(this, expression);
            }
            return;
        }
        if(!StringUtils.hasText(Expression.of(rhs).requireLeft())) {
            throw Checks.notSupported(this, rhs);
        }
        if(rhs.startsWith("{") && rhs.endsWith("}")) {
            return;
        }
        throw Checks.notSupported(this, rhs);
    }

    private Type getType(Expression<String> expression) {
        if(isRightAnExpression(expression)) {
            return Type.OBJ_RHS;
        }
        return WebExpressionKey.isNameValueType(expression.requireLeft())
                ? Type.NON_OBJ_RHS__PAIR_TYPE : Type.NON_OBJ_RHS;
    }

    private boolean hasValue(Object fromWebRequest, String key) {
        switch(key) {
            case ATTRIBUTE:
                return fromWebRequest != null;
            case AUTH_SCHEME:
            case USER_ROLE:
                return fromWebRequest != null && StringUtils.hasText(fromWebRequest.toString());
            case COOKIE:
                RequestInfo.Cookie cookie = (RequestInfo.Cookie)fromWebRequest;
                return StringUtils.hasText(cookie.name()) && StringUtils.hasText(cookie.value());
            case HEADER:
            case PARAMETER:
                return !((List)fromWebRequest).isEmpty();
            default : throw new AssertionError();
        }
    }

    private String requireName(String name, Object unsupported) {
        if (!StringUtils.hasText(name)) {
            throw Checks.notSupported(this, unsupported);
        }
        return name;
    }

    private Transformer<?> getTransformer(String key) {
        if (WebExpressionKey.LOCALE.equals(key)) {
            return stringToLocaleConverter;
        } else {
            return noopConverter;
        }
    }

    private Object splitIntoArrayIfNeed(String name, String fromExpression, Object fromWebRequest, Transformer<?> transformer) {
        if (fromExpression == null) {
            return fromExpression;
        }
        if (!hasArrayBrackets(fromExpression)) {
            return fromExpression;
        }
        final String value = withoutArrayBrackets(fromExpression);
        final String arraySeparator;
        if (value.contains("|")) {
            arraySeparator = "|";
        } else if (value.contains("&")) {
            arraySeparator = "&";
        } else {
            return value;
        }
        final String [] parts = Arrays
                .stream(value.split("[" + Pattern.quote(arraySeparator) + "]", 2))
                .map(String::trim)
                .toArray(String[]::new);
        return splitIntoArrayIfNeed(name, fromExpression, arraySeparator, parts, fromWebRequest, transformer);
    }

    private Object splitIntoArrayIfNeed(String name, Object fromWebRequest, Transformer<?> transformer) {
        return splitIntoArrayIfNeed(name, "", "&", new String[0], fromWebRequest, transformer);
    }

    private Object splitIntoArrayIfNeed(
            String name, String fromExpression, String arraySeparator, String [] parts, Object fromWebRequest, Transformer<?> transformer) {
        final Object [] transformed = transformer.transform(name, parts, toList(fromWebRequest));
        if (transformed.length == 0) {
            return "";
        }
        if (transformed.length == 1) {
            return transformed[0];
        }
        return new Composite(fromExpression, arraySeparator, transformed);
    }

    private List toList(Object val){
        return val instanceof List ? (List)val : val instanceof Object []
                ? Arrays.asList((Object[])val) : Collections.singletonList(val);
    }

    private boolean hasArrayBrackets(String value) {
        return value.startsWith("[") && value.endsWith("]");
    }

    private String withoutArrayBrackets(String value) {
        return without(value, "[", "]");
    }

    private String without(String value, String prefix, String suffix) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.startsWith(prefix)) {
            value = value.substring(prefix.length());
        }
        if (value.endsWith(suffix)) {
            value = value.substring(0, value.length() - suffix.length());
        }
        return value;
    }

    private Object convertArrayToListIfNeed(Object obj) {
        return obj instanceof Object[] ? Arrays.asList((Object[])obj) : obj;
    }

    private Object getValue(HttpServletRequest request, String type, String name) {
        switch(type) {
            case ATTRIBUTE: return getInfo(request).getAttribute(name);
            case AUTH_SCHEME: return getInfo(request).getAuthScheme();
            case COOKIE: return getInfo(request).getCookies().stream()
                    .filter(c -> name.equals(c.name()))
                    .findAny().orElse(RequestInfo.Cookie.of("", ""));
            case HEADER: return getInfo(request).getHeaders(name);
            case PARAMETER: return getInfo(request).getParameters(name);
            case IP:
            case REMOTE_ADDRESS: return getInfo(request).getRemoteAddr();
            case LOCALE: return getInfo(request).getLocales();
            case USER_ROLE: return getInfo(request).isUserInRole(name) ? name : "";
            case USER_PRINCIPAL: return getInfo(request).getUserPrincipal().getName();
            case REQUEST_URI: return getInfo(request).getRequestUri();
            case SESSION_ID: return getInfo(request).getSessionId();
            default: throw Checks.notSupported(this, type);
        }
    }

    private RequestInfo getInfo(HttpServletRequest request) {
        return RequestInfo.of(request);
    }

    @Override
    public String toString() {
        return "DefaultWebExpressionMatcher{delegate=" + delegate + '}';
    }

    private interface Transformer<T>{
        T [] transform(String name, String [] toTransform, List<T> fromWebRequest);
    }

    private static class StringToLocaleTransformer implements Transformer<Locale>{
        @Override
        public Locale[] transform(String name, String[] toTransform, List<Locale> fromWebRequest) {
            if (toTransform.length == 0) {
                return new Locale[0];
            }
            return Arrays.stream(toTransform).map(sval -> toLocale(sval))
                    .collect(Collectors.toList()).toArray(new Locale[0]);
        }
        private Locale toLocale(String value) {
            return Locale.forLanguageTag(value.replace('_', '-'));
        }
    }

    private static class NoopTransformer implements Transformer<String> {
        @Override public String[] transform(String name, String[] toTransform, List<String> fromWebRequest) {
            return toTransform;
        }
    }

    private static final class Composite{
        private final String raw;
        private final Object [] values;
        private final io.github.poshjosh.ratelimiter.util.Operator operator;
        private Composite(String raw, String operatorSymbol, Object[] values) {
            this.raw = Objects.requireNonNull(raw);
            this.values = Objects.requireNonNull(values);
            this.operator = io.github.poshjosh.ratelimiter.util.Operator.ofSymbol(operatorSymbol);
        }
        @Override public String toString() {
            return raw;
        }
    }
}
