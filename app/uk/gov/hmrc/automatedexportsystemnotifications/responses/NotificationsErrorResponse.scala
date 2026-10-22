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
      case error: NotificationsServiceValidationError =>
        error.toXml(label)

final case class NotificationsServiceValidationError(message: String) extends NotificationsServiceErrorResponseError:
  override def toXml(label: String): NodeSeq =
    val children: Elem =
      <message>{message}</message>

    XmlWriter.elem(label, children)

object NotificationsServiceValidationError:
  def fromHeaderValidationError(error: HeaderValidationError): NotificationsServiceValidationError =
    NotificationsServiceValidationError(error.message)

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
            errors.toList.map(NotificationsServiceValidationError.fromHeaderValidationError)
          )
        case _ =>
          NotificationsErrorResponse(status, message, errors = Nil)
end NotificationsErrorResponse
