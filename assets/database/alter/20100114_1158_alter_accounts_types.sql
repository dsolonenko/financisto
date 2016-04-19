UPDATE account SET card_issuer=type where type in ('VISA','VISA_ELECTRON','MASTERCARD','MAESTRO','AMEX');
UPDATE account SET type='DEBIT_CARD' where type in ('VISA','VISA_ELECTRON','MASTERCARD','MAESTRO','AMEX');
