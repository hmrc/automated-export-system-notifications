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
import uk.gov.hmrc.automatedexportsystemnotifications.models.errors.{ErrorMapper, ErrorSource}
import uk.gov.hmrc.automatedexportsystemnotifications.models.errors.UnifiedErrorCode.*

class ErrorMapperSpec extends BaseSpec {

  "ErrorMapper.toUnified" - {

    "map IE906 known codes correctly" in {
      ErrorMapper.toUnified(ErrorSource.IE906, 8)  shouldBe InvalidFormatDucr
      ErrorMapper.toUnified(ErrorSource.IE906, 12) shouldBe CodelistViolation
      ErrorMapper.toUnified(ErrorSource.IE906, 13) shouldBe MissingConditionViolation
      ErrorMapper.toUnified(ErrorSource.IE906, 14) shouldBe RuleViolation
      ErrorMapper.toUnified(ErrorSource.IE906, 15) shouldBe ConditionNotAllowedViolation
      ErrorMapper.toUnified(ErrorSource.IE906, 26) shouldBe DuplicatedMessageId
      ErrorMapper.toUnified(ErrorSource.IE906, 27) shouldBe RoleBaseAuthFailed
      ErrorMapper.toUnified(ErrorSource.IE906, 35) shouldBe MucrShut
      ErrorMapper.toUnified(ErrorSource.IE906, 50) shouldBe TransitionalConstraintViolation
      ErrorMapper.toUnified(ErrorSource.IE906, 51) shouldBe EDIViolation
      ErrorMapper.toUnified(ErrorSource.IE906, 52) shouldBe FunctionalViolation
      ErrorMapper.toUnified(ErrorSource.IE906, 90) shouldBe UnknownMrn
      ErrorMapper.toUnified(ErrorSource.IE906, 92) shouldBe MessageOutOfSequence
      ErrorMapper.toUnified(ErrorSource.IE906, 93) shouldBe InvalidMrn
      ErrorMapper.toUnified(ErrorSource.IE906, 94) shouldBe InvalidDiscrepancies
      ErrorMapper.toUnified(ErrorSource.IE906, 95) shouldBe InvalidAmendment
      ErrorMapper.toUnified(ErrorSource.IE906, 96) shouldBe DiversionRejectedInvalidDeclaration
      ErrorMapper.toUnified(ErrorSource.IE906, 97) shouldBe DiversionRejectedUnknownMrn
      ErrorMapper.toUnified(ErrorSource.IE906, 98) shouldBe DiversionRejectedAlreadyExited
      ErrorMapper.toUnified(ErrorSource.IE906, 99) shouldBe DiversionRejectedOther

    }

    "map IE917 known codes correctly" in {
      ErrorMapper.toUnified(ErrorSource.IE917, 12) shouldBe IncorrectEnumeration
      ErrorMapper.toUnified(ErrorSource.IE917, 13) shouldBe MissingField
      ErrorMapper.toUnified(ErrorSource.IE917, 15) shouldBe NotSupportedInPosition
      ErrorMapper.toUnified(ErrorSource.IE917, 18) shouldBe UnspecifiedErrorOther
      ErrorMapper.toUnified(ErrorSource.IE917, 35) shouldBe TooManyRepetitions
      ErrorMapper.toUnified(ErrorSource.IE917, 39) shouldBe ElementTooLong
      ErrorMapper.toUnified(ErrorSource.IE917, 40) shouldBe ElementTooShort
      ErrorMapper.toUnified(ErrorSource.IE917, 50) shouldBe InvalidValueSpecificType
      ErrorMapper.toUnified(ErrorSource.IE917, 51) shouldBe InvalidValuePattern
      ErrorMapper.toUnified(ErrorSource.IE917, 52) shouldBe InvalidXmlFormat
      ErrorMapper.toUnified(ErrorSource.IE917, 53) shouldBe InvalidCharacters
      ErrorMapper.toUnified(ErrorSource.IE917, 54) shouldBe ValueLowerAllowedLowestLimit
      ErrorMapper.toUnified(ErrorSource.IE917, 55) shouldBe ValueGreaterAllowedUpperLimit
      ErrorMapper.toUnified(ErrorSource.IE917, 56) shouldBe ValueLowerOrEqualAllowedLowestLimit
      ErrorMapper.toUnified(ErrorSource.IE917, 57) shouldBe ValueGreaterOrEqualAllowedLowestLimit

    }

    "return Unknown for unknown IE906 code" in {
      ErrorMapper.toUnified(ErrorSource.IE906, 12345) shouldBe Unknown
    }

    "return Unknown for unknown IE917 code" in {
      ErrorMapper.toUnified(ErrorSource.IE917, 999) shouldBe Unknown
    }

    "return Unknown for negative codes" in {
      ErrorMapper.toUnified(ErrorSource.IE906, -1) shouldBe Unknown
      ErrorMapper.toUnified(ErrorSource.IE917, -1) shouldBe Unknown
    }
  }
}
