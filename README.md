# rate limiter web core

Enterprise rate limiter for java web apps, based on
[rate-limiter-annotation](https://github.com/poshjosh/rate-limiter-annotation).

We believe that rate limiting should be as simple as:

```java
@Rate("10/s") // 10 permits per second for all methods in this class
@RequestMapping("/api/v1")
public class GreetingResource {

  @Rate(permits=10, when="web.request.user.role = GUEST")
  @GetMapping("/smile")
  public String smile() {
    return ":)";
  }

  @Rate(permits=1, when="jvm.memory.available < 1gb & web.request.ip in com.mypackage.Bots.ips()")
  @GetMapping("/greet")
  public String greet(@RequestParam("who") String who) {
    return "Hello " + who;
  }
}
```

Please first read the [rate-limiter-annotation documentation](https://github.com/poshjosh/rate-limiter-annotation).

Some custom implementations:

- [rate-limiter-spring](https://github.com/poshjosh/rate-limiter-spring).

- [rate-limiter-javaee](https://github.com/poshjosh/rate-limiter-javaee).

To add a dependency on `rate-limiter-web-core` using Maven, use the following:

```xml
        <dependency>
            <groupId>io.github.poshjosh</groupId>
            <artifactId>rate-limiter-web-core</artifactId>
            <version>0.8.0</version> 
        </dependency>
```

## Quick Start

__Annotate the resource you want to rate limit__

```java
@Controller
@RequestMapping("/api")
class GreetingResource {

  // 2 calls per second, if the header X-Rate-Limited has a value
  @Rate("2/s")
  @RateCondition("web.request.header = X-Rate-Limited")
  @GetMapping("/smile")
  String smile() {
    return ":)";
  }
}
```

__(Optional) Configure rate limiting__

Limiters, matchers, caches and listeners, could be configured by implementing and
exposing a `RateLimiterConfigurer` as shown below:

```java
@Configuration 
public class Configurer implements RateLimiterConfigurer {

  @Override 
  public void configureMatchers(Registry<Matcher<RequestInfo>> matcherRegistry) {

    // Register request matchers
    // -------------------------

    // Identify resources to rate-limit by session id
    matcherRegistry
            .register("limitBySession", request -> request.getSession().getId());

    // Identify resources to rate-limit by the presence of request parameter "utm_source"
    matcherRegistry
            .register("limitByUtmSource", request -> request.getParameter("utm_source"));

    // Rate limit users from a specific utm_source e.g facebook
    matcherRegistry
            .register("limitByUtmSourceIsFacebook", request -> "facebook".equals(request.getParameter("utm_source")));
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


## Naming Conventions 

Limiters, matchers, caches or listeners could be registered using either a class ID, 
a method ID, or a string name. When using a string name, it should match one of the following:

- __A group__ - The name of a `@RateGroup` annotation.
- __A class__ - The fully qualified name of a class e.g: `com.example.web.resources.GreetingResource`
- __A method__ - The identifier of a method eg: `com.example.web.resources.GreetingResource.greet(java.lang.String)`
- __A property__ - One of the keys in the `Map` returned by `RateLimitProperties#getRates()`

When rate limits are specified via annotations, then the corresponding matcher for the annotated class
or method is automatically created. However, this automatic creation does not happen when rate limits are 
specified via properties. This means you need to explicitly register a matcher for each key in the `Map`
returned by `RateLimitProperties#getRates()`. 

The following code will not lead to any rate limiting. Unless, we explicitly register a matcher
for each key in the returned map.

```java
public class RateLimitPropertiesImpl implements RateLimitProperties {

  // other code

  @Override
  public Map<String, Rates> getRates() {
    return Collections.singletonMap("default", Rates.of(Rate.ofMinutes(10)));
  }
}
```

You could bind rate limits from properties to a class or method. For example to bind to 
class `MyRateLimitedResource.class`:

```java
public class RateLimitPropertiesImpl implements RateLimitProperties, RateLimiterConfigurer {

    private final Object resource = MyRateLimitedResource.class;
    
    @Override
    public void configure(Registries registries) {
        registries.matchers().register(resource, request -> request.getRequestURI());
    }
    
    @Override
    public List<String> getResourcePackages() {
        return Collections.singletonList(this.getClass().getPackage().getName());
    }

    @Override
    public Map<String, Rates> getRates() {
        return Collections.singletonMap(RateId.of(resource), Rates.of(Rate.ofMinutes(10)));
    }
}
```

## Match order

Matchers are invoked in this order:

- Path pattern matcher - Created by default - (Matches path patterns e.g `@GetMapping("/api/v1")`)
- Custom expression matcher ([see expression language](docs/RATE-CONDITION-EXPRESSION-LANGUAGE.md))
- System expression matcher - Created by default - ([see expression language](docs/RATE-CONDITION-EXPRESSION-LANGUAGE.md))
- Custom registered matcher 

## Ways and Means

There are 2 ways to rate limit a web application (You could use both):

### 1. Use the `@Rate` and/or `@RateGroup` annotation

__Example using Springframework__

```java


@Controller
@RequestMapping("/api")
class GreetingResource {

  // Only 99 calls to this path is allowed per second
  @Rate("99/s")
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

  // Only 99 calls to this path is allowed per second
  @Rate("99/s")
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

import RateLimitProperties;

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
    public Map<String, Rates> getRates() {
        return Collections.singletonMap("limitBySession", Rates.of(getRates()));
    }

    private Rate[] getRates() {
        return new Rate[] { Rate.of(1, Duration.ofMinutes(1)) };
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
