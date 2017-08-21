package mwittmann.resolvable.delayed2

/*
import scalaz.Free.Trampoline
import scalaz.Scalaz._
import scalaz._

trait DelayedResolvableDefinition2[T[_]] {
  implicit val resolvedMonad: Monad[T]

  type ResolveViaContext[A]

  sealed trait Resolvable[S, A] {
    def flatMap[B](fn: A => Resolvable[Unit, B]): Resolvable[Unit, B] = ResolvableMonad.bind(this)(fn)

    def map[B](fn: A => B): Resolvable[Unit, B] = ResolvableMonad.map(this)(fn)
  }

  case class Resolved[A](resolved: T[A]) extends Resolvable[Unit, A]
  def resolved[A](resolved: T[A]): Resolvable[A] = Resolved(resolved)

  case class ViaContext[A](resolvableThing: ResolveViaContext[A]) extends Resolvable[A]
  def viaContext[A](contextFn: ResolveViaContext[A]): Resolvable[A] = ViaContext(contextFn)

  case class Bound[A](resolvableFnT: Trampoline[(Context) => Trampoline[T[A]]]) extends Resolvable[A]
  def bound[A](resolvableFnT: Trampoline[(Context) => Trampoline[T[A]]]) = Bound(resolvableFnT)

  case class BoundWithViaContext[A, B](
    resolvableFnT: Trampoline[(Context) => Trampoline[T[A]]],
    viaContext: A => ViaContext[B]
  ) extends Resolvable[B]

  def boundWithViaContext[A, B](
    resolvableFnT: Trampoline[(Context) => Trampoline[T[A]]],
    viaContext: A => ViaContext[B]
  ) = BoundWithViaContext(resolvableFnT, viaContext)

  trait Context {
    def resolveInContext[A](resolveViaContext: ResolveViaContext[A]): T[A]

    def resolve[A](cio: Resolvable[A]): T[A] = cio match {
      case Resolved(resolved) => resolved

      case Bound(fn) => {
        val fn2 = fn.run
        val r = fn2(this)
        r.run
      }

      case ViaContext(resolvableThing) => resolveInContext(resolvableThing)

      case BoundWithViaContext[S, A](resolvableFnT, viaContext) => {
        val a = resolvableFnT.run
        val b: Trampoline[T[S]] = a(this)
        val c: T[S] = b.run
        val d: T[A] = c.flatMap(resolve(viaContext))
        d
      }
    }
  }

  object ResolvableMonad extends Monad[Resolvable] {
    override def point[A](a: => A): Resolvable[A] = resolved(a.point[T])

    override def bind[A, B](ta: Resolvable[A])(fn: (A) => Resolvable[B]): Resolvable[B] =
      ta match {
        case ViaContext(resolvableThing) => Bound {
          Trampoline.delay { c =>
            Trampoline.done {
              c.resolveInContext(resolvableThing).flatMap(a => c.resolve(fn(a)))
            }
          }
        }

        case Resolved(v) => Bound {
          Trampoline.delay { c =>
            Trampoline.done {
              v.flatMap(a => c.resolve(fn(a)))
            }
          }
        }

        case Bound(resolvableFnT: Trampoline[(Context) => Trampoline[T[A]]]) => Bound {
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
*/