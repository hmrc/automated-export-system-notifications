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
