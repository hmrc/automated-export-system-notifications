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

package uk.gov.hmrc.automatedexportsystemnotifications.parsers

import uk.gov.hmrc.automatedexportsystemnotifications.helpers.BaseSpec
import uk.gov.hmrc.automatedexportsystemnotifications.models.requests.{AckBody, FunctionalError, IE906Body, IE917Body, IncomingPayload, XMLError}

class EisPayloadXmlParserSpec extends BaseSpec {

  "EisPayloadXmlParser.parse" - {

    "parse AckBody payload (CC507C)" in {
      val xml =
        """<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |    <Header>
          |        <messageSender>NECA.XI</messageSender>
          |        <messageRecipient>GB123456789000</messageRecipient>
          |        <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
          |        <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d</messageIdentification>
          |        <messageType>ACK</messageType>
          |        <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d</correlationIdentifier>
          |    </Header>
          |  <Body>
          |        <messageCode>CC507C</messageCode>
          |        <actionCode>1</actionCode>
          |        <MRN>26GB123456789ABCDE1</MRN>
          |  </Body>
          |</AESDigitalNotification>""".stripMargin

      val result = EisPayloadXmlParser.parse(xml)

      result shouldBe Right(
        IncomingPayload.Ack(
          AckBody(
            MessageCode = "CC507C",
            ActionCode = 1,
            MRN = "26GB123456789ABCDE1",
            eori = "GB123456789000"
          )
        )
      )
    }

    "parse IE506Body payload (CC906C) with multiple FunctionalError entries" in {
      val xml =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |    <Header>
          |        <messageSender>NECA.XI</messageSender>
          |        <messageRecipient>GB123456789000</messageRecipient>
          |        <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
          |        <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d</messageIdentification>
          |        <messageType>CD906C</messageType>
          |        <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d</correlationIdentifier>
          |    </Header>
          |    <Body>
          |        <messageCode>CC507C</messageCode>
          |        <MRN>26GB123456789ABCDE1</MRN>
          |        <FunctionalError>
          |           <errorPointer>ptr-1</errorPointer>
          |           <errorCode>100</errorCode>
          |           <errorReason>reason-1</errorReason>
          |           <originalAttributeValue>attr-1</originalAttributeValue>
          |       </FunctionalError>
          |       <FunctionalError>
          |           <errorPointer>ptr-2</errorPointer>
          |           <errorCode>200</errorCode>
          |           <errorReason>reason-2</errorReason>
          |       </FunctionalError>
          |    </Body>
          |</AESDigitalNotification>""".stripMargin

      val result = EisPayloadXmlParser.parse(xml)

      result shouldBe Right(
        IncomingPayload.IE906(
          IE906Body(
            MessageCode = "CD906C",
            MRN = "26GB123456789ABCDE1",
            eori = "GB123456789000",
            FunctionalError = List(
              FunctionalError("ptr-1", 100, "reason-1", Some("attr-1")),
              FunctionalError("ptr-2", 200, "reason-2", None)
            )
          )
        )
      )
    }

    "parse IE917Body payload (CD917C) with multiple XmlError entries" in {
      val xml =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |    <Header>
          |        <messageSender>NECA.XI</messageSender>
          |        <messageRecipient>GB123456789000</messageRecipient>
          |        <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
          |        <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d2</messageIdentification>
          |        <messageType>CD917C</messageType>
          |        <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d3</correlationIdentifier>
          |    </Header>
          |
          |    <Body>
          |        <MRN>26GB123456789ABCDE1</MRN>
          |            <XMLError>
          |            <errorPointer>xptr-1</errorPointer>
          |            <errorCode>300</errorCode>
          |            <errorText>ERRTXT1</errorText>
          |            <originalAttributeValue>orig-1</originalAttributeValue>
          |          </XMLError>
          |          <XMLError>
          |            <errorPointer>xptr-2</errorPointer>
          |            <errorCode>301</errorCode>
          |            <errorText>ERRTXT2</errorText>
          |          </XMLError>
          |  </Body>
          |</AESDigitalNotification>""".stripMargin

      val result = EisPayloadXmlParser.parse(xml)

      result shouldBe Right(
        IncomingPayload.IE917(
          IE917Body(
            MessageCode = "CD917C",
            MRN = "26GB123456789ABCDE1",
            eori = "GB123456789000",
            XmlError = List(
              XMLError("xptr-1", 300, "ERRTXT1", Some("orig-1")),
              XMLError("xptr-2", 301, "ERRTXT2", None)
            )
          )
        )
      )
    }

    "return Left for unsupported MessageType" in {
      val xml =
        """<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |  <Header>
          |        <messageSender>NECA.XI</messageSender>
          |        <messageRecipient>GB123456789000</messageRecipient>
          |        <messageType>UNKNOWN</messageType>
          |  </Header>
          |  <Body>
          |   <actionCode>1</actionCode>
          |   <messageCode>UNKNOWN</messageCode>
          |   <MRN>MRN000</MRN>
          |</Body>
          |</AESDigitalNotification>""".stripMargin

      val result = EisPayloadXmlParser.parse(xml)

      result shouldBe Left("Unsupported messageType: UNKNOWN")
    }

    "return Left when required MessageType is missing" in {
      val xml =
        """<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |  <Header>
          |        <messageSender>NECA.XI</messageSender>
          |        <messageRecipient>GB123456789000</messageRecipient>
          |  </Header>
          |  <Body>
          |    <MRN>MRN000</MRN>
          |    <actionCode>1</actionCode>
          |</Body>
          |</AESDigitalNotification>""".stripMargin

      val result = EisPayloadXmlParser.parse(xml)

      result shouldBe Left("Missing required field: messageType")
    }

    "return Left when ActionCode is not an integer for CC507C" in {
      val xml =
        """<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |  <Header>
          |        <messageSender>NECA.XI</messageSender>
          |        <messageRecipient>GB123456789000</messageRecipient>
          |        <messageType>ACK</messageType>
          |  </Header>
          |  <Body>
          |     <messageCode>CC507C</messageCode>
          |     <actionCode>abc</actionCode>
          |     <MRN>MRN123</MRN>
          |  </Body>
          |</AESDigitalNotification>""".stripMargin

      val result = EisPayloadXmlParser.parse(xml)

      result shouldBe Left("Field actionCode must be an Int, got: abc")
    }
  }
}
