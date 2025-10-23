package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should update account balance"
    request {
        method PUT()
        url "/api/accounts/1/balance"
        headers {
            contentType(applicationJson())
        }
        body(200.00)
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            message: "Balance updated successfully to: 200.00"
        ])
    }
}
