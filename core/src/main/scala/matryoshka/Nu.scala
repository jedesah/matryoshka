/*
 * Copyright 2014–2016 SlamData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package matryoshka

import scalaz._, Scalaz._

/** This is for coinductive (potentially infinite) recursive structures, models
  * the concept of “codata”, aka, the “greatest fixed point”.
  */
sealed abstract class Nu[F[_]] {
  type A
  val a: A
  val unNu: A => F[A]
}
object Nu {
  def apply[F[_], B](f: B => F[B], b: B): Nu[F] =
    new Nu[F] {
      type A = B
      val a = b
      val unNu = f
    }

  implicit def nuMatryoshka[F[_]]:
      Recursive.Aux[Nu[F], F] with Corecursive.Aux[Nu[F], F] =
    new Recursive[Nu[F]] with Corecursive[Nu[F]] {
      type Base[A] = F[A]

      def project(t: Nu[F]) = t.unNu(t.a).map(Nu(t.unNu, _))

      // FIXME: ugh, shouldn’t have to redefine `colambek` in here?
      def embed(t: F[Nu[F]]) = ana(t)(_ ∘ project)
      override def ana[A](a: A)(f: A => F[A]) = Nu(f, a)
    }

  implicit def equal[F[_]](
    implicit F: Equal ~> λ[α => Equal[Based[Nu[F]]#Base[α]]]):
      Equal[Nu[F]] =
    Recursive.equal[Nu[F]]

  implicit def show[F[_]](
    implicit  F: Show ~> λ[α => Show[Based[Nu[F]]#Base[α]]]):
      Show[Nu[F]] =
    Recursive.show[Nu[F]]
}
