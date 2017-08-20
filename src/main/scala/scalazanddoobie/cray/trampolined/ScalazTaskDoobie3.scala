package scalazanddoobie.cray.trampolined

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
  implicit val resolvedApplicative: Applicative[T]
  val resolvedTraverse: Traverse[T]
//  implicit val resolvedMonad: Traverse[T]

  implicit val trampolineMonad: Monad[Trampoline] = Monad[Trampoline]
  implicit val trampolineApp: Applicative[Trampoline] = Applicative[Trampoline]

  sealed trait Resolvable[A] {
    def flatMap[B](fn: A => Resolvable[B]): Resolvable[B] = ResolvableMonad.bind(this)(fn)

    def map[B](fn: A => B): Resolvable[B] = ResolvableMonad.map(this)(fn)
  }

  trait Context {
    def resolve[A](cio: Resolvable[A]): Trampoline[T[A]]
  }

  case class FromResolved[A](resolved: T[A]) extends Resolvable[A]
  def fromResolved[A](resolved: T[A]): Resolvable[A] = FromResolved(resolved)

//  case class FromContextResolvable[A](contextFn: Context => Trampoline[T[A]]) extends Resolvable[A]
//  def fromContextResolvable[A](contextFn: Context => Trampoline[T[A]]): Resolvable[A] = FromContextResolvable(contextFn)

//  case class FromBind[S, A](rs: Resolvable[S], fn: S => Resolvable[A]) extends Resolvable[A]
//  def fromBind[S, A](crs: Resolvable[S], fn: S => Resolvable[A]): Resolvable[A] = FromBind(crs, fn)
  case class FromBind[S, A](fn: (Resolvable[S] => Trampoline[T[S]]) => Trampoline[T[Resolvable[A]]]) extends Resolvable[A]
  def fromBind[S, A](fn: (Resolvable[S] => Trampoline[T[S]]) => Trampoline[T[Resolvable[A]]]): Resolvable[A] = FromBind(fn)


  case class FromTrampolineTResolvable[A](t: Trampoline[Task[Resolvable[A]]]) extends Resolvable[A]
  def fromTrampolineTResolvable[A](t: Trampoline[Task[Resolvable[A]]]): Resolvable[A] = FromTrampolineTResolvable(t)

  object ResolvableMonad extends Monad[Resolvable] {
    override def bind[A, B](ta: Resolvable[A])(fn: (A) => Resolvable[B]): Resolvable[B] =
      FromBind { fnC: (Resolvable[A] => Trampoline[T[A]]) =>
        val s: Trampoline[T[A]] = Trampoline.suspend(fnC(ta))
        val t: Trampoline[T[Resolvable[B]]] = s.map((ta: T[A]) => ta.map((a: A) => fn(a)))
        t
      }

//      FromBind(ta, fn)

//      FromContextResolvable { context =>
//        val r1: Trampoline[T[A]] = context.resolve(ta)
//
//        val r2b: Trampoline[T[Trampoline[T[B]]]] = r1.map(ta => ta.map(a => context.resolve(fn(a))))
//
//        val r3b: T[Trampoline[Trampoline[T[B]]]] = r2b.sequenceU
//
//        val r4b: T[Trampoline[T[B]]] = r3b.map(_.flatMap(Predef.identity))
//
//        val r5b: Trampoline[T[T[B]]] = ToTraverseOps(r4b)(resolvedTraverse).sequenceU
//
//        val r6b: Trampoline[T[B]] = r5b.map(_.flatMap(Predef.identity))
//
//        r6b
//      }


//      FromContextResolvable { context =>
//        context
//          .resolve(ta)
//          .flatMap { (r: T[A]) =>
//            val r2: T[Nothing] = r.flatMap { (a: A) =>
//              val r3: Trampoline[T[B]] = context.resolve(fn(a))
//              r3
//            }
//            r2
//          }
//          //.flatMap((a: T[A]) => context.resolve(fn(a)))
//      }

    override def point[A](a: => A): Resolvable[A] =
      FromResolved { resolvedMonad.point(a) }
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
    implicit val resolvedApplicative: Applicative[Task] = Task.taskInstance
//    implicit val resolvedTraverse: Traverse[Task] = Traverse[Task]
    implicit val resolvedTraverse: Traverse[Task] = Traverse[Task]

    class ContextImpl(trans: Transactor[Task]) extends Context {
      override def resolve[A](resolvable: Resolvable[A]): Trampoline[Task[A]] = resolvable match {
        case FromResolved(task)                   => Trampoline.done(task)
//        case FromContextResolvable(f)             => f(this)

        case FromBind(fn)                     => {
          //fn: (Resolvable[S] => Trampoline[S]) => Trampoline[Resolvable[A]]

          //(Resolvable[S] => Trampoline[T[S]])
          val r: Trampoline[Task[Resolvable[A]]] = fn(resolve)
          val r2: Trampoline[Task[Trampoline[Task[A]]]] = r.map(_.map(resolvable => resolve(resolvable)))
//          val r3: Task[Trampoline[Trampoline[Task[A]]]] = r2.sequence
          val r3: Task[Trampoline[Trampoline[Task[A]]]] = ToTraverseOps(r2).sequence
          val r4: Task[Trampoline[Task[A]]] = r3.map(_.flatMap(Predef.identity))
//          val r5: Trampoline[Task[Task[A]]] = r4.sequenceU
          val r5: Trampoline[Task[Task[A]]] = ToTraverseOps(r4).sequenceU
          val r6: Trampoline[Task[A]] = r5.map(_.flatMap(Predef.identity))
          r6

          //fromTrampolineTResolvable(r)
        }


        //        case FromBind(rs, fn)                     => {
//          Trampoline.suspend {
//            val r1: Trampoline[Task[A]] = resolve(rs)
//
//            val r2b: Trampoline[Task[Trampoline[Task[A]]]] = r1.map(ta => ta.map(a => context.resolve(fn(a))))
//
//            val r3b: Task[Trampoline[Trampoline[Task[A]]]] = r2b.sequenceU
//
//            val r4b: Task[Trampoline[Task[A]]] = r3b.map(_.flatMap(Predef.identity))
//
//            val r5b: Trampoline[Task[Task[A]]] = ToTraverseOps(r4b)(resolvedTraverse).sequenceU
//
//            val r6b: Trampoline[Task[A]] = r5b.map(_.flatMap(Predef.identity))
//
//            r6b
//          }
//            val r1: Trampoline[T[A]] = context.resolve(ta)
//
//            val r2b: Trampoline[T[Trampoline[T[B]]]] = r1.map(ta => ta.map(a => context.resolve(fn(a))))
//
//            val r3b: T[Trampoline[Trampoline[T[B]]]] = r2b.sequenceU
//
//            val r4b: T[Trampoline[T[B]]] = r3b.map(_.flatMap(Predef.identity))
//
//            val r5b: Trampoline[T[T[B]]] = ToTraverseOps(r4b)(resolvedTraverse).sequenceU
//
//            val r6b: Trampoline[T[B]] = r5b.map(_.flatMap(Predef.identity))
//        }
        case FromConnectionIO(cio)                => Trampoline.done(trans.trans { cio })
      }
    }

    case class FromConnectionIO[A](cio: ConnectionIO[A]) extends Resolvable[A]
  }

  import ResolvableToTask._

  val context = new ContextImpl(transactor)

  def main(args: Array[String]): Unit = {

//  import scalazanddoobie.cray.isolartrampoline.Main.ResolvableToTask

  //    val x = Task(Some("a"))
//    val y = x.sequenceU

    val fromTask = FromResolved(Task.point(1))
    val fromCio = FromConnectionIO(2.point[ConnectionIO])

//    val fromContextResolvable =
//      for {
//        a <- fromTask
//        b <- fromCio
//      } yield a + b

    //println(context.resolve(fromContextResolvable).unsafePerformSync)

    val nestedResolvable: Resolvable[Int] =
      (0 to 100000).foldLeft(fromResolved(Task.point(1))) { case (resolvable: Resolvable[Int], _: Int) => {
        resolvable.flatMap(v => fromResolved(Task.point(v+1)))
      }}


     println(context.resolve(nestedResolvable).run)



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
