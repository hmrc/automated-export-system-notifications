package uk.gov.hmrc.automatedexportsystemnotifications.errors

import cats.data.NonEmptyList
import play.api.http.Status

sealed abstract class NotificationsServiceError:
  val message:   String
  val status:    Int
  val exception: Option[Throwable]

enum RequestError(val message: String, val status: Int) extends NotificationsServiceError:
  val exception: Option[Throwable] = None

  case ExpectedXmlBodyError extends RequestError("The body of the request is not valid XML", Status.UNSUPPORTED_MEDIA_TYPE)

sealed abstract class ValidationError

enum HeaderValidationError(val headerName: String, val message: String) extends ValidationError:
  case MissingRequiredHeader(
    override val headerName: String
  ) extends HeaderValidationError(headerName, s"Missing required header $headerName")
  case InvalidHeader(
    override val headerName: String,
    reason:                  String
  ) extends HeaderValidationError(headerName, s"Header $headerName is invalid: $reason")

final case class HeaderFailedValidationError(errors: NonEmptyList[HeaderValidationError]) extends NotificationsServiceError:
  val message:   String            = "Header validation failed"
  val status:    Int               = Status.BAD_REQUEST
  val exception: Option[Throwable] = None
