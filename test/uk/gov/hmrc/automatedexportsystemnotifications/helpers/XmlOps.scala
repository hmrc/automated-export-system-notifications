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

package uk.gov.hmrc.automatedexportsystemnotifications.helpers

import java.io.StringReader
import scala.util.Using
import scala.util.Using.Releasable
import scala.xml.*

/** XML normalization method used in [[https://github.com/hmrc/automated-export-system/]] */
object XmlOps:
  private def trimKeepOneSpace(str: String): NodeSeq =
    val stringBuilder: StringBuilder = StringBuilder()

    str.trim.foreach(c =>
      if !c.isWhitespace then stringBuilder.append(c)
      else if stringBuilder.nonEmpty && stringBuilder.last != ' ' then stringBuilder.append(' ')
    )

    stringBuilder.result() match
      case ""  => NodeSeq.Empty
      case res => Text(res)

  private def trimSpaces(n: Node): NodeSeq =
    n match
      case Text(text) => trimKeepOneSpace(text)
      case _          => n

  def normalize(nodes: NodeSeq): NodeSeq =
    def combineConsecutiveText(nodes: NodeSeq): NodeSeq =
      val normalizedNodes: Seq[Node] = nodes.foldRight(List.empty) {
        case (Text(textNode), Text(textAcc) :: nodes) =>
          Text(textNode + textAcc) :: nodes
        case (node, acc) =>
          node :: acc
      }

      normalizedNodes

    def rec(n: Node): NodeSeq =
      n match
        case Group(nodes) =>
          combineConsecutiveText(nodes.flatMap(rec)).flatMap(trimSpaces)
        case Elem(str, str1, data, binding, child*) =>
          val children: NodeSeq = child.flatMap(rec)

          val combinedChildren: NodeSeq = combineConsecutiveText(children).flatMap(trimSpaces)

          Elem(str, str1, data, binding, false, combinedChildren*)
        case a: Atom[_] => Text(a.text)
        case _ => n

    combineConsecutiveText(nodes.flatMap(rec))
  end normalize

  private def loadXml[R: Releasable](resource: => R, toInputSource: R => InputSource): Either[Throwable, NodeSeq] =
    Using(resource)(r => XML.load(toInputSource(r))).toEither

  def loadXmlFromString(xmlString: String): Either[Throwable, NodeSeq] =
    loadXml(StringReader(xmlString), Source.fromReader)

  def loadXmlFromPath(path: String): Either[Throwable, NodeSeq] =
    loadXml(getClass.getResourceAsStream(path), Source.fromInputStream)
end XmlOps
