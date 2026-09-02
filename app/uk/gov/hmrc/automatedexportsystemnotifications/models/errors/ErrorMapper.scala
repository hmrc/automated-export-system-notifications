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

package uk.gov.hmrc.automatedexportsystemnotifications.models.errors

import uk.gov.hmrc.automatedexportsystemnotifications.models.errors.ErrorSource.{IE906, IE917}
import uk.gov.hmrc.automatedexportsystemnotifications.models.errors.UnifiedErrorCode.*

object ErrorMapper:
  private val ie906Map: Map[Int, UnifiedErrorCode] = Map(
    8  -> InvalidFormatDucr,
    12 -> CodelistViolation,
    13 -> MissingConditionViolation,
    14 -> RuleViolation,
    15 -> ConditionNotAllowedViolation,
    26 -> DuplicatedMessageId,
    27 -> RoleBaseAuthFailed,
    35 -> MucrShut,
    50 -> TransitionalConstraintViolation,
    51 -> EDIViolation,
    52 -> FunctionalViolation,
    90 -> UnknownMrn,
    92 -> MessageOutOfSequence,
    93 -> InvalidMrn,
    94 -> InvalidDiscrepancies,
    95 -> InvalidAmendment,
    96 -> DiversionRejectedInvalidDeclaration,
    97 -> DiversionRejectedUnknownMrn,
    98 -> DiversionRejectedAlreadyExited,
    99 -> DiversionRejectedOther
  )

  private val ie917Map: Map[Int, UnifiedErrorCode] = Map(
    13 -> MissingField
  )

  def toUnified(source: ErrorSource, rawCode: Int): UnifiedErrorCode =
    source match {
      case IE906 => ie906Map.getOrElse(rawCode, Unknown)
      case IE917 => ie917Map.getOrElse(rawCode, Unknown)
    }
