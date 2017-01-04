#!/usr/bin/env bash
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

while :
do
    declare -a AccountIds=('joe' 'sarah' 'fred' 'jane' 'brian' 'steve');

    declare -a ItemIds=('laptop' 'car' 'book' 'chair' 'table' 'pencil' 'dvd');

    itemId=`echo ${ItemIds[$[ RANDOM % ${#ItemIds[@]} ]]}`
    accountId=`echo ${AccountIds[$[ RANDOM % ${#AccountIds[@]} ]]}`

    quantity=`echo $[ 1 + $[ RANDOM % 10 ]]`

    echo "Creating an order for account=$accountId item=$itemId quantity=$quantity"

    data="{\"accountId\":\"$accountId\",\"itemId\":\"$itemId\",\"quantity\": $quantity}"

    curl -X POST -H "Content-Type: application/json" -d "$data" http://localhost:8180/orders

    echo
    echo

    sleep 0.3
done

