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

set -x

# generate the docs
./mvnw clean compile -Pdocgen -DskipTests -Dcheckstyle.skip -Dlicense.skip

FILE_NAME="rest-btm.adoc"
FILE_PATH="server/rest/target/generated/$FILE_NAME"

# don't push the empty docs
[[ -s $FILE_PATH ]] || {
  echo "$FILE_PATH is empty" && exit 1
}

REPO="hawkular/hawkular.github.io"
BRANCH="swagger"
SHA=`curl -Ls https://api.github.com/repos/$REPO/contents/$FILE_NAME?ref=$BRANCH | grep '"sha"' | cut -d '"' -f4`
CONTENT=`openssl enc -base64 -in $FILE_PATH | sed ':a;N;$!ba;s/\n//g'`

# update the adoc file using GitHub api
curl -Lis -X PUT -H "Authorization: token $DEPLOY_TOKEN" -d "{\"path\": \"$FILE_NAME\", \"message\": \"Travis CI (btm): updating swagger documentation\", \"commiter\": {\"name\": \"Travis CI\", \"email\": \"foo@bar.com\"}, \"sha\": \"$SHA\", \"content\": \"$CONTENT\", \"branch\": \"$BRANCH\"}" https://api.github.com/repos/$REPO/contents/$FILE_NAME

