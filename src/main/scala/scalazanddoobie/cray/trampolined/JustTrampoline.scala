//package scalazanddoobie.cray.trampolined
//
//import scalaz.Monad
//
//object JustTrampoline {
//
//  sealed trait Trampoline[A]
//
//  class Done[A](val v: A) extends Trampoline[A]
//
//  class More[A](val v: () => A) extends Trampoline[A]
//
//  implicit object TrampolineMonad extends Monad[Trampoline] {
//    override def point[A](a: => A): Trampoline[A] = new Done(a)
//
//    override def bind[A, B](fa: Trampoline[A])(f: (A) => Trampoline[B]): Trampoline[B] = fa match {
//      case d: Done[A] =>  f(d.v)
//      case m: More[A] =>  new More(() => {
//
//      })
//    }
//
//  }
//
//  def badAdd(v: Int): Int = {
//    if (v == 0)
//      0
//    else
//      badAdd(v-1) + 1
//  }
//
//  def goodAdd(v: Int): Trampoline[Int] = {
//    if (v == 0)
//      new Done(0)
//    else
//      goodAdd(v-1) + 1
//  }
//
//
//  def main(args: Array[String]): Unit = {
//    println(badAdd(10))
//    println(badAdd(10000000))
//
//
//  }
//}
