Feature: Transaction history
  As a Remit user
  I want to see the payments I have sent
  So that I know what went where, and when

  Scenario: Sent payments are listed newest first
    Given I am signed in
    And I have sent "5.00" "USD" to "alice@example.com"
    And I have sent "7.25" "EUR" to "bob@example.com"
    When I open my transaction history
    Then I see 2 payments
    And the newest payment is "7.25" "EUR" to "bob@example.com"

  Scenario: History updates in real time
    Given I am signed in
    And I am viewing my transaction history
    When a payment of "12.00" "USD" to "dave@example.com" is recorded for me
    Then my history shows a payment to "dave@example.com" without refreshing

  Scenario: Other people's payments are never shown
    Given I am signed in as "user-a"
    And user "user-b" has sent "99.00" "USD" to "eve@example.com"
    When I open my transaction history
    Then I see 0 payments
