Debugging
=========

You can print the `plan` to get detailed info on what will happen during instantiation. The printout includes file:line info
so your IDE can show you where the binding was defined!

```scala
val plan = Injector().plan(module)

System.err.println(plan)
```

![print-test-plan](media/print-test-plan.png)

You can also query a plan to see the dependencies and reverse dependencies of a specific class and their order of instantiation:

```scala
// Print dependencies
System.err.println(plan.topology.dependencies.tree(DIKey.get[Circular1]))
// Print reverse dependencies
System.err.println(plan.topology.dependees.tree(DIKey.get[Circular1]))
```

![print-dependencies](media/print-dependencies.png)

The printer highlights circular dependencies.

distage also uses some macros to create `TagK`s and [function bindings](#function-bindings),
you can turn on macro debug output during compilation by setting `-Dizumi.distage.debug.macro=true` java property:

```bash
sbt -Dizumi.distage.debug.macro=true compile
```

Macros power `distage-static` module, an alternative backend that does not use JVM runtime reflection to instantiate classes and auto-traits.

### GraphViz Dump

@@@ warning { title='TODO' }
Sorry, this page is not ready yet
@@@

Add `GraphDumpBootstrapModule` to `Injector`'s configuration to enable printing of graphviz files with graph representation of the `Plan`.

```scala
val injector = Injector(new GraphDumpBootstrapModule())
```

Files will be saved to `./target/plan-last-full.gv` and `./target/plan-last-nogc.gv`

Use command-line:

```bash
dot -Tpng target/plan-last-nogc.gv -o out.png
```

To render `GraphViz` files into a viewable PNG image. You need GraphViz installed on your system.

![plan-graph](media/plan-graph.png)
