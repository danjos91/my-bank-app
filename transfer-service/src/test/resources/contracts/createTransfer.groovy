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
            id: $(anyNumber()),
            fromAccountId: 1,
            toAccountId: 2,
            amount: 50.00,
            description: "Test transfer",
            status: "COMPLETED",
            createdAt: $(regex('[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?')),
            updatedAt: $(regex('[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?'))
        ])
    }
}
