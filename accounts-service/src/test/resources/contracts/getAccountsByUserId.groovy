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
                createdAt: $(regex(~/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?$/)),
                updatedAt: $(regex(~/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?$/))
            ]
        ])
    }
}
