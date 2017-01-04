#!/bin/bash
#
# Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
# and other contributors as indicated by the @author tags.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


curl -ivX POST -H 'Content-Type: application/json' 'http://localhost:8080/ticket-monster/rest/bookings' -d \
    '{"ticketRequests":[{"ticketPrice":5,"quantity":1},{"ticketPrice":12,"quantity":2}],"email":"user@email.com","performance":"3"}'
