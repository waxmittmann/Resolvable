package mwittmann.packagefold

import scala.util.Try

object FlatMap {

  sealed trait FlatMap[A, B] {
    def run: Option[B]
  }

  case class DirectFlatMap[A, B](a: Option[A], fn: A => Option[B]) extends FlatMap[A, B] {
    def run: Option[B] = a.flatMap(fn)
  }

  case class NestedFlatMap[A, B](fa: FlatMap[_, A], fn: A => Option[B]) extends FlatMap[A, B] {
    def run: Option[B] = {
      fa.run.flatMap(fn)
    }
  }


  def main(args: Array[String]): Unit = {
    DirectFlatMap[String, Int](
      DirectFlatMap[Int, String](Some(1), (v: Int) => Some((v + 1).toString)).run,
      (v: String) => Try(v.toInt).fold(_ => None, v => Some(v+1))
    ).run


    val fm =
      List(1, 2, 3, 4, 5).foldLeft(DirectFlatMap[Int, Int](Some(0), (a: Int) => Some(a)) : FlatMap[Int, Int])((flatmap, value) => {
        NestedFlatMap(flatmap, v => Some(v + value)) : FlatMap[Int, Int]
      })
    println(fm.run)
  }

}
