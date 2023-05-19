[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.jqwik/jqwik/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.jqwik/jqwik)
[![Javadocs](http://javadoc.io/badge/net.jqwik/jqwik-api.svg)](https://jqwik.net/docs/current/javadoc/index.html)
[![CI Status](https://github.com/jqwik-team/jqwik/workflows/CI/badge.svg?branch=main)](https://github.com/jqwik-team/jqwik/actions)

# jqwik

An alternative 
[test engine for the JUnit 5 platform](https://junit.org/junit5/docs/current/user-guide/#launcher-api-engines-custom)
that focuses on Property-Based Testing.

## Dynamic parameters

This fork mainly adds "dynamic parameters". They can be used like this:

```kotlin
@Property(generation = GenerationMode.RANDOMIZED)
fun example() {
    val string by arbitrary { Arbitraries.strings() }

    assertThat(string + "Test").endsWith("Test")
}
```

Which is equivalent to:

```kotlin
@Property(generation = GenerationMode.RANDOMIZED)
fun example(@ForAll string: String) {
    assertThat(string + "Test").endsWith("Test")
}
```

Although, due to the experimental nature of this feature, things might break.

## See the [jqwik website](http://jqwik.net) for further details and documentation.


