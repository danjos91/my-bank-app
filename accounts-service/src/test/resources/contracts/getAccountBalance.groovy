package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should get account balance"
    request {
        method GET()
        url "/api/accounts/1/balance"
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body(100.00)
    }
}
