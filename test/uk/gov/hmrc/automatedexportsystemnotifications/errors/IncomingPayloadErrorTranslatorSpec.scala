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

package uk.gov.hmrc.automatedexportsystemnotifications.errors

import uk.gov.hmrc.automatedexportsystemnotifications.helpers.BaseSpec
import uk.gov.hmrc.automatedexportsystemnotifications.models.errors.{IncomingPayloadErrorTranslator, UnifiedErrorCode}
import uk.gov.hmrc.automatedexportsystemnotifications.models.requests.*

class IncomingPayloadErrorTranslatorSpec extends BaseSpec:

  "IncomingPayloadErrorTranslator.toUnifiedErrors" - {

    "map IE906 FunctionalError entries to UnifiedErrorMetadata" in {
      val payload = IncomingPayload.IE906(
        IE906Body(
          MRN = "mrn",
          eori = "eori",
          FunctionalError = List(
            FunctionalError(
              errorPointer = " Body.ExportOperation.MRN ",
              errorCode = 90,
              errorReason = "some reason",
              originalAttribute = Some(" 26GB123 ")
            )
          )
        )
      )

      val result = IncomingPayloadErrorTranslator.toUnifiedErrors(payload)

      result                      should have size 1
      result.head.unifiedCode   shouldBe UnifiedErrorCode.UnknownMrn
      result.head.description   shouldBe UnifiedErrorCode.UnknownMrn.description
      result.head.path          shouldBe Some("Body.ExportOperation.MRN")
      result.head.originalValue shouldBe Some("26GB123")
    }

    "map IE917 XmlError entries to UnifiedErrorMetadata" in {
      val payload = IncomingPayload.IE917(
        IE917Body(
          MRN = "mrn",
          eori = "eori",
          XmlError = List(
            XMLError(
              errorPointer = " Body.Some.Path ",
              errorCode = 13,
              errorText = "missing field",
              originalAttribute = Some("  abc  ")
            )
          )
        )
      )

      val result = IncomingPayloadErrorTranslator.toUnifiedErrors(payload)

      result                      should have size 1
      result.head.unifiedCode   shouldBe UnifiedErrorCode.MissingField
      result.head.description   shouldBe UnifiedErrorCode.MissingField.description
      result.head.path          shouldBe Some("Body.Some.Path")
      result.head.originalValue shouldBe Some("abc")
    }

    "set path/originalValue to None when blank after trimming" in {
      val payload = IncomingPayload.IE906(
        IE906Body(
          MRN = "mrn",
          eori = "eori",
          FunctionalError = List(
            FunctionalError(
              errorPointer = "   ",
              errorCode = 90,
              errorReason = "some reason",
              originalAttribute = Some("   ")
            )
          )
        )
      )

      val result = IncomingPayloadErrorTranslator.toUnifiedErrors(payload)

      result.head.path          shouldBe None
      result.head.originalValue shouldBe None
    }

    "return empty list for non-error payloads (e.g. Ack)" in {
      val payload = IncomingPayload.Ack(AckBody(ActionCode = 1, MRN = "mrn", eori = "eori"))

      IncomingPayloadErrorTranslator.toUnifiedErrors(payload) shouldBe Nil
    }
  }
