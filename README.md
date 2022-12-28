# rate limiter web core

Light-weight rate limiting library for java web apps, based on
[rate-limiter](https://github.com/poshjosh/rate-limiter).

Please first read the [rate-limiter documentation](https://github.com/poshjosh/rate-limiter).

## Quick Start

__Annotate the resource you want to rate imit__

```java
import com.looseboxes.ratelimiter.annotations.RateLimit;

@Controller
@RequestMapping("/api")
class GreetingResource {

    // Only 99 calls to this path is allowed per minute
    @RateLimit(limit = 99, duration = 1, timeUnit = TimeUnit.MINUTE)
    @RateLimitGroup("limitBySession")
    @GetMapping("/greet")
    String greet(String name) {
        return "Hello " + name;
    }
}
```

__Configure rate limiting__

```java
package com.looseboxes.ratelimiter.web.spring;

import com.looseboxes.ratelimiter.cache.JavaRateCache;
import com.looseboxes.ratelimiter.web.core.Registries;
import com.looseboxes.ratelimiter.web.core.RateLimiterConfigurer;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;

@Configuration 
public class RateLimiterConfigurerImpl
        implements RateLimiterConfigurer<HttpServletRequest> {

  @Override 
  public void configure(Registries<HttpServletRequest> registry) {

    // Register RateRecordedListeners
    // ------------------------------

    // If you do not register a listener, the default listener throws an exception
    registry.listeners().register((resource, resourceId, amount, exceededLimits) -> {

      // For example, log the limit that was exceeded
      System.out.println(
              "For " + resourceId + ", the following limits are exceeded: " + exceededLimits);
    });

    // Register request matchers
    // -------------------------

    // Identify resources to rate-limit by session id
    registry.matchers().register("limitBySession", request -> request.getSession().getId());

    // Identify resources to rate-limit by the presence of request parameter "utm_source"
    registry.matchers().register("limitByUtmSource", request -> request.getParameter("utm_source"));

    // Rate limit users from a specific utm_source e.g facebook
    registry.matchers().register("limitByUtmSourceIsFacebook",
            request -> "facebook".equals(request.getParameter("utm_source")));

    // You could use a variety of Cache flavours
    // -----------------------------------------

    javax.cache.Cache javaxCache = null; // PROVIDE THIS
    registry.caches().register("limitBySession", new JavaRateCache<>(javaxCache));
  }
}
```

## Path binding

The `@RateLimit` and `@RateLimitGroup` annotations are bound to paths.

For the specification of these annotations, please read the [rate-limiter documentation](https://github.com/poshjosh/rate-limiter).
In addition, the following applies:

- The `@RateLimit` annotation must be placed together with path related annotations e.g:
  Springframeworks's `@RequestMapping`, `@Get` etc or JAX-RS `@Path` etc

- The `@RateLimit` annotation is bound to the path with which it is co-located, not the resource.
  This means that the `@RateLimit` annotation below will match all request paths beginning with `/api/v1`
  even those paths specified on other resources and methods.

```java
@Path("/api/v1")
@RateLimit(limit = 20, duration = 1, timeUnit = TimeUnit.MINUTES)
class RateLimitedResource{
    
}
```

## Naming Conventions for `RequestMatcher`s

A `RequestMatcher` may be registered using either a class name, a method name, or a string name.
When a string name is used, it should match one of the following:

- __A group__ - The name of a `@RateLimitGroup` annotation.
- __A class__ - The fully qualified name of a class e.g: `com.example.web.resources.GreetingResource`
- __A method__ - The identifier of a method eg: `com.example.web.resources.GreetingResource.greet(java.lang.String)`
- __A property__ - One of the keys in the `Map` returned by `RateLimitProperties#getRateLimitConfigs()`

When rate limits are specified via annotations, then the corresponding matcher for the annotated class
or method is automatically created. However, this automatic creation does not happen when rate limits are 
specified via properties. This means you need to explicitly register a matcher for each key in the `Map`
returned by `RateLimitProperties#getRateLimitConfigs()`. 

The following code will not lead to any rate limiting. Unless, we explicitly register a matcher
for each key in the returned map.

```java
public class RateLimitPropertiesImpl implements RateLimitProperties {

  // other code

  @Override
  public Map<String, Rates> getRateLimitConfigs() {
    return Collections.singletonMap("default", Rates.of(Rate.of(10, Duration.ofMinutes(1))));
  }
}
```

You could bind rate limits from properties to a class or method. For example to bind to 
class `MyRateLimitedResource.class`. To bind to a method replace `IdProvider.ofClass()` 
with `IdProvider.ofMethod()`.

```java
public class RateLimitPropertiesImpl implements RateLimitProperties, RateLimiterConfigurer<HttpServletRequest> {

    private final String resourceId = IdProvider.ofClass().getId(MyRateLimitedResource.class);
    
    @Override
    public void configure(Registries<HttpServletRequest> registries) {
        registries.matchers().register(resourceId, request -> request.getRequestURI());
    }
    
    @Override
    public List<String> getResourcePackages() {
        return Collections.singletonList(this.getClass().getPackage().getName());
    }

    @Override
    public Map<String, Rates> getRateLimitConfigs() {
        return Collections.singletonMap(resourceId, Rates.of(Rate.of(10, Duration.ofMinutes(1))));
    }
}
```

## Ways and Means

There are 2 ways to rate limit a web application:

### 1. Use the `@RateLimit` and/or `@RateLimitGroup` annotation

__Example using Springframework__

```java
import com.looseboxes.ratelimiter.annotations.RateLimit;

@Controller
@RequestMapping("/api")
class GreetingResource {

    // Only 99 calls to this path is allowed per minute
    @RateLimit(limit = 99, duration = 1, timeUnit = TimeUnit.MINUTES)
    @GetMapping("/greet")
    String greet() {
        return "Hello World!";
    }
}
```

__Example using JAX-RS__

```java
import com.looseboxes.ratelimiter.annotations.RateLimit;

@Path("/api")
class GreetingResource {

    // Only 99 calls to this path is allowed per minute
    @RateLimit(limit = 99, duration = 1, timeUnit = TimeUnit.MINUTES)
    @GET
    @Path("/greet")
    @Produces("text/plan")
    String greet() {
        return "Hello World!";
    }
}
```
  
### 2. Define rate limit properties

Example class that implements the required properties.

```java
package com.example.web;

import com.looseboxes.ratelimiter.util.Rate;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RateLimitPropertiesImpl implements RateLimitProperties {

  @Override
  public List<String> getResourcePackages() {
    return Collections.singletonList("com.example.web.resources");
  }

  @Override
  public Map<String, Rates> getRateLimitConfigs() {
    return Collections
            .singletonMap("limitBySession", Rates.of(getRates()));
  }

  private Rate[] getRates() {
    return new Rate[]{Rate.of(1, Duration.ofMinutes(1))};
  }
}
```

_Make sure this class is available for injection into other resources/beans._

The properties the user defines should be used to create a rate limiter which will be automatically applied to
every request the web application handles. 

Enjoy! :wink:
