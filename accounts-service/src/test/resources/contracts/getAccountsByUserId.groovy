package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should get accounts by user ID"
    request {
        method GET()
        url "/api/accounts/users/1/accounts"
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            [
                id: 1,
                userId: 1,
                username: "testuser",
                balance: 100.00,
                createdAt: "2023-01-01T00:00:00",
                updatedAt: "2023-01-01T00:00:00"
            ]
        ])
    }
}
