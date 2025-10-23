package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should create a transfer"
    request {
        method POST()
        url "/api/transfers"
        headers {
            contentType(applicationJson())
        }
        body([
            fromAccountId: 1,
            toAccountId: 2,
            amount: 50.00,
            description: "Test transfer"
        ])
    }
    response {
        status 201
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            fromAccountId: 1,
            toAccountId: 2,
            amount: 50.00,
            description: "Test transfer",
            status: "COMPLETED",
            createdAt: "2023-01-01T00:00:00",
            updatedAt: "2023-01-01T00:00:00"
        ])
    }
}
