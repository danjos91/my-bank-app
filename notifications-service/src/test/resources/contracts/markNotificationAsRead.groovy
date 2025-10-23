package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should mark notification as read"
    request {
        method PUT()
        url "/api/notifications/1/read"
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            userId: 1,
            type: "INFO",
            title: "Test Notification",
            message: "This is a test notification",
            isRead: true,
            createdAt: "2023-01-01T00:00:00",
            updatedAt: "2023-01-01T00:00:00"
        ])
    }
}
