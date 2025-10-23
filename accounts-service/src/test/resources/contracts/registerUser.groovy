package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should register a new user"
    request {
        method POST()
        url "/api/accounts/users/register"
        headers {
            contentType(applicationJson())
        }
        body([
            username: "newuser",
            email: "newuser@example.com",
            password: "password123",
            firstName: "New",
            lastName: "User",
            birthDate: "1990-01-01"
        ])
    }
    response {
        status 201
        headers {
            contentType(applicationJson())
        }
        body([
            message: "User registered successfully with ID: 1"
        ])
    }
}
