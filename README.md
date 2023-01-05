# rate limiter web core

Light-weight rate limiting library for java web apps, based on
[rate-limiter-annotation](https://github.com/poshjosh/rate-limiter-annotation).

Please first read the [rate-limiter-annotation documentation](https://github.com/poshjosh/rate-limiter-annotation).


## Quick Start

__Annotate the resource you want to rate imit__

```java


@Controller
@RequestMapping("/api")
class GreetingResource {

  // Only 99 calls to this path is allowed per minute
  @RateGroup("limitBySession")
  @Rate(permits = 99, timeUnit = TimeUnit.MINUTES)
  @GetMapping("/greet")
  String greet(String name) {
    return "Hello " + name;
  }
}
```

__Configure rate limiting__

```java
package com.looseboxes.ratelimiter.web.spring;

import JavaRateCache;
import com.looseboxes.ratelimiter.web.core.Registries;
import com.looseboxes.ratelimiter.web.core.ResourceLimiterConfigurer;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class RateLimiterConfigurerImpl
        implements ResourceLimiterConfigurer<HttpServletRequest> {

  @Override
  public void configure(Registries<HttpServletRequest> registries) {

    // Register consumption listeners
    // ------------------------------

    registries.listeners().register((context, resourceId, hits, limit) -> {

      // For example, log the limit that was exceeded
      System.out.println("For " + resourceId + ", the following limits are exceeded: " + limit);
    });

    // Register request matchers
    // -------------------------

    // Identify resources to rate-limit by session id
    registries.matchers().register("limitBySession", request -> request.getSession().getId());

    // Identify resources to rate-limit by the presence of request parameter "utm_source"
    registries.matchers().register("limitByUtmSource", request -> request.getParameter("utm_source"));

    // Rate limit users from a specific utm_source e.g facebook
    registries.matchers().register("limitByUtmSourceIsFacebook",
            request -> "facebook".equals(request.getParameter("utm_source")));

    // You could use a variety of Cache flavours
    // -----------------------------------------

    javax.cache.Cache javaxCache = null; // PROVIDE THIS
    registries.caches().register("limitBySession", new JavaRateCache<>(javaxCache));
  }
}
```

## Path binding

Please first read the [annotation specs](https://github.com/poshjosh/rate-limiter-annotation/blob/main/docs/ANNOTATION_SPECS.md). It is concise.

The `@Rate` and `@RateGroup` annotations are bound to paths.

For the specification of these annotations, please read the [rate-limiter documentation](https://github.com/poshjosh/rate-limiter).
In addition, the following applies:

- The `@Rate` annotation must be placed together with path related annotations e.g:
  Springframeworks's `@RequestMapping`, `@Get` etc or JAX-RS `@Path` etc

- The `@Rate` annotation is bound to the path with which it is co-located, not the resource.
  This means that the `@Rate` annotation below will match all request paths beginning with `/api/v1`
  even those paths specified on other resources and methods.

```java
@Path("/api/v1")
@Rate(limit = 20, timeUnit = TimeUnit.MINUTES)
class RateLimitedResource{
    
}
```

## Naming Conventions for `RequestMatcher`s

A `RequestMatcher` may be registered using either a class name, a method name, or a string name.
When a string name is used, it should match one of the following:

- __A group__ - The name of a `@RateGroup` annotation.
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
    return Collections.singletonMap("default", Rates.of(Rate.ofMinutes(10)));
  }
}
```

You could bind rate limits from properties to a class or method. For example to bind to 
class `MyRateLimitedResource.class`. To bind to a method replace `IdProvider.ofClass()` 
with `IdProvider.ofMethod()`.

```java
public class RateLimitPropertiesImpl implements RateLimitProperties, RateLimiterConfigurer<HttpServletRequest> {

    private final String resourceId = ElementId.of(MyRateLimitedResource.class);
    
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
        return Collections.singletonMap(resourceId, Rates.of(Rate.ofMinutes(10)));
    }
}
```

## Ways and Means

There are 2 ways to rate limit a web application:

### 1. Use the `@Rate` and/or `@RateGroup` annotation

__Example using Springframework__

```java


@Controller
@RequestMapping("/api")
class GreetingResource {

  // Only 99 calls to this path is allowed per minute
  @GetMapping("/greet")
  String greet() {
    return "Hello World!";
  }
}
```

__Example using JAX-RS__

```java


@Path("/api")
class GreetingResource {

  // Only 99 calls to this path is allowed per minute
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

## Dependents

The following depend on this library:

- [rate-limiter-spring](https://github.com/poshjosh/rate-limiter-spring).

- [rate-limiter-javaee](https://github.com/poshjosh/rate-limiter-javaee).


Enjoy! :wink:
