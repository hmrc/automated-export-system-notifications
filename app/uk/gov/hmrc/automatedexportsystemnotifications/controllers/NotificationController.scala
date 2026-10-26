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

import cats.Functor
import cats.data.{EitherT, NonEmptyList}
import cats.implicits.catsSyntaxEither
import cats.syntax.all.toBifunctorOps
import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.automatedexportsystemnotifications.config.EisBearerTokenConfig
import uk.gov.hmrc.automatedexportsystemnotifications.controllers.actions.{CorrelatedPayloadActionRefiner, NotificationsXmlPayloadActionRefiner}
import uk.gov.hmrc.automatedexportsystemnotifications.controllers.filters.{RequiredAuthorizationFilter, RequiredHeadersFilter}
import uk.gov.hmrc.automatedexportsystemnotifications.errors.{ExpectedXmlBodyError, NotificationsServiceError, RequestError, XmlFailedReadError}
import uk.gov.hmrc.automatedexportsystemnotifications.models.*
import uk.gov.hmrc.automatedexportsystemnotifications.models.aesRequest.{NotificationError, NotificationPayload, NotificationStatus}
import uk.gov.hmrc.automatedexportsystemnotifications.models.errors.{ErrorMapper, ErrorSource, UnifiedErrorCode}
import uk.gov.hmrc.automatedexportsystemnotifications.models.http.CustomHeaderNames
import uk.gov.hmrc.automatedexportsystemnotifications.models.requests.{AckBody, IncomingPayload}
import uk.gov.hmrc.automatedexportsystemnotifications.parsers.EisPayloadXmlParser
import uk.gov.hmrc.automatedexportsystemnotifications.responses.NotificationsErrorResponse.toErrorResponse
import uk.gov.hmrc.automatedexportsystemnotifications.services.AesService
import uk.gov.hmrc.automatedexportsystemnotifications.validation.RequiredHeaderValidator
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{Clock, OffsetDateTime}
import java.util.Locale
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex
import scala.xml.NodeSeq

@Singleton()
class NotificationController @Inject() (
  cc:                                   ControllerComponents,
  notificationsXmlPayloadActionRefiner: NotificationsXmlPayloadActionRefiner,
  correlatedPayloadActionRefiner:       CorrelatedPayloadActionRefiner,
  requiredHeadersFilter:                RequiredHeadersFilter,
  requiredAuthorizationFilter:          RequiredAuthorizationFilter[EisBearerTokenConfig],
  aesService:                           AesService,
  clock:                                Clock
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging:
  import NotificationController.eitherTNotificationsServiceErrorWiden

  import scala.language.implicitConversions

  private def isXmlContentType(request: RequestHeader): Boolean = {
    val applicationXmlRegex: Regex = "application/.*\\+xml.*".r

    request.contentType.exists { value =>
      val lowerCaseValue: String = value.toLowerCase(Locale.ENGLISH)

      lowerCaseValue.startsWith("text/xml") || lowerCaseValue
        .startsWith("application/xml") || applicationXmlRegex.matches(lowerCaseValue)
    }
  }

  private lazy val xmlBodyParser: BodyParser[NodeSeq] =
    parse.using { request =>
      if isXmlContentType(request) then parse.xml
      else
        parse.error {
          val error: RequestError = ExpectedXmlBodyError

          val context: Seq[String] =
            Seq(error.message)
              ++ request.headers
                .get(CustomHeaderNames.X_CORRELATION_ID)
                .map(value => s"correlationId=$value")

          logger.warn(context.mkString(" "))

          Future.successful(ExpectedXmlBodyError.toErrorResponse.toResult)
        }
    }

  private def notificationAction: Action[NodeSeq] =
    Action(xmlBodyParser)
      .andThen(notificationsXmlPayloadActionRefiner)
      .andThen(correlatedPayloadActionRefiner)
      .async { implicit req =>
        val result: EitherT[Future, NotificationsServiceError, Unit] =
          readNotificationXml(req.xml, req.correlationId)
            .flatMap(payload =>
              val notification: NotificationPayload = toNotification(payload, req.correlationId)

              aesService
                .sendNotification(
                  correlationId = notification.correlationId,
                  eori = notification.eori,
                  mrn = notification.mrn,
                  status = notification.status,
                  errors = notification.errors
                )
            )

        result.fold(
          error => error.toErrorResponse.toResult,
          _ => Results.NoContent
        )
      }

  def notification: EssentialAction =
    val requiredNotificationHeadersFilter: EssentialFilter = requiredHeadersFilter(
      NonEmptyList.one(RequiredHeaderValidator.xCorrelationId)
    )

    Filters(notificationAction, requiredNotificationHeadersFilter, requiredAuthorizationFilter)

  private def readNotificationXml(xml: NodeSeq, correlationId: String): EitherT[Future, XmlFailedReadError, IncomingPayload] =
    EitherT(Future.successful(EisPayloadXmlParser.parse(xml).leftMap { error =>
      val xmlFailedReadError: XmlFailedReadError = XmlFailedReadError(error)

      logger.warn(s"${xmlFailedReadError.message}. correlationId=$correlationId")

      xmlFailedReadError
    }))

  private def toNotification(in: IncomingPayload, correlationId: String): NotificationPayload = {
    val now = OffsetDateTime.now(clock)

    in match {
      case IncomingPayload.IE906(b) =>
        val payload = NotificationPayload(
          correlationId = correlationId,
          eori = b.eori,
          mrn = b.MRN,
          dateCreated = now,
          status = NotificationStatus.Rejected,
          errors = Some(b.FunctionalError.map: e =>
            val unified: UnifiedErrorCode = ErrorMapper.toUnified(ErrorSource.IE906, e.errorCode)
            NotificationError(
              code = unified.code,
              description = Some(unified.description),
              path = e.errorPointer,
              originalValue = e.originalAttribute
            ))
        )
        val unifiedCodes: String =
          payload.errors
            .getOrElse(Nil)
            .map(_.code)
            .distinct
            .mkString(",")
        logger.warn(s"Notification errors from EORI ${b.eori}, MRN ${b.MRN} with correlationId $correlationId:  $unifiedCodes")
        payload

      case IncomingPayload.IE917(b) =>
        val payload = NotificationPayload(
          correlationId,
          b.eori,
          b.MRN,
          now,
          NotificationStatus.Rejected,
          errors = Some(b.XmlError.map: e =>
            val unified: UnifiedErrorCode = ErrorMapper.toUnified(ErrorSource.IE917, e.errorCode)
            NotificationError(
              code = unified.code,
              description = Some(unified.description),
              path = e.errorPointer,
              originalValue = e.originalAttribute
            ))
        )
        val unifiedCodes: String =
          payload.errors
            .getOrElse(Nil)
            .map(_.code)
            .distinct
            .mkString(",")
        logger.warn(s"Notification errors from EORI ${b.eori}, MRN ${b.MRN} with correlationId $correlationId:  $unifiedCodes")
        payload

      case IncomingPayload.Ack(a) =>
        val status = a.ActionCode match {
          case AckBody.ActionCodes.ACKNOWLEDGED_AND_PROCESSED => NotificationStatus.Accepted
          case AckBody.ActionCodes.DIVERSION                  => NotificationStatus.Diversion
          case _                                              => NotificationStatus.Rejected
        }
        NotificationPayload(correlationId, a.eori, a.MRN, now, status, None)

    }
  }

object NotificationController:
  given eitherTNotificationsServiceErrorWiden[F[_]: Functor, A <: NotificationsServiceError, B]
    : Conversion[EitherT[F, A, B], EitherT[F, NotificationsServiceError, B]] =
    et => et.leftWiden
