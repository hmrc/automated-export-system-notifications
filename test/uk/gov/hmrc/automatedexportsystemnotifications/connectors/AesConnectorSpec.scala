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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.BaseSpec
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future

class AesConnectorSpec extends BaseSpec:

  trait Setup:
    val mockHttp           = mock[HttpClientV2]
    val mockRequestBuilder = mock[RequestBuilder]
    when(mockAppConfig.aesToken).thenReturn("test-token")
    when(mockAppConfig.aesEndPoint).thenReturn("http://localhost:1111/notification")
    when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any[String])(any(), any(), any())).thenReturn(mockRequestBuilder)

  "AesConnector.send" - {

    "return Right(()) when downstream returns 204" in new Setup {
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(204, "")))

      val connector = new AesConnector(mockAppConfig, mockHttp)

      val result = connector.send("<notification/>").futureValue

      result shouldBe Right(())

      verify(mockRequestBuilder).setHeader(
        "Authorization" -> "test-token",
        "Content-Type"  -> "application/xml; charset=utf-8"
      )
      verify(mockRequestBuilder).withBody(any())(any(), any(), any())
    }

    "return Left when downstream returns non-204" in new Setup {
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(500, "some-error")))

      val connector = new AesConnector(mockAppConfig, mockHttp)

      val result = connector.send("<notification/>").futureValue

      result shouldBe Left("Expected 204, got 500. Body: some-error")
    }
  }
