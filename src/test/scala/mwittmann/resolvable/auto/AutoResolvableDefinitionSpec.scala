package mwittmann.resolvable.auto

import doobie.imports
import doobie.imports.{ConnectionIO, DriverManagerTransactor, Transactor}
import org.specs2.Specification

import scalaz._, Scalaz._
import scalaz.concurrent.Task


object AutoResolvableDefinitionSpec extends Specification { override val is =
  s2"""
    ConnectionIO should
       resolve when shallowly flatmapped $checkShallowlyNestedConnectionIOResolves
       resolve when deeply flatmapped $checkDeeplyNestedConnectionIOResolves

    auto.ResolvableDefinition should
      work with a deeply nested mix of resolvables and resolved values $checkResolvableWorksWithShallowlyNestedResolvableViaContextAndFromResolved
      work with a shallowly nested mix of resolvables and resolved values $checkResolvableWorksWithDeeplyNestedResolvableViaContextAndFromResolved
  """
  //produce resolvable that is stack-safe $checkResolvableStackSafe

  val transactor: imports.Transactor[Task] = DriverManagerTransactor[Task](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql:miles",
    user = "miles",
    pass = "miles"
  )

  case class AutoResolvableToTask(transactor: Transactor[Task]) extends AutoResolvableDefinition[Task] {
    implicit val resolvedMonad: Monad[Task] = Task.taskInstance
    override type ResolveViaContext[A] = ConnectionIO[A]

    override def resolveInContext[A](resolveViaContext: ConnectionIO[A]): Task[A] =
      transactor.trans { resolveViaContext }
  }

  object AutoResolvableToTaskImpl$ extends AutoResolvableToTask(transactor)

  import AutoResolvableToTaskImpl$._

  def checkResolvableStackSafe = {
    val nestedResolvable =
      (0 to 100000).foldLeft(fromResolved(Task.point(1))) { case (resolvable: Resolvable[Int], _: Int) => {
        resolvable.flatMap(v => fromResolved(Task.point(v+1)))
      }}

    AutoResolvableToTaskImpl$.resolve(nestedResolvable).unsafePerformSync shouldEqual 100002
  }

  def checkResolvableWorksWithShallowlyNestedResolvableViaContextAndFromResolved= {
    val t0 = System.nanoTime()
    val nestedResolvable =
      (0 to 50).foldLeft(resolvableViaContext(1.point[ConnectionIO])) { case (resolvable: Resolvable[Int], v: Int) => {
        resolvable.flatMap(v => resolvableViaContext((v+1).point[ConnectionIO]))
      }}

    AutoResolvableToTaskImpl$.resolve(nestedResolvable).unsafePerformSync shouldEqual 502
    val t1 = System.nanoTime()
    println("(Resolvable shallow) Elapsed time: " + ((t1 - t0) / 1000000000.0) + "s")
    ok
  }

  def checkShallowlyNestedConnectionIOResolves = {
    val t0 = System.nanoTime()
    val nestedResolvable =
      (0 to 50).foldLeft(1.point[ConnectionIO]) { case (resolvable: ConnectionIO[Int], v: Int) => {
        resolvable.flatMap(v => (v + 1).point[ConnectionIO])
      }}

    transactor.trans { nestedResolvable }.unsafePerformSync shouldEqual 502
    val t1 = System.nanoTime()
    println("(ConnectionIO shallow) Elapsed time: " + ((t1 - t0) / 1000000000.0) + "s")
    ok
  }

  def checkResolvableWorksWithDeeplyNestedResolvableViaContextAndFromResolved= {
    val t0 = System.nanoTime()
    val nestedResolvable =
      (0 to 10000).foldLeft(resolvableViaContext(1.point[ConnectionIO])) { case (resolvable: Resolvable[Int], v: Int) => {
        resolvable.flatMap(v => resolvableViaContext((v+1).point[ConnectionIO]))
      }}

    AutoResolvableToTaskImpl$.resolve(nestedResolvable).unsafePerformSync shouldEqual 10002
    val t1 = System.nanoTime()
    println("(Resolvable deep) Elapsed time: " + ((t1 - t0) / 1000000000.0) + "s")
    ok
  }


  def checkDeeplyNestedConnectionIOResolves = {
    val t0 = System.nanoTime()
    val nestedResolvable =
      (0 to 10000).foldLeft(1.point[ConnectionIO]) { case (resolvable: ConnectionIO[Int], v: Int) => {
        resolvable.flatMap(v => (v + 1).point[ConnectionIO])
      }}

    transactor.trans { nestedResolvable }.unsafePerformSync shouldEqual 10002
    val t1 = System.nanoTime()
    println("(ConnectionIO deep) Elapsed time: " + ((t1 - t0) / 1000000000.0) + "s")
    ok
  }
}
