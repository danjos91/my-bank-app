package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should create a notification"
    request {
        method POST()
        url "/api/notifications"
        headers {
            contentType(applicationJson())
        }
        body([
            userId: 1,
            type: "INFO",
            title: "Test Notification",
            message: "This is a test notification"
        ])
    }
    response {
        status 201
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            userId: 1,
            type: "INFO",
            title: "Test Notification",
            message: "This is a test notification",
            isRead: false,
            createdAt: "2023-01-01T00:00:00",
            updatedAt: "2023-01-01T00:00:00"
        ])
    }
}
