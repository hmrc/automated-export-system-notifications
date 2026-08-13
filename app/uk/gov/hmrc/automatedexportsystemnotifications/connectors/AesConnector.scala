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

import play.api.Configuration
import play.api.http.ContentTypes
import play.api.libs.ws.writeableOf_String
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AesConnector @Inject() (
  config: Configuration,
  http:   HttpClientV2
)(implicit ec: ExecutionContext) {

  private val endpoint = "http://" +
    config.get[String]("microservice.services.aes.host") + ":" + config.get[String](
      "microservice.services.aes.port"
    ) + "/automated-export-system/notifications"
  private val authToken = config.get[String]("microservice.services.aes.bearer-token")

  def send(xml: String)(implicit hc: HeaderCarrier): Future[Either[String, Unit]] =
    http
      .post(url"$endpoint")
      .setHeader(
        "Authorization" -> authToken,
        "Content-Type"  -> ContentTypes.XML
      )
      .withBody(xml)
      .execute[HttpResponse]
      .map { resp =>
        if (resp.status == 204) Right(())
        else Left(s"Expected 204, got ${resp.status}. Body: ${resp.body}")
      }
}
