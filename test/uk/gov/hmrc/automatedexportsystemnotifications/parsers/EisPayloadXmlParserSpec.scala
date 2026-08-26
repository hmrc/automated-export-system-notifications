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
          |     </Header>
          |     <Body>
          |         <messageCode>CC507C</messageCode>
          |         <actionCode>1</actionCode>
          |         <MRN>MRN123</MRN>
          |     </Body>
          |</AESDigitalNotification>""".stripMargin

      val result = EisPayloadXmlParser.parse(xml)

      result shouldBe Right(
        IncomingPayload.Ack(
          AckBody(
            MessageCode = "CC507C",
            ActionCode = 1,
            MRN = "MRN123",
            eori = "GB123456789000"
          )
        )
      )
    }

    "parse IE506Body payload (CC906C) with multiple FunctionalError entries" in {
      val xml =
        """<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |  <Header>
          |        <messageSender>NECA.XI</messageSender>
          |        <messageRecipient>GB123456789000</messageRecipient>
          |  </Header>
          |  <Body>
          |     <messageCode>CC906C</messageCode>
          |      <MRN>MRN506</MRN>
          |      <FunctionalError>
          |          <errorPointer>ptr-1</errorPointer>
          |          <errorCode>100</errorCode>
          |          <errorReason>reason-1</errorReason>
          |          <originalAttribute>attr-1</originalAttribute>
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
            MessageCode = "CC906C",
            MRN = "MRN506",
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
        """<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |  <Header>
          |        <messageSender>NECA.XI</messageSender>
          |        <messageRecipient>GB123456789000</messageRecipient>
          |  </Header>
          |  <Body>
          |  <messageCode>CD917C</messageCode>
          |  <MRN>MRN917</MRN>
          |  <XMLError>
          |    <errorPointer>xptr-1</errorPointer>
          |    <errorCode>300</errorCode>
          |    <errorText>ERRTXT1</errorText>
          |    <originalAttribute>orig-1</originalAttribute>
          |  </XMLError>
          |  <XMLError>
          |    <errorPointer>xptr-2</errorPointer>
          |    <errorCode>301</errorCode>
          |    <errorText>ERRTXT2</errorText>
          |  </XMLError>
          |  </Body>
          |</AESDigitalNotification>""".stripMargin

      val result = EisPayloadXmlParser.parse(xml)

      result shouldBe Right(
        IncomingPayload.IE917(
          IE917Body(
            MessageCode = "CD917C",
            MRN = "MRN917",
            eori = "GB123456789000",
            XmlError = List(
              XMLError("xptr-1", 300, "ERRTXT1", Some("orig-1")),
              XMLError("xptr-2", 301, "ERRTXT2", None)
            )
          )
        )
      )
    }

    "return Left for unsupported MessageCode" in {
      val xml =
        """<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |  <Header>
          |        <messageSender>NECA.XI</messageSender>
          |        <messageRecipient>GB123456789000</messageRecipient>
          |  </Header>
          |  <Body>
          |  <messageCode>UNKNOWN</messageCode>
          |  <MRN>MRN000</MRN>
          |</Body>
          |</AESDigitalNotification>""".stripMargin

      val result = EisPayloadXmlParser.parse(xml)

      result shouldBe Left("Unsupported messageCode: UNKNOWN")
    }

    "return Left when required MessageCode is missing" in {
      val xml =
        """<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |  <Header>
          |        <messageSender>NECA.XI</messageSender>
          |        <messageRecipient>GB123456789000</messageRecipient>
          |  </Header>
          |  <Body>
          |  <MRN>MRN000</MRN>
          |</Body>
          |</AESDigitalNotification>""".stripMargin

      val result = EisPayloadXmlParser.parse(xml)

      result shouldBe Left("Missing required field: messageCode")
    }

    "return Left when ActionCode is not an integer for CC507C" in {
      val xml =
        """<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |  <Header>
          |        <messageSender>NECA.XI</messageSender>
          |        <messageRecipient>GB123456789000</messageRecipient>
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
