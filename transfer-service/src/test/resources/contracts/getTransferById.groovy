package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should get transfer by ID"
    request {
        method GET()
        url "/api/transfers/1"
    }
    response {
        status 200
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
