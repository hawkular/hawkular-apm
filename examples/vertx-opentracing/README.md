# OpenTracing - Microservices with vert.x

This application provides a simple order management system. When a user places an order, the Order Manager communicates with:

- Account Manager to verify the account id is known
- Inventory Manager to check there is enough stock
- Once the order is confirmed and acknowledged to the user, it will be stored in the Order Log and the Inventory Manager
will be updated.

NOTE: Once the end user response has been returned, to confirm the order, the notification to the Order Log and Inventory
Manager is performed via publishing a message on a shared destination.

The other operation that can be performed by an end user is to list the current orders associated with an account id.

This example features a pub-sub architecture via an event bus, using [Eclipse Vert.x](http://vertx.io). Each `Verticle` is a
microservice and is deployed into a different service under OpenShift. Hawkular APM acts here as an OpenTracing provider and is
able to capture data from all services, displaying this data on its UI, like the screenshots below:

![Distributed tracing - Aggregated view](screenshot-1.png?raw=true "Distributed tracing - Aggregated view")


![Distributed tracing - Instance view](screenshot-2.png?raw=true "Distributed tracing - Instance view")

## Running on a local OpenShift cluster

The following sections show how to deploy this example, along with an instance of Hawkular APM on OpenShift.

### Setting it up - Option 1 - Automated with Ansible

To setup the whole infra needed for this example, just run the following command:

    ansible-playbook vertx-opentracing.yml

If you don't have Ansible installed, refer to its [installation instructions](http://docs.ansible.com/ansible/intro_installation.html).
For recent versions of Fedora, it might be as easy as running `dnf install ansible`.

This might take a while to finish, specially on the first run. It creates an OpenShift cluster, deploys a Hawkular APM server,
builds the examples and install them as services into OpenShift.

You'll need to make note of the hostname for the `order-manager` service, or you can run this command:

    oc get route order-manager

To access the OpenShift console, type in `oc status` and check the address on the first line of the output. The username and
password is `developer` / `developer`

At the end of the process, you should be able to get a JSON as response to this command:

    curl -X POST -d '{"accountId":"fred","itemId":"laptop","quantity":2}' http://order-manager-order-manager.<your OpenShift IP>.xip.io/orders

See [Example Requests](#example-requests) for further commands.

### Setting it up - Option 2 - Manual

To run this, you'll need an OpenShift cluster (local or otherwise), with Hawkular APM running on it. This can be accomplished
with the following commands on Fedora 24 (or later):

    sudo iptables -F
    oc cluster up
    oc create -f https://raw.githubusercontent.com/jboss-dockerfiles/hawkular-apm/master/openshift-templates/hawkular-apm-server-deployment.yml

The `oc` command is available on the package `origin-clients`. On a recent version of Fedora, it can be installed with `dnf install origin-clients`

Make note of the UI address provided by the `oc cluster up` command.
Once Hawkular APM is up and running, the example as a whole needs to be build and the microservices needs to be added to OpenShift.
The following commands can be executed in order to accomplish that:

    mvn clean install
    ## account manager
    cd account-manager
    oc new-build --binary --name=account-manager -l app=account-manager
    oc start-build account-manager --from-dir=. --follow
    oc new-app account-manager HAWKULAR_APM_URI="http://hawkular-apm" HAWKULAR_APM_USERNAME="admin" HAWKULAR_APM_PASSWORD="password" -l app=account-manager
    cd ..
    ## inventory manager
    cd inventory-manager
    oc new-build --binary --name=inventory-manager -l app=inventory-manager
    oc start-build inventory-manager --from-dir=. --follow
    oc new-app inventory-manager HAWKULAR_APM_URI="http://hawkular-apm" HAWKULAR_APM_USERNAME="admin" HAWKULAR_APM_PASSWORD="password" -l app=inventory-manager
    cd ..
    ## order log
    cd order-log
    oc new-build --binary --name=order-log -l app=order-log
    oc start-build order-log --from-dir=. --follow
    oc new-app order-log HAWKULAR_APM_URI="http://hawkular-apm" HAWKULAR_APM_USERNAME="admin" HAWKULAR_APM_PASSWORD="password" -l app=order-log
    cd ..
    ## order manager
    cd order-manager
    oc new-build --binary --name=order-manager -l app=order-manager
    oc start-build order-manager --from-dir=. --follow
    oc new-app order-manager HAWKULAR_APM_URI="http://hawkular-apm" HAWKULAR_APM_USERNAME="admin" HAWKULAR_APM_PASSWORD="password" -l app=order-manager
    oc expose service order-manager
    cd ..

Login to the OpenShift UI at the address given by the `oc cluster up` command and wait until all services are "blue", meaning
that they are ready. At this point, you can execute the following command to test, replacing the hostname by the appropriate value,
which can be seen on the OpenShift UI:

    curl -X POST -d '{"accountId":"fred","itemId":"laptop","quantity":2}' http://order-manager-myproject.<your OpenShift IP>.xip.io/orders

You can find the actual hostname to the Order Manager example with the following command:

  oc get route order-manager

The Hawkular APM UI can be accessed via https://hawkular-apm-myproject.<your OpenShift IP>.xip.io/ . The username is `admin` and the
password is `password`.

See [Example Requests](#example-requests) for further commands.

## Running on local host

It's possible to run the examples outside of OpenShift as well. Assuming a Hawkular APM server running on `http://localhost:8080`,
it's necessary to first export the required environment variables:

    export HAWKULAR_APM_URI="http://127.0.0.1:8080"
    export HAWKULAR_APM_USERNAME="jdoe"
    export HAWKULAR_APM_PASSWORD="password"

Then, each microservice has to be started, possibly on its own terminal (don't forget to set the env vars above on each terminal).
From the directory of this README file, run:

    java -jar account-manager/target/hawkular-apm-examples-vertx-opentracing-account-manager-fat.jar -cluster -DHAWKULAR_APM_SERVICE_NAME=account-manager
    java -jar inventory-manager/target/hawkular-apm-examples-vertx-opentracing-inventory-manager-fat.jar -cluster -DHAWKULAR_APM_SERVICE_NAME=inventory-manager
    java -jar order-log/target/hawkular-apm-examples-vertx-opentracing-order-log-fat.jar -cluster -DHAWKULAR_APM_SERVICE_NAME=order-log
    java -jar order-manager/target/hawkular-apm-examples-vertx-opentracing-order-manager-fat.jar -cluster -conf order-manager/src/main/resources/config.json -DHAWKULAR_APM_SERVICE_NAME=order-manager

Note that this last one has a `-conf` option, passing a JSON file as parameter. This JSON file sets the port to 8180, as we have
a Hawkular APM already on port 8080.

To test, run:

    curl -X POST -d '{"accountId":"fred","itemId":"laptop","quantity":2}' http://127.0.0.1:8180/orders

See [Example Requests](#example-requests) for further commands.

## Example requests

For the following examples, we'll use `${ORDER_MANAGER_SERVER}` to refer to the example's server. It might be a simple hostname,
like when it's deployed on OpenShift, or `hostname:port`, like when it's deployed locally (`localhost:8180`, for instance).

Valid account ids are `fred`, `joe`, `jane`, `steve` and `brian`.

Valid items are `laptop` (quantity 5), `car` (quantity 8), `book` (quantity 9), `chair` (quantity 7) and `dvd` (quantity 6).

To place a valid order, call:

```shell
curl -X POST -d '{"accountId":"fred","itemId":"laptop","quantity":2}' http://${ORDER_MANAGER_SERVER}/orders

OR

./order.sh fred laptop 2
```

Try changing the account id, item id or quantity (i.e. 6) and see the various error messages. For example,

```shell
curl -X POST -d '{"accountId":"sarah","itemId":"laptop","quantity":2}' http://${ORDER_MANAGER_SERVER}/orders

OR

./order.sh sarah laptop 2
```

will generated an `Account not found` error message.

Another command that can be performed is to list the current orders for an account:

```shell
curl -X GET -d '{"accountId":"fred"}' http://${ORDER_MANAGER_SERVER}/orders

OR

./list.sh fred
```
