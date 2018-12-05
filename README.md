# ePromise
A node.js-like library providing Promise for Android

```java
new Promise(handle -> {

        // some work
        object value = ...;
        handle.resolve(value);

}).then((value, handle) -> {

        String upper = value.toString().toUpper();
        handle.resolve(upper);

});

```
