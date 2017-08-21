package mwittmann.resolvable.auto2

import scala.reflect.internal.Precedence
import scalaz.Scalaz._
import scalaz._
import scala.reflect.runtime.universe._

/*
trait AutoResolvableDefinition2[T[_]] {
  implicit val resolvedMonad: Monad[T]

  type ResolveViaContext[A]

  implicit val resolvedResolveViaContext: Monad[ResolveViaContext]

  sealed trait Resolvable[A] {
    val tag: TypeTag[_]

    def flatMap[B](fn: A => Resolvable[B]): Resolvable[B] = ResolvableMonad.bind(this)(fn)

    def map[B](fn: A => B): Resolvable[B] = ResolvableMonad.map(this)(fn)
  }

  case class FromResolved[A](resolved: T[A]) extends Resolvable[A] {
    override val tag = typeTag[this.type]
  }
  def fromResolved[A](resolved: T[A]): Resolvable[A] = FromResolved(resolved)

  case class ResolvableViaContext[A](resolvableThing: ResolveViaContext[A]) extends Resolvable[A] {
    override val tag = typeTag[this.type]
  }
  def resolvableViaContext[A](resolvableThing: ResolveViaContext[A]): Resolvable[A] = ResolvableViaContext(resolvableThing)


  //def resolveInContext[A](resolveViaContext: ResolveViaContext[A]): T[A]
  def resolveInContext[A](resolveViaContext: ResolveViaContext[A]): Resolvable[A]

  def resolve[A](resolvable: Resolvable[A]): T[A] = resolvable match {
    case FromResolved(resolved) => resolved
    case ResolvableViaContext(resolvableThing) => resolveInContext(resolvableThing)
  }

  object ResolvableMonad extends Monad[Resolvable] {
    override def point[A](a: => A): Resolvable[A] = fromResolved(a.point[T])

    override def bind[A, B](ta: Resolvable[A])(fn: (A) => Resolvable[B]): Resolvable[B] =
      ta match {
        case ResolvableViaContext(resolvableThing) => {
          fn match {
            //case fn: (A => ResolvableViaContext)

            case fn: (A => ResolvableViaContext[B]) => {
              val x1: ResolveViaContext[B] = resolvableThing.flatMap(a => {
                val x2: ResolveViaContext[B] = fn(a).resolvableThing
                x2
              })
              ResolvableViaContext(x1)
            }

            case otherFn: (A => Resolvable[B]) => {
              val x1: T[A] = resolveInContext(resolvableThing)
              val x2: T[B] = x1.map(otherFn).flatMap(resolve)
              FromResolved(x2)
            }
          }

//          val x1: T[A] = resolveInContext(resolvableThing)
//          val x2: T[Resolvable[B]] = x1.map(fn)
//          x2.
//
//          FromResolved(x2)
        }

        case FromResolved(v) => {
          val x2: T[B] = v.map(fn).flatMap(resolve)
          FromResolved(x2)
        }
      }
  }
}
*/