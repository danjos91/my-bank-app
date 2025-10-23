package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should get notifications by user ID"
    request {
        method GET()
        url "/api/notifications/user/1"
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
                type: "INFO",
                title: "Test Notification",
                message: "This is a test notification",
                isRead: false,
                createdAt: "2023-01-01T00:00:00",
                updatedAt: "2023-01-01T00:00:00"
            ]
        ])
    }
}
