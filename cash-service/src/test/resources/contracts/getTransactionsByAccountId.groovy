package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should get transactions by account ID"
    request {
        method GET()
        url "/api/cash/account/1/transactions"
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            [
                id: 1,
                accountId: 1,
                amount: 100.00,
                transactionType: "DEPOSIT",
                description: "Test deposit",
                transactionDate: "2023-01-01T00:00:00"
            ]
        ])
    }
}
