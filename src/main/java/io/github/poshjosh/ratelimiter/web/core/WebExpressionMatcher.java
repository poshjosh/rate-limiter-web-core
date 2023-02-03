package io.github.poshjosh.ratelimiter.web.core;

import io.github.poshjosh.ratelimiter.expression.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class WebExpressionMatcher<R>
        implements ExpressionMatcher<R, Object>, WebExpressionKey,
        ExpressionParser<R, Object>, ExpressionResolver<Object>{

    public static WebExpressionMatcher<HttpServletRequest> ofHttpServletRequest() {
        return new WebExpressionMatcher<HttpServletRequest>() {
            @Override protected RequestInfo info(HttpServletRequest request) {
                return RequestInfo.of(request);
            }
        };
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebExpressionMatcher.class);

    interface Transformer<T>{
        T [] transform(String name, String [] toTransform, List<T> fromRequest);
    }

    private static class StringToLocaleTransformer implements Transformer<Locale>{
        @Override
        public Locale[] transform(String name, String[] toTransform, List<Locale> fromRequest) {
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
        @Override public String[] transform(String name, String[] toTransform, List<String> fromRequest) {
            return toTransform;
        }
    }

    private static final class Composite{
        private final String raw;
        private final Object [] values;
        private final io.github.poshjosh.ratelimiter.Operator operator;
        private Composite(String raw, String operatorSymbol, Object[] values) {
            this.raw = Objects.requireNonNull(raw);
            this.values = Objects.requireNonNull(values);
            this.operator = io.github.poshjosh.ratelimiter.Operator.ofSymbol(operatorSymbol);
        }
        @Override public String toString() {
            return raw;
        }
    }

    protected abstract RequestInfo info(R request);

    private final ExpressionMatcher<R, Object> delegate;

    private final Transformer<Locale> stringToLocaleConverter;
    private final Transformer<String> noopConverter;

    protected WebExpressionMatcher() {
        delegate = ExpressionMatcher.of(this, this, ATTRIBUTE + "=0");
        stringToLocaleConverter = new StringToLocaleTransformer();
        noopConverter = new NoopTransformer();
    }

    @Override
    public String matchOrNull(R request) {
        return delegate.matchOrNull(request);
    }

    @Override
    public ExpressionMatcher<R, Object> with(String expression) {
        return delegate.with(expression);
    }

    @Override
    public ExpressionMatcher<R, Object> with(Expression<String> expression) {
        return delegate.with(expression);
    }

    @Override
    public boolean isSupported(String expression) {
        return delegate.isSupported(expression);
    }

    @Override
    public boolean isSupported(Expression<String> expression) {
        if (!isSupported(expression.getOperator())) {
            return false;
        }
        if (!WebExpressionKey.isKey(expression.getLeft())){
            return false;
        }
        validate(expression);
        return true;
    }

    private void validate(Expression<String> expression) {
        final Type type = getType(expression);
        final String rhs = expression.getRight();
        if (!Type.OBJ_RHS.equals(type)) {
            if(Type.NON_OBJ_RHS__PAIR_TYPE.equals(type) && rhs.isEmpty()) {
                throw Checks.notSupported(this, expression);
            }
            return;
        }
        if(Expression.ofLenient(rhs).getLeft().isEmpty()) {
            throw Checks.notSupported(this, rhs);
        }
        if(rhs.startsWith("{") && rhs.endsWith("}")) {
            return;
        }
        throw Checks.notSupported(this, rhs);
    }

    private enum Type{OBJ_RHS, NON_OBJ_RHS, NON_OBJ_RHS__PAIR_TYPE}

    private Type getType(Expression<String> expression) {
        if (expression.getRight().contains("=")) {
            return Type.OBJ_RHS;
        }
        return WebExpressionKey.isNameValueType(expression.getLeft())
                ? Type.NON_OBJ_RHS__PAIR_TYPE : Type.NON_OBJ_RHS;
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
     * web.session.user.role=GUEST                  means: match if the user role is GUEST
     * We resolve the above as follows:
     *  - If the user is in role GUEST: GUEST=GUEST
     *  - If the user is not in role GUEST: GUEST=''
     * </pre>
     *
     * @param request The web request
     * @param expression The expression to be parsed
     * @return A parsed version of the expression
     */
    @Override
    public Expression<Object> parse(R request, Expression<String> expression) {
        if (!isSupported(expression)) {
            throw Checks.notSupported(this, expression);
        }
        final Type type = getType(expression);
        final String key = expression.getLeft();
        final String name;
        final Expression<Object> result;
        if (Type.OBJ_RHS.equals(type)) {
            final Expression<String> rhs = Expression
                    .ofLenient(withoutObjectBrackets(expression.getRight()));
            name = requireName(rhs.getLeft(), rhs);
            final Object fromRequest = getValue(request, key, name);
            final Object input;
            if (COOKIE.equals(key)) {
                input = hasValue(fromRequest, key, name) ? fromRequest : "";
            } else {
                input = splitIntoArrayIfNeed(
                        rhs.getLeft(), rhs.getRight(), fromRequest, getTransformer(key));
            }
            result = expression.with(input, fromRequest);
        } else {
            if(Type.NON_OBJ_RHS__PAIR_TYPE.equals(type)) {
                name = requireName(expression.getRight(), expression);
                final Object fromRequest = getValue(request, key, name);
                final boolean hasValue = hasValue(fromRequest, key, name);
                if (COOKIE.equals(key)) {
                    final Object input = hasValue ? fromRequest : "";
                    result = expression.with(input, fromRequest);
                } else {
                    if (!hasValue) {
                        result = Expression.FALSE;
                    } else {
                        // web.request.header=Content-Type  means: If the content type has a value
                        // We resolve the above to expression: Content-Type!=''  (i.e not equals)
                        //
                        final Object input = splitIntoArrayIfNeed(name, fromRequest, getTransformer(key));
                        result = expression.with(input, fromRequest).flipOperator();
                    }
                }
            } else {
                name = "";
                final Object fromRequest = getValue(request, key, name);
                final Object input = splitIntoArrayIfNeed(
                        "", expression.getRight(), fromRequest, getTransformer(key));
                result = expression.with(input, fromRequest);
            }
        }
        LOG.debug("Type: {}, key: {}, name: {}, output: {}, input: {}",
                type, key, name, result, expression);
        return result;
    }

    private boolean hasValue(Object fromRequest, String key, String name) {
        switch(key) {
            case ATTRIBUTE:
                return fromRequest != null;
            case AUTH_SCHEME:
                return fromRequest != null && !fromRequest.toString().isEmpty();
            case COOKIE:
                RequestInfo.Cookie cookie = (RequestInfo.Cookie)fromRequest;
                return !cookie.name().isEmpty() && !cookie.value().isEmpty();
            case HEADER:
            case PARAMETER:
                return !((List)fromRequest).isEmpty();
            case USER_ROLE:
                return !"".equals(fromRequest);
            default : throw new AssertionError();
        }
    }

    private String requireName(String name, Object unsupported) {
        if (name.isEmpty()) {
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

    private String withoutObjectBrackets(String value) {
        return without(value, "{", "}");
    }

    private Object splitIntoArrayIfNeed(String name, String input, Object fromRequest, Transformer<?> transformer) {
        if (!hasArrayBrackets(input)) {
            return input;
        }
        final String value = withoutArrayBrackets(input);
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
        return splitIntoArrayIfNeed(name, input, arraySeparator, parts, fromRequest, transformer);
    }

    private Object splitIntoArrayIfNeed(String name, Object fromRequest, Transformer<?> transformer) {
        return splitIntoArrayIfNeed(name, "", "&", new String[0], fromRequest, transformer);
    }

    private Object splitIntoArrayIfNeed(
            String name, String input, String arraySeparator, String [] parts, Object fromRequest, Transformer<?> transformer) {
        final Object [] transformed = transformer.transform(name, parts, toList(fromRequest));
        if (transformed.length == 0) {
            return "";
        }
        if (transformed.length == 1) {
            return transformed[0];
        }
        return new Composite(input, arraySeparator, transformed);
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
        if (value.isEmpty()) {
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

    @Override
    public boolean resolve(Expression<Object> expression) {
        final Object input = expression.getLeft();
        final Object fromRequest = expression.getRight();
        final boolean result = resolve(input, fromRequest);
        return expression.getOperator().isNegation() != result;
    }

    private boolean resolve(Object input, Object fromRequest) {
        fromRequest = convertArrayToListIfNeed(fromRequest); // Support for arrays
        if (input instanceof Composite) {
            Composite composite = (Composite)input;
            Object [] inputArr = composite.values;
            if (fromRequest instanceof List) {
                List fromReqList = (List)fromRequest;
                return MatchUtils.matches(composite.operator, fromReqList, inputArr);
            }
            return MatchUtils.matches(composite.operator, fromRequest, inputArr);
        }
        if (fromRequest instanceof List) {
            List fromReqList = (List)fromRequest;
            return fromReqList.size() == 1 ? Objects.equals(fromReqList.get(0), input) : false;
        }
        return Objects.equals(fromRequest, input);
    }

    private Object convertArrayToListIfNeed(Object fromRequest) {
        return fromRequest instanceof Object[] ? Arrays.asList((Object[])fromRequest) : fromRequest;
    }

    private Object getValue(R request, String type, String name) {
        switch(type) {
            case ATTRIBUTE: return info(request).getAttribute(name);
            case AUTH_SCHEME: return info(request).getAuthScheme();
            case COOKIE: return info(request).getCookies().stream()
                    .filter(c -> name.equals(c.name()))
                    .findAny().orElse(RequestInfo.Cookie.of("", ""));
            case HEADER: return info(request).getHeaders(name);
            case PARAMETER: return info(request).getParameters(name);
            case REMOTE_ADDRESS: return info(request).getRemoteAddr();
            case LOCALE: return info(request).getLocales();
            case USER_ROLE: return info(request).isUserInRole(name) ? name : "";
            case USER_PRINCIPAL: return info(request).getUserPrincipal().getName();
            case REQUEST_URI: return info(request).getRequestUri();
            case SESSION_ID: return info(request).getSessionId();
            default: throw Checks.notSupported(this, type);
        }
    }

    @Override
    public boolean isSupported(Operator operator) {
        return Operator.EQUALS.equals(operator.positive());
    }
}
