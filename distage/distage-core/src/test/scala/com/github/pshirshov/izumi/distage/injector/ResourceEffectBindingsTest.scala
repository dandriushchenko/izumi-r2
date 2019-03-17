package com.github.pshirshov.izumi.distage.injector

import com.github.pshirshov.izumi.distage.fixtures.CircularCases.CircularCase3.SelfReference
import com.github.pshirshov.izumi.distage.fixtures.ResourceCases.ClassResourceCase.{SimpleResource, SuspendResource}
import com.github.pshirshov.izumi.distage.fixtures.ResourceCases._
import com.github.pshirshov.izumi.fundamentals.platform.functional.Identity
import distage.{DIKey, Id, ModuleDef, PlannerInput}
import org.scalatest.WordSpec

import scala.collection.immutable.Queue

class ResourceEffectBindingsTest extends WordSpec with MkInjector {

  final type Fn[+A] = Suspend2[Nothing, A]
  final type Ft[+A] = Suspend2[Throwable, A]

  "Effect bindings" should {

    "work in a basic case in Identity monad" in {
      val definition = PlannerInput(new ModuleDef {
        make[Int].named("2").from(2)
        make[Int].fromEffect[Identity, Int] { i: Int @Id("2") => 10 + i }
      }, roots = DIKey.get[Int])

      val injector = mkInjector()
      val plan = injector.plan(definition)
      val context = injector.produceUnsafe(plan)

      assert(context.get[Int] == 12)
    }

    "work in a basic case in Suspend2 monad" in {
      val definition = PlannerInput(new ModuleDef {
        make[Int].named("2").from(2)
        make[Int].fromEffect { i: Int @Id("2") => Suspend2(10 + i) }
      }, roots = DIKey.get[Int])

      val injector = mkInjector()
      val plan = injector.plan(definition)

      val context = injector.produceUnsafeF[Suspend2[Throwable, ?]](plan).unsafeRun()

      assert(context.get[Int] == 12)
    }

    "work with constructor binding" in {
      val definition = PlannerInput(new ModuleDef {
        make[Int].named("2").from(2)
        make[Int].fromEffect[Suspend2[Nothing, ?], Int, IntSuspend]
      }, roots = DIKey.get[Int])

      val injector = mkInjector()
      val plan = injector.plan(definition)

      val context = injector.produceUnsafeF[Suspend2[Nothing, ?]](plan).unsafeRun()

      assert(context.get[Int] == 12)
    }

    "execute effects again in reference bindings" in {
      val execIncrement = (_: Ref[Fn, Int]).update(_ + 1)

      val definition = PlannerInput(new ModuleDef {
        make[Ref[Fn, Int]].fromEffect(Ref[Fn](0))

        make[Fn[Int]].from(execIncrement)

        make[Int].named("1").refEffect[Fn, Int]
        make[Int].named("2").refEffect[Fn, Int]
      })

      val injector = mkInjector()
      val plan = injector.plan(definition)

      val context = injector.produceUnsafeF[Suspend2[Nothing, ?]](plan).unsafeRun()

      assert(context.get[Int]("1") == 1)
      assert(context.get[Int]("2") == 2)
      assert(context.get[Ref[Fn, Int]].get.unsafeRun() == 2)
    }

    "Support self-referencing circular effects" in {
      import com.github.pshirshov.izumi.distage.fixtures.CircularCases.CircularCase3._

      val definition = PlannerInput(new ModuleDef {
        make[Ref[Fn, Boolean]].fromEffect(Ref[Fn](false))
        make[SelfReference].fromEffect {
          (ref: Ref[Fn, Boolean], self: SelfReference) =>
            ref.update(!_).flatMap(_ => Suspend2(new SelfReference(self)))
        }
      })

      val injector = mkInjector()
      val plan = injector.plan(definition)
      val context = injector.produceUnsafeF[Suspend2[Throwable, ?]](plan).unsafeRun()

      val instance = context.get[SelfReference]

      assert(instance eq instance.self)
      assert(context.get[Ref[Fn, Boolean]].get.unsafeRun())
    }

    "support Identity effects in Suspend monad" in {
      val definition = PlannerInput(new ModuleDef {
        make[Int].named("2").from(2)
        make[Int].fromEffect[Identity, Int] { i: Int @Id("2") => 10 + i }
      }, roots = DIKey.get[Int])

      val injector = mkInjector()
      val plan = injector.plan(definition)
      val context = injector.produceUnsafeF[Suspend2[Throwable, ?]](plan).unsafeRun()

      assert(context.get[Int] == 12)
    }

    "work with set bindings" in {
      val definition = PlannerInput(new ModuleDef {
        make[Ref[Fn, Set[Char]]].fromEffect(Ref[Fn](Set.empty[Char]))

        many[Char]
          .addEffect(Suspend2('a'))
          .addEffect(Suspend2('b'))

        make[Unit].fromEffect {
          (ref: Ref[Fn, Set[Char]], set: Set[Char]) =>
            ref.update(_ ++ set).void
        }
        make[Unit].named("1").fromEffect {
          ref: Ref[Fn, Set[Char]] =>
            ref.update(_ + 'z').void
        }
        make[Unit].named("2").fromEffect {
          (_: Unit, _: Unit @Id("1"), ref: Ref[Fn, Set[Char]]) =>
            ref.update(_.map(_.toUpper)).void
        }
      })

      val injector = mkInjector()
      val plan = injector.plan(definition)
      val context = injector.produceUnsafeF[Suspend2[Throwable, ?]](plan).unsafeRun()

      assert(context.get[Set[Char]] == "ab".toSet)
      assert(context.get[Ref[Fn, Set[Char]]].get.unsafeRun() == "ABZ".toSet)
    }

  }

  "Resource bindings" should {

    "work in a basic case in Identity monad" in {
      import ClassResourceCase._

      val definition = PlannerInput(new ModuleDef {
        make[Res].fromResource[SimpleResource]// FIXME: syntax
      })

      val injector = mkInjector()
      val plan = injector.plan(definition)

      val instance = injector.produce(plan).use {
        context =>
          val instance = context.get[Res]
          assert(instance.initialized)
          instance
      }

      assert(!instance.initialized)
    }

    "work in a basic case in Suspend2 monad" in {
      import ClassResourceCase._

      val definition = PlannerInput(new ModuleDef {
        make[Res].fromResource[SuspendResource]
      })

      val injector = mkInjector()
      val plan = injector.plan(definition)

      val instance = injector.produceF[Suspend2[Throwable, ?]](plan).use {
        context =>
          val instance = context.get[Res]
          assert(instance.initialized)
          Suspend2(instance)
      }.unsafeRun()

      assert(!instance.initialized)
    }

    "Support mutually-referent circular resources" in {
      import CircularResourceCase._

      val definition = PlannerInput(new ModuleDef {
        make[Ref[Fn, Queue[Ops]]].fromEffect(Ref[Fn](Queue.empty[Ops]))
        many[IntegrationComponent]
          .ref[S3Component]
        make[S3Component].fromResource(s3ComponentResource[Fn] _)
        make[S3Client].fromResource(s3clientResource[Fn] _)
      }, DIKey.get[S3Client])

      val injector = mkInjector()
      val plan = injector.plan(definition)

      val context = injector.produceF[Suspend2[Nothing, ?]](plan).use {
        Suspend2(_)
      }.unsafeRun()

      val s3Component = context.get[S3Component]
      val s3Client = context.get[S3Client]

      assert(s3Component eq s3Client.c)
      assert(s3Client eq s3Component.s)

      val startOps = context.get[Ref[Fn, Queue[Ops]]].get.unsafeRun().take(2)
      assert(startOps.toSet == Set(ComponentStart, ClientStart))

      val expectStopOps = startOps.reverse.map(_.invert)
      assert(context.get[Ref[Fn, Queue[Ops]]].get.unsafeRun().slice(2, 4) == expectStopOps)
    }

  }

}

