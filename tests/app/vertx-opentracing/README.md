# Vertx Application instrumented using Opentracing API

This application provides a simple order management system. When a user places an order, the Order Manager communicates with:

- Account Manager to verify the account id is known
- Inventory Manager to check there is enough stock
- Once the order is confirmed and acknowledged to the user, it will be stored in the Order Log and the Inventory Manager
will be updated.

NOTE: Once the end user response has been returned, to confirm the order, the notification to the Order Log and Inventory
Manager is performed via publishing a message on a shared destination.

The other operation that can be performed by an end user is to list the current orders associated with an account id.

## Run

First step is to start up the APM server.

Then open a console window from this folder and build the module:

```shell
$ mvn clean install -Pitest
```
Note: the `itest` profile is required if intending to run using docker.

Now you can run the example vert.x application, using either maven:

```shell
$ export HAWKULAR_APM_URI=http://localhost:8080
$ export HAWKULAR_APM_USERNAME=jdoe
$ export HAWKULAR_APM_PASSWORD=password
$ mvn exec:java
```
or docker:

```shell
$ docker run -it --rm -p 8180:8180 -e HAWKULAR_APM_URI=http://172.17.0.1:8080 -e HAWKULAR_APM_USERNAME=jdoe -e HAWKULAR_APM_PASSWORD=password hawkular/apm-tests-app-vertx-opentracing
```

These commands use the username _jdoe_ and password _password_. These values should be replaced by appropriate credentials
configured within your APM server.


## Example requests

Valid account ids are `fred`, `joe`, `jane`, `steve` and `brian`.

Valid items are `laptop` (quantity 5), `car` (quantity 8), `book` (quantity 9), `chair` (quantity 7) and `dvd` (quantity 6).

To place a valid order, call:

```shell
curl -X POST -d '{"accountId":"fred","itemId":"laptop","quantity":2}' http://localhost:8180/orders

OR

./order.sh fred laptop 2
```

Try changing the account id, item id or quantity (i.e. 6) and see the various error messages. For example,

```shell
curl -X POST -d '{"accountId":"sarah","itemId":"laptop","quantity":2}' http://localhost:8180/orders

OR

./order.sh sarah laptop 2
```

will generated an `Account not found` error message.

Another command that can be performed is to list the current orders for an account:

```shell
curl -X GET -d '{"accountId":"fred"}' http://localhost:8180/orders

OR

./list.sh fred
```


