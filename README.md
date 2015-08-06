# Simple Wallet

This is a simple wallet simply maintains a running account balance allowing you to credit, debit, or review the current balance of the wallet.   This is simply for pure demonstration on various wearable technologies where a javacard secure element is available.

## Installation

_build/simplewallet.cap_ is provided for installation on a javacard enabled secure element through whatever provisioning tools you have available.

## Information

Applet AID
```
F000A0000E00
```

## Usage

Before any operation, the applet must be selected throught the following APDU command:
```
00A4040006F000A0000E0000
```

### Get Balance
Request APDU
```
B050000002
```
* If 0x9000 successful response, the balance is returned in response data.


### Increment Balance
```
B040000001[AMOUNT, 1 Bytes]
```
* Note: The wallet is not currently implemented to protect against a max balance of 32767.

Sample, increase balance by 5:
```
B04000000105
```

### Decrease Balance
```
B030000001[AMOUNT, 1 Bytes]
```
* If 0x9000 successful response, the balance has been reduced by the specified amount.
* If 0xFF85 failed response, the current balance is not enough for the specified amount.

Sample, decrease balance by 5:
```
B03000000105
```
