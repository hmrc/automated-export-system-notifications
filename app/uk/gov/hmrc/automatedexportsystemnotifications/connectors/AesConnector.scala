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

package uk.gov.hmrc.automatedexportsystemnotifications.connectors

import cats.data.EitherT
import play.api.Logging
import play.api.http.{ContentTypes, Status}
import play.api.libs.ws.writeableOf_String
import uk.gov.hmrc.automatedexportsystemnotifications.config.AppConfig
import uk.gov.hmrc.automatedexportsystemnotifications.errors.ConnectorError
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class AesConnector @Inject() (
  appConfig: AppConfig,
  http:      HttpClientV2
)(implicit ec: ExecutionContext)
    extends Logging:

  private val endpoint  = appConfig.aesEndPoint
  private val authToken = appConfig.aesToken

  def send(xml: String, correlationId: String)(implicit hc: HeaderCarrier): EitherT[Future, ConnectorError, Unit] =
    EitherT(
      http
        .post(url"$endpoint")
        .setHeader(
          "Authorization" -> authToken,
          "Content-Type"  -> ContentTypes.XML
        )
        .withBody(xml)
        .execute[HttpResponse]
        .map { resp =>
          if (resp.status == Status.NO_CONTENT) Right(())
          else {
            logger.warn(
              s"AES request succeeded, but got unexpected status: ${resp.status} " +
                "method=POST " +
                s"url=$endpoint " +
                s"correlationId=$correlationId " +
                s"body=${resp.body}"
            )

            Left(ConnectorError.UnexpectedStatusError("POST", endpoint, resp.status))
          }
        }
        .recover { case NonFatal(t) =>
          logger.warn(
            s"AES request failed " +
              s"method=POST " +
              s"url=$endpoint " +
              s"correlationId=$correlationId",
            t
          )

          Left(
            ConnectorError.UnexpectedError(
              "POST",
              endpoint,
              t
            )
          )
        }
    )
