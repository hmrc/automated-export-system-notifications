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
              .satisfies(_.endsWith("ASD"), name => HeaderValidationError.InvalidHeader(name, "reason1"))
              .satisfies(_.startsWith("ABC"), name => HeaderValidationError.InvalidHeader(name, "reason2"))

          val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest()

          validator.validate(request).toEither.value shouldBe ()
        }

        "when there are multiple rules and the header is present" in {
          val validator: HeaderValidator =
            HeaderValidator("header")
              .satisfies(_.endsWith("suffix"), name => HeaderValidationError.InvalidHeader(name, "reason1"))
              .satisfies(_.startsWith("Prefix"), name => HeaderValidationError.InvalidHeader(name, "reason2"))

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
              .satisfies(_.endsWith("ASD"), name => HeaderValidationError.InvalidHeader(name, "reason1"))
              .satisfies(_.nonEmpty, name => HeaderValidationError.InvalidHeader(name, "reason2"))
              .satisfies(_.startsWith("ABC"), name => HeaderValidationError.InvalidHeader(name, "reason3"))

          val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest()
              .withHeaders("header" -> "VALUE")

          validator.validate(request).toEither.left.value shouldBe
            NonEmptyList.of(
              HeaderValidationError.InvalidHeader("header", "reason1"),
              HeaderValidationError.InvalidHeader("header", "reason3")
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
              .satisfies(_.endsWith("suffix"), name => HeaderValidationError.InvalidHeader(name, "reason1"))
              .satisfies(_.startsWith("Prefix"), name => HeaderValidationError.InvalidHeader(name, "reason2"))

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
              .satisfies(_.endsWith("ASD"), name => HeaderValidationError.InvalidHeader(name, "reason1"))
              .satisfies(_.nonEmpty, name => HeaderValidationError.InvalidHeader(name, "reason2"))
              .satisfies(_.startsWith("ABC"), name => HeaderValidationError.InvalidHeader(name, "reason3"))

          val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest()
              .withHeaders("header" -> "VALUE")

          validator.validate(request).toEither.left.value shouldBe
            NonEmptyList.of(
              HeaderValidationError.InvalidHeader("header", "reason1"),
              HeaderValidationError.InvalidHeader("header", "reason3")
            )
        }
      }
    }
  }
