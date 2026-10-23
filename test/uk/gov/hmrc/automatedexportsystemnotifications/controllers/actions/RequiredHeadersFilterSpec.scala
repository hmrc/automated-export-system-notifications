package uk.gov.hmrc.automatedexportsystemnotifications.controllers.actions

import cats.data.NonEmptyList
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import play.api.http.MimeTypes
import play.api.libs.streams.Accumulator
import play.api.mvc.{AnyContentAsEmpty, EssentialAction, Result, Results}
import play.api.test.Helpers.writeableOf_AnyContentAsEmpty
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import uk.gov.hmrc.automatedexportsystemnotifications.controllers.filters.RequiredHeadersFilter
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.XmlOps
import uk.gov.hmrc.automatedexportsystemnotifications.validation.{HeaderValidator, RequiredHeaderValidator}

import scala.concurrent.Future
import scala.xml.NodeSeq

class RequiredHeadersFilterSpec extends AnyFreeSpecLike, Matchers, EitherValues, DefaultAwaitTimeout:
  object TestData:
    val successfulAction: EssentialAction =
      _ => Accumulator.done(Results.Ok("success"))

  given materializer: Materializer = Materializer(ActorSystem())

  val requiredHeadersFilter: RequiredHeadersFilter = RequiredHeadersFilter()

  "RequiredHeadersFilter" - {

    "should return an EssentialFilter" - {

      "that allows a RequestHeader with all headers present" in {
        val headerValidator1: RequiredHeaderValidator =
          HeaderValidator("header1").required

        val headerValidator2: RequiredHeaderValidator =
          HeaderValidator("header2").required

        val headerValidator3: RequiredHeaderValidator =
          HeaderValidator("header3").required

        val filteredAction: EssentialAction = requiredHeadersFilter(
          NonEmptyList.of(
            headerValidator1,
            headerValidator2,
            headerValidator3
          )
        )(TestData.successfulAction)

        val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest()
            .withHeaders(
              "header1" -> "value1",
              "header2" -> "value2",
              "header3" -> "value3"
            )

        val result: Future[Result] = Helpers.call(filteredAction, request)

        Helpers.status(result)          shouldBe Helpers.OK
        Helpers.contentAsString(result) shouldBe "success"
      }

      "that rejects a RequestHeader with missing required headers" in {
        val headerValidator1: RequiredHeaderValidator =
          HeaderValidator("header1").required

        val headerValidator2: RequiredHeaderValidator =
          HeaderValidator("header2").required

        val headerValidator3: RequiredHeaderValidator =
          HeaderValidator("header3").required

        val filteredAction: EssentialAction = requiredHeadersFilter(
          NonEmptyList.of(
            headerValidator1,
            headerValidator2,
            headerValidator3
          )
        )(TestData.successfulAction)

        val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest()
            .withHeaders(
              "header3" -> "value3"
            )

        val headerFailedValidationErrorResponseXml =
          <NotificationsErrorResponse>
            <status>400</status>
            <message>Header validation failed</message>
            <errors>
              <error>
                <message>Missing required header header1</message>
              </error>
              <error>
                <message>Missing required header header2</message>
              </error>
            </errors>
          </NotificationsErrorResponse>

        val result: Future[Result] = Helpers.call(filteredAction, request)

        val resultContent: String  = Helpers.contentAsString(result)
        val resultXml:     NodeSeq = XmlOps.loadXmlFromString(resultContent).value

        Helpers.status(result)      shouldBe Helpers.BAD_REQUEST
        Helpers.contentType(result) shouldBe Some(MimeTypes.XML)
        XmlOps.normalize(resultXml) shouldBe XmlOps.normalize(headerFailedValidationErrorResponseXml)
      }
    }
  }
