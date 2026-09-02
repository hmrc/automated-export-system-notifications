/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.automatedexportsystemnotifications.parsers
import scala.util.Try
import scala.xml.Node

object Helpers:
  val EisNs = "http://www.hmrc.gsi.gov.uk/eis"

  def requiredText(node: scala.xml.Node, label: String): Either[String, String] = {
    val value = (node \ label).text.trim
    if (value.nonEmpty) Right(value) else Left(s"Missing required field: $label")
  }

  def optionalText(node: scala.xml.Node, label: String): Option[String] = {
    val value = (node \ label).text.trim
    if (value.isEmpty) None else Some(value)
  }

  def requiredInt(node: scala.xml.Node, label: String): Either[String, Int] =
    requiredText(node, label).flatMap { s =>
      Try(s.toInt).toEither.left.map(_ => s"Field $label must be an Int, got: $s")
    }

  def requiredNode(node: Node, label: String): Either[String, Node] = {
    val n = (node \ label).headOption
    n.toRight(s"Missing required element: $label")
  }

  def sequence[A](xs: List[Either[String, A]]): Either[String, List[A]] =
    xs.foldRight(Right(Nil): Either[String, List[A]]) { (e, acc) =>
      for {
        h <- e
        t <- acc
      } yield h :: t
    }

  def descElems(node: Node, local: String, ns: String = EisNs): Seq[Node] =
    (node \\ "_").collect {
      case n: Node if n.label == local && n.namespace == ns => n
    }
