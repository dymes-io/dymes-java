<!--
SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors

SPDX-License-Identifier: Apache-2.0
-->

# Dymes Java Conventions

## Coding Conventions

Please adhere to these coding conventions when maintaining/contributing to Dymes.

### API Contracts

#### Value types

Value types (e.g. DTOs) must be immutable, preferably created using [builders](#builders),
but simple value types may be created using `of(..)` [factory methods](#factory-methods).

Using `record` instead of `class` is a cheap and easy way to achieve immutability
(and get sensible defaults for `#hashCode`, `#equals` and `#toString` to boot).

#### Builders

Builders follow these conventions:

- Attributes are added using `with...` calls
    - `null` values throw `NullPointerException`
    - Invalid/Illegal values throw `IllegalArgumentException`
- Final validation and construction using a `build()` call.
    - Required values being `null` throws `NullPointerException`
    - Invalid/Illegal combinations of values (e.g. mutually exclusive flags) throws `IllegalStateException`
- Optionally, allow the use of a prototype `usingPrototype(proto)`
- Optionally, allow the use of default values `usingDefaults()`

> Using a prototype or defaults to seed values should be performed *before* any `with...` calls

#### Factory methods

Factory methods follow these conventions:

- Factory methods should be named `of(..)`
- Argument checking conventions:
    - `null` values throw `NullPointerException`
    - Invalid/Illegal values throw `IllegalArgumentException`

#### Exceptions

Checked exceptions are the work of the devil and must be avoided at all costs,
use `RuntimeException` and subclasses only. Some supporting arguments for this view may be found
[here](http://articles.vconst.com/2009/08/error-handling-and-exceptions-in-java.html)
and [there](https://ohadshai.medium.com/its-almost-2020-and-yet-checked-exceptions-are-still-a-thing-bfaa24c8997e).

If you cannot handle an exception, *do not* catch and rethrow, or worse, swallow such an exception silently.

It is, however, sensible and encouraged to encapsulate checked exceptions or
provide a decorator with additional context, such as catching a `404` and wrapping/replacing
it with a `ServiceCallFailedException` for example.

#### Resource management

Components holding resources (database connections, caches, etc.) should allow use
in `try-with-resources` blocks by implementing the `Autoclosable` interface.

No exceptions are allowed from a `close()` call, it should be best-effort, and any exceptions
that occur inside should be logged at `warning` level and *not* propagated.

This allows the safe and sane use of `close()` in `finally` blocks, since any propagated exceptions
may be non-deterministically suppressed and lost if thrown.
See the [Java Language Specification](https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-14.20.2) regarding this issue.

### Logging

Logging should be performed using the [SLF4J](https://www.slf4j.org) API only.

For interop with misbehaving dependencies, calls to `log4j 2.x`, `log4j 1.x` and `java.util.logging` are forwarded to `SLF4J`.

All uppercase symbol naming is intended for *CONSTANTS*, so, very suitable for scalars and immutable value types.
*NOT* suitable for mutable, active objects such as loggers! So kindly stop using `LOGGER` or `LOG` for your logger,
stick to `logger`, e.g.
```java
private static final Logger logger = LoggerFactory.getLogger("logger name");
```

And if anyone pipes up with "but... it's what we use everywhere else", kindly remind them that:
> 'A foolish consistency is the hobgoblin of little minds...' -- Emerson


## Commit Message Guidelines

The Dymes commit guidelines are largely based on the [Conventional Commits](https://www.conventionalcommits.org) conventions.

The commit message structure is as follows:

```
    <type>(subsystem): <summary>
    [optional body]
```

### Commit message type

Commit type must be one of the following:

| Commit Type | Description                                                                     |
|-------------|---------------------------------------------------------------------------------|
| feat        | A new feature or enhancement                                                    |
| fix         | A bug fix                                                                       |
| doc         | A documentation-only change                                                     |
| ci          | CI or build configuration and/or scripts                                        |
| perf        | Performance enhancement                                                         |
| reno        | Renovations (refactoring), code changes that neither fix a bug or add a feature |
| chore       | Bumping dependencies, adapting to API changes                                   |
| test        | Adding or fixing tests                                                          |
| ops         | Ops-related, such as Terraform, k8s or helm changes                             |

### Commit message subsystem

This indicates which subsystem in particular was affected by
the change, otherwise a cross-cutting change is assumed.

Dymes Java subsystems:

| Subsystem    | Description                    |
|--------------|--------------------------------|
| java-client  | Java client code               |
| java-model   | Java client model              |
| java-testing | Test support code              |

### Commit message summary

Use the commit summary to provide a succinct description of the change.

### Commit message body

Explain the motivation for the change in the commit message body.

This should explain _why_ you are making the change.

You can include a comparison of the previous behavior with the new behavior in order to illustrate the impact of the change.
