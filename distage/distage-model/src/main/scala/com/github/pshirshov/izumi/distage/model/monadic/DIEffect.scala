package com.github.pshirshov.izumi.distage.model.monadic

import com.github.pshirshov.izumi.functional.bio.BIO
import com.github.pshirshov.izumi.fundamentals.platform.functional.Identity
import com.github.pshirshov.izumi.fundamentals.platform.language.Quirks._

trait DIEffect[F[_]] {
  def pure[A](a: A): F[A]
  def map[A, B](fa: F[A])(f: A => B): F[B]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  // FIXME: tailRecM?

  /** A weaker version of `delay`. Does not guarantee _actual_
    * suspension of side-effects, because DIEffect[Identity] is allowed */
  def maybeSuspend[A](eff: => A): F[A]

  /** A stronger version of `handleErrorWith`, the difference is that this will _also_ intercept Throwable defects in `ZIO`, not only typed errors */
  def definitelyRecover[A](action: => F[A], recover: Throwable => F[A]): F[A]
//  def definitelyAttempt(fa: )

  final val unit: F[Unit] = pure(())

  final def widen[A, B >: A](fa: F[A]): F[B] = fa.asInstanceOf[F[B]]
  final def traverse_[A](l: Iterable[A])(f: A => F[Unit]): F[Unit] = {
    // FIXME: foldRight for right-associative flatMaps?
    l.foldLeft[F[Unit]](unit) { (acc, a) =>
      flatMap(acc)(_ => f(a))
    }
  }
  final def foldLeftM[S, A](in: Iterable[A])(zero: S)(f: (S, A) => F[S]): F[S] =
    in.foldLeft[F[S]](pure(zero)) { (acc, a) =>
      flatMap(acc)(f(_, a))
    }
}

object DIEffect
  extends FromCats {

  def apply[F[_]: DIEffect]: DIEffect[F] = implicitly

  object syntax {
    implicit final class DIEffectSyntax[F[_], A](private val fa: F[A]) extends AnyVal {
      @inline def map[B](f: A => B)(implicit F: DIEffect[F]): F[B] = F.map(fa)(f)
      @inline def flatMap[B](f: A => F[B])(implicit F: DIEffect[F]): F[B] = F.flatMap(fa)(f)

//      @inline def definitelyRecover[A1 >: A](recover: Throwable => F[A1])(implicit F: DIEffect[F]): F[A1] = {
//        F.definitelyRecover(F.widen[A1](fa), )
//      }
    }
  }

  implicit val diEffectIdentity: DIEffect[Identity] = new DIEffect[Identity] {
    override def pure[A](a: A): Identity[A] = a
    override def flatMap[A, B](a: A)(f: A => Identity[B]): Identity[B] = f(a)
    override def map[A, B](fa: Identity[A])(f: A => B): Identity[B] = f(fa)
    override def maybeSuspend[A](eff: => A): Identity[A] = eff
    override def definitelyRecover[A](fa: => Identity[A], recover: Throwable => Identity[A]): Identity[A] = {
      try fa catch { case t: Throwable => recover(t) }
    }
  }

  implicit def fromBIO[F[+_, +_], E <: Throwable](implicit F: BIO[F]): DIEffect[F[E, ?]] = new DIEffect[F[E, ?]] {
    import BIO._

    override def pure[A](a: A): F[E, A] = F.now(a)
    override def map[A, B](fa: F[E, A])(f: A => B): F[E, B] = F.map(fa)(f)
    override def flatMap[A, B](fa: F[E, A])(f: A => F[E, B]): F[E, B] = F.flatMap(fa)(f)

    override def maybeSuspend[A](eff: => A): F[E, A] = F.sync(eff)

    override def definitelyRecover[A](fa: => F[E, A], recover: Throwable => F[E, A]): F[E, A] = {
      fa.sandbox.catchAll(recover apply _.toThrowable)
    }
  }

//  def x = fromCats[Chain, Monad[Chain]]
//  def x = fromCats[Chain, Monad]
//  def x = DIMonad[List](fromCats(J.j,catsStdInstancesForList))
  def x = DIEffect[cats.effect.SyncIO]
}

trait FromCats {

  implicit def fromCatsEffect[F[_], R[_[_]]](implicit l: IsSync[R], F0: R[F]): DIEffect[F] = new DIEffect[F] {
    l.discard()
    val F = F0.asInstanceOf[cats.effect.Sync[F]]
    override def pure[A](a: A): F[A] = F.pure(a)
    override def map[A, B](fa: F[A])(f: A => B): F[B] = F.map(fa)(f)
    override def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = F.flatMap(fa)(f)

    override def maybeSuspend[A](eff: => A): F[A] = F.delay(eff)

    override def definitelyRecover[A](fa: => F[A], recover: Throwable => F[A]): F[A] = {
      F.handleErrorWith(F.suspend(fa))(recover)
    }
  }

  class IsSync[R[_[_]]]
  object IsSync {
    implicit val j: IsSync[cats.effect.Sync] = new IsSync
  }

}