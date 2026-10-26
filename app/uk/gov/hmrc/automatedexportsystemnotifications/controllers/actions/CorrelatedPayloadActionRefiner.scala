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

package uk.gov.hmrc.automatedexportsystemnotifications.controllers.actions

import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.automatedexportsystemnotifications.controllers.actions.requests.{CorrelatedNotificationRequest, NotificationsXmlPayloadRequest}
import uk.gov.hmrc.automatedexportsystemnotifications.errors.{MissingCorrelationIdHeader, RequestError}
import uk.gov.hmrc.automatedexportsystemnotifications.models.http.CustomHeaderNames
import uk.gov.hmrc.automatedexportsystemnotifications.responses.NotificationsErrorResponse.toErrorResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class CorrelatedPayloadActionRefiner @Inject() ()(using protected val executionContext: ExecutionContext)
    extends ActionRefiner[NotificationsXmlPayloadRequest, CorrelatedNotificationRequest],
      Logging:
  protected def refine[A](request: NotificationsXmlPayloadRequest[A]): Future[Either[Result, CorrelatedNotificationRequest[A]]] =
    Future.successful(
      request.headers.get(CustomHeaderNames.X_CORRELATION_ID) match
        case Some(value) =>
          val headersToLog: Seq[(String, String)] =
            request.headers.headers.filterNot { case (name, _) =>
              name.equalsIgnoreCase("Authorization")
            }

          val payload: NodeSeq = request.xml

          logger.debug(
            s"Received notification from HMRC. " +
              s"Headers: ${headersToLog.map { case (name, value) => s"$name=$value" }.mkString(", ")}. " +
              s"Payload: ${request.xml}"
          )

          Right(CorrelatedNotificationRequest(payload, correlationId = value, request))
        case None =>
          val error: RequestError = MissingCorrelationIdHeader

          logger.warn(error.message)

          Left(error.toErrorResponse.toResult)
    )
