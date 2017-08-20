package scalazanddoobie.cray.abstracted

import doobie.free.connection
import doobie.imports

import scalaz.concurrent.{Future, Task}
import doobie.imports._

import scalaz._
import Scalaz._
import scala.util.Try


class AbstractedTaskable[T[_]]()(implicit tm: Monad[T]) {

  trait Context {
    //def cioToTask[A](cio: ConnectionIO[A]): T[A]
    def taskableToTask[A](cio: ConnectionIO[A]): T[A]
  }

  trait Taskable[A] {
    def execute(context: Context): T[A]

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

  case class FromTask[A](task: T[A]) extends Taskable[A] {
    override def execute(context: Context): T[A] = task
  }

  case class FromContextResolvable[A](resolvable: Context => T[A]) extends Taskable[A] {
    override def execute(context: Context): T[A] = resolvable(context)
  }

  object TaskableMonad extends Monad[Taskable] {
    override def bind[A, B](ta: Taskable[A])(fn: (A) => Taskable[B]): Taskable[B] =
      FromContextResolvable { context =>
        ta
          .execute(context)
          .flatMap(a => fn(a).execute(context))
      }

    override def point[A](a: => A): Taskable[A] =
      FromContextResolvable { _ => a.point[T] }
  }

}

object Main {

  val transactor: imports.Transactor[Task] = DriverManagerTransactor[Task](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql:miles",
    user = "miles",
    pass = "miles"
  )

  val taskable = new AbstractedTaskable[Task]()

  case class FromConnectionIO[A](cio: ConnectionIO[A]) extends taskable.Taskable[A] {
    override def execute(context: taskable.Context): Task[A] = context.taskableToTask(cio)
  }

  object Context extends taskable.Context {
    override def taskableToTask[A](cio: ConnectionIO[A]): Task[A] = transactor.trans { cio }
  }

  def main(args: Array[String]): Unit = {

    val fromTask = taskable.FromTask(Task.point(1))
    val fromCio = FromConnectionIO(2.point[ConnectionIO])

    val fromContextResolvable =
      for {
        a <- fromTask
        b <- fromCio
      } yield a + b

    println(fromContextResolvable.execute(Context).unsafePerformSync)
  }

}
