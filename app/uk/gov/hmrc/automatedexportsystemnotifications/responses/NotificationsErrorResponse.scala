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

package uk.gov.hmrc.automatedexportsystemnotifications.responses

import play.api.http.Writeable
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.automatedexportsystemnotifications.errors.{HeaderFailedValidationError, HeaderValidationError, NotificationsServiceError}
import uk.gov.hmrc.automatedexportsystemnotifications.xml.XmlWriter

import scala.xml.{Elem, NodeSeq, TopScope}

final case class NotificationsErrorResponse(status: Int, message: String, errors: List[NotificationsServiceErrorResponseError]):
  def toXml: NodeSeq =
    val statusXml:  Elem    = <status>{status}</status>
    val messageXml: Elem    = <message>{message}</message>
    val errorsXml:  NodeSeq = XmlWriter.optElem(
      "errors",
      XmlWriter.list[NotificationsServiceErrorResponseError](
        "error",
        (error, label) => error.toXml(label),
        errors
      )
    )

    val children: NodeSeq =
      statusXml
        ++ messageXml
        ++ errorsXml

    XmlWriter.elem("NotificationsErrorResponse", children)

  def toResult(using Writeable[NodeSeq]): Result =
    Status(status)(toXml)

sealed abstract class NotificationsServiceErrorResponseError:
  def toXml(label: String): NodeSeq =
    this match
      case error: NotificationsServiceHeaderValidationError =>
        error.toXml(label)

final case class NotificationsServiceHeaderValidationError(
  headerName: String,
  message:    String
) extends NotificationsServiceErrorResponseError:
  override def toXml(label: String): NodeSeq =
    val headerNameXml: Elem =
      <headerName>{headerName}</headerName>

    val messageXml: Elem =
      <message>{message}</message>

    val children: NodeSeq =
      headerNameXml
        ++ messageXml

    XmlWriter.elem(label, children)

object NotificationsServiceHeaderValidationError:
  def fromHeaderValidationError(error: HeaderValidationError): NotificationsServiceHeaderValidationError =
    NotificationsServiceHeaderValidationError(error.headerName, error.message)

object NotificationsErrorResponse:
  extension (error: NotificationsServiceError)
    def toErrorResponse: NotificationsErrorResponse =
      val status:  Int    = error.status
      val message: String = error.message

      error match
        case HeaderFailedValidationError(errors) =>
          NotificationsErrorResponse(
            status,
            message,
            errors.toList.map(NotificationsServiceHeaderValidationError.fromHeaderValidationError)
          )
        case _ =>
          NotificationsErrorResponse(status, message, errors = Nil)
end NotificationsErrorResponse
