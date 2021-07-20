package playground

import scala.language.postfixOps

/**
 * User: patricio
 * Date: 17/7/21
 * Time: 10:48
 */
/*object CategoryTheory extends App {

  class A
  class B
  class C

  // GoF | F, G and GoF are morphisms
  def f(a: A): B = new B // A -> B

  def g(b: B): C = new C // B -> C

  def gOf(a: A): C = g(f(a)) // A -> C | Composition

  // Composition Props
  /*
  1 . Associative
   */

  class D

  def h(c: C) = new D

  h(g(f(new A))) == (f _ andThen g andThen h)(new A)


  class Category[R, S] extends (R => S) {

    /**
     * Identity [X -> X]
     * @param x X
     * @return X
     */
    def id(x: R => S): R => S = x

    def o[T](f: Category[R,S], g: S => T): R => T =
      f andThen g

    override def apply(v1: R => S): S = {
      v1.a
    }
  }

  object Category

  val z = new Category[A]()
  val y = new Category[B]()
  val x = new Category[C]()

  z.o()



}*/
