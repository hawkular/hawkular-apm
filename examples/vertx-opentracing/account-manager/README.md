Account Manager
===

This is a simple microservice that demonstrates a service that is not exposed to the public internet but is able to join
a clustered event bus and, as such, receive and respond to events.

Setting it up
---

In addition to the [main instructions](../README.md), the following has to be executed from this directory to deploy
this as a microservice in OpenShift:

    oc new-build --binary --name=account-manager -l app=account-manager
    oc start-build account-manager --from-dir=. --follow
    oc new-app account-manager HAWKULAR_APM_URI="http://hawkular-apm" HAWKULAR_APM_USERNAME="admin" HAWKULAR_APM_PASSWORD="password" -l app=account-manager
