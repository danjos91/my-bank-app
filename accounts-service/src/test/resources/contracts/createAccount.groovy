package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should create account for user"
    request {
        method POST()
        url "/api/accounts/users/1/accounts"
    }
    response {
        status 201
        headers {
            contentType(applicationJson())
        }
        body([
            message: $(regex("Account created successfully with ID: \\d+"))
        ])
    }
}
