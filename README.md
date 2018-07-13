# valuetype-lworld
Tests of Java value type (lworld prototype)


### build the source

I use pro as a build tool so the first thing is to download the a version of pro with the l-word patch,
like this

```
export PRO_SPECIAL_BUILD='early-access-lworld'
/usr/jdk/jdk-11/bin/java pro_wrapper.java
```

then you can build the source like this
```
./pro/bin/pro
```
this will also execute the JUnit tests


### run a JMH test

if you have already a version of Java patched with l-world path, you can run by example the ReifiedListBenchMark like this
```
/usr/jdk/jdk-11-lworld/bin/java -XX:+EnableValhalla --module-path target/test/artifact:deps -m fr.umlv.valuetype/fr.umlv.valuetype.perf.ReifiedListBenchMark
```

otherwise, you can use the jdk embeded with pro
```
./pro/bin/java -XX:+EnableValhalla --module-path target/test/artifact:deps -m fr.umlv.valuetype/fr.umlv.valuetype.perf.ReifiedListBenchMark
```

you can also run all benchmarks using the perfer plugin of pro
```
./pro/bin/pro build build.pro perfer
```

