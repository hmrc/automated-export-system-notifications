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

package uk.gov.hmrc.automatedexportsystemnotifications.controllers

import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.automatedexportsystemnotifications.controllers.actions.ValidatedRequestAction
import uk.gov.hmrc.automatedexportsystemnotifications.models.*
import uk.gov.hmrc.automatedexportsystemnotifications.models.aesRequest.{NotificationError, NotificationPayload, NotificationStatus}
import uk.gov.hmrc.automatedexportsystemnotifications.models.requests.{AckBody, IncomingPayload}
import uk.gov.hmrc.automatedexportsystemnotifications.parsers.EisPayloadXmlParser
import uk.gov.hmrc.automatedexportsystemnotifications.services.AesService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.time.{Clock, OffsetDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
@Singleton()
class NotificationController @Inject() (
  cc:                     ControllerComponents,
  validatedRequestAction: ValidatedRequestAction,
  aesService:             AesService,
  clock:                  Clock
)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with Logging {

  def handle: Action[AnyContent] = validatedRequestAction.async { implicit req =>
    process(req).map {
      case Right(_) =>
        NoContent
      case Left(err: ClientError) =>
        logger.warn(err.message)
        BadRequest(err.message)
      case Left(err: DownstreamError) =>
        logger.error(err.message)
        BadGateway("Failed to call AES backend service")
    }
  }

  private def process(req: Request[AnyContent])(implicit hc: HeaderCarrier): Future[Either[AppError, Unit]] =
    extractXml(req) match {
      case None =>
        Future.successful(Left(ClientError("Missing XML payload")))

      case Some(xml) =>
        EisPayloadXmlParser.parse(xml) match {
          case Left(parseErr) =>
            Future.successful(Left(ClientError(s"Invalid inbound payload: $parseErr")))

          case Right(incoming) =>
            val out = toNotification(incoming, req)
            aesService
              .sendNotification(
                correlationId = out.correlationId,
                eori = out.eori,
                mrn = out.mrn,
                status = out.status,
                errors = out.errors
              )
              .map(_.left.map(e => DownstreamError(s"AES call failed: $e")))
        }
    }

  private def extractXml(req: Request[AnyContent]): Option[String] =
    req.body.asXml
      .map(_.toString)
      .orElse(req.body.asText)
      .orElse(req.body.asRaw.flatMap(_.asBytes().map(_.utf8String)))

  private def toNotification(in: IncomingPayload, req: RequestHeader): NotificationPayload = {
    val correlationId = req.headers.get("x-correlation-id").getOrElse("")
    val now           = OffsetDateTime.now(clock)

    in match {
      case IncomingPayload.Ack(a) =>
        val status = a.ActionCode match {
          case AckBody.ActionCodes.ACKNOWLEDGED_AND_PROCESSED => NotificationStatus.Accepted
          case AckBody.ActionCodes.DIVERSION                  => NotificationStatus.Diversion
          case _                                              => NotificationStatus.Rejected
        }
        NotificationPayload(correlationId, a.eori, a.MRN, now, status, Nil)

      case IncomingPayload.IE906(b) =>
        NotificationPayload(
          correlationId,
          b.eori,
          b.MRN,
          now,
          NotificationStatus.Rejected,
          b.FunctionalError.map(e => NotificationError(e.errorCode.toString, Some(e.errorReason), Some(e.errorPointer)))
        )

      case IncomingPayload.IE917(b) =>
        NotificationPayload(
          correlationId,
          b.eori,
          b.MRN,
          now,
          NotificationStatus.Rejected,
          b.XmlError.map(e => NotificationError(e.errorCode.toString, Some(e.errorText), Some(e.errorPointer)))
        )
    }
  }

  sealed trait AppError { def message: String }
  case class ClientError(message: String) extends AppError
  case class DownstreamError(message: String) extends AppError

  private implicit def hc(implicit request: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter.fromRequestAndSession(request, request.session)
}
