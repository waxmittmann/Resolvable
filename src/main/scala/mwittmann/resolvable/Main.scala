package mwittmann.resolvable

import doobie.imports
import doobie.imports.{ConnectionIO, DriverManagerTransactor, Transactor}

import scalaz._, Scalaz._
import scalaz.Monad
import scalaz.concurrent.Task

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
      override def resolveInContext[A](resolveViaContext: ConnectionIO[A]): Task[A] =
        trans.trans { resolveViaContext }
    }
  }

  import ResolvableToTask._

  val context = new ContextImpl(transactor)

  def main(args: Array[String]): Unit = {

    val fromTask = FromResolved(Task.point(1))
    val fromCio = ResolvableViaContext(2.point[ConnectionIO])

    val nestedResolvable =
      (0 to 100000).foldLeft(fromResolved(Task.point(1))) { case (resolvable: Resolvable[Int], _: Int) => {
        resolvable.flatMap(v => fromResolved(Task.point(v+1)))
      }}

    println(context.resolve(nestedResolvable).unsafePerformSync)

  }
}
