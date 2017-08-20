package scalazanddoobie.cray.isolartrampoline

/*
import doobie.imports
import doobie.imports._

import scalaz.Free.Trampoline
import scalaz._
import Scalaz._
import scalaz.concurrent.Task
import scalaz.std.AllInstances


trait ResolvableInstance[T[_]] {
  implicit val resolvedMonad: Monad[T]
//  implicit val resolvedApplicative: Applicative[T]

  implicit val trampolineMonad: Monad[Trampoline] = Monad[Trampoline]
//  implicit val trampolineTraverse: Monad[Trampoline] = Monad[Trampoline]
  implicit val trampolineApp: Applicative[Trampoline] = Applicative[Trampoline]

//  val resolvedTraverse: Traverse[T]

//  assert(resolvedTraverse != null)

  sealed trait Resolvable[A] {
    def flatMap[B](fn: A => Resolvable[B]): Resolvable[B] = ResolvableMonad.bind(this)(fn)

    def map[B](fn: A => B): Resolvable[B] = ResolvableMonad.map(this)(fn)
  }

  trait Context {
    def resolve[A](cio: Resolvable[A]): Trampoline[T[A]]
  }

  case class FromResolved[A](resolved: T[A]) extends Resolvable[A]
  def fromResolved[A](resolved: T[A]): Resolvable[A] = FromResolved(resolved)

  case class FromContextResolvable[A](contextFn: Context => Trampoline[T[A]]) extends Resolvable[A]
  def fromContextResolvable[A](contextFn: Context => Trampoline[T[A]]): Resolvable[A] = FromContextResolvable(contextFn)

//  List(1).sequenceU

  object ResolvableMonad extends Monad[Resolvable] {
    override def bind[A, B](ta: Resolvable[A])(fn: (A) => Resolvable[B]): Resolvable[B] =
      FromContextResolvable { context =>
        Trampoline.suspend {
          val r1: Trampoline[T[A]] = context.resolve(ta)
          val r2: Trampoline[T[Resolvable[B]]] = r1.map(_.map(fn))
          val r3: Trampoline[T[Trampoline[T[B]]]] = r2.map(_.map(context.resolve))
//          val r4: Trampoline[Trampoline[T[T[B]]]] = r3.map(t => TraverseOps(t).sequenceU)
          val r4: Trampoline[Trampoline[T[T[B]]]] = r3.map(t => ToTraverseOps(t)(resolvedTraverse).sequenceU)

          //val r4: Trampoline[Trampoline[T[T[B]]]] = r3.map(t => ToTraverseOps(t)(resolvedTraverse).sequenceU)

//          val r4: Trampoline[Trampoline[T[T[B]]]] = r3.map(Task.)

//          val r3: T[T[Trampoline[T[B]]]] = r2.map(_.map(context.resolve))




          val r5: Trampoline[T[T[B]]] = r4.flatMap(Predef.identity)
          val r6: Trampoline[T[B]] = r5.map(_.flatMap(Predef.identity))
          r6
//          context
//            .resolve(ta)
//            .flatMap(ta =>
//              ta.flatMap(a =>
//                context.resolve(fn(a)))

//            .flatMap(a => context.resolve(fn(a)))
        }
      }

    override def point[A](a: => A): Resolvable[A] =
      FromResolved { a.point[T] }
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
    val resolvedApplicative: Applicative[Task] = Task.taskInstance
//    implicit val resolvedTraverse: Traverse[Task] = Traverse[Task]

    //    val resolvedTraverse: Traverse[Task] = Free.freeTraverse
    //val resolvedTraverse: Traverse[Task] = Task.

    class ContextImpl(trans: Transactor[Task]) extends Context {
      override def resolve[A](resolvable: Resolvable[A]): Trampoline[Task[A]] = resolvable match {
        case FromResolved(task)                   => Trampoline.done(task)
        case FromContextResolvable(f)             => f(this)
        case FromConnectionIO(cio)                => Trampoline.done(trans.trans { cio })
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

    println(context.resolve(fromContextResolvable).run.unsafePerformSync)

    val nestedResolvable =
      (0 to 100000).foldLeft(fromResolved(Task.point(1))) { case (resolvable: Resolvable[Int], _: Int) => {
        resolvable.flatMap(v => fromResolved(Task.point(v+1)))
      }}

   println(context.resolve(nestedResolvable).run.unsafePerformSync)

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