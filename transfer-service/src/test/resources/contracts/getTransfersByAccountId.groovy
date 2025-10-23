package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should get transfers by account ID"
    request {
        method GET()
        url "/api/transfers/account/1"
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            [
                id: 1,
                fromAccountId: 1,
                toAccountId: 2,
                amount: 50.00,
                description: "Test transfer",
                status: "COMPLETED",
                createdAt: "2023-01-01T00:00:00",
                updatedAt: "2023-01-01T00:00:00"
            ]
        ])
    }
}
