package scalazanddoobie.cray.trampolined
/*
import scala.annotation.tailrec
import scalaz.Monad

object TrampoliningTest {

  /*
        FromContextResolvable { context =>
        context
          .resolve(ta)
          .flatMap(a => context.resolve(fn(a)))
      }

   */




//  object ResolvableMonad extends Monad[Resolvable] {
//    override def bind[A, B](ta: Resolvable[A])(fn: (A) => Resolvable[B]): Resolvable[B] =
//      FromContextResolvable { context =>
//        context
//          .resolve(ta)
//          .flatMap(a => context.resolve(fn(a)))
//      }
//
//    override def point[A](a: => A): Resolvable[A] =
//      FromContextResolvable { _ => a.point[T] }
//  }


  object NestedMonad extends Monad[Nested] {
//    override def bind[A, B](ta: Nested[A])(fn: (A) => Nested[B]): Nested[B] =
//      NestedOp { context =>
//        val a = context.resolve(ta)
//        val nb = fn(a)
//        val b = context.resolve(nb)
//        b
//      }


    def bind[A, B](ta: Nested[A])(fn: (A) => Nested[B]): NestedOpTrampoline[B] =
      NestedOpTrampoline { context =>
        val trampA: Trampoline[A] = context.resolveOne(ta)
        trampA match {
          case Done(a) =>
          case More(na) =>
        }

//        fn(a) match {
//          case nl @ NestedLeaf(v: B)         => nl
//          case NestedOpTrampoline(ca: (Context) => Nested[B])     => NestedOpTrampoline((c: Context) => ca(c))
//        }
      }

    override def point[A](a: => A): Nested[A] =
      NestedLeaf(a)
  }

  sealed trait Trampoline[A]
  case class Done[A](v: A) extends Trampoline[A]
  case class More[A](v: Nested[A]) extends Trampoline[A]

  class Context {
    @tailrec
    final def resolve[A](nested: Nested[A]): A = nested match {
      case NestedLeaf(a) => a
      case NestedOpTrampoline(fn) => {
        val na: Nested[A] = fn(this)
        resolve(na)
      }
    }

    @tailrec
    final def resolveT[A](nested: Trampoline[A]): A = nested match {
      case Done(v: A) => v
      case More(v: Nested[A]) => resolveT(resolveOne(v))
    }


    final def resolveOne[A](nested: Nested[A]): Trampoline[A] = nested match {
      case NestedLeaf(a: A) => Done(a)
      case NestedOpTrampoline(fn) => {
        val na: Nested[A] = fn(this)
        More(na)
      }
    }

//    final def resolveTramp[A](tramp: Trampoline[A]): Trampoline[A] = tramp match {
//      case Done(v: A) =>
//      case More(v: Nested[A]) =>
//    }

  }

  sealed trait Nested[A]
//  {
//    def resolve(context: Context): A
//  }

  case class NestedLeaf[A](a: A) extends Nested[A]
//  {
//    override def resolve(context: Context): A = a
//  }

//  case class NestedOp[A](ca: Context => A) extends Nested[A] {
//    override def resolve(context: Context): A = ca(context)
//  }

  case class NestedOpTrampoline[A](ca: Context => Nested[A]) extends Nested[A]
//  {
//    override def resolve(context: Context): A = context[A](ca)
//  }

  //NestedOp(1 + NestedOp(1 + NestedOp(1 + NestedOp(1 + NestedLeaf(1)))))
  //NestedOp(v => NestedOp(1 + NestedOp(1 + NestedOp(1 + NestedLeaf(1)))))



  def main(args: Array[String]): Unit = {

    val nestedResolvable =
      (0 to 100000).foldLeft(NestedLeaf(1): Nested[Int]) { case (nested: Nested[Int], _: Int) => {
        NestedMonad.bind(nested)(v => NestedLeaf(v+1) : Nested[Int])
      }}

//    println(new Context().resolve(nestedResolvable))
    println(new Context().resolveT(More(nestedResolvable)))
//    println(nestedResolvable.resolve(new Context()))
//    println(new COntnestedResolvable.resolve(new Context()))

  }

}
*/