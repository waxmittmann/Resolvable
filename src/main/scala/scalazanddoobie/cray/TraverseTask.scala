package scalazanddoobie.cray

import scalaz._
import Scalaz._
import scalaz.Free.Trampoline
import scalaz.concurrent.Task

object TraverseTask {

  trait Context {
    def resolveThing[A](resolvableThing: ResolvableThing[A]): Task[A]

    def resolve[A](resolvable: Resolvable[A]): Task[A] = resolvable match {
      case ResolvableViaContext(resolvableThing) => resolveThing(resolvableThing)

      case DirectlyResolvable(v) => v

      case BoundResolvableViaContext(fn) => {
        val fn2 = fn.run
        val r = fn2(this)
        r.run
      }
    }
  }

//  trait ResolvableThing[A]
  case class ResolvableThing[A](v: A)

  sealed trait Resolvable[A]
  case class ResolvableViaContext[A](resolvableThing: ResolvableThing[A]) extends Resolvable[A]
  case class DirectlyResolvable[A](v: Task[A]) extends Resolvable[A]
  case class BoundResolvableViaContext[A](fn: Trampoline[(Context) => Trampoline[Task[A]]]) extends Resolvable[A]

  def resolvableViaContext[A](resolvableThing: ResolvableThing[A]): Resolvable[A] = ResolvableViaContext(resolvableThing)

  def directlyResolvable[A](v: Task[A]): Resolvable[A] = DirectlyResolvable(v)

  def boundResolvableViaContext[A](fn: Trampoline[(Context) => Trampoline[Task[A]]]): Resolvable[A] = BoundResolvableViaContext(fn)

  implicit object ResolvableMonad extends Monad[Resolvable] {
    override def point[A](a: => A): Resolvable[A] = DirectlyResolvable(Task(a))

    override def bind[A, B](fa: Resolvable[A])(f: (A) => Resolvable[B]): Resolvable[B] = {
      fa match {
        case ResolvableViaContext(resolvableThing) => BoundResolvableViaContext {
          Trampoline.delay { c =>
            Trampoline.delay(c.resolveThing(resolvableThing).flatMap(a => c.resolve(f(a))))
          }
        }

        case DirectlyResolvable(v) => BoundResolvableViaContext {
          Trampoline.delay { c =>
            Trampoline.delay(v.flatMap(a => c.resolve(f(a))))
          }
        }

        case BoundResolvableViaContext(resolvableFnT: Trampoline[(Context) => Trampoline[Task[A]]]) => BoundResolvableViaContext {
          resolvableFnT.flatMap((f1: (Context => Trampoline[Task[A]])) => {
            Trampoline.done {
              (c: Context) =>
                Trampoline.suspend {
                  f1(c).map { _.flatMap { a =>
                      c.resolve(f(a))
                    }
                  }
                }
            }
          })
        }
      }
    }
  }

  def main(args: Array[String]): Unit = {
//    val nestedOption =
//      (0 to 1000000).foldLeft(some(1)) { case (resolvable: Option[Int], _: Int) => {
//        resolvable.flatMap(v => Some(v + 1))
//      }}
//
//    println(nestedOption)

    val context = new Context {
      override def resolveThing[A](resolvableThing: ResolvableThing[A]): Task[A] = Task(resolvableThing.v)
    }


    val nestedResolvable =
      (0 to 100000).foldLeft(directlyResolvable(Task.point(1))) { case (resolvable: Resolvable[Int], _: Int) => {
        ResolvableMonad.bind(resolvable)((v: Int) => directlyResolvable(Task.point(v+1)))
        //resolvable.flatMap((v: Int) => directlyResolvable(Task.point(v+1)))
      }}

    println(context.resolve(nestedResolvable).unsafePerformSync)


  }


}
