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
                notificationType: "ACCOUNT_CREATED",
                title: "Test Notification",
                message: "This is a test notification",
                isRead: false,
                createdAt: $(regex('[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?')),
                readAt: null,
                formattedCreatedAt: $(regex('.*')),
                formattedReadAt: null
            ]
        ])
    }
}
