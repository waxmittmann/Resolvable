package scalazanddoobie.cray.p3

import doobie.free.connection

import scalaz.concurrent.{Future, Task}
import doobie.imports._

import scalaz._
import Scalaz._
import scala.util.Try


sealed trait Context {
  def cioToTask[A](cio: ConnectionIO[A]): Task[A]
}

sealed trait Taskable[A] {
  def execute(context: Context): Task[A]

  def flatMap[B](fn: A => Taskable[B]): Taskable[B] =
    TaskableMonad.bind(this)(fn)

  def map[B](fn: A => B): Taskable[B] =
    TaskableMonad.map(this)(fn)

/* Manual ways of defining map in terms of flatmap, just cause it's cool */
//    flatMap((a: A) => {
//      TaskableMonad.point(fn(a))
//    })

//    flatMap(fn.andThen((b: B) => TaskableMonad.point(b)))

}

case class FromTask[A](task: Task[A]) extends Taskable[A] {
  override def execute(context: Context): Task[A] = task
}

case class FromConnectionIO[A](cio: ConnectionIO[A]) extends Taskable[A] {
  override def execute(context: Context): Task[A] = context.cioToTask(cio)
}

case class FromContextResolvable[A](resolvable: Context => Task[A]) extends Taskable[A] {
  override def execute(context: Context): Task[A] = resolvable(context)
}

object TaskableMonad extends Monad[Taskable] {
  override def bind[A, B](ta: Taskable[A])(fn: (A) => Taskable[B]): Taskable[B] =
    FromContextResolvable { context =>
      ta
        .execute(context)
        .flatMap(a => fn(a).execute(context))
    }

  override def point[A](a: => A): Taskable[A] =
    FromContextResolvable { _ => Task.point(a) }
}

object Main {

  val transactor = DriverManagerTransactor[Task](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql:miles",
    user = "miles",
    pass = "miles"
  )

  def main(args: Array[String]): Unit = {

    val context = new Context {
      override def cioToTask[A](cio: ConnectionIO[A]): Task[A] = transactor.trans { cio }
    }

    val fromTask = FromTask(Task.point(1))
    val fromCio = FromConnectionIO(2.point[ConnectionIO])

    val fromContextResolvable =
      for {
        a <- fromTask
        b <- fromCio
      } yield a + b

    println(fromContextResolvable.execute(context).unsafePerformSync)

  }

}