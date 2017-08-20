package scalazanddoobie.cray.trampolined

/*

import doobie.free.connection
import doobie.imports

import scalaz.concurrent.{Future, Task}
import doobie.imports._

import scalaz._
import Scalaz._
import scala.annotation.tailrec
import scala.util.Try
import scalazanddoobie.cray.trampolined.Main.ResolvableToTask

trait ResolvableInstance[T[_]] {
  implicit val resolvedMonad: Monad[T]

  sealed trait Resolvable[A] {
    def flatMap[B](fn: A => Resolvable[B]): Resolvable[B] = ResolvableMonad.bind(this)(fn)

    def map[B](fn: A => B): Resolvable[B] = ResolvableMonad.map(this)(fn)
  }

  trait Context {
    def resolve[A](cio: Resolvable[A]): T[A]
  }

  case class FromResolved[A](resolved: T[A]) extends Resolvable[A]
  def fromResolved[A](resolved: T[A]): Resolvable[A] = FromResolved(resolved)

  //case class FromContextResolvable2[A, B](ta: Resolvable[A], f: A => Resolvable[B]) extends Resolvable[B]
  case class FromContextResolvable2[A, B](ta: Resolvable[A], f: A => Resolvable[B]) extends Resolvable[B]

//  case class FromContextResolvable[A](contextFn: Context => T[A]) extends Resolvable[A]
//  def fromContextResolvable[A](contextFn: Context => T[A]): Resolvable[A] = FromContextResolvable(contextFn)

  object ResolvableMonad extends Monad[Resolvable] {
    override def bind[A, B](ta: Resolvable[A])(fn: (A) => Resolvable[B]): Resolvable[B] =
      FromContextResolvable2(ta, fn)

//      FromContextResolvable { context =>
//        context
//          .resolve(ta)
//          .flatMap(a => context.resolve(fn(a)))
//      }

    override def point[A](a: => A): Resolvable[A] = ???
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

    class ContextImpl(trans: Transactor[Task]) extends Context {
      override def resolve[A](resolvable: Resolvable[A]): Task[A] = resolvable match {
        case FromResolved(task)                   => task


        case FromContextResolvable2(ra: ResolvableToTask.Resolvable[Any], f)        => {
          val a: ResolvableToTask.Resolvable[A] = f(ra)

        }
        case FromConnectionIO(cio)                => trans.trans { cio }
      }
    }

    case class FromConnectionIO[A](cio: ConnectionIO[A]) extends Resolvable[A]
  }

  import ResolvableToTask._

  val context = new ContextImpl(transactor)

  def main(args: Array[String]): Unit = {

    val fromTask = FromResolved(Task.point(1))
    val fromCio = FromConnectionIO(2.point[ConnectionIO])

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

//   println(context.resolve(nestedResolvable).unsafePerformSync)

//    val nestedOption =
//      (0 to 100000).foldLeft(some(1)) { case (resolvable: Option[Int], _: Int) => {
//        resolvable.flatMap(v => Some(v + 1))
//      }}
//
//
//    println(nestedOption)

  }

}
*/