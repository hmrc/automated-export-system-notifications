package uk.gov.hmrc.automatedexportsystemnotifications.xml

import scala.xml.*

object XmlWriter:
  def elem(label: String, children: NodeSeq): Elem =
    elemWithScope(label, TopScope, children)

  private def elemWithScope(label: String, scope: NamespaceBinding, children: NodeSeq): Elem =
    Elem(scope.prefix, label, Null, scope, false, children*)

  def optElem(label: String, children: NodeSeq): NodeSeq =
    if children.isEmpty then NodeSeq.Empty
    else elem(label, children)

  def list[T](label: String, f: (T, String) => NodeSeq, l: List[T]): NodeSeq =
    l.flatMap(f(_, label))
