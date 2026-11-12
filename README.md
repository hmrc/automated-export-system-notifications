
# automated-export-system-notifications

This is the notification service for the automated export system. It is responsible for sending async notifications to update submitted requests.

### Running locally
It runs on port 5003.
To start the service and dependendencies in service manager, run `sm2 --start AUTOMATED_EXPORT_SERVICE_ALL`
For more information on the payload and schemas,go to [our confluence page](https://confluence.tools.tax.service.gov.uk/spaces/AES/pages/1309999134/Request+payload)

### Running all tests and coverage report
To run all tests and generate a coverage report, use this command
`sbt scalafmt test:scalafmt it:test::scalafmt coverage test it/test coverageReport`

### Endpoints
`POST        /notification `: This is the notification endpoint that receives notifications from the EIS and sends them to the frontend. The payload is in XML format. For more information on the payload and schemas, go to [our confluence page](https://confluence.tools.tax.service.gov.uk/spaces/AES/pages/1364590770/Request+from+notification+endpoint)

## Success
```<?xml version="1.0" encoding="UTF-8"?>
<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
    <Header>
        <messageSender>NECA.XI</messageSender>
        <messageRecipient>GB123456789000</messageRecipient>
        <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
        <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d</messageIdentification>
        <messageType>ACK</messageType>
        <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d</correlationIdentifier>
    </Header>
  <Body>
        <messageCode>CC507C</messageCode>
        <actionCode>1</actionCode>
        <MRN>26GB123456789ABCDE1</MRN>
  </Body>
</AESDigitalNotification>
```

## Validation Error
```<?xml version="1.0" encoding="UTF-8"?>
<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
    <Header>
        <messageSender>NECA.XI</messageSender>
        <messageRecipient>GB123456789000</messageRecipient>
        <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
        <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d2</messageIdentification>
        <messageType>CD917C</messageType>
        <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d3</correlationIdentifier>
    </Header>

    <Body>
        <messageCode>CC906C</messageCode>
        <MRN>26GB123456789ABCDE1</MRN>
        <XMLError>
		<errorPointer>/CD917C/Body/ExportOperation/Type</errorPointer>
                <errorCode>13</errorCode>
                <errorText>ERR02</errorText>
                <originalAttributeValue>4</originalAttributeValue>
		</XMLError>
    </Body>
</AESDigitalNotification>
```

## Functional Error

```<?xml version="1.0" encoding="UTF-8"?>
<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
    <Header>
        <messageSender>NECA.XI</messageSender>
        <messageRecipient>GB123456789000</messageRecipient>
        <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
        <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d</messageIdentification>
        <messageType>CD906C</messageType>
        <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d</correlationIdentifier>
    </Header>
    <Body>
        <messageCode>CC906C</messageCode>
        <MRN>26GB123456789ABCDE1</MRN>
            <FunctionalError>
                <errorPointer>/CD906C/body/ExportOperation/MRN</errorPointer>
                <errorCode>90</errorCode>
                <errorReason>ERR001</errorReason>
                <originalAttributeValue>26GB123456789ABCDE1</originalAttributeValue>
            </FunctionalError>
    </Body>
</AESDigitalNotification>
```

### License

This code is open source software licensed under the [Apache 2.0 License](LICENSE).