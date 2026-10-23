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

package uk.gov.hmrc.automatedexportsystemnotifications.validation

import cats.data.NonEmptyList
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.automatedexportsystemnotifications.errors.HeaderValidationError

class HeaderValidatorSpec extends AnyFreeSpecLike, Matchers, EitherValues:
  "HeaderValidator" - {

    ".validate" - {

      "should successfully validate the header" - {

        "when the validator contains no rules" in {
          val validator: HeaderValidator = HeaderValidator("name")

          val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest()

          validator.validate(request).toEither.value shouldBe ()
        }

        "when there are multiple rules but header is not present" in {
          val validator: HeaderValidator =
            HeaderValidator("header")
              .check(_.endsWith("ASD"), name => HeaderValidationError.InvalidHeader(name, Some("reason1")))
              .check(_.startsWith("ABC"), name => HeaderValidationError.InvalidHeader(name, Some("reason2")))

          val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest()

          validator.validate(request).toEither.value shouldBe ()
        }

        "when there are multiple rules and the header is present" in {
          val validator: HeaderValidator =
            HeaderValidator("header")
              .check(_.endsWith("suffix"), name => HeaderValidationError.InvalidHeader(name, Some("reason1")))
              .check(_.startsWith("Prefix"), name => HeaderValidationError.InvalidHeader(name, Some("reason2")))

          val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest()
              .withHeaders("Header" -> "Prefix suffix")

          validator.validate(request).toEither.value shouldBe ()
        }
      }

      "should fail to validate the header" - {

        "when there are multiple unsatisfied rules" in {
          val validator: HeaderValidator =
            HeaderValidator("header")
              .check(_.endsWith("ASD"), name => HeaderValidationError.InvalidHeader(name, Some("reason1")))
              .check(_.nonEmpty, name => HeaderValidationError.InvalidHeader(name, Some("reason2")))
              .check(_.startsWith("ABC"), name => HeaderValidationError.InvalidHeader(name, Some("reason3")))

          val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest()
              .withHeaders("header" -> "VALUE")

          validator.validate(request).toEither.left.value shouldBe
            NonEmptyList.of(
              HeaderValidationError.InvalidHeader("header", Some("reason1")),
              HeaderValidationError.InvalidHeader("header", Some("reason3"))
            )
        }
      }
    }
  }

  "RequiredHeaderValidator" - {

    ".validate" - {

      "should successfully validate the header" - {

        "when the validator contains no rules and the header is present" in {
          val validator: RequiredHeaderValidator =
            HeaderValidator("header").required

          val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest()
              .withHeaders("HEADEr" -> "value")

          validator.validate(request).toEither.value shouldBe ()
        }

        "when there are multiple rules and the header is present" in {
          val validator: RequiredHeaderValidator =
            HeaderValidator("header").required
              .check(_.endsWith("suffix"), name => HeaderValidationError.InvalidHeader(name, Some("reason1")))
              .check(_.startsWith("Prefix"), name => HeaderValidationError.InvalidHeader(name, Some("reason2")))

          val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest()
              .withHeaders("Header" -> "Prefix suffix")

          validator.validate(request).toEither.value shouldBe ()
        }
      }

      "should fail to validate the header" - {

        "when there are multiple unsatisfied rules" in {
          val validator: RequiredHeaderValidator =
            HeaderValidator("header").required
              .check(_.endsWith("ASD"), name => HeaderValidationError.InvalidHeader(name, Some("reason1")))
              .check(_.nonEmpty, name => HeaderValidationError.InvalidHeader(name, Some("reason2")))
              .check(_.startsWith("ABC"), name => HeaderValidationError.InvalidHeader(name, Some("reason3")))

          val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest()
              .withHeaders("header" -> "VALUE")

          validator.validate(request).toEither.left.value shouldBe
            NonEmptyList.of(
              HeaderValidationError.InvalidHeader("header", Some("reason1")),
              HeaderValidationError.InvalidHeader("header", Some("reason3"))
            )
        }
      }
    }
  }
