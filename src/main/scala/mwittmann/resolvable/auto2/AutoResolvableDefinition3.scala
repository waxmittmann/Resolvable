package mwittmann.resolvable.auto2

import scala.reflect.runtime.universe._
import scalaz.Scalaz._
import scalaz._

import mwittmann.packagefold.FlatMapDefn

trait AutoResolvableDefinition2[T[_]] {
  implicit val resolvedMonad: Monad[T]

  type ResolvableThing[A]

  implicit val resolvedResolveViaContext: Monad[ResolvableThing]

  object FlatMap extends FlatMapDefn[Resolvable]
  import FlatMap._

  sealed trait Resolvable[A] {
    val tag: TypeTag[_]

    def flatMap[B](fn: A => Resolvable[B]): Resolvable[B] = ResolvableMonad.bind(this)(fn)

    def map[B](fn: A => B): Resolvable[B] = ResolvableMonad.map(this)(fn)
  }

  case class Resolved[A](resolved: T[A]) extends Resolvable[A] {
    override val tag = typeTag[this.type]
  }
  def resolved[A](resolved: T[A]): Resolvable[A] = Resolved(resolved)

  case class ViaContext[A](resolvableThing: ResolvableThing[A]) extends Resolvable[A] {
    override val tag = typeTag[this.type]
  }
  def viaContext[A](resolvableThing: ResolvableThing[A]): Resolvable[A] = ViaContext(resolvableThing)


  //def resolveInContext[A](resolveViaContext: ResolveViaContext[A]): T[A]
  def resolveInContext[A](resolveViaContext: ResolvableThing[A]): T[A]

  def mergeInContext[A](
    resolveViaContext: ResolvableThing[A]//,
//    resolvable: Resolvable[B]
  ): Resolvable[A]

  def resolve[A](resolvable: Resolvable[A]): T[A] = resolvable match {
    case Resolved(resolved) => resolved

      // Danger danger
    case ViaContext(resolvableThing) =>
      resolveInContext(
        resolvableThing
      )
  }

  def doit[A, B](
    resolvableThing: ResolvableThing[A],
    fn: A => Resolvable[B]
  ): Resolvable[B] = {

    val x1: ResolvableThing[Resolvable[B]] = resolvableThing.map { (a: A) =>
      val value: Resolvable[B] = fn(a)
      value match {
        case fr @ Resolved(_) => fr
        case ViaContext(resolvableThing2) => ViaContext(resolvableThing2)
      }
    }

  }

  object ResolvableMonad extends Monad[Resolvable] {
    override def point[A](a: => A): Resolvable[A] = resolved(a.point[T])

    override def bind[A, B](ta: Resolvable[A])(fn: (A) => Resolvable[B]): Resolvable[B] =
      ta match {
        case ViaContext(resolvableThing) => {
//          val x1: ResolveViaContext[Resolvable[B]] = resolvableThing.map { (a: A) =>
//            val value: Resolvable[B] = fn(a)
//            value match {
//              case fr @ FromResolved(_) => fr
//              case ResolvableViaContext(resolvableThing2) => ResolvableViaContext(resolvableThing2)
//            }
//          }

          val x: FlatMap.FlatMap[A, B] = flatMap(ta, fn)

          val x1: ResolvableThing[Resolvable[B]] = resolvableThing.map { (a: A) =>
            val value: Resolvable[B] = fn(a)
            value match {
              case fr @ Resolved(_) => fr
              case ViaContext(resolvableThing2) => ViaContext(resolvableThing2)
            }
          }


//          resolveInContext(resolvableThing) match {
//            case FromResolved(resolved) =>
//
//            case ResolvableViaContext(resolvableThing2) => resolvableThing2.flatMap(a => resolvableThing.flatMap)
//          }

//          fn match {
            //case fn: (A => ResolvableViaContext)

//            case fn: (A => ResolvableViaContext[B]) => {
//              val x1: ResolveViaContext[B] = resolvableThing.flatMap(a => {
//                val x2: ResolveViaContext[B] = fn(a).resolvableThing
//                x2
//              })
//              ResolvableViaContext(x1)
//            }
//
//            case otherFn: (A => Resolvable[B]) => {
//              val x1: T[A] = resolveInContext(resolvableThing)
//              val x2: T[B] = x1.map(otherFn).flatMap(resolve)
//              FromResolved(x2)
//            }
          }

//          val x1: T[A] = resolveInContext(resolvableThing)
//          val x2: T[Resolvable[B]] = x1.map(fn)
//          x2.
//
//          FromResolved(x2)
        }

        case Resolved(v) => {
          val x2: T[B] = v.map(fn).flatMap(resolve)
          Resolved(x2)
        }
      }
  }
}
