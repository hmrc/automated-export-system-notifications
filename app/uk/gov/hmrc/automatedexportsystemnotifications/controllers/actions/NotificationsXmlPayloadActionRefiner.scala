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
import play.api.mvc.{ActionRefiner, AnyContentAsXml, Request, Result}
import uk.gov.hmrc.automatedexportsystemnotifications.controllers.actions.requests.NotificationsXmlPayloadRequest
import uk.gov.hmrc.automatedexportsystemnotifications.errors.{ExpectedXmlBodyError, RequestError}
import uk.gov.hmrc.automatedexportsystemnotifications.models.http.CustomHeaderNames
import uk.gov.hmrc.automatedexportsystemnotifications.responses.NotificationsErrorResponse.toErrorResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class NotificationsXmlPayloadActionRefiner @Inject() ()(using protected val executionContext: ExecutionContext)
    extends ActionRefiner[Request, NotificationsXmlPayloadRequest]
    with Logging:
  protected def refine[A](request: Request[A]): Future[Either[Result, NotificationsXmlPayloadRequest[A]]] =
    Future.successful(
      request.body match
        case AnyContentAsXml(xml) =>
          Right(NotificationsXmlPayloadRequest(xml, request))
        case xml: NodeSeq =>
          Right(NotificationsXmlPayloadRequest(xml, request))
        case _ =>
          val error: RequestError = ExpectedXmlBodyError

          val context: Seq[String] =
            Seq(error.message)
              ++ request.headers
                .get(CustomHeaderNames.X_CORRELATION_ID)
                .map(value => s"correlationId=$value")

          logger.warn(context.mkString(" "))

          Left(error.toErrorResponse.toResult)
    )
