package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should get user by ID"
    request {
        method GET()
        url "/api/accounts/users/1"
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            firstName: "Test",
            lastName: "User",
            email: "test@example.com",
            birthDate: "1990-01-01",
            username: "testuser"
        ])
    }
}
