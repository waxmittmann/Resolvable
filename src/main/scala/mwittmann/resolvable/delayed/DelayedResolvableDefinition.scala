package mwittmann.resolvable.delayed

import scalaz.Free.Trampoline
import scalaz.Scalaz._
import scalaz._

trait DelayedResolvableDefinition[T[_]] {
  implicit val resolvedMonad: Monad[T]

  type ResolveViaContext[A]

  sealed trait Resolvable[A] {
    def flatMap[B](fn: A => Resolvable[B]): Resolvable[B] = ResolvableMonad.bind(this)(fn)

    def map[B](fn: A => B): Resolvable[B] = ResolvableMonad.map(this)(fn)
  }

  case class FromResolved[A](resolved: T[A]) extends Resolvable[A]

  def fromResolved[A](resolved: T[A]): Resolvable[A] = FromResolved(resolved)

  case class ResolvableViaContext[A](resolvableThing: ResolveViaContext[A]) extends Resolvable[A]

  def resolvableViaContext[A](contextFn: ResolveViaContext[A]): Resolvable[A] = ResolvableViaContext(contextFn)

  case class BoundResolvableViaContext[A](resolvableFnT: Trampoline[(Context) => Trampoline[T[A]]]) extends Resolvable[A]

  def boundResolvableViaContext[A](resolvableFnT: Trampoline[(Context) => Trampoline[T[A]]]) = BoundResolvableViaContext(resolvableFnT)

  trait Context {
    def resolveInContext[A](resolveViaContext: ResolveViaContext[A]): T[A]

    def resolve[A](cio: Resolvable[A]): T[A] = cio match {
      case FromResolved(resolved) => resolved

      case BoundResolvableViaContext(fn) => {
        val fn2 = fn.run
        val r = fn2(this)
        r.run
      }

      case ResolvableViaContext(resolvableThing) => resolveInContext(resolvableThing)
    }
  }

  object ResolvableMonad extends Monad[Resolvable] {
    override def point[A](a: => A): Resolvable[A] = fromResolved(a.point[T])

    override def bind[A, B](ta: Resolvable[A])(fn: (A) => Resolvable[B]): Resolvable[B] =
      ta match {
        case ResolvableViaContext(resolvableThing) => BoundResolvableViaContext {
          Trampoline.delay { c =>
            Trampoline.done {
              c.resolveInContext(resolvableThing).flatMap(a => c.resolve(fn(a)))
            }
          }
        }

        case FromResolved(v) => BoundResolvableViaContext {
          Trampoline.delay { c =>
            Trampoline.done {
              v.flatMap(a => c.resolve(fn(a)))
            }
          }
        }

        case BoundResolvableViaContext(resolvableFnT: Trampoline[(Context) => Trampoline[T[A]]]) => BoundResolvableViaContext {
          Trampoline.suspend {
            resolvableFnT.flatMap((f1: (Context => Trampoline[T[A]])) => {
              Trampoline.delay {
                (c: Context) =>
                  Trampoline.suspend {
                    f1(c).map { (ta: T[A]) =>
                      ta.flatMap { a =>
                        c.resolve(fn(a))
                      }
                    }
                  }
              }
            })
          }
        }
      }
  }
}