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

package uk.gov.hmrc.automatedexportsystemnotifications.controllers.filters

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.MimeTypes
import play.api.libs.streams.Accumulator
import play.api.mvc.{AnyContentAsEmpty, EssentialAction, Result, Results}
import play.api.test.Helpers.writeableOf_AnyContentAsEmpty
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import uk.gov.hmrc.automatedexportsystemnotifications.config.BearerTokenConfig
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.XmlOps

import scala.concurrent.Future
import scala.xml.{Elem, NodeSeq}

class RequiredAuthorizationFilterSpec extends AnyFreeSpecLike, Matchers, EitherValues, DefaultAwaitTimeout, MockitoSugar:
  object TestData:
    val successfulAction: EssentialAction =
      _ => Accumulator.done(Results.Ok("success"))

    val token: String = "token"
  end TestData

  given materializer: Materializer = Materializer(ActorSystem())

  val bearerTokenConfig: BearerTokenConfig = new BearerTokenConfig("service"):
    def token: String = TestData.token

  val requiredAuthorizationFilter: RequiredAuthorizationFilter[BearerTokenConfig] =
    RequiredAuthorizationFilter(bearerTokenConfig)

  "RequiredAuthorizationFilter" - {

    "should allow a RequestHeader with a valid Authorization header" in {
      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest()
          .withHeaders(Helpers.AUTHORIZATION -> s"   Bearer    ${TestData.token}   ")

      val result: Future[Result] =
        Helpers.call(requiredAuthorizationFilter(TestData.successfulAction), request)

      Helpers.status(result)          shouldBe Helpers.OK
      Helpers.contentAsString(result) shouldBe "success"
    }

    "should reject a RequestHeader with a missing Authorization header" in {
      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest()

      val result: Future[Result] =
        Helpers.call(requiredAuthorizationFilter(TestData.successfulAction), request)

      val resultContent: String  = Helpers.contentAsString(result)
      val resultXml:     NodeSeq = XmlOps.loadXmlFromString(resultContent).value

      val invalidAuthorizationHeaderXml: Elem =
        <NotificationsErrorResponse>
          <status>401</status>
          <message>Request Authorization header is invalid. Missing required header</message>
        </NotificationsErrorResponse>

      Helpers.status(result)      shouldBe Helpers.UNAUTHORIZED
      Helpers.contentType(result) shouldBe Some(MimeTypes.XML)
      XmlOps.normalize(resultXml) shouldBe XmlOps.normalize(invalidAuthorizationHeaderXml)
    }

    "should reject a RequestHeader with an incorrect Authorization header format" in {
      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest()
          .withHeaders(Helpers.AUTHORIZATION -> s"NotBearer ${TestData.token}")

      val result: Future[Result] =
        Helpers.call(requiredAuthorizationFilter(TestData.successfulAction), request)

      val resultContent: String  = Helpers.contentAsString(result)
      val resultXml:     NodeSeq = XmlOps.loadXmlFromString(resultContent).value

      val invalidAuthorizationHeaderXml: Elem =
        <NotificationsErrorResponse>
          <status>401</status>
          <message>Request Authorization header is invalid. Invalid header. Incorrect format</message>
        </NotificationsErrorResponse>

      Helpers.status(result)      shouldBe Helpers.UNAUTHORIZED
      Helpers.contentType(result) shouldBe Some(MimeTypes.XML)
      XmlOps.normalize(resultXml) shouldBe XmlOps.normalize(invalidAuthorizationHeaderXml)
    }

    "should reject a RequestHeader where the Authorization token is invalid" in {
      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest()
          .withHeaders(Helpers.AUTHORIZATION -> s"Bearer invalidToken")

      val result: Future[Result] =
        Helpers.call(requiredAuthorizationFilter(TestData.successfulAction), request)

      val resultContent: String  = Helpers.contentAsString(result)
      val resultXml:     NodeSeq = XmlOps.loadXmlFromString(resultContent).value

      val invalidAuthorizationHeaderXml: Elem =
        <NotificationsErrorResponse>
          <status>401</status>
          <message>Request Authorization header is invalid. Invalid header.</message>
        </NotificationsErrorResponse>

      Helpers.status(result)      shouldBe Helpers.UNAUTHORIZED
      Helpers.contentType(result) shouldBe Some(MimeTypes.XML)
      XmlOps.normalize(resultXml) shouldBe XmlOps.normalize(invalidAuthorizationHeaderXml)
    }
  }
