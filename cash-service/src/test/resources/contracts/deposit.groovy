package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should process a deposit"
    request {
        method POST()
        url "/api/cash/deposit"
        headers {
            contentType(applicationJson())
        }
        body([
            accountId: 1,
            amount: 100.00,
            description: "Test deposit"
        ])
    }
    response {
        status 201
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            accountId: 1,
            amount: 100.00,
            transactionType: "DEPOSIT",
            description: "Test deposit",
            transactionDate: "2023-01-01T00:00:00"
        ])
    }
}
