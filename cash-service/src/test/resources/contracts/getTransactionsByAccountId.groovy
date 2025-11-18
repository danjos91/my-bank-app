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
                id: $(anyNumber()),
                accountId: 1,
                amount: 100.00,
                transactionType: "DEPOSIT",
                description: "Test deposit",
                timestamp: $(regex('[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?'))
            ]
        ])
    }
}
