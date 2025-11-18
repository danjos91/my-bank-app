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
            notificationType: "ACCOUNT_CREATED",
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
            id: $(anyNumber()),
            userId: 1,
            notificationType: "ACCOUNT_CREATED",
            title: "Test Notification",
            message: "This is a test notification",
            isRead: false,
            createdAt: $(regex('[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?')),
            readAt: null,
            formattedCreatedAt: $(regex('.*')),
            formattedReadAt: null
        ])
    }
}
