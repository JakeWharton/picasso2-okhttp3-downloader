Picasso 2 OkHttp 3 Downloader
=============================

A OkHttp 3 downloader implementation for Picasso 2.

⚠️ Use only with Picasso 2.5.2 or lower, otherwise images won't load. In Picasso 2.71828 and higher, there's now built-in equivalent `com.squareup.picasso.OkHttp3Downloader`. 



Usage
-----

Create an `OkHttp3Downloader` instance wrapping your `OkHttpClient` or `Call.Factory` and pass it
to `downloader`.

```java
OkHttpClient client = // ...
Picasso picasso = new Picasso.Builder(context)
    .downloader(new OkHttp3Downloader(client))
    .build()
```

You can also use the the other constructors for a default `OkHttpClient` instance.



Download
--------

Gradle:
```groovy
compile 'com.jakewharton.picasso:picasso2-okhttp3-downloader:1.1.0'
```
or Maven:
```xml
<dependency>
  <groupId>com.jakewharton.picasso</groupId>
  <artifactId>picasso2-okhttp3-downloader</artifactId>
  <version>1.1.0</version>
</dependency>
```



License
-------

    Copyright 2016 Jake Wharton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
