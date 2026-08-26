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
import uk.gov.hmrc.automatedexportsystemnotifications.models.requests.{FunctionalError, IncomingPayload, XMLError}

object IncomingPayloadErrorTranslator:
  private def fromIe906Error(err: FunctionalError): UnifiedErrorMetadata = {
    val unified = ErrorMapper.toUnified(IE906, err.errorCode)
    UnifiedErrorMetadata(
      unifiedCode = unified,
      path = Option(err.errorPointer).map(_.trim).filter(_.nonEmpty),
      description = unified.description,
      originalValue = err.originalAttribute.map(_.trim).filter(_.nonEmpty)
    )
  }

  private def fromIe917Error(err: XMLError): UnifiedErrorMetadata = {
    val unified = ErrorMapper.toUnified(IE917, err.errorCode)
    UnifiedErrorMetadata(
      unifiedCode = unified,
      path = Option(err.errorPointer).map(_.trim).filter(_.nonEmpty),
      description = unified.description,
      originalValue = err.originalAttribute.map(_.trim).filter(_.nonEmpty)
    )
  }

  def toUnifiedErrors(payload: IncomingPayload): List[UnifiedErrorMetadata] =
    payload match {
      case IncomingPayload.IE906(b) => b.FunctionalError.map(IncomingPayloadErrorTranslator.fromIe906Error)
      case IncomingPayload.IE917(b) => b.XmlError.map(IncomingPayloadErrorTranslator.fromIe917Error)
      case _                        => Nil
    }
