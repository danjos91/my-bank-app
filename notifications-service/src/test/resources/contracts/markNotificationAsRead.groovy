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
            notificationType: "ACCOUNT_CREATED",
            title: "Test Notification",
            message: "This is a test notification",
            isRead: true,
            createdAt: $(regex('[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?')),
            readAt: $(regex('[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?')),
            formattedCreatedAt: $(regex('.*')),
            formattedReadAt: $(regex('.*'))
        ])
    }
}
