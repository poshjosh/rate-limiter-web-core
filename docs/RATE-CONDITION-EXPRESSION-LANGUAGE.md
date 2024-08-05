# RateCondition Expression Language (Web request implementation)

A language for expressing the condition for rate limiting.

Please first read the [core specification](https://github.com/poshjosh/rate-limiter-annotation/blob/main/docs/RATE-CONDITION-EXPRESSION-LANGUAGE.md).

### Format

An expression is of format `LHS` `OPERATOR` `RHS` e.g `jvm.thread.count.started > 99`

`LHS` = `jvm.thread.count`,  `OPERATOR` = `>`,  `RHS` = `99`

| format                   | example                                            | description                                                       |  
|--------------------------|----------------------------------------------------|-------------------------------------------------------------------|
| LHS = RHS                | web.request.header[X-RateLimit-Limit] !=           | true, when the X-RateLimit-Limit header exists                    |  
| LHS[key] = val           | web.request.parameter[limited] = true              | true, when request parameter limited equals true                  |  
| LHS = [A &#9122; B]      | web.request.user.role = [GUEST &#9122; RESTRICTED] | true, when the user role is either GUEST or RESTRICTED            |
| LHS[key] = [A &#9122; B] | web.request.cookie[name] = [val_0 &#9122; val_1]   | true, when cookie named `name` is either val_0 or val_1           |  
| LHS[key] = [A & B]       | web.request.header[name] = [val_0 & val_1]         | true, when header named `name` has both val_0 and val_1 as values |  

Example:

```java
// 5 permits per second when available system memory is less than 1 GB
@Rate(permits = 5, when = "jvm.memory.available < 1GB")
class ResourceA{ }

class ResourceB{
    // 2 permits per second when available system memory is less than 1 GB, and user role is GUEST
    @Rate(permits = 2, when = "jvm.memory.available < 1GB & web.request.user.role = GUEST")
    void smile() {
        return ":)";
    }
}
```

### Supported LHS

In addition to those listed in the [annotation specification](https://github.com/poshjosh/rate-limiter-annotation/blob/main/docs/RATE-CONDITION-EXPRESSION-LANGUAGE.md), the following are supported:

`web.request.attribute`

`web.request.auth.scheme`

`web.request.header`

`web.request.locale`

`web.request.parameter`

`web.request.ip`

`web.request.remote.address`

`web.request.uri`

`web.request.cookie`

`web.request.user.principal`

`web.request.user.role`

`web.session.id`

 
