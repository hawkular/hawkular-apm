#!/bin/bash

curl -ivX POST -H 'Content-Type: application/json' 'http://localhost:8080/ticket-monster/rest/bookings' -d \
    '{"ticketRequests":[{"ticketPrice":5,"quantity":1},{"ticketPrice":12,"quantity":2}],"email":"user@email.com","performance":"3"}'
