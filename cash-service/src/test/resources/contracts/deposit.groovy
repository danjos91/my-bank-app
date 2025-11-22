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
            id: $(anyNumber()),
            accountId: 1,
            amount: 100.00,
            transactionType: "DEPOSIT",
            description: "Test deposit",
            timestamp: $(regex('[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?'))
        ])
    }
}
