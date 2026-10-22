package uk.gov.hmrc.automatedexportsystemnotifications.helpers

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
end XmlOps
