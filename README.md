# ePromise [![](https://jitpack.io/v/emranbm/ePromise.svg)](https://jitpack.io/#emranbm/ePromise)
A node.js-like library providing Promise for Android

## Usage
```java
new Promise(handle -> {

        // some work
        // This is done in an async task
        object value = ...;
        handle.resolve(value);

}).then((value, handle) -> {

        String upper = value.toString().toUpper();
        handle.resolve(upper);

});

```
## Gradle
Add it in your root build.gradle at the end of repositories:
```gradle
allprojects {
        repositories {
                ...
                maven { url 'https://jitpack.io' }
        }
}
```
Then add the dependency
```gradle
dependencies {
        implementation 'com.github.emranbm:ePromise:1.2.0'
}
```
That's it!
