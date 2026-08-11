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

import scala.xml.Node

object XmlNav {

  def child(node: Node, local: String): Option[Node] =
    node.child.collectFirst {
      case n if n.label == local => n
    }

  def requiredNode(node: Node, local: String): Either[String, Node] =
    child(node, local).toRight(s"Missing required element: $local")

  def requiredText(node: Node, local: String): Either[String, String] =
    child(node, local)
      .map(_.text.trim)
      .filter(_.nonEmpty)
      .toRight(s"Missing required field: $local")
}
