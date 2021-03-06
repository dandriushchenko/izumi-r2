Cookbook
========

### Inner Classes and Path-Dependent Types

To instantiate path-dependent types via constructor, their prefix type has to be present in DI object graph:

```scala mdoc:reset
import distage._

trait Path {
  class A
  class B
}

val path = new Path {}

val module = new ModuleDef {
  make[path.A]
  make[path.type].from[path.type](path: path.type)
}
```

The same applies to type projections:

```scala mdoc
val module1 = new ModuleDef {
  make[Path#B]
  make[Path].from(new Path {})
}
```

Provider and instance bindings and also compile-time mode in `distage-static` module do not require the singleton type prefix to be present in DI object graph:

```scala mdoc
val module2 = new ModuleDef {
  make[Path#B].from {
    val path = new Path {}
    new path.B
  }
}
```

### Depending on Locator

Classes can depend on the Locator:

```scala
import distage._

class A(all: LocatorRef) {
  def c = all.get.get[C]
}
class B
class C

val module = new ModuleDef {
  make[A]
  make[B]
  make[C]
}

val locator = Injector().produce(module)

assert(locator.get[A].c eq locator.get[C]) 
```

It is recommended to avoid this if possible, doing so is often a sign of broken application design.
