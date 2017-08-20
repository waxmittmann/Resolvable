package scalazanddoobie.cray.trampolined

object Trampoline2001 {

  def main(args: Array[String]): Unit = {



  }




  case class Resolvable[A](a: A)

  object Context {
    def resolve[A](resolvable: Resolvable[A]) =
      resolvable.a
  }

}
