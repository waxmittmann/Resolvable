package mwittmann.resolvable.auto

import scalaz.Scalaz._
import scalaz._

trait AutoResolvableDefinition[T[_]] {
  implicit val resolvedMonad: Monad[T]

  type ResolveViaContext[A]

  sealed trait Resolvable[A] {
    def flatMap[B](fn: A => Resolvable[B]): Resolvable[B] = ResolvableMonad.bind(this)(fn)

    def map[B](fn: A => B): Resolvable[B] = ResolvableMonad.map(this)(fn)
  }

  case class FromResolved[A](resolved: T[A]) extends Resolvable[A]
  def fromResolved[A](resolved: T[A]): Resolvable[A] = FromResolved(resolved)

  case class ResolvableViaContext[A](resolvableThing: ResolveViaContext[A]) extends Resolvable[A]
  def resolvableViaContext[A](resolvableThing: ResolveViaContext[A]): Resolvable[A] = ResolvableViaContext(resolvableThing)

  def resolveInContext[A](resolveViaContext: ResolveViaContext[A]): T[A]
  def resolve[A](resolvable: Resolvable[A]): T[A] = resolvable match {
    case FromResolved(resolved) => resolved
    case ResolvableViaContext(resolvableThing) => resolveInContext(resolvableThing)
  }

  object ResolvableMonad extends Monad[Resolvable] {
    override def point[A](a: => A): Resolvable[A] = fromResolved(a.point[T])

    override def bind[A, B](ta: Resolvable[A])(fn: (A) => Resolvable[B]): Resolvable[B] =
      ta match {
        case ResolvableViaContext(resolvableThing) => {
          val x1: T[A] = resolveInContext(resolvableThing)
          val x2: T[B] = x1.map(fn).flatMap(resolve)
          FromResolved(x2)
        }

        case FromResolved(v) => {
          val x2: T[B] = v.map(fn).flatMap(resolve)
          FromResolved(x2)
        }
      }
  }
}