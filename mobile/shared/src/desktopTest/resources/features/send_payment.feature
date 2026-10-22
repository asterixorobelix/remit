Feature: Send a payment
  As a Remit user
  I want to send money to someone by email
  So that they receive exactly the amount I entered, once

  Background:
    Given I am signed in

  Scenario: A valid payment is processed and saved to Firestore
    Given I enter a payment of "10.50" "USD" to "bob@example.com"
    When I submit the payment
    Then the payment is accepted
    And the payments API received "10.50" "USD" for "bob@example.com" with an idempotency key
    And Firestore has a payment of 1050 cents in "USD" to "bob@example.com" from me

  Scenario Outline: Invalid details are rejected before anything is sent
    Given I enter a payment of "<amount>" "<currency>" to "<recipient>"
    When I submit the payment
    Then the payment is rejected with "<error>"
    And nothing is sent to the payments API

    Examples:
      | recipient       | amount   | currency | error                    |
      | bob             | 10       | USD      | EMAIL_INVALID            |
      | bob@example.com | 0        | USD      | AMOUNT_NOT_POSITIVE      |
      | bob@example.com | 1,000    | USD      | AMOUNT_TOO_MANY_DECIMALS |
      | bob@example.com | 10000.01 | EUR      | AMOUNT_ABOVE_LIMIT       |
      | bob@example.com | 10       | GBP      | CURRENCY_UNSUPPORTED     |

  Scenario: Retrying after a timeout does not pay twice
    Given the payments API times out once after processing the payment
    And I enter a payment of "25" "EUR" to "carol@example.com"
    When I submit the payment
    Then the payment fails with "TIMEOUT"
    When I submit the payment again
    Then the payment is accepted
    And exactly 1 payment to "carol@example.com" is recorded

  Scenario: A payment interrupted by the app closing is finished safely after reopening
    Given the payments API times out once after processing the payment
    And I enter a payment of "40" "USD" to "erin@example.com"
    When I submit the payment
    Then the payment fails with "TIMEOUT"
    When the app is closed and reopened
    And I send the unfinished payment again
    Then the payment is accepted
    And exactly 1 payment to "erin@example.com" is recorded

  Scenario: The same details sent as a new payment are a second payment
    Given I have sent "5.00" "USD" to "dave@example.com"
    And I have sent "5.00" "USD" to "dave@example.com"
    Then exactly 2 payments to "dave@example.com" are recorded
