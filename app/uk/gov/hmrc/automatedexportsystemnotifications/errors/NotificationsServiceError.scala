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

package uk.gov.hmrc.automatedexportsystemnotifications.errors

import cats.data.NonEmptyList
import play.api.http.Status

sealed abstract class NotificationsServiceError:
  val message:   String
  val status:    Int
  val exception: Option[Throwable]

sealed abstract class RequestError extends NotificationsServiceError

case object ExpectedXmlBodyError extends RequestError:
  val message:   String            = "The body of the request is not valid XML"
  val status:    Int               = Status.UNSUPPORTED_MEDIA_TYPE
  val exception: Option[Throwable] = None

case object MissingCorrelationIdHeader extends RequestError:
  val message:   String            = "Request X-Correlation-Id header is missing"
  val status:    Int               = Status.BAD_REQUEST
  val exception: Option[Throwable] = None

final case class InvalidAuthorizationHeader(reasons: List[String]) extends RequestError:
  val message: String =
    (Seq(s"Request Authorization header is invalid.") :+ reasons.mkString(", ")).mkString(" ")
  val status:    Int               = Status.UNAUTHORIZED
  val exception: Option[Throwable] = None

final case class XmlFailedReadError(error: String) extends NotificationsServiceError:
  val message:   String            = s"XML failed deserialization. $error"
  val status:    Int               = Status.UNPROCESSABLE_ENTITY
  val exception: Option[Throwable] = None

sealed abstract class ValidationError

enum HeaderValidationError(val headerName: String, val message: String) extends ValidationError:
  case MissingRequiredHeader(
    override val headerName: String
  ) extends HeaderValidationError(headerName, s"Missing required header")
  case InvalidHeader(
    override val headerName: String,
    reason:                  Option[String]
  ) extends HeaderValidationError(
        headerName,
        (Seq(s"Invalid header.") ++ reason).mkString(" ")
      )

final case class HeaderFailedValidationError(errors: NonEmptyList[HeaderValidationError]) extends RequestError:
  val message:   String            = "Header validation failed"
  val status:    Int               = Status.BAD_REQUEST
  val exception: Option[Throwable] = None

enum AesServiceError(val message: String, val status: Int) extends NotificationsServiceError:
  val exception: Option[Throwable] = None

  case NotificationFailedError(override val message: String, override val status: Int) extends AesServiceError(message, status)

enum ConnectorError(
  val details:   String,
  val method:    String,
  val url:       String,
  val status:    Int,
  val exception: Option[Throwable]
) extends NotificationsServiceError:
  val message: String = s"Error on $method request to $url. $details"

  case UnexpectedStatusError(
    override val method: String,
    override val url:    String,
    incomingStatus:      Int
  ) extends ConnectorError(s"Response status $incomingStatus was not expected", method, url, Status.BAD_GATEWAY, None)

  case UnexpectedError(
    override val method: String,
    override val url:    String,
    ex:                  Throwable
  ) extends ConnectorError(s"Unexpected error encountered while performing request", method, url, Status.INTERNAL_SERVER_ERROR, Some(ex))
