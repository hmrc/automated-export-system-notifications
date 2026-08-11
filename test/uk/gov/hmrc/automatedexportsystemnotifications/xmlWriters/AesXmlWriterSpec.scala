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

package uk.gov.hmrc.automatedexportsystemnotifications.xmlWriters

import org.scalactic.Prettifier.default
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.BaseSpec
import uk.gov.hmrc.automatedexportsystemnotifications.models.aesRequest.{NotificationPayload, NotificationStatus}

class AesXmlWriterSpec extends BaseSpec {

  import java.time.OffsetDateTime
  import java.util.UUID

  class AesNotificationWriterSpec extends BaseSpec {

    "AesNotificationWriter.toXml" - {

      "create XML with required fields and status when no errors" in {
        val payload = NotificationPayload(
          correlationId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000").toString,
          dateCreated = OffsetDateTime.parse("2026-08-11T10:15:30Z"),
          eori = "GB123456",
          mrn = "MRN123456",
          status = NotificationStatus.Accepted,
          errors = Nil
        )

        val result = AesNotificationWriter.toXml(payload)

        result.isRight shouldBe true
        val xml = result.toOption.get

        (xml \ "correlationId").text shouldBe "123e4567-e89b-12d3-a456-426614174000"
        (xml \ "eori").text          shouldBe "GB123456"
        (xml \ "mrn").text           shouldBe "MRN123456"
        (xml \ "dateCreated").text   shouldBe "2026-08-11T10:15:30Z"
        (xml \ "status").text        shouldBe "1"
        (xml \ "errors").isEmpty     shouldBe true
      }
    }
  }

}
