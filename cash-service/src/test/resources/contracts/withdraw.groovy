package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should process a withdrawal"
    request {
        method POST()
        url "/api/cash/withdraw"
        headers {
            contentType(applicationJson())
        }
        body([
            accountId: 1,
            amount: 50.00,
            description: "Test withdrawal"
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
            amount: 50.00,
            transactionType: "WITHDRAWAL",
            description: "Test withdrawal",
            timestamp: $(regex('[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?'))
        ])
    }
}
