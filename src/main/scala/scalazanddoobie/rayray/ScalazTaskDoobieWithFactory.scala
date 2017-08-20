package scalazanddoobie.rayray

import doobie.free.connection
import doobie.imports

import scalaz.concurrent.{Future, Task}
import doobie.imports._

import scalaz._
import Scalaz._
import scala.annotation.tailrec
import scala.util.Try
import scalaz.Free.Trampoline

trait ResolvableInstance[T[_]] {
  implicit val resolvedMonad: Monad[T]

  type ResolveViaContext[A]

  sealed trait Resolvable[A] {
    def flatMap[B](fn: A => Resolvable[B]): Resolvable[B] = ResolvableMonad.bind(this)(fn)

    def map[B](fn: A => B): Resolvable[B] = ResolvableMonad.map(this)(fn)
  }

  trait Context {
    def resolveB[A](resolveViaContext: ResolveViaContext[A]): T[A]

    def resolve[A](cio: Resolvable[A]): T[A] = cio match {
      case FromResolved(resolved) => resolved
//      case ResolvableViaContext(resolvableThing) =>

      case BoundResolvableViaContext(fn) => {
        val fn2 = fn.run
        val r = fn2(this)
        r.run
      }
    }
  }

  case class FromResolved[A](resolved: T[A]) extends Resolvable[A]
  def fromResolved[A](resolved: T[A]): Resolvable[A] = FromResolved(resolved)

  case class ResolvableViaContext[A](resolvableThing: ResolveViaContext[A]) extends Resolvable[A]
  def resolvableViaContext[A](contextFn: ResolveViaContext[A]): Resolvable[A] = ResolvableViaContext(contextFn)

//  case class FromContextResolvable[A](contextFn: ResolvableViaContext[A]) extends Resolvable[A]
//  def fromContextResolvable[A](contextFn: ResolvableViaContext[A]): Resolvable[A] = FromContextResolvable(contextFn)

  case class BoundResolvableViaContext[A](resolvableFnT: Trampoline[(Context) => Trampoline[T[A]]]) extends Resolvable[A]
  def boundResolvableViaContext[A](resolvableFnT: Trampoline[(Context) => Trampoline[T[A]]]) = BoundResolvableViaContext(resolvableFnT)

  object ResolvableMonad extends Monad[Resolvable] {
    override def point[A](a: => A): Resolvable[A] = fromResolved(a.point[T])

    override def bind[A, B](ta: Resolvable[A])(fn: (A) => Resolvable[B]): Resolvable[B] =
      ta match {
        case ResolvableViaContext(resolvableThing) => BoundResolvableViaContext {
          Trampoline.delay { c =>
            Trampoline.done { c.resolveB(resolvableThing).flatMap(a => c.resolve(fn(a))) }
          }
        }

        case FromResolved(v) => BoundResolvableViaContext {
          Trampoline.delay { c =>
            Trampoline.done { v.flatMap(a => c.resolve(fn(a))) }
          }
        }

        case BoundResolvableViaContext(resolvableFnT: Trampoline[(Context) => Trampoline[T[A]]]) => BoundResolvableViaContext {
          Trampoline.suspend {
            resolvableFnT.flatMap((f1: (Context => Trampoline[T[A]])) => {
              Trampoline.delay {
                (c: Context) =>
                  Trampoline.suspend {
                    val x1: Trampoline[T[B]] = f1(c).map { (ta: T[A]) =>
                      ta.flatMap { a =>
                        c.resolve(fn(a))
                      }
                    }
                    x1
                  }
              }
            })
          }
        }

//        case BoundResolvableViaContext(resolvableFnT: Trampoline[(Context) => T[A]]) => BoundResolvableViaContext {
//          resolvableFnT.flatMap((f1: (Context => T[A])) => {
//            Trampoline.delay {
//              (c: Context) => {
//                f1(c).flatMap { a =>
//                  c.resolve(fn(a))
//                }
//              }
//            }
//          })
//        }
      }

//      FromContextResolvable { context =>
//        context
//          .resolve(ta)
//          .flatMap(a => context.resolve(fn(a)))
//      }
//
//    override def point[A](a: => A): Resolvable[A] =
//      FromContextResolvable { _ => a.point[T] }
  }
}

object Main {

  val transactor: imports.Transactor[Task] = DriverManagerTransactor[Task](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql:miles",
    user = "miles",
    pass = "miles"
  )

  object ResolvableToTask extends ResolvableInstance[Task] {
    implicit val resolvedMonad: Monad[Task] = Task.taskInstance
    override type ResolveViaContext[A] = ConnectionIO[A]

    class ContextImpl(trans: Transactor[Task]) extends Context {
//      override def resolve[A](resolvable: Resolvable[A]): Task[A] = resolvable match {
//        case FromResolved(task)                   => task
//        case FromContextResolvable(f)             => f(this)
//        case FromConnectionIO(cio)                => trans.trans { cio }
//      }
      override def resolveB[A](resolveViaContext: ConnectionIO[A]): Task[A] =
      trans.trans { resolveViaContext }
    }
  }

  import ResolvableToTask._

  val context = new ContextImpl(transactor)

  def main(args: Array[String]): Unit = {

    val fromTask = FromResolved(Task.point(1))
    val fromCio = ResolvableViaContext(2.point[ConnectionIO])

    val fromContextResolvable =
      for {
        a <- fromTask
        b <- fromCio
      } yield a + b

    println(context.resolve(fromContextResolvable).unsafePerformSync)

    val nestedResolvable =
      (0 to 100000).foldLeft(fromResolved(Task.point(1))) { case (resolvable: Resolvable[Int], _: Int) => {
        resolvable.flatMap(v => fromResolved(Task.point(v+1)))
      }}

   println(context.resolve(nestedResolvable).unsafePerformSync)

//    val nestedOption =
//      (0 to 100000).foldLeft(some(1)) { case (resolvable: Option[Int], _: Int) => {
//        resolvable.flatMap(v => Some(v + 1))
//      }}
//
//    println(nestedOption)

  }

}
