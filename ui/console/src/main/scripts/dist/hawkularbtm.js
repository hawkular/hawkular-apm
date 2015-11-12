/// Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///   http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
/// Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///   http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
/// <reference path="../libs/hawtio-utilities/defs.d.ts"/>

/// Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///   http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
/// <reference path="../../includes.ts"/>
var BTM;
(function (BTM) {
    BTM.pluginName = "hawtio-assembly";
    BTM.log = Logger.get(BTM.pluginName);
    BTM.templatePath = "plugins/btm/html";
})(BTM || (BTM = {}));

/// Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///   http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
/// <reference path="../../includes.ts"/>
/// <reference path="btmGlobals.ts"/>
var BTM;
(function (BTM) {
    BTM._module = angular.module(BTM.pluginName, ["xeditable", "ui.bootstrap"]);
    var tab = undefined;
    BTM._module.config(["$locationProvider", "$routeProvider", "HawtioNavBuilderProvider",
        function ($locationProvider, $routeProvider, builder) {
            tab = builder.create()
                .id(BTM.pluginName)
                .title(function () { return "Business Transactions"; })
                .href(function () { return "/"; })
                .build();
            builder.configureRouting($routeProvider, tab);
            $locationProvider.html5Mode(true);
            $routeProvider.
                when('/', {
                templateUrl: 'plugins/btm/html/btm.html',
                controller: 'BTM.BTMController'
            }).
                when('/active', {
                templateUrl: 'plugins/btm/html/btm.html',
                controller: 'BTM.BTMController'
            }).
                when('/candidates', {
                templateUrl: 'plugins/btm/html/btxncandidates.html',
                controller: 'BTM.BTMCandidatesController'
            }).
                when('/disabled', {
                templateUrl: 'plugins/btm/html/btxndisabled.html',
                controller: 'BTM.BTMDisabledController'
            }).
                when('/ignored', {
                templateUrl: 'plugins/btm/html/btxnignored.html',
                controller: 'BTM.BTMIgnoredController'
            }).
                when('/config/:businesstransaction', {
                templateUrl: 'plugins/btm/html/btxnconfig.html',
                controller: 'BTM.BTxnConfigController'
            }).
                when('/info/:businesstransaction', {
                templateUrl: 'plugins/btm/html/btxninfo.html',
                controller: 'BTM.BTxnInfoController'
            });
        }]);
    BTM._module.run(function ($http, $location) {
        if ($location.absUrl().indexOf('http://localhost:2772/') === 0) {
            $http.defaults.headers.common.Authorization = 'Basic amRvZTpwYXNzd29yZA==';
        }
    });
    BTM._module.run(function (editableOptions) {
        editableOptions.theme = 'bs3';
    });
    BTM._module.run(["HawtioNav", function (HawtioNav) {
            HawtioNav.add(tab);
            BTM.log.debug("loaded");
        }]);
    hawtioPluginLoader.addModule(BTM.pluginName);
})(BTM || (BTM = {}));

/// Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///   http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
/// <reference path="btmPlugin.ts"/>
var BTM;
(function (BTM) {
    BTM.BTMController = BTM._module.controller("BTM.BTMController", ["$scope", "$http", '$location', '$interval', function ($scope, $http, $location, $interval) {
            $scope.newBTxnName = '';
            $scope.candidateCount = 0;
            $scope.reload = function () {
                $http.get('/hawkular/btm/config/businesstxnsummary').then(function (resp) {
                    $scope.businessTransactions = [];
                    for (var i = 0; i < resp.data.length; i++) {
                        var btxn = {
                            summary: resp.data[i],
                            count: undefined,
                            faultcount: undefined,
                            percentile95: undefined,
                            alerts: undefined
                        };
                        $scope.businessTransactions.add(btxn);
                        $scope.getBusinessTxnDetails(btxn);
                    }
                }, function (resp) {
                    console.log("Failed to get business txn summaries: " + resp);
                });
                $http.get('/hawkular/btm/analytics/businesstxn/unbounduris').then(function (resp) {
                    $scope.candidateCount = Object.keys(resp.data).length;
                }, function (resp) {
                    console.log("Failed to get candidate count: " + resp);
                });
            };
            $scope.reload();
            $interval(function () {
                $scope.reload();
            }, 10000);
            $scope.getBusinessTxnDetails = function (btxn) {
                $http.get('/hawkular/btm/analytics/businesstxn/completion/count?name=' + btxn.summary.name).then(function (resp) {
                    btxn.count = resp.data;
                }, function (resp) {
                    console.log("Failed to get count: " + resp);
                });
                $http.get('/hawkular/btm/analytics/businesstxn/completion/percentiles?name=' + btxn.summary.name).then(function (resp) {
                    if (resp.data.percentiles[95] > 0) {
                        btxn.percentile95 = Math.round(resp.data.percentiles[95] / 1000000) / 1000;
                    }
                    else {
                        btxn.percentile95 = 0;
                    }
                }, function (resp) {
                    console.log("Failed to get completion percentiles: " + resp);
                });
                $http.get('/hawkular/btm/analytics/businesstxn/completion/faultcount?name=' + btxn.summary.name).then(function (resp) {
                    btxn.faultcount = resp.data;
                }, function (resp) {
                    console.log("Failed to get fault count: " + resp);
                });
                $http.get('/hawkular/btm/analytics/alerts/count/' + btxn.summary.name).then(function (resp) {
                    btxn.alerts = resp.data;
                }, function (resp) {
                    console.log("Failed to get alerts count: " + resp);
                });
            };
            $scope.deleteBusinessTxn = function (btxn) {
                if (confirm('Are you sure you want to delete business transaction \"' + btxn.summary.name + '\"?')) {
                    $http.delete('/hawkular/btm/config/businesstxn/' + btxn.summary.name).then(function (resp) {
                        console.log('Deleted: ' + btxn.summary.name);
                        $scope.businessTransactions.remove(btxn);
                    }, function (resp) {
                        console.log("Failed to delete business txn '" + btxn.summary.name + "': " + resp);
                    });
                }
            };
        }]);
})(BTM || (BTM = {}));

/// Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///   http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
/// <reference path="btmPlugin.ts"/>
var BTM;
(function (BTM) {
    BTM.BTMCandidatesController = BTM._module.controller("BTM.BTMCandidatesController", ["$scope", "$http", '$location', '$uibModal', '$interval', function ($scope, $http, $location, $uibModal, $interval) {
            $scope.newBTxnName = '';
            $scope.existingBTxnName = '';
            $scope.selecteduris = [];
            $scope.candidateCount = 0;
            $http.get('/hawkular/btm/config/businesstxnsummary').then(function (resp) {
                $scope.businessTransactions = resp.data;
            }, function (resp) {
                console.log("Failed to get business txn summaries: " + resp);
            });
            $scope.reload = function () {
                $http.get('/hawkular/btm/analytics/businesstxn/unbounduris').then(function (resp) {
                    $scope.unbounduris = resp.data;
                    $scope.candidateCount = Object.keys(resp.data).length;
                }, function (resp) {
                    console.log("Failed to get unbound URIs: " + resp);
                });
            };
            $scope.reload();
            $interval(function () {
                $scope.reload();
            }, 10000);
            $scope.addBusinessTxn = function () {
                var defn = {
                    filter: {
                        inclusions: $scope.selecteduris
                    }
                };
                $http.put('/hawkular/btm/config/businesstxn/' + $scope.newBTxnName, defn).then(function (resp) {
                    $location.path('config/' + $scope.newBTxnName);
                }, function (resp) {
                    console.log("Failed to add business txn '" + $scope.newBTxnName + "': " + resp);
                });
            };
            $scope.ignoreBusinessTxn = function () {
                var defn = {
                    level: 'Ignore',
                    filter: {
                        inclusions: $scope.selecteduris
                    }
                };
                $http.put('/hawkular/btm/config/businesstxn/' + $scope.newBTxnName, defn).then(function (resp) {
                    $location.path('config/' + $scope.newBTxnName);
                }, function (resp) {
                    console.log("Failed to ignore business txn '" + $scope.newBTxnName + "': " + resp);
                });
            };
            $scope.updateBusinessTxn = function () {
                $http.get('/hawkular/btm/config/businesstxn/' + $scope.existingBTxnName).then(function (resp) {
                    var btxn = resp.data;
                    for (var i = 0; i < $scope.selecteduris.length; i++) {
                        if (btxn.filter.inclusions.indexOf($scope.selecteduris[i]) === -1) {
                            btxn.filter.inclusions.add($scope.selecteduris[i]);
                        }
                    }
                    $http.put('/hawkular/btm/config/businesstxn/' + $scope.existingBTxnName, btxn).then(function (resp) {
                        console.log("Saved updated business txn '" + $scope.existingBTxnName + "': " + resp);
                        $location.path('config/' + $scope.existingBTxnName);
                    }, function (resp) {
                        console.log("Failed to save business txn '" + $scope.existingBTxnName + "': " + resp);
                    });
                }, function (resp) {
                    console.log("Failed to get business txn '" + $scope.existingBTxnName + "': " + resp);
                });
            };
            $scope.selectionChanged = function (uri) {
                var regex = $scope.escapeRegExp(uri);
                if ($scope.selecteduris.contains(regex)) {
                    $scope.selecteduris.remove(regex);
                }
                else {
                    $scope.selecteduris.add(regex);
                }
            };
            $scope.isSelected = function (uri) {
                var regex = $scope.escapeRegExp(uri);
                return $scope.selecteduris.contains(regex);
            };
            $scope.getLevel = function (level) {
                if (level === 'All') {
                    return "Active";
                }
                return level;
            };
            $scope.escapeRegExp = function (str) {
                return "^" + str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&") + "$";
            };
        }]);
})(BTM || (BTM = {}));

/// Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///   http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
/// <reference path="btmPlugin.ts"/>
var BTM;
(function (BTM) {
    BTM.BTxnConfigController = BTM._module.controller("BTM.BTxnConfigController", ["$scope", "$routeParams", "$http", '$location', '$interval', function ($scope, $routeParams, $http, $location, $interval) {
            $scope.businessTransactionName = $routeParams.businesstransaction;
            $scope.dirty = false;
            $scope.newInclusionFilter = '';
            $scope.newExclusionFilter = '';
            $http.get('/hawkular/btm/config/businesstxn/' + $scope.businessTransactionName).then(function (resp) {
                $scope.businessTransaction = resp.data;
                $scope.original = angular.copy($scope.businessTransaction);
            }, function (resp) {
                console.log("Failed to get business txn '" + $scope.businessTransactionName + "': " + resp);
            });
            $http.get('/hawkular/btm/analytics/businesstxn/unbounduris').then(function (resp) {
                $scope.unboundURIs = [];
                for (var key in resp.data) {
                    if (key !== undefined) {
                        var array = resp.data[key];
                        for (var i = 0; i < array.length; i++) {
                            var regex = $scope.escapeRegExp(array[i]);
                            $scope.unboundURIs.add(regex);
                        }
                    }
                }
            }, function (resp) {
                console.log("Failed to get unbound URIs: " + resp);
            });
            $scope.reload = function () {
                $http.get('/hawkular/btm/analytics/businesstxn/bounduris/' + $scope.businessTransactionName).then(function (resp) {
                    $scope.boundURIs = [];
                    for (var i = 0; i < resp.data.length; i++) {
                        var regex = $scope.escapeRegExp(resp.data[i]);
                        $scope.boundURIs.add(regex);
                    }
                }, function (resp) {
                    console.log("Failed to get bound URIs for business txn '" + $scope.businessTransactionName + "': " + resp);
                });
            };
            $scope.reload();
            $interval(function () {
                $scope.reload();
            }, 10000);
            $scope.addInclusionFilter = function () {
                console.log('Add inclusion filter: ' + $scope.newInclusionFilter);
                if ($scope.businessTransaction.filter === null) {
                    $scope.businessTransaction.filter = {
                        inclusions: [],
                        exclusions: []
                    };
                }
                $scope.businessTransaction.filter.inclusions.add($scope.newInclusionFilter);
                $scope.setDirty();
                $scope.newInclusionFilter = '';
            };
            $scope.removeInclusionFilter = function (inclusion) {
                $scope.businessTransaction.filter.inclusions.remove(inclusion);
                $scope.setDirty();
            };
            $scope.addExclusionFilter = function () {
                console.log('Add exclusion filter: ' + $scope.newExclusionFilter);
                if ($scope.businessTransaction.filter === null) {
                    $scope.businessTransaction.filter = {
                        inclusions: [],
                        exclusions: []
                    };
                }
                $scope.businessTransaction.filter.exclusions.add($scope.newExclusionFilter);
                $scope.setDirty();
                $scope.newExclusionFilter = '';
            };
            $scope.removeExclusionFilter = function (exclusion) {
                $scope.businessTransaction.filter.exclusions.remove(exclusion);
                $scope.setDirty();
            };
            $scope.getExpressionText = function (expression) {
                if (expression === undefined) {
                    return "";
                }
                if (expression.type === "XML") {
                    return expression.source + "[" + expression.key + "]" + " xpath=" + expression.xpath;
                }
                if (expression.type === "JSON") {
                    return expression.source + "[" + expression.key + "]" + " jsonpath=" + expression.jsonpath;
                }
                if (expression.type === "Text") {
                    return expression.source + "[" + expression.key + "]";
                }
                return "Unknown expression type";
            };
            $scope.changedExpressionType = function (expression) {
                $scope.setDirty();
                expression.key = undefined;
                expression.source = undefined;
                expression.xpath = undefined;
                expression.jsonpath = undefined;
                if (expression.type === 'XML' || expression.type === 'JSON' || expression.type === 'Text') {
                    expression.key = '0';
                    expression.source = 'Content';
                }
            };
            $scope.changedActionType = function (action) {
                $scope.setDirty();
                action.name = undefined;
                action.type = undefined;
                action.scope = undefined;
                action.template = undefined;
                action.predicate = undefined;
                action.expression = undefined;
            };
            $scope.addProcessor = function () {
                $scope.setDirty();
                $scope.businessTransaction.processors.add({
                    description: "Processor " + ($scope.businessTransaction.processors.length + 1),
                    nodeType: "Consumer",
                    direction: "In",
                    actions: []
                });
            };
            $scope.deleteProcessor = function (processor) {
                if (confirm('Are you sure you want to delete the processor?')) {
                    $scope.setDirty();
                    $scope.businessTransaction.processors.remove(processor);
                }
            };
            $scope.addAction = function (processor) {
                $scope.setDirty();
                processor.actions.add({
                    description: "Action " + (processor.actions.length + 1)
                });
            };
            $scope.deleteAction = function (processor, action) {
                if (confirm('Are you sure you want to delete the action?')) {
                    $scope.setDirty();
                    processor.actions.remove(action);
                }
            };
            $scope.setDirty = function () {
                $scope.dirty = true;
            };
            $scope.reset = function () {
                $scope.businessTransaction = angular.copy($scope.original);
                $scope.dirty = false;
            };
            $scope.save = function () {
                $http.put('/hawkular/btm/config/businesstxn/' + $scope.businessTransactionName, $scope.businessTransaction).then(function (resp) {
                    $scope.original = angular.copy($scope.businessTransaction);
                    $scope.dirty = false;
                }, function (resp) {
                    console.log("Failed to save business txn '" + $scope.businessTransactionName + "': " + resp);
                });
            };
            $http.get('/hawkular/btm/config/businesstxn/' + $scope.businessTransactionName).then(function (resp) {
                $scope.businessTransaction = resp.data;
                $scope.original = angular.copy($scope.businessTransaction);
            }, function (resp) {
                console.log("Failed to get business txn '" + $scope.businessTransactionName + "': " + resp);
            });
            $scope.escapeRegExp = function (str) {
                return "^" + str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&") + "$";
            };
        }]);
})(BTM || (BTM = {}));

/// Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///   http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
/// <reference path="btmPlugin.ts"/>
var BTM;
(function (BTM) {
    BTM.BTMDisabledController = BTM._module.controller("BTM.BTMDisabledController", ["$scope", "$http", '$location', '$interval', function ($scope, $http, $location, $interval) {
            $scope.newBTxnName = '';
            $scope.candidateCount = 0;
            $scope.reload = function () {
                $http.get('/hawkular/btm/config/businesstxnsummary').then(function (resp) {
                    $scope.businessTransactions = [];
                    for (var i = 0; i < resp.data.length; i++) {
                        var btxn = {
                            summary: resp.data[i]
                        };
                        $scope.businessTransactions.add(btxn);
                    }
                }, function (resp) {
                    console.log("Failed to get business txn summaries: " + resp);
                });
                $http.get('/hawkular/btm/analytics/businesstxn/unbounduris').then(function (resp) {
                    $scope.candidateCount = Object.keys(resp.data).length;
                }, function (resp) {
                    console.log("Failed to get candidate count: " + resp);
                });
            };
            $scope.reload();
            $scope.deleteBusinessTxn = function (btxn) {
                if (confirm('Are you sure you want to delete business transaction \"' + btxn.summary.name + '\"?')) {
                    $http.delete('/hawkular/btm/config/businesstxn/' + btxn.summary.name).then(function (resp) {
                        console.log('Deleted: ' + btxn.summary.name);
                        $scope.businessTransactions.remove(btxn);
                    }, function (resp) {
                        console.log("Failed to delete business txn '" + btxn.summary.name + "': " + resp);
                    });
                }
            };
        }]);
})(BTM || (BTM = {}));

/// Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///   http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
/// <reference path="btmPlugin.ts"/>
var BTM;
(function (BTM) {
    BTM.BTMIgnoredController = BTM._module.controller("BTM.BTMIgnoredController", ["$scope", "$http", '$location', '$interval', function ($scope, $http, $location, $interval) {
            $scope.newBTxnName = '';
            $scope.candidateCount = 0;
            $scope.reload = function () {
                $http.get('/hawkular/btm/config/businesstxnsummary').then(function (resp) {
                    $scope.businessTransactions = [];
                    for (var i = 0; i < resp.data.length; i++) {
                        var btxn = {
                            summary: resp.data[i]
                        };
                        $scope.businessTransactions.add(btxn);
                    }
                }, function (resp) {
                    console.log("Failed to get business txn summaries: " + resp);
                });
                $http.get('/hawkular/btm/analytics/businesstxn/unbounduris').then(function (resp) {
                    $scope.candidateCount = Object.keys(resp.data).length;
                }, function (resp) {
                    console.log("Failed to get candidate count: " + resp);
                });
            };
            $scope.reload();
            $scope.deleteBusinessTxn = function (btxn) {
                if (confirm('Are you sure you want to delete business transaction \"' + btxn.summary.name + '\"?')) {
                    $http.delete('/hawkular/btm/config/businesstxn/' + btxn.summary.name).then(function (resp) {
                        console.log('Deleted: ' + btxn.summary.name);
                        $scope.businessTransactions.remove(btxn);
                    }, function (resp) {
                        console.log("Failed to delete business txn '" + btxn.summary.name + "': " + resp);
                    });
                }
            };
        }]);
})(BTM || (BTM = {}));

/// Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///   http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
/// <reference path="btmPlugin.ts"/>
var BTM;
(function (BTM) {
    BTM.BTxnInfoController = BTM._module.controller("BTM.BTxnInfoController", ["$scope", "$routeParams", "$http", '$location', '$interval', function ($scope, $routeParams, $http, $location, $interval) {
            $scope.businessTransactionName = $routeParams.businesstransaction;
            $scope.criteria = {
                name: $scope.businessTransactionName,
                startTime: -3600000,
                endTime: 0
            };
            $scope.config = {
                interval: 60000
            };
            $scope.reload = function () {
                $http.post('/hawkular/btm/analytics/businesstxn/completion/statistics?interval=' + $scope.config.interval, $scope.criteria).then(function (resp) {
                    $scope.statistics = resp.data;
                    $scope.ctlinechart.load({
                        json: $scope.statistics,
                        keys: {
                            value: ['min', 'average', 'max', 'count'],
                            x: 'timestamp'
                        }
                    });
                }, function (resp) {
                    console.log("Failed to get statistics: " + resp);
                });
            };
            $scope.reload();
            $interval(function () {
                $scope.reload();
            }, 10000);
            $scope.initGraph = function () {
                $scope.ctlinechart = c3.generate({
                    bindto: '#completiontimelinechart',
                    data: {
                        json: [],
                        axes: {
                            min: 'y',
                            average: 'y',
                            max: 'y',
                            count: 'y2'
                        },
                        type: 'line',
                        types: {
                            count: 'bar'
                        },
                        keys: {
                            value: ['min', 'average', 'max', 'count'],
                            x: 'timestamp'
                        }
                    },
                    color: {
                        pattern: ['#e5e600', '#33cc33', '#ff0000', '#99ccff']
                    },
                    axis: {
                        x: {
                            type: 'timeseries',
                            tick: {
                                culling: {
                                    max: 6
                                },
                                format: '%Y-%m-%d %H:%M:%S'
                            }
                        },
                        y2: {
                            show: true
                        }
                    }
                });
            };
            $scope.initGraph();
        }]);
})(BTM || (BTM = {}));

//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbImluY2x1ZGVzLnRzIiwiYnRtL3RzL2J0bUdsb2JhbHMudHMiLCJidG0vdHMvYnRtUGx1Z2luLnRzIiwiYnRtL3RzL2J0bS50cyIsImJ0bS90cy9idHhuY2FuZGlkYXRlcy50cyIsImJ0bS90cy9idHhuY29uZmlnLnRzIiwiYnRtL3RzL2J0eG5kaXNhYmxlZC50cyIsImJ0bS90cy9idHhuaWdub3JlZC50cyIsImJ0bS90cy9idHhuaW5mby50cyJdLCJuYW1lcyI6WyJCVE0iXSwibWFwcGluZ3MiOiJBQUFBLDJEQUEyRDtBQUMzRCw0REFBNEQ7QUFDNUQsR0FBRztBQUNILG1FQUFtRTtBQUNuRSxvRUFBb0U7QUFDcEUsMkNBQTJDO0FBQzNDLEdBQUc7QUFDSCxnREFBZ0Q7QUFDaEQsR0FBRztBQUNILHVFQUF1RTtBQUN2RSxxRUFBcUU7QUFDckUsNEVBQTRFO0FBQzVFLHVFQUF1RTtBQUN2RSxrQ0FBa0M7QUFFbEMsMERBQTBEOztBQ2YxRCwyREFBMkQ7QUFDM0QsNERBQTREO0FBQzVELEdBQUc7QUFDSCxtRUFBbUU7QUFDbkUsb0VBQW9FO0FBQ3BFLDJDQUEyQztBQUMzQyxHQUFHO0FBQ0gsZ0RBQWdEO0FBQ2hELEdBQUc7QUFDSCx1RUFBdUU7QUFDdkUscUVBQXFFO0FBQ3JFLDRFQUE0RTtBQUM1RSx1RUFBdUU7QUFDdkUsa0NBQWtDO0FBRWxDLHlDQUF5QztBQUN6QyxJQUFPLEdBQUcsQ0FPVDtBQVBELFdBQU8sR0FBRyxFQUFDLENBQUM7SUFFQ0EsY0FBVUEsR0FBR0EsaUJBQWlCQSxDQUFDQTtJQUUvQkEsT0FBR0EsR0FBbUJBLE1BQU1BLENBQUNBLEdBQUdBLENBQUNBLGNBQVVBLENBQUNBLENBQUNBO0lBRTdDQSxnQkFBWUEsR0FBR0Esa0JBQWtCQSxDQUFDQTtBQUMvQ0EsQ0FBQ0EsRUFQTSxHQUFHLEtBQUgsR0FBRyxRQU9UOztBQ3ZCRCwyREFBMkQ7QUFDM0QsNERBQTREO0FBQzVELEdBQUc7QUFDSCxtRUFBbUU7QUFDbkUsb0VBQW9FO0FBQ3BF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QUU7NEJBQ0YsSUFBSSxFQUFFLElBQUk7eUJBQ1g7cUJBQ0Y7aUJBQ0YsQ0FBQyxDQUFDO1lBRUwsQ0FBQyxDQUFDQTtZQUVGQSxNQUFNQSxDQUFDQSxTQUFTQSxFQUFFQSxDQUFDQTtRQUVyQkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7QUFFTkEsQ0FBQ0EsRUF2Rk0sR0FBRyxLQUFILEdBQUcsUUF1RlQiLCJmaWxlIjoiY29tcGlsZWQuanMiLCJzb3VyY2VzQ29udGVudCI6WyIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cIi4uL2xpYnMvaGF3dGlvLXV0aWxpdGllcy9kZWZzLmQudHNcIi8+XG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cIi4uLy4uL2luY2x1ZGVzLnRzXCIvPlxubW9kdWxlIEJUTSB7XG5cbiAgZXhwb3J0IHZhciBwbHVnaW5OYW1lID0gXCJoYXd0aW8tYXNzZW1ibHlcIjtcblxuICBleHBvcnQgdmFyIGxvZzogTG9nZ2luZy5Mb2dnZXIgPSBMb2dnZXIuZ2V0KHBsdWdpbk5hbWUpO1xuXG4gIGV4cG9ydCB2YXIgdGVtcGxhdGVQYXRoID0gXCJwbHVnaW5zL2J0bS9odG1sXCI7XG59XG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cIi4uLy4uL2luY2x1ZGVzLnRzXCIvPlxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cImJ0bUdsb2JhbHMudHNcIi8+XG5tb2R1bGUgQlRNIHtcblxuICBleHBvcnQgdmFyIF9tb2R1bGUgPSBhbmd1bGFyLm1vZHVsZShCVE0ucGx1Z2luTmFtZSwgW1wieGVkaXRhYmxlXCIsXCJ1aS5ib290c3RyYXBcIl0pO1xuXG4gIHZhciB0YWIgPSB1bmRlZmluZWQ7XG5cbiAgX21vZHVsZS5jb25maWcoW1wiJGxvY2F0aW9uUHJvdmlkZXJcIiwgXCIkcm91dGVQcm92aWRlclwiLCBcIkhhd3Rpb05hdkJ1aWxkZXJQcm92aWRlclwiLFxuICAgICgkbG9jYXRpb25Qcm92aWRlciwgJHJvdXRlUHJvdmlkZXI6IG5nLnJvdXRlLklSb3V0ZVByb3ZpZGVyLCBidWlsZGVyOiBIYXd0aW9NYWluTmF2LkJ1aWxkZXJGYWN0b3J5KSA9PiB7XG4gICAgdGFiID0gYnVpbGRlci5jcmVhdGUoKVxuICAgICAgLmlkKEJUTS5wbHVnaW5OYW1lKVxuICAgICAgLnRpdGxlKCgpID0+IFwiQnVzaW5lc3MgVHJhbnNhY3Rpb25zXCIpXG4gICAgICAuaHJlZigoKSA9PiBcIi9cIilcbiAgICAgIC5idWlsZCgpO1xuICAgIGJ1aWxkZXIuY29uZmlndXJlUm91dGluZygkcm91dGVQcm92aWRlciwgdGFiKTtcbiAgICAkbG9jYXRpb25Qcm92aWRlci5odG1sNU1vZGUodHJ1ZSk7XG4gICAgJHJvdXRlUHJvdmlkZXIuXG4gICAgICB3aGVuKCcvJywge1xuICAgICAgICB0ZW1wbGF0ZVVybDogJ3BsdWdpbnMvYnRtL2h0bWwvYnRtLmh0bWwnLFxuICAgICAgICBjb250cm9sbGVyOiAnQlRNLkJUTUNvbnRyb2xsZXInXG4gICAgICB9KS5cbiAgICAgIHdoZW4oJy9hY3RpdmUnLCB7XG4gICAgICAgIHRlbXBsYXRlVXJsOiAncGx1Z2lucy9idG0vaHRtbC9idG0uaHRtbCcsXG4gICAgICAgIGNvbnRyb2xsZXI6ICdCVE0uQlRNQ29udHJvbGxlcidcbiAgICAgIH0pLlxuICAgICAgd2hlbignL2NhbmRpZGF0ZXMnLCB7XG4gICAgICAgIHRlbXBsYXRlVXJsOiAncGx1Z2lucy9idG0vaHRtbC9idHhuY2FuZGlkYXRlcy5odG1sJyxcbiAgICAgICAgY29udHJvbGxlcjogJ0JUTS5CVE1DYW5kaWRhdGVzQ29udHJvbGxlcidcbiAgICAgIH0pLlxuICAgICAgd2hlbignL2Rpc2FibGVkJywge1xuICAgICAgICB0ZW1wbGF0ZVVybDogJ3BsdWdpbnMvYnRtL2h0bWwvYnR4bmRpc2FibGVkLmh0bWwnLFxuICAgICAgICBjb250cm9sbGVyOiAnQlRNLkJUTURpc2FibGVkQ29udHJvbGxlcidcbiAgICAgIH0pLlxuICAgICAgd2hlbignL2lnbm9yZWQnLCB7XG4gICAgICAgIHRlbXBsYXRlVXJsOiAncGx1Z2lucy9idG0vaHRtbC9idHhuaWdub3JlZC5odG1sJyxcbiAgICAgICAgY29udHJvbGxlcjogJ0JUTS5CVE1JZ25vcmVkQ29udHJvbGxlcidcbiAgICAgIH0pLlxuICAgICAgd2hlbignL2NvbmZpZy86YnVzaW5lc3N0cmFuc2FjdGlvbicsIHtcbiAgICAgICAgdGVtcGxhdGVVcmw6ICdwbHVnaW5zL2J0bS9odG1sL2J0eG5jb25maWcuaHRtbCcsXG4gICAgICAgIGNvbnRyb2xsZXI6ICdCVE0uQlR4bkNvbmZpZ0NvbnRyb2xsZXInXG4gICAgICB9KS5cbiAgICAgIHdoZW4oJy9pbmZvLzpidXNpbmVzc3RyYW5zYWN0aW9uJywge1xuICAgICAgICB0ZW1wbGF0ZVVybDogJ3BsdWdpbnMvYnRtL2h0bWwvYnR4bmluZm8uaHRtbCcsXG4gICAgICAgIGNvbnRyb2xsZXI6ICdCVE0uQlR4bkluZm9Db250cm9sbGVyJ1xuICAgICAgfSk7XG4gIH1dKTtcblxuICBfbW9kdWxlLnJ1bihmdW5jdGlvbigkaHR0cCwkbG9jYXRpb24pIHtcbiAgICAvLyBPbmx5IHNldCBhdXRob3JpemF0aW9uIGlmIHVzaW5nIGRldmVsb3BtZW50IFVSTFxuICAgIGlmICgkbG9jYXRpb24uYWJzVXJsKCkuaW5kZXhPZignaHR0cDovL2xvY2FsaG9zdDoyNzcyLycpID09PSAwKSB7XG4gICAgICAkaHR0cC5kZWZhdWx0cy5oZWFkZXJzLmNvbW1vbi5BdXRob3JpemF0aW9uID0gJ0Jhc2ljIGFtUnZaVHB3WVhOemQyOXlaQT09JztcbiAgICB9XG4gIH0pO1xuXG4gIF9tb2R1bGUucnVuKGZ1bmN0aW9uKGVkaXRhYmxlT3B0aW9ucykge1xuICAgIGVkaXRhYmxlT3B0aW9ucy50aGVtZSA9ICdiczMnOyAvLyBib290c3RyYXAzIHRoZW1lLiBDYW4gYmUgYWxzbyAnYnMyJywgJ2RlZmF1bHQnXG4gIH0pO1xuXG4gIF9tb2R1bGUucnVuKFtcIkhhd3Rpb05hdlwiLCAoSGF3dGlvTmF2OiBIYXd0aW9NYWluTmF2LlJlZ2lzdHJ5KSA9PiB7XG4gICAgSGF3dGlvTmF2LmFkZCh0YWIpO1xuICAgIGxvZy5kZWJ1ZyhcImxvYWRlZFwiKTtcbiAgfV0pO1xuXG4gIGhhd3Rpb1BsdWdpbkxvYWRlci5hZGRNb2R1bGUoQlRNLnBsdWdpbk5hbWUpO1xufVxuIiwiLy8vIENvcHlyaWdodCAyMDE0LTIwMTUgUmVkIEhhdCwgSW5jLiBhbmQvb3IgaXRzIGFmZmlsaWF0ZXNcbi8vLyBhbmQgb3RoZXIgY29udHJpYnV0b3JzIGFzIGluZGljYXRlZCBieSB0aGUgQGF1dGhvciB0YWdzLlxuLy8vXG4vLy8gTGljZW5zZWQgdW5kZXIgdGhlIEFwYWNoZSBMaWNlbnNlLCBWZXJzaW9uIDIuMCAodGhlIFwiTGljZW5zZVwiKTtcbi8vLyB5b3UgbWF5IG5vdCB1c2UgdGhpcyBmaWxlIGV4Y2VwdCBpbiBjb21wbGlhbmNlIHdpdGggdGhlIExpY2Vuc2UuXG4vLy8gWW91IG1heSBvYnRhaW4gYSBjb3B5IG9mIHRoZSBMaWNlbnNlIGF0XG4vLy9cbi8vLyAgIGh0dHA6Ly93d3cuYXBhY2hlLm9yZy9saWNlbnNlcy9MSUNFTlNFLTIuMFxuLy8vXG4vLy8gVW5sZXNzIHJlcXVpcmVkIGJ5IGFwcGxpY2FibGUgbGF3IG9yIGFncmVlZCB0byBpbiB3cml0aW5nLCBzb2Z0d2FyZVxuLy8vIGRpc3RyaWJ1dGVkIHVuZGVyIHRoZSBMaWNlbnNlIGlzIGRpc3RyaWJ1dGVkIG9uIGFuIFwiQVMgSVNcIiBCQVNJUyxcbi8vLyBXSVRIT1VUIFdBUlJBTlRJRVMgT1IgQ09ORElUSU9OUyBPRiBBTlkgS0lORCwgZWl0aGVyIGV4cHJlc3Mgb3IgaW1wbGllZC5cbi8vLyBTZWUgdGhlIExpY2Vuc2UgZm9yIHRoZSBzcGVjaWZpYyBsYW5ndWFnZSBnb3Zlcm5pbmcgcGVybWlzc2lvbnMgYW5kXG4vLy8gbGltaXRhdGlvbnMgdW5kZXIgdGhlIExpY2Vuc2UuXG5cbi8vLyA8cmVmZXJlbmNlIHBhdGg9XCJidG1QbHVnaW4udHNcIi8+XG5tb2R1bGUgQlRNIHtcblxuICBleHBvcnQgdmFyIEJUTUNvbnRyb2xsZXIgPSBfbW9kdWxlLmNvbnRyb2xsZXIoXCJCVE0uQlRNQ29udHJvbGxlclwiLCBbXCIkc2NvcGVcIiwgXCIkaHR0cFwiLCAnJGxvY2F0aW9uJywgJyRpbnRlcnZhbCcsICgkc2NvcGUsICRodHRwLCAkbG9jYXRpb24sICRpbnRlcnZhbCkgPT4ge1xuXG4gICAgJHNjb3BlLm5ld0JUeG5OYW1lID0gJyc7XG4gICAgJHNjb3BlLmNhbmRpZGF0ZUNvdW50ID0gMDtcblxuICAgICRzY29wZS5yZWxvYWQgPSBmdW5jdGlvbigpIHtcbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG5zdW1tYXJ5JykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucyA9IFtdO1xuICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IHJlc3AuZGF0YS5sZW5ndGg7IGkrKykge1xuICAgICAgICAgIHZhciBidHhuID0ge1xuICAgICAgICAgICAgc3VtbWFyeTogcmVzcC5kYXRhW2ldLFxuICAgICAgICAgICAgY291bnQ6IHVuZGVmaW5lZCxcbiAgICAgICAgICAgIGZhdWx0Y291bnQ6IHVuZGVmaW5lZCxcbiAgICAgICAgICAgIHBlcmNlbnRpbGU5NTogdW5kZWZpbmVkLFxuICAgICAgICAgICAgYWxlcnRzOiB1bmRlZmluZWRcbiAgICAgICAgICB9O1xuICAgICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucy5hZGQoYnR4bik7XG5cbiAgICAgICAgICAkc2NvcGUuZ2V0QnVzaW5lc3NUeG5EZXRhaWxzKGJ0eG4pO1xuICAgICAgICB9XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGJ1c2luZXNzIHR4biBzdW1tYXJpZXM6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG5cbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYnVzaW5lc3N0eG4vdW5ib3VuZHVyaXMnKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgJHNjb3BlLmNhbmRpZGF0ZUNvdW50ID0gT2JqZWN0LmtleXMocmVzcC5kYXRhKS5sZW5ndGg7XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGNhbmRpZGF0ZSBjb3VudDogXCIrcmVzcCk7XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbG9hZCgpO1xuXG4gICAgJGludGVydmFsKGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLnJlbG9hZCgpO1xuICAgIH0sMTAwMDApO1xuXG4gICAgJHNjb3BlLmdldEJ1c2luZXNzVHhuRGV0YWlscyA9IGZ1bmN0aW9uKGJ0eG4pIHtcbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYnVzaW5lc3N0eG4vY29tcGxldGlvbi9jb3VudD9uYW1lPScrYnR4bi5zdW1tYXJ5Lm5hbWUpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBidHhuLmNvdW50ID0gcmVzcC5kYXRhO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBjb3VudDogXCIrcmVzcCk7XG4gICAgICB9KTtcblxuICAgICAgJGh0dHAuZ2V0KCcvaGF3a3VsYXIvYnRtL2FuYWx5dGljcy9idXNpbmVzc3R4bi9jb21wbGV0aW9uL3BlcmNlbnRpbGVzP25hbWU9JytidHhuLnN1bW1hcnkubmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGlmIChyZXNwLmRhdGEucGVyY2VudGlsZXNbOTVdID4gMCkge1xuICAgICAgICAgIGJ0eG4ucGVyY2VudGlsZTk1ID0gTWF0aC5yb3VuZCggcmVzcC5kYXRhLnBlcmNlbnRpbGVzWzk1XSAvIDEwMDAwMDAgKSAvIDEwMDA7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgYnR4bi5wZXJjZW50aWxlOTUgPSAwO1xuICAgICAgICB9XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGNvbXBsZXRpb24gcGVyY2VudGlsZXM6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG5cbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYnVzaW5lc3N0eG4vY29tcGxldGlvbi9mYXVsdGNvdW50P25hbWU9JytidHhuLnN1bW1hcnkubmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGJ0eG4uZmF1bHRjb3VudCA9IHJlc3AuZGF0YTtcbiAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgZmF1bHQgY291bnQ6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG5cbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYWxlcnRzL2NvdW50LycrYnR4bi5zdW1tYXJ5Lm5hbWUpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBidHhuLmFsZXJ0cyA9IHJlc3AuZGF0YTtcbiAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgYWxlcnRzIGNvdW50OiBcIityZXNwKTtcbiAgICAgIH0pO1xuICAgIH07XG5cbiAgICAkc2NvcGUuZGVsZXRlQnVzaW5lc3NUeG4gPSBmdW5jdGlvbihidHhuKSB7XG4gICAgICBpZiAoY29uZmlybSgnQXJlIHlvdSBzdXJlIHlvdSB3YW50IHRvIGRlbGV0ZSBidXNpbmVzcyB0cmFuc2FjdGlvbiBcXFwiJytidHhuLnN1bW1hcnkubmFtZSsnXFxcIj8nKSkge1xuICAgICAgICAkaHR0cC5kZWxldGUoJy9oYXdrdWxhci9idG0vY29uZmlnL2J1c2luZXNzdHhuLycrYnR4bi5zdW1tYXJ5Lm5hbWUpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAgIGNvbnNvbGUubG9nKCdEZWxldGVkOiAnK2J0eG4uc3VtbWFyeS5uYW1lKTtcbiAgICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMucmVtb3ZlKGJ0eG4pO1xuICAgICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBkZWxldGUgYnVzaW5lc3MgdHhuICdcIitidHhuLnN1bW1hcnkubmFtZStcIic6IFwiK3Jlc3ApO1xuICAgICAgICB9KTtcbiAgICAgIH1cbiAgICB9O1xuXG4gIH1dKTtcblxufVxuIiwiLy8vIENvcHlyaWdodCAyMDE0LTIwMTUgUmVkIEhhdCwgSW5jLiBhbmQvb3IgaXRzIGFmZmlsaWF0ZXNcbi8vLyBhbmQgb3RoZXIgY29udHJpYnV0b3JzIGFzIGluZGljYXRlZCBieSB0aGUgQGF1dGhvciB0YWdzLlxuLy8vXG4vLy8gTGljZW5zZWQgdW5kZXIgdGhlIEFwYWNoZSBMaWNlbnNlLCBWZXJzaW9uIDIuMCAodGhlIFwiTGljZW5zZVwiKTtcbi8vLyB5b3UgbWF5IG5vdCB1c2UgdGhpcyBmaWxlIGV4Y2VwdCBpbiBjb21wbGlhbmNlIHdpdGggdGhlIExpY2Vuc2UuXG4vLy8gWW91IG1heSBvYnRhaW4gYSBjb3B5IG9mIHRoZSBMaWNlbnNlIGF0XG4vLy9cbi8vLyAgIGh0dHA6Ly93d3cuYXBhY2hlLm9yZy9saWNlbnNlcy9MSUNFTlNFLTIuMFxuLy8vXG4vLy8gVW5sZXNzIHJlcXVpcmVkIGJ5IGFwcGxpY2FibGUgbGF3IG9yIGFncmVlZCB0byBpbiB3cml0aW5nLCBzb2Z0d2FyZVxuLy8vIGRpc3RyaWJ1dGVkIHVuZGVyIHRoZSBMaWNlbnNlIGlzIGRpc3RyaWJ1dGVkIG9uIGFuIFwiQVMgSVNcIiBCQVNJUyxcbi8vLyBXSVRIT1VUIFdBUlJBTlRJRVMgT1IgQ09ORElUSU9OUyBPRiBBTlkgS0lORCwgZWl0aGVyIGV4cHJlc3Mgb3IgaW1wbGllZC5cbi8vLyBTZWUgdGhlIExpY2Vuc2UgZm9yIHRoZSBzcGVjaWZpYyBsYW5ndWFnZSBnb3Zlcm5pbmcgcGVybWlzc2lvbnMgYW5kXG4vLy8gbGltaXRhdGlvbnMgdW5kZXIgdGhlIExpY2Vuc2UuXG5cbi8vLyA8cmVmZXJlbmNlIHBhdGg9XCJidG1QbHVnaW4udHNcIi8+XG5tb2R1bGUgQlRNIHtcblxuICBleHBvcnQgdmFyIEJUTUNhbmRpZGF0ZXNDb250cm9sbGVyID0gX21vZHVsZS5jb250cm9sbGVyKFwiQlRNLkJUTUNhbmRpZGF0ZXNDb250cm9sbGVyXCIsIFtcIiRzY29wZVwiLCBcIiRodHRwXCIsICckbG9jYXRpb24nLCAnJHVpYk1vZGFsJywgJyRpbnRlcnZhbCcsICgkc2NvcGUsICRodHRwLCAkbG9jYXRpb24sICR1aWJNb2RhbCwgJGludGVydmFsKSA9PiB7XG5cbiAgICAkc2NvcGUubmV3QlR4bk5hbWUgPSAnJztcbiAgICAkc2NvcGUuZXhpc3RpbmdCVHhuTmFtZSA9ICcnO1xuICAgICRzY29wZS5zZWxlY3RlZHVyaXMgPSBbIF07XG4gICAgJHNjb3BlLmNhbmRpZGF0ZUNvdW50ID0gMDtcblxuICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG5zdW1tYXJ5JykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMgPSByZXNwLmRhdGE7XG4gICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgYnVzaW5lc3MgdHhuIHN1bW1hcmllczogXCIrcmVzcCk7XG4gICAgfSk7XG5cbiAgICAkc2NvcGUucmVsb2FkID0gZnVuY3Rpb24oKSB7XG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL3VuYm91bmR1cmlzJykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS51bmJvdW5kdXJpcyA9IHJlc3AuZGF0YTtcbiAgICAgICAgJHNjb3BlLmNhbmRpZGF0ZUNvdW50ID0gT2JqZWN0LmtleXMocmVzcC5kYXRhKS5sZW5ndGg7XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IHVuYm91bmQgVVJJczogXCIrcmVzcCk7XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbG9hZCgpO1xuXG4gICAgJGludGVydmFsKGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLnJlbG9hZCgpO1xuICAgIH0sMTAwMDApO1xuXG4gICAgJHNjb3BlLmFkZEJ1c2luZXNzVHhuID0gZnVuY3Rpb24oKSB7XG4gICAgICB2YXIgZGVmbiA9IHtcbiAgICAgICAgZmlsdGVyOiB7XG4gICAgICAgICAgaW5jbHVzaW9uczogJHNjb3BlLnNlbGVjdGVkdXJpc1xuICAgICAgICB9XG4gICAgICB9O1xuICAgICAgJGh0dHAucHV0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5uZXdCVHhuTmFtZSwgZGVmbikudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRsb2NhdGlvbi5wYXRoKCdjb25maWcvJyskc2NvcGUubmV3QlR4bk5hbWUpO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGFkZCBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5uZXdCVHhuTmFtZStcIic6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS5pZ25vcmVCdXNpbmVzc1R4biA9IGZ1bmN0aW9uKCkge1xuICAgICAgdmFyIGRlZm4gPSB7XG4gICAgICAgIGxldmVsOiAnSWdub3JlJyxcbiAgICAgICAgZmlsdGVyOiB7XG4gICAgICAgICAgaW5jbHVzaW9uczogJHNjb3BlLnNlbGVjdGVkdXJpc1xuICAgICAgICB9XG4gICAgICB9O1xuICAgICAgJGh0dHAucHV0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5uZXdCVHhuTmFtZSwgZGVmbikudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRsb2NhdGlvbi5wYXRoKCdjb25maWcvJyskc2NvcGUubmV3QlR4bk5hbWUpO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGlnbm9yZSBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5uZXdCVHhuTmFtZStcIic6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS51cGRhdGVCdXNpbmVzc1R4biA9IGZ1bmN0aW9uKCkge1xuICAgICAgJGh0dHAuZ2V0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgdmFyIGJ0eG4gPSByZXNwLmRhdGE7XG4gICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgJHNjb3BlLnNlbGVjdGVkdXJpcy5sZW5ndGg7IGkrKykge1xuICAgICAgICAgIGlmIChidHhuLmZpbHRlci5pbmNsdXNpb25zLmluZGV4T2YoJHNjb3BlLnNlbGVjdGVkdXJpc1tpXSkgPT09IC0xKSB7XG4gICAgICAgICAgICBidHhuLmZpbHRlci5pbmNsdXNpb25zLmFkZCgkc2NvcGUuc2VsZWN0ZWR1cmlzW2ldKTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgJGh0dHAucHV0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lLGJ0eG4pLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAgIGNvbnNvbGUubG9nKFwiU2F2ZWQgdXBkYXRlZCBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lK1wiJzogXCIrcmVzcCk7XG4gICAgICAgICAgJGxvY2F0aW9uLnBhdGgoJ2NvbmZpZy8nKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lKTtcbiAgICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gc2F2ZSBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lK1wiJzogXCIrcmVzcCk7XG4gICAgICAgIH0pO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lK1wiJzogXCIrcmVzcCk7XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnNlbGVjdGlvbkNoYW5nZWQgPSBmdW5jdGlvbih1cmkpIHtcbiAgICAgIHZhciByZWdleCA9ICRzY29wZS5lc2NhcGVSZWdFeHAodXJpKTtcbiAgICAgIGlmICgkc2NvcGUuc2VsZWN0ZWR1cmlzLmNvbnRhaW5zKHJlZ2V4KSkge1xuICAgICAgICAkc2NvcGUuc2VsZWN0ZWR1cmlzLnJlbW92ZShyZWdleCk7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICAkc2NvcGUuc2VsZWN0ZWR1cmlzLmFkZChyZWdleCk7XG4gICAgICB9XG4gICAgfTtcbiAgICBcbiAgICAkc2NvcGUuaXNTZWxlY3RlZCA9IGZ1bmN0aW9uKHVyaSkge1xuICAgICAgdmFyIHJlZ2V4ID0gJHNjb3BlLmVzY2FwZVJlZ0V4cCh1cmkpO1xuICAgICAgcmV0dXJuICRzY29wZS5zZWxlY3RlZHVyaXMuY29udGFpbnMocmVnZXgpO1xuICAgIH07XG4gICAgXG4gICAgJHNjb3BlLmdldExldmVsID0gZnVuY3Rpb24obGV2ZWwpIHtcbiAgICAgIGlmIChsZXZlbCA9PT0gJ0FsbCcpIHtcbiAgICAgICAgcmV0dXJuIFwiQWN0aXZlXCI7XG4gICAgICB9XG4gICAgICByZXR1cm4gbGV2ZWw7XG4gICAgfTtcblxuICAgICRzY29wZS5lc2NhcGVSZWdFeHAgPSBmdW5jdGlvbihzdHIpIHtcbiAgICAgIHJldHVybiBcIl5cIiArIHN0ci5yZXBsYWNlKC9bXFwtXFxbXFxdXFwvXFx7XFx9XFwoXFwpXFwqXFwrXFw/XFwuXFxcXFxcXlxcJFxcfF0vZywgXCJcXFxcJCZcIikgKyBcIiRcIjtcbiAgICB9O1xuXG4gIH1dKTtcblxufVxuXG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cImJ0bVBsdWdpbi50c1wiLz5cbm1vZHVsZSBCVE0ge1xuXG4gIGV4cG9ydCB2YXIgQlR4bkNvbmZpZ0NvbnRyb2xsZXIgPSBfbW9kdWxlLmNvbnRyb2xsZXIoXCJCVE0uQlR4bkNvbmZpZ0NvbnRyb2xsZXJcIiwgW1wiJHNjb3BlXCIsIFwiJHJvdXRlUGFyYW1zXCIsIFwiJGh0dHBcIiwgJyRsb2NhdGlvbicsICckaW50ZXJ2YWwnLCAoJHNjb3BlLCAkcm91dGVQYXJhbXMsICRodHRwLCAkbG9jYXRpb24sICRpbnRlcnZhbCkgPT4ge1xuXG4gICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lID0gJHJvdXRlUGFyYW1zLmJ1c2luZXNzdHJhbnNhY3Rpb247XG4gICAgJHNjb3BlLmRpcnR5ID0gZmFsc2U7XG5cbiAgICAkc2NvcGUubmV3SW5jbHVzaW9uRmlsdGVyID0gJyc7XG4gICAgJHNjb3BlLm5ld0V4Y2x1c2lvbkZpbHRlciA9ICcnO1xuXG4gICAgJGh0dHAuZ2V0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uTmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbiA9IHJlc3AuZGF0YTtcbiAgICAgICRzY29wZS5vcmlnaW5hbCA9IGFuZ3VsYXIuY29weSgkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbik7XG4gICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgYnVzaW5lc3MgdHhuICdcIiskc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbk5hbWUrXCInOiBcIityZXNwKTtcbiAgICB9KTtcblxuICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYnVzaW5lc3N0eG4vdW5ib3VuZHVyaXMnKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICRzY29wZS51bmJvdW5kVVJJcyA9IFsgXTtcbiAgICAgIGZvciAodmFyIGtleSBpbiByZXNwLmRhdGEpIHtcbiAgICAgICAgaWYgKGtleSAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgdmFyIGFycmF5PXJlc3AuZGF0YVtrZXldO1xuICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgYXJyYXkubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgICAgIHZhciByZWdleCA9ICRzY29wZS5lc2NhcGVSZWdFeHAoYXJyYXlbaV0pO1xuICAgICAgICAgICAgJHNjb3BlLnVuYm91bmRVUklzLmFkZChyZWdleCk7XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9XG4gICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgdW5ib3VuZCBVUklzOiBcIityZXNwKTtcbiAgICB9KTtcblxuICAgICRzY29wZS5yZWxvYWQgPSBmdW5jdGlvbigpIHtcbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYnVzaW5lc3N0eG4vYm91bmR1cmlzLycrJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgJHNjb3BlLmJvdW5kVVJJcyA9IFsgXTtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCByZXNwLmRhdGEubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgICB2YXIgcmVnZXggPSAkc2NvcGUuZXNjYXBlUmVnRXhwKHJlc3AuZGF0YVtpXSk7XG4gICAgICAgICAgJHNjb3BlLmJvdW5kVVJJcy5hZGQocmVnZXgpO1xuICAgICAgICB9XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGJvdW5kIFVSSXMgZm9yIGJ1c2luZXNzIHR4biAnXCIrJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lK1wiJzogXCIrcmVzcCk7XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbG9hZCgpO1xuXG4gICAgJGludGVydmFsKGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLnJlbG9hZCgpO1xuICAgIH0sMTAwMDApO1xuXG4gICAgJHNjb3BlLmFkZEluY2x1c2lvbkZpbHRlciA9IGZ1bmN0aW9uKCkge1xuICAgICAgY29uc29sZS5sb2coJ0FkZCBpbmNsdXNpb24gZmlsdGVyOiAnKyRzY29wZS5uZXdJbmNsdXNpb25GaWx0ZXIpO1xuICAgICAgaWYgKCRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uLmZpbHRlciA9PT0gbnVsbCkge1xuICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbi5maWx0ZXIgPSB7XG4gICAgICAgICAgaW5jbHVzaW9uczogW10sXG4gICAgICAgICAgZXhjbHVzaW9uczogW11cbiAgICAgICAgfTtcbiAgICAgIH1cbiAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uLmZpbHRlci5pbmNsdXNpb25zLmFkZCgkc2NvcGUubmV3SW5jbHVzaW9uRmlsdGVyKTtcbiAgICAgICRzY29wZS5zZXREaXJ0eSgpO1xuICAgICAgJHNjb3BlLm5ld0luY2x1c2lvbkZpbHRlciA9ICcnO1xuICAgIH07XG5cbiAgICAkc2NvcGUucmVtb3ZlSW5jbHVzaW9uRmlsdGVyID0gZnVuY3Rpb24oaW5jbHVzaW9uKSB7XG4gICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbi5maWx0ZXIuaW5jbHVzaW9ucy5yZW1vdmUoaW5jbHVzaW9uKTtcbiAgICAgICRzY29wZS5zZXREaXJ0eSgpO1xuICAgIH07XG5cbiAgICAkc2NvcGUuYWRkRXhjbHVzaW9uRmlsdGVyID0gZnVuY3Rpb24oKSB7XG4gICAgICBjb25zb2xlLmxvZygnQWRkIGV4Y2x1c2lvbiBmaWx0ZXI6ICcrJHNjb3BlLm5ld0V4Y2x1c2lvbkZpbHRlcik7XG4gICAgICBpZiAoJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24uZmlsdGVyID09PSBudWxsKSB7XG4gICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uLmZpbHRlciA9IHtcbiAgICAgICAgICBpbmNsdXNpb25zOiBbXSxcbiAgICAgICAgICBleGNsdXNpb25zOiBbXVxuICAgICAgICB9O1xuICAgICAgfVxuICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24uZmlsdGVyLmV4Y2x1c2lvbnMuYWRkKCRzY29wZS5uZXdFeGNsdXNpb25GaWx0ZXIpO1xuICAgICAgJHNjb3BlLnNldERpcnR5KCk7XG4gICAgICAkc2NvcGUubmV3RXhjbHVzaW9uRmlsdGVyID0gJyc7XG4gICAgfTtcblxuICAgICRzY29wZS5yZW1vdmVFeGNsdXNpb25GaWx0ZXIgPSBmdW5jdGlvbihleGNsdXNpb24pIHtcbiAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uLmZpbHRlci5leGNsdXNpb25zLnJlbW92ZShleGNsdXNpb24pO1xuICAgICAgJHNjb3BlLnNldERpcnR5KCk7XG4gICAgfTtcblxuICAgICRzY29wZS5nZXRFeHByZXNzaW9uVGV4dCA9IGZ1bmN0aW9uKGV4cHJlc3Npb24pIHtcbiAgICAgIGlmIChleHByZXNzaW9uID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgcmV0dXJuIFwiXCI7XG4gICAgICB9XG4gICAgICBpZiAoZXhwcmVzc2lvbi50eXBlID09PSBcIlhNTFwiKSB7XG4gICAgICAgIHJldHVybiBleHByZXNzaW9uLnNvdXJjZSArIFwiW1wiICsgZXhwcmVzc2lvbi5rZXkgKyBcIl1cIiArIFwiIHhwYXRoPVwiICsgZXhwcmVzc2lvbi54cGF0aDtcbiAgICAgIH1cbiAgICAgIGlmIChleHByZXNzaW9uLnR5cGUgPT09IFwiSlNPTlwiKSB7XG4gICAgICAgIHJldHVybiBleHByZXNzaW9uLnNvdXJjZSArIFwiW1wiICsgZXhwcmVzc2lvbi5rZXkgKyBcIl1cIiArIFwiIGpzb25wYXRoPVwiICsgZXhwcmVzc2lvbi5qc29ucGF0aDtcbiAgICAgIH1cbiAgICAgIGlmIChleHByZXNzaW9uLnR5cGUgPT09IFwiVGV4dFwiKSB7XG4gICAgICAgIHJldHVybiBleHByZXNzaW9uLnNvdXJjZSArIFwiW1wiICsgZXhwcmVzc2lvbi5rZXkgKyBcIl1cIjtcbiAgICAgIH1cbiAgICAgIHJldHVybiBcIlVua25vd24gZXhwcmVzc2lvbiB0eXBlXCI7XG4gICAgfTtcblxuICAgICRzY29wZS5jaGFuZ2VkRXhwcmVzc2lvblR5cGUgPSBmdW5jdGlvbihleHByZXNzaW9uKSB7XG4gICAgICAkc2NvcGUuc2V0RGlydHkoKTtcbiAgICAgIGV4cHJlc3Npb24ua2V5ID0gdW5kZWZpbmVkO1xuICAgICAgZXhwcmVzc2lvbi5zb3VyY2UgPSB1bmRlZmluZWQ7XG4gICAgICBleHByZXNzaW9uLnhwYXRoID0gdW5kZWZpbmVkO1xuICAgICAgZXhwcmVzc2lvbi5qc29ucGF0aCA9IHVuZGVmaW5lZDtcblxuICAgICAgaWYgKGV4cHJlc3Npb24udHlwZSA9PT0gJ1hNTCcgfHwgZXhwcmVzc2lvbi50eXBlID09PSAnSlNPTicgfHwgZXhwcmVzc2lvbi50eXBlID09PSAnVGV4dCcpIHtcbiAgICAgICAgZXhwcmVzc2lvbi5rZXkgPSAnMCc7XG4gICAgICAgIGV4cHJlc3Npb24uc291cmNlID0gJ0NvbnRlbnQnO1xuICAgICAgfVxuICAgIH07XG5cbiAgICAkc2NvcGUuY2hhbmdlZEFjdGlvblR5cGUgPSBmdW5jdGlvbihhY3Rpb24pIHtcbiAgICAgICRzY29wZS5zZXREaXJ0eSgpO1xuICAgICAgYWN0aW9uLm5hbWUgPSB1bmRlZmluZWQ7XG4gICAgICBhY3Rpb24udHlwZSA9IHVuZGVmaW5lZDtcbiAgICAgIGFjdGlvbi5zY29wZSA9IHVuZGVmaW5lZDtcbiAgICAgIGFjdGlvbi50ZW1wbGF0ZSA9IHVuZGVmaW5lZDtcbiAgICAgIGFjdGlvbi5wcmVkaWNhdGUgPSB1bmRlZmluZWQ7XG4gICAgICBhY3Rpb24uZXhwcmVzc2lvbiA9IHVuZGVmaW5lZDtcbiAgICB9O1xuXG4gICAgJHNjb3BlLmFkZFByb2Nlc3NvciA9IGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLnNldERpcnR5KCk7XG4gICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbi5wcm9jZXNzb3JzLmFkZCh7XG4gICAgICAgIGRlc2NyaXB0aW9uOiBcIlByb2Nlc3NvciBcIiArICgkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbi5wcm9jZXNzb3JzLmxlbmd0aCArIDEpLFxuICAgICAgICBub2RlVHlwZTogXCJDb25zdW1lclwiLFxuICAgICAgICBkaXJlY3Rpb246IFwiSW5cIixcbiAgICAgICAgYWN0aW9uczogW11cbiAgICAgIH0pO1xuICAgIH07XG5cbiAgICAkc2NvcGUuZGVsZXRlUHJvY2Vzc29yID0gZnVuY3Rpb24ocHJvY2Vzc29yKSB7XG4gICAgICBpZiAoY29uZmlybSgnQXJlIHlvdSBzdXJlIHlvdSB3YW50IHRvIGRlbGV0ZSB0aGUgcHJvY2Vzc29yPycpKSB7XG4gICAgICAgICRzY29wZS5zZXREaXJ0eSgpO1xuICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbi5wcm9jZXNzb3JzLnJlbW92ZShwcm9jZXNzb3IpO1xuICAgICAgfVxuICAgIH07XG5cbiAgICAkc2NvcGUuYWRkQWN0aW9uID0gZnVuY3Rpb24ocHJvY2Vzc29yKSB7XG4gICAgICAkc2NvcGUuc2V0RGlydHkoKTtcbiAgICAgIHByb2Nlc3Nvci5hY3Rpb25zLmFkZCh7XG4gICAgICAgIGRlc2NyaXB0aW9uOiBcIkFjdGlvbiBcIiArIChwcm9jZXNzb3IuYWN0aW9ucy5sZW5ndGggKyAxKVxuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS5kZWxldGVBY3Rpb24gPSBmdW5jdGlvbihwcm9jZXNzb3IsYWN0aW9uKSB7XG4gICAgICBpZiAoY29uZmlybSgnQXJlIHlvdSBzdXJlIHlvdSB3YW50IHRvIGRlbGV0ZSB0aGUgYWN0aW9uPycpKSB7XG4gICAgICAgICRzY29wZS5zZXREaXJ0eSgpO1xuICAgICAgICBwcm9jZXNzb3IuYWN0aW9ucy5yZW1vdmUoYWN0aW9uKTtcbiAgICAgIH1cbiAgICB9O1xuXG4gICAgJHNjb3BlLnNldERpcnR5ID0gZnVuY3Rpb24oKSB7XG4gICAgICAkc2NvcGUuZGlydHkgPSB0cnVlO1xuICAgIH07XG5cbiAgICAkc2NvcGUucmVzZXQgPSBmdW5jdGlvbigpIHtcbiAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uID0gYW5ndWxhci5jb3B5KCRzY29wZS5vcmlnaW5hbCk7XG4gICAgICAkc2NvcGUuZGlydHkgPSBmYWxzZTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnNhdmUgPSBmdW5jdGlvbigpIHtcbiAgICAgICRodHRwLnB1dCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG4vJyskc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbk5hbWUsJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24pLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAkc2NvcGUub3JpZ2luYWwgPSBhbmd1bGFyLmNvcHkoJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24pO1xuICAgICAgICAkc2NvcGUuZGlydHkgPSBmYWxzZTtcbiAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBzYXZlIGJ1c2luZXNzIHR4biAnXCIrJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lK1wiJzogXCIrcmVzcCk7XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJGh0dHAuZ2V0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uTmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbiA9IHJlc3AuZGF0YTtcbiAgICAgICRzY29wZS5vcmlnaW5hbCA9IGFuZ3VsYXIuY29weSgkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbik7XG4gICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgYnVzaW5lc3MgdHhuICdcIiskc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbk5hbWUrXCInOiBcIityZXNwKTtcbiAgICB9KTtcblxuICAgICRzY29wZS5lc2NhcGVSZWdFeHAgPSBmdW5jdGlvbihzdHIpIHtcbiAgICAgIHJldHVybiBcIl5cIiArIHN0ci5yZXBsYWNlKC9bXFwtXFxbXFxdXFwvXFx7XFx9XFwoXFwpXFwqXFwrXFw/XFwuXFxcXFxcXlxcJFxcfF0vZywgXCJcXFxcJCZcIikgKyBcIiRcIjtcbiAgICB9O1xuXG4gIH1dKTtcblxufVxuIiwiLy8vIENvcHlyaWdodCAyMDE0LTIwMTUgUmVkIEhhdCwgSW5jLiBhbmQvb3IgaXRzIGFmZmlsaWF0ZXNcbi8vLyBhbmQgb3RoZXIgY29udHJpYnV0b3JzIGFzIGluZGljYXRlZCBieSB0aGUgQGF1dGhvciB0YWdzLlxuLy8vXG4vLy8gTGljZW5zZWQgdW5kZXIgdGhlIEFwYWNoZSBMaWNlbnNlLCBWZXJzaW9uIDIuMCAodGhlIFwiTGljZW5zZVwiKTtcbi8vLyB5b3UgbWF5IG5vdCB1c2UgdGhpcyBmaWxlIGV4Y2VwdCBpbiBjb21wbGlhbmNlIHdpdGggdGhlIExpY2Vuc2UuXG4vLy8gWW91IG1heSBvYnRhaW4gYSBjb3B5IG9mIHRoZSBMaWNlbnNlIGF0XG4vLy9cbi8vLyAgIGh0dHA6Ly93d3cuYXBhY2hlLm9yZy9saWNlbnNlcy9MSUNFTlNFLTIuMFxuLy8vXG4vLy8gVW5sZXNzIHJlcXVpcmVkIGJ5IGFwcGxpY2FibGUgbGF3IG9yIGFncmVlZCB0byBpbiB3cml0aW5nLCBzb2Z0d2FyZVxuLy8vIGRpc3RyaWJ1dGVkIHVuZGVyIHRoZSBMaWNlbnNlIGlzIGRpc3RyaWJ1dGVkIG9uIGFuIFwiQVMgSVNcIiBCQVNJUyxcbi8vLyBXSVRIT1VUIFdBUlJBTlRJRVMgT1IgQ09ORElUSU9OUyBPRiBBTlkgS0lORCwgZWl0aGVyIGV4cHJlc3Mgb3IgaW1wbGllZC5cbi8vLyBTZWUgdGhlIExpY2Vuc2UgZm9yIHRoZSBzcGVjaWZpYyBsYW5ndWFnZSBnb3Zlcm5pbmcgcGVybWlzc2lvbnMgYW5kXG4vLy8gbGltaXRhdGlvbnMgdW5kZXIgdGhlIExpY2Vuc2UuXG5cbi8vLyA8cmVmZXJlbmNlIHBhdGg9XCJidG1QbHVnaW4udHNcIi8+XG5tb2R1bGUgQlRNIHtcblxuICBleHBvcnQgdmFyIEJUTURpc2FibGVkQ29udHJvbGxlciA9IF9tb2R1bGUuY29udHJvbGxlcihcIkJUTS5CVE1EaXNhYmxlZENvbnRyb2xsZXJcIiwgW1wiJHNjb3BlXCIsIFwiJGh0dHBcIiwgJyRsb2NhdGlvbicsICckaW50ZXJ2YWwnLCAoJHNjb3BlLCAkaHR0cCwgJGxvY2F0aW9uLCAkaW50ZXJ2YWwpID0+IHtcblxuICAgICRzY29wZS5uZXdCVHhuTmFtZSA9ICcnO1xuICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IDA7XG5cbiAgICAkc2NvcGUucmVsb2FkID0gZnVuY3Rpb24oKSB7XG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vY29uZmlnL2J1c2luZXNzdHhuc3VtbWFyeScpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMgPSBbXTtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCByZXNwLmRhdGEubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgICB2YXIgYnR4biA9IHtcbiAgICAgICAgICAgIHN1bW1hcnk6IHJlc3AuZGF0YVtpXVxuICAgICAgICAgIH07XG4gICAgICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25zLmFkZChidHhuKTtcbiAgICAgICAgfVxuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBidXNpbmVzcyB0eG4gc3VtbWFyaWVzOiBcIityZXNwKTtcbiAgICAgIH0pO1xuXG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL3VuYm91bmR1cmlzJykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IE9iamVjdC5rZXlzKHJlc3AuZGF0YSkubGVuZ3RoO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBjYW5kaWRhdGUgY291bnQ6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS5yZWxvYWQoKTtcblxuICAgICRzY29wZS5kZWxldGVCdXNpbmVzc1R4biA9IGZ1bmN0aW9uKGJ0eG4pIHtcbiAgICAgIGlmIChjb25maXJtKCdBcmUgeW91IHN1cmUgeW91IHdhbnQgdG8gZGVsZXRlIGJ1c2luZXNzIHRyYW5zYWN0aW9uIFxcXCInK2J0eG4uc3VtbWFyeS5uYW1lKydcXFwiPycpKSB7XG4gICAgICAgICRodHRwLmRlbGV0ZSgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG4vJytidHhuLnN1bW1hcnkubmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICAgY29uc29sZS5sb2coJ0RlbGV0ZWQ6ICcrYnR4bi5zdW1tYXJ5Lm5hbWUpO1xuICAgICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucy5yZW1vdmUoYnR4bik7XG4gICAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGRlbGV0ZSBidXNpbmVzcyB0eG4gJ1wiK2J0eG4uc3VtbWFyeS5uYW1lK1wiJzogXCIrcmVzcCk7XG4gICAgICAgIH0pO1xuICAgICAgfVxuICAgIH07XG5cbiAgfV0pO1xuXG59XG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cImJ0bVBsdWdpbi50c1wiLz5cbm1vZHVsZSBCVE0ge1xuXG4gIGV4cG9ydCB2YXIgQlRNSWdub3JlZENvbnRyb2xsZXIgPSBfbW9kdWxlLmNvbnRyb2xsZXIoXCJCVE0uQlRNSWdub3JlZENvbnRyb2xsZXJcIiwgW1wiJHNjb3BlXCIsIFwiJGh0dHBcIiwgJyRsb2NhdGlvbicsICckaW50ZXJ2YWwnLCAoJHNjb3BlLCAkaHR0cCwgJGxvY2F0aW9uLCAkaW50ZXJ2YWwpID0+IHtcblxuICAgICRzY29wZS5uZXdCVHhuTmFtZSA9ICcnO1xuICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IDA7XG5cbiAgICAkc2NvcGUucmVsb2FkID0gZnVuY3Rpb24oKSB7XG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vY29uZmlnL2J1c2luZXNzdHhuc3VtbWFyeScpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMgPSBbXTtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCByZXNwLmRhdGEubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgICB2YXIgYnR4biA9IHtcbiAgICAgICAgICAgIHN1bW1hcnk6IHJlc3AuZGF0YVtpXVxuICAgICAgICAgIH07XG4gICAgICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25zLmFkZChidHhuKTtcbiAgICAgICAgfVxuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBidXNpbmVzcyB0eG4gc3VtbWFyaWVzOiBcIityZXNwKTtcbiAgICAgIH0pO1xuXG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL3VuYm91bmR1cmlzJykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IE9iamVjdC5rZXlzKHJlc3AuZGF0YSkubGVuZ3RoO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBjYW5kaWRhdGUgY291bnQ6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS5yZWxvYWQoKTtcblxuICAgICRzY29wZS5kZWxldGVCdXNpbmVzc1R4biA9IGZ1bmN0aW9uKGJ0eG4pIHtcbiAgICAgIGlmIChjb25maXJtKCdBcmUgeW91IHN1cmUgeW91IHdhbnQgdG8gZGVsZXRlIGJ1c2luZXNzIHRyYW5zYWN0aW9uIFxcXCInK2J0eG4uc3VtbWFyeS5uYW1lKydcXFwiPycpKSB7XG4gICAgICAgICRodHRwLmRlbGV0ZSgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG4vJytidHhuLnN1bW1hcnkubmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICAgY29uc29sZS5sb2coJ0RlbGV0ZWQ6ICcrYnR4bi5zdW1tYXJ5Lm5hbWUpO1xuICAgICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucy5yZW1vdmUoYnR4bik7XG4gICAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGRlbGV0ZSBidXNpbmVzcyB0eG4gJ1wiK2J0eG4uc3VtbWFyeS5uYW1lK1wiJzogXCIrcmVzcCk7XG4gICAgICAgIH0pO1xuICAgICAgfVxuICAgIH07XG5cbiAgfV0pO1xuXG59XG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cImJ0bVBsdWdpbi50c1wiLz5cbm1vZHVsZSBCVE0ge1xuXG4gIGRlY2xhcmUgdmFyIGMzOiBhbnk7XG5cbiAgZXhwb3J0IHZhciBCVHhuSW5mb0NvbnRyb2xsZXIgPSBfbW9kdWxlLmNvbnRyb2xsZXIoXCJCVE0uQlR4bkluZm9Db250cm9sbGVyXCIsIFtcIiRzY29wZVwiLCBcIiRyb3V0ZVBhcmFtc1wiLCBcIiRodHRwXCIsICckbG9jYXRpb24nLCAnJGludGVydmFsJywgKCRzY29wZSwgJHJvdXRlUGFyYW1zLCAkaHR0cCwgJGxvY2F0aW9uLCAkaW50ZXJ2YWwpID0+IHtcblxuICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uTmFtZSA9ICRyb3V0ZVBhcmFtcy5idXNpbmVzc3RyYW5zYWN0aW9uO1xuXG4gICAgJHNjb3BlLmNyaXRlcmlhID0ge1xuICAgICAgbmFtZTogJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lLFxuICAgICAgc3RhcnRUaW1lOiAtMzYwMDAwMCxcbiAgICAgIGVuZFRpbWU6IDBcbiAgICB9O1xuXG4gICAgJHNjb3BlLmNvbmZpZyA9IHtcbiAgICAgIGludGVydmFsOiA2MDAwMFxuICAgIH07XG5cbiAgICAkc2NvcGUucmVsb2FkID0gZnVuY3Rpb24oKSB7XG4gICAgICAkaHR0cC5wb3N0KCcvaGF3a3VsYXIvYnRtL2FuYWx5dGljcy9idXNpbmVzc3R4bi9jb21wbGV0aW9uL3N0YXRpc3RpY3M/aW50ZXJ2YWw9Jyskc2NvcGUuY29uZmlnLmludGVydmFsLCAkc2NvcGUuY3JpdGVyaWEpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAkc2NvcGUuc3RhdGlzdGljcyA9IHJlc3AuZGF0YTtcbiAgICAgICAgXG4gICAgICAgICRzY29wZS5jdGxpbmVjaGFydC5sb2FkKHtcbiAgICAgICAgICBqc29uOiAkc2NvcGUuc3RhdGlzdGljcyxcbiAgICAgICAgICBrZXlzOiB7XG4gICAgICAgICAgICB2YWx1ZTogWydtaW4nLCdhdmVyYWdlJywnbWF4JywnY291bnQnXSxcbiAgICAgICAgICAgIHg6ICd0aW1lc3RhbXAnXG4gICAgICAgICAgfVxuICAgICAgICB9KTtcblxuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBzdGF0aXN0aWNzOiBcIityZXNwKTtcbiAgICAgIH0pO1xuICAgIH07XG5cbiAgICAkc2NvcGUucmVsb2FkKCk7XG5cbiAgICAkaW50ZXJ2YWwoZnVuY3Rpb24oKSB7XG4gICAgICAkc2NvcGUucmVsb2FkKCk7XG4gICAgfSwxMDAwMCk7XG5cbiAgICAkc2NvcGUuaW5pdEdyYXBoID0gZnVuY3Rpb24oKSB7XG4gICAgICAkc2NvcGUuY3RsaW5lY2hhcnQgPSBjMy5nZW5lcmF0ZSh7XG4gICAgICAgIGJpbmR0bzogJyNjb21wbGV0aW9udGltZWxpbmVjaGFydCcsXG4gICAgICAgIGRhdGE6IHtcbiAgICAgICAgICBqc29uOiBbXG4gICAgICAgICAgXSxcbiAgICAgICAgICBheGVzOiB7XG4gICAgICAgICAgICBtaW46ICd5JyxcbiAgICAgICAgICAgIGF2ZXJhZ2U6ICd5JyxcbiAgICAgICAgICAgIG1heDogJ3knLFxuICAgICAgICAgICAgY291bnQ6ICd5MidcbiAgICAgICAgICB9LFxuICAgICAgICAgIHR5cGU6ICdsaW5lJyxcbiAgICAgICAgICB0eXBlczoge1xuICAgICAgICAgICAgY291bnQ6ICdiYXInXG4gICAgICAgICAgfSxcbiAgICAgICAgICBrZXlzOiB7XG4gICAgICAgICAgICB2YWx1ZTogWydtaW4nLCdhdmVyYWdlJywnbWF4JywnY291bnQnXSxcbiAgICAgICAgICAgIHg6ICd0aW1lc3RhbXAnXG4gICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBjb2xvcjoge1xuICAgICAgICAgIHBhdHRlcm46IFsnI2U1ZTYwMCcsICcjMzNjYzMzJywgJyNmZjAwMDAnLCAnIzk5Y2NmZiddXG4gICAgICAgIH0sXG4gICAgICAgIGF4aXM6IHtcbiAgICAgICAgICB4OiB7XG4gICAgICAgICAgICB0eXBlOiAndGltZXNlcmllcycsXG4gICAgICAgICAgICB0aWNrOiB7XG4gICAgICAgICAgICAgIGN1bGxpbmc6IHtcbiAgICAgICAgICAgICAgICBtYXg6IDYgLy8gdGhlIG51bWJlciBvZiB0aWNrIHRleHRzIHdpbGwgYmUgYWRqdXN0ZWQgdG8gbGVzcyB0aGFuIHRoaXMgdmFsdWVcbiAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgZm9ybWF0OiAnJVktJW0tJWQgJUg6JU06JVMnXG4gICAgICAgICAgICB9XG4gICAgICAgICAgfSxcbiAgICAgICAgICB5Mjoge1xuICAgICAgICAgICAgc2hvdzogdHJ1ZVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgfSk7XG5cbiAgICB9O1xuICAgIFxuICAgICRzY29wZS5pbml0R3JhcGgoKTtcblxuICB9XSk7XG5cbn1cbiJdLCJzb3VyY2VSb290IjoiL3NvdXJjZS8ifQ==

angular.module("hawkularbtm-templates", []).run(["$templateCache", function($templateCache) {$templateCache.put("plugins/btm/html/btm.html","<div class=\"col-md-12\" ng-controller=\"BTM.BTMController\">\n  <div class=\"text-center hk-urls-list hk-spinner-container\" ng-hide=\"businessTransactions\">\n    <div class=\"spinner spinner-lg\"></div>\n    <p class=\"hk-spinner-legend-below\">Loading...</p>\n  </div>\n\n  <div class=\"row\" ng-show=\"businessTransactions\" >\n    <hawkular-subtab class=\"hk-align-center\">\n      <div class=\"hk-nav-tabs-container\">\n        <ul class=\"nav nav-tabs nav-tabs-pf\">\n          <li class=\"active\"><a href=\"active\" >Active</a></li>\n          <li><a href=\"candidates\">Candidates <i class=\"fa fa-flag\" ng-hide=\"candidateCount === 0\"></i></a></li>\n          <li><a href=\"disabled\">Disabled</a></li>\n          <li><a href=\"ignored\">Ignored</a></li>\n        </ul>\n      </div>\n    </hawkular-subtab>\n\n    <section id=\"active\" class=\"hk-tab-content\">\n\n      <div class=\"col-md-9 hk-align-center\">\n        <ul class=\"list-group\" >\n          <br>\n          <div class=\"row\" >\n            Search: <input ng-model=\"query\">\n            <a href=\"/hawkular-ui/btm-analytics\" class=\"btn btn-info pull-right\" target=\"_blank\">View Analytics</a>\n          </div>\n\n          <div class=\"hk-url-item\" ng-repeat=\"btxn in businessTransactions | filter:query\" >\n            <div class=\"panel panel-default hk-url-heading\" ng-show=\"btxn.summary.level === \'All\'\">\n              <a href=\"info/{{btxn.summary.name}}\">{{btxn.summary.name}}</a>\n              <span class=\"hk-settings pull-right\">\n                <a href=\"config/{{btxn.summary.name}}\" ><i class=\"fa fa-cog\"></i></a>\n                <a href=\"#\" ng-click=\"deleteBusinessTxn(btxn)\"><i class=\"fa fa-trash-o\"></i></a>\n              </span>\n            </div>\n\n            <div class=\"panel panel-default hk-summary\" ng-show=\"btxn.summary.level === \'All\'\">\n              <div class=\"row\">\n                <div class=\"col-sm-3 hk-summary-item\">\n                  <a href=\"info/{{btxn.summary.name}}\">\n                    <span class=\"hk-data\" ng-show=\"btxn.count !== undefined\">{{btxn.count}}</i></span>\n                    <span class=\"hk-data spinner\" ng-hide=\"btxn.count !== undefined\" popover=\"Your data is being collected. You should see something in a few seconds.\" popover-trigger=\"mouseenter\" popover-placement=\"bottom\"></span>\n                    <span class=\"hk-item\">Transactions (per hour)</span>\n                  </a>\n                </div>\n                <div class=\"col-sm-3 hk-summary-item\">\n                  <a href=\"info/{{btxn.summary.name}}\">\n                    <span class=\"hk-data\" ng-show=\"btxn.percentile95 !== undefined\">{{btxn.percentile95}} sec</i></span>\n                    <span class=\"hk-data spinner\" ng-hide=\"btxn.percentile95 !== undefined\" popover=\"Your data is being collected. You should see something in a few seconds.\" popover-trigger=\"mouseenter\" popover-placement=\"bottom\"></span>\n                    <span class=\"hk-item\">Completion (95%)</span>\n                  </a>\n                </div>\n                <div class=\"col-sm-3 hk-summary-item\">\n                  <a href=\"info/{{btxn.summary.name}}\">\n                    <span class=\"hk-data\" ng-show=\"btxn.faultcount !== undefined\">{{btxn.faultcount}}</i></span>\n                    <span class=\"hk-data spinner\" ng-hide=\"btxn.faultcount !== undefined\" popover=\"Your data is being collected. You should see something in a few seconds.\" popover-trigger=\"mouseenter\" popover-placement=\"bottom\"></span>\n                    <span class=\"hk-item\">Faults</span>\n                  </a>\n                </div>\n                <div class=\"col-sm-3 hk-summary-item\">\n                  <a href=\"info/{{btxn.summary.name}}\">\n                    <span class=\"hk-data\" ng-show=\"btxn.alerts !== undefined\">{{btxn.alerts}} <i class=\"fa fa-flag\" ng-show=\"btxn.alerts > 0\"></i></span>\n                    <span class=\"hk-data spinner\" ng-hide=\"btxn.alerts !== undefined\" popover=\"Your data is being collected. You should see something in a few seconds.\" popover-trigger=\"mouseenter\" popover-placement=\"bottom\"></span>\n                    <span class=\"hk-item\">Alerts</span>\n                  </a>\n                </div>\n              </div>\n\n            </div>\n\n          </div>\n       </ul>\n      </div>\n    </section>\n  </div>\n</div>\n\n");
$templateCache.put("plugins/btm/html/btxncandidates.html","<div class=\"col-md-12\" ng-controller=\"BTM.BTMCandidatesController\">\n\n  <div class=\"text-center hk-urls-list hk-spinner-container\" ng-hide=\"unbounduris\">\n    <div class=\"spinner spinner-lg\"></div>\n    <p class=\"hk-spinner-legend-below\">Loading...</p>\n  </div>\n\n  <div class=\"row\" ng-show=\"unbounduris\" >\n    <hawkular-subtab class=\"hk-align-center\">\n      <div class=\"hk-nav-tabs-container\">\n        <ul class=\"nav nav-tabs nav-tabs-pf\">\n          <li><a href=\"active\" >Active</a></li>\n          <li class=\"active\"><a href=\"candidates\">Candidates <i class=\"fa fa-flag\" ng-hide=\"candidateCount === 0\"></i></a></li>\n          <li><a href=\"disabled\">Disabled</a></li>\n          <li><a href=\"ignored\">Ignored</a></li>\n        </ul>\n      </div>\n    </hawkular-subtab>\n\n    <section id=\"active\" class=\"hk-tab-content\">\n\n      <br>\n\n      <form class=\"form-horizontal hk-add-url\" name=\"addBTxnForm\" role=\"form\" novalidate >\n        <div class=\"form-group input\">\n          <div class=\"col-lg-6 col-sm-8 col-xs-12 hk-align-center\">\n            <div class=\"input-group input-group-lg\">\n              <input type=\"text\" class=\"form-control\" name=\"newBTxnNameField\"\n                   ng-model=\"newBTxnName\" ng-model-options=\"{ updateOn: \'default blur\'}\"\n                   placeholder=\"Business transaction name\">\n              <span class=\"input-group-btn\">\n                <input class=\"btn btn-primary\" type=\"submit\" ng-disabled=\"!newBTxnName\" ng-click=\"addBusinessTxn()\" value=\"Manage\" />\n                <input class=\"btn btn-danger\" type=\"button\" ng-disabled=\"!newBTxnName\" ng-click=\"ignoreBusinessTxn()\" value=\"Ignore\" />\n              </span>\n\n              <span class=\"input-group-btn\">\n              </span>\n\n              <select id=\"repeatSelect\" class=\"form-control\" ng-model=\"existingBTxnName\" >\n                <option value=\"\">Select existing ....</i></option>\n                <option ng-repeat=\"btxn in businessTransactions\" value=\"{{btxn.name}}\">{{btxn.name}} ({{getLevel(btxn.level)}})</option>\n              </select>\n              <span class=\"input-group-btn\">\n                <input class=\"btn btn-primary\" type=\"submit\" ng-disabled=\"!existingBTxnName || selecteduris.length == 0\" ng-click=\"updateBusinessTxn()\" value=\"Update\" />\n              </span>\n            </div>\n          </div>\n        </div>\n      </form>\n\n      <div class=\"col-md-9 hk-align-center\">\n        <ul class=\"list-group\" >\n          <br>\n          <div class=\"row\" >\n            Search: <input ng-model=\"query\">\n          </div>\n          <br>\n\n          <div class=\"panel panel-default hk-url-heading\">\n            <div ng-repeat=\"uriinfo in unbounduris | filter:query\" >\n              <label>\n                <input type=\"checkbox\" name=\"selectedURIs[]\"\n                  value=\"{{uriinfo.uri}}\"\n                  ng-checked=\"isSelected(uriinfo.uri)\"\n                  ng-click=\"selectionChanged(uriinfo.uri)\"\n                  ng-disabled=\"!newBTxnName && !existingBTxnName\">\n                  <span ng-hide=\"!newBTxnName && !existingBTxnName\" style=\"color:black\">{{uriinfo.uri}} [ {{uriinfo.endpointType}}]</span>\n                  <span ng-show=\"!newBTxnName && !existingBTxnName\" style=\"color:grey\"><i>{{uriinfo.uri}} [ {{uriinfo.endpointType}}]</i></span>\n              </label>\n            </div>\n          </div>\n       </ul>\n      </div>\n    </section>\n  </div>\n</div>\n\n");
$templateCache.put("plugins/btm/html/btxnconfig.html","<div class=\"row\">\n  <div class=\"col-md-12\" ng-controller=\"BTM.BTxnConfigController\">\n    <h1><span style=\"color:grey\">{{businessTransactionName}}</span></h1>\n\n    <button type=\"button\" class=\"btn btn-success btn-sm\" ng-click=\"save()\" ng-disabled=\"!dirty\">Save</button>\n    <button type=\"button\" class=\"btn btn-danger btn-sm\" ng-click=\"reset()\" ng-disabled=\"!dirty\">Discard</button>\n\n    <br>\n    <br>\n\n    <a href=\"#\" editable-textarea=\"businessTransaction.description\" e-rows=\"14\" e-cols=\"120\" rows=\"7\" onaftersave=\"setDirty()\" >\n        <pre><i>{{ businessTransaction.description || \'No description\' }}</i></pre>\n    </a>\n\n    <div class=\"col-md-12\" >\n      <h2>Filters</h2>\n    </div>\n\n    <div class=\"col-md-6\" >\n\n      <h4>Inclusion</h4>\n\n      <!-- TODO: Use angular-ui/bootstrap typeahead to autofill possible inclusion URIs -->\n\n      <ul class=\"list-group\">\n        <li class=\"list-group-item\" ng-repeat=\"inclusion in businessTransaction.filter.inclusions\" >{{inclusion}}<span class=\"glyphicon glyphicon-remove pull-right\" aria-hidden=\"true\" ng-click=\"removeInclusionFilter(inclusion)\"></span></li>\n        <li class=\"list-group-item\" >\n          <form class=\"form-horizontal hk-add-url\" name=\"addInclusionForm\" role=\"form\" autocomplete=\"off\" ng-submit=\"addInclusionFilter()\">\n            <div class=\"input-group input-group-lg\">\n              <input type=\"text\" class=\"form-control\" name=\"newInclusionFilterField\"\n                   ng-model=\"newInclusionFilter\" ng-model-options=\"{ updateOn: \'default blur\'}\"\n                   placeholder=\"Enter an inclusion filter (regular expression)\"\n                   uib-typeahead=\"uri for uri in unboundURIs | filter:$viewValue | limitTo:12\" required>\n              <span class=\"input-group-btn\">\n                <button class=\"btn btn-primary\" type=\"submit\" ng-disabled=\"!newInclusionFilter\" >\n                  <div ng-show=\"addProgress\" class=\"spinner spinner-sm\"></div>\n                  <span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\"\"></span>\n                </button>\n              </span>\n            </div>\n          </form>\n        </li>\n      </ul>\n    </div>\n\n    <div class=\"col-md-6\" >\n      <h4>Exclusion (applied after inclusions)</h4>\n\n      <ul class=\"list-group\">\n        <li class=\"list-group-item\" ng-repeat=\"exclusion in businessTransaction.filter.exclusions\" >{{exclusion}}<span class=\"glyphicon glyphicon-remove pull-right\" aria-hidden=\"true\" ng-click=\"removeExclusionFilter(exclusion)\"></span></li>\n        <li class=\"list-group-item\" >\n          <form class=\"form-horizontal hk-add-url\" name=\"addExclusionForm\" role=\"form\" autocomplete=\"off\" novalidate ng-submit=\"addExclusionFilter()\">\n            <div class=\"input-group input-group-lg\">\n              <input type=\"text\" class=\"form-control\" name=\"newExclusionFilterField\"\n                   ng-model=\"newExclusionFilter\" ng-model-options=\"{ updateOn: \'default blur\'}\"\n                   placeholder=\"Enter an exclusion filter (regular expression)\"\n                   uib-typeahead=\"uri for uri in unboundURIs | filter:$viewValue | limitTo:12\" required>\n              <span class=\"input-group-btn\">\n                <button class=\"btn btn-primary\" type=\"submit\" ng-disabled=\"!newExclusionFilter\" >\n                  <div ng-show=\"addProgress\" class=\"spinner spinner-sm\"></div>\n                  <span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\"\"></span>\n                </button>\n              </span>\n            </div>\n          </form>\n        </li>\n      </ul>\n    </div>\n\n    <!-- TODO: Styles -->\n\n    <div class=\"col-md-12\" >\n      <form>\n        <div class=\"form-group\">\n          <label for=\"level\" style=\"width: 10%\" >Reporting Level:</label>\n          <select name=\"nodeType\" ng-model=\"businessTransaction.level\" ng-change=\"setDirty()\" style=\"width: 10%\">\n            <option value=\"All\">All</option>\n            <option value=\"None\">None</option>\n            <option value=\"Ignore\">Ignore</option>\n          </select>\n        </div>\n      </form>\n    </div>\n\n    <div class=\"col-md-12\" >\n      <h2>Processors <a class=\"btn btn-primary\" ng-click=\"addProcessor()\"><span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\"\"></span></a></h2>\n    </div>\n\n    <div class=\"col-md-12\" >\n\n      <uib-accordion>\n        <uib-accordion-group ng-repeat=\"processor in businessTransaction.processors\" is-open=\"false\" is-disabled=\"false\">\n          <uib-accordion-heading>{{processor.description}} <a class=\"btn btn-link hk-delete pull-right\" href=\"#\" uibTooltip=\"Delete\" tooltip-trigger tooltip-placement=\"top\" ng-click=\"deleteProcessor(processor)\"><i class=\"fa fa-trash-o\"></i></a></uib-accordion-heading>\n\n          <form>\n            <div class=\"form-group\">\n              <label for=\"description\" style=\"width: 15%\" >Description:</label>\n              <input type=\"text\" name=\"description\" ng-model=\"processor.description\" ng-change=\"setDirty()\" style=\"width: 80%\" >\n            </div>\n\n            <div class=\"form-group\">\n              <label for=\"nodeType\" style=\"width: 15%\" > Node type: </label>\n              <select name=\"nodeType\" ng-model=\"processor.nodeType\" ng-change=\"setDirty()\" style=\"width: 30%\">\n                <option value=\"Consumer\">Consumer</option>\n                <option value=\"Producer\">Producer</option>\n                <option value=\"Component\">Component</option>\n              </select>\n\n              <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n              <label for=\"direction\" style=\"width: 15%\" >Direction: </label>\n              <select name=\"direction\" ng-model=\"processor.direction\" ng-change=\"setDirty()\" style=\"width: 30%\">\n                <option value=\"In\">In</option>\n                <option value=\"Out\">Out</option>\n              </select>\n\n              <label for=\"uriFilter\" style=\"width: 15%\" >URI filter:</label>\n              <input type=\"text\" name=\"uriFilter\"\n                   ng-model=\"processor.uriFilter\" ng-model-options=\"{ updateOn: \'default blur\'}\"\n                   placeholder=\"Enter URI filter (regular expression)\"\n                   uib-typeahead=\"uri for uri in boundURIs | filter:$viewValue | limitTo:12\"\n                   ng-change=\"setDirty()\" style=\"width: 80%\" >\n\n              <label for=\"operation\" style=\"width: 15%\" >Operation:</label>\n              <input type=\"text\" name=\"operation\" ng-model=\"processor.operation\" ng-change=\"setDirty()\" style=\"width: 30%\" >\n\n              <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n              <label for=\"faultFilter\" style=\"width: 15%\" >Fault filter:</label>\n              <input type=\"text\" name=\"faultFilter\" ng-model=\"processor.faultFilter\" ng-change=\"setDirty()\" style=\"width: 30%\" >\n            </div>\n\n            <div class=\"form-group\">\n              <label for=\"predicateType\" style=\"width: 15%\" >Predicate Type: </label>\n              <select name=\"predicateType\" ng-model=\"processor.predicate.type\" ng-change=\"changedExpressionType(processor.predicate)\" style=\"width: 30%\">\n                <option value=\"\"></option>\n                <option value=\"Literal\">Literal</option>\n                <option value=\"XML\">XML</option>\n                <option value=\"JSON\">JSON</option>\n                <option value=\"Text\">Text</option>\n                <option value=\"FreeForm\">Free Form</option>\n              </select>\n\n              <br>\n\n              <label for=\"predicateSource\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\">Source: </label>\n              <select name=\"predicateSource\" ng-model=\"processor.predicate.source\" ng-change=\"setDirty()\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\" style=\"width: 30%\">\n                <option value=\"Content\">Content</option>\n                <option value=\"Header\">Header</option>\n              </select>\n\n              <label style=\"width: 5%\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\"></label> <!-- TODO: Must be a better way -->\n\n              <label for=\"predicateKey\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\">Key: </label>\n              <input type=\"text\" name=\"predicateKey\" ng-model=\"processor.predicate.key\" ng-change=\"setDirty()\" style=\"width: 30%\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\">\n\n              <label for=\"predicateXPath\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'XML\'\">XPath: <a href=\"http://www.w3schools.com/xsl/xpath_syntax.asp\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n              <input type=\"text\" name=\"predicateXPath\" ng-model=\"processor.predicate.xpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"processor.predicate.type === \'XML\'\">\n\n              <label for=\"predicateJSONPath\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'JSON\'\">JSONPath: <a href=\"http://goessner.net/articles/JsonPath/\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n              <input type=\"text\" name=\"predicateJSONPath\" ng-model=\"processor.predicate.jsonpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"processor.predicate.type === \'JSON\'\">\n\n              <label for=\"predicateValue\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'FreeForm\' || action.expression.type === \'Literal\'\">Value:</label>\n              <input type=\"text\" name=\"predicateValue\" ng-model=\"processor.predicate.value\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"processor.predicate.type === \'FreeForm\' || action.expression.type === \'Literal\'\">\n            </div>\n          </form>\n\n          <h4>Actions <a class=\"btn btn-primary\" ng-click=\"addAction(processor)\"><span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\"\"></span></a></h4>\n\n          <uib-accordion>\n            <uib-accordion-group ng-repeat=\"action in processor.actions\" is-open=\"false\" is-disabled=\"false\">\n              <uib-accordion-heading>[ {{action.actionType}} {{action.name}} ]: {{action.description}} <a class=\"btn btn-link hk-delete pull-right\" href=\"#\" uibTooltip=\"Delete\" tooltip-trigger tooltip-placement=\"top\" ng-click=\"deleteAction(processor,action)\"><i class=\"fa fa-trash-o\"></i></a></uib-accordion-heading>\n\n              <form>\n                <div class=\"form-group\">\n                  <label for=\"description\" style=\"width: 15%\" >Description:</label>\n                  <input type=\"text\" name=\"description\" ng-model=\"action.description\" ng-change=\"setDirty()\" style=\"width: 80%\" >\n                </div>\n\n                <div class=\"form-group\">\n                  <label for=\"actionPredicateType\" style=\"width: 15%\" >Predicate Type: </label>\n                  <select name=\"actionPredicateType\" ng-model=\"action.predicate.type\" ng-change=\"changedExpressionType(action.predicate)\" style=\"width: 30%\">\n                    <option value=\"\"></option>\n                    <option value=\"Literal\">Literal</option>\n                    <option value=\"XML\">XML</option>\n                    <option value=\"JSON\">JSON</option>\n                    <option value=\"Text\">Text</option>\n                    <option value=\"FreeForm\">Free Form</option>\n                  </select>\n\n                  <br>\n\n                  <label for=\"actionPredicateSource\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'XML\' || action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\">Predicate Source: </label>\n                  <select name=\"actionPredicateSource\" ng-model=\"action.predicate.source\" ng-change=\"setDirty()\" ng-show=\"action.predicate.type === \'XML\' ||action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\" style=\"width: 30%\">\n                    <option value=\"Content\">Content</option>\n                    <option value=\"Header\">Header</option>\n                  </select>\n\n                  <label style=\"width: 5%\" ng-show=\"action.predicate.type === \'XML\' || action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\"></label> <!-- TODO: Must be a better way -->\n\n                  <label for=\"actionPredicateKey\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'XML\' || action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\">Predicate Key: </label>\n                  <input type=\"text\" name=\"actionPredicateKey\" ng-model=\"action.predicate.key\" ng-change=\"setDirty()\" style=\"width: 30%\" ng-show=\"action.predicate.type === \'XML\' || action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\">\n\n                  <label for=\"actionPredicateXPath\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'XML\'\">Predicate XPath: <a href=\"http://www.w3schools.com/xsl/xpath_syntax.asp\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n                  <input type=\"text\" name=\"actionPredicateXPath\" ng-model=\"action.predicate.xpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.predicate.type === \'XML\'\">\n\n                  <label for=\"actionPredicateJSONPath\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'JSON\'\">Predicate JSONPath: <a href=\"http://goessner.net/articles/JsonPath/\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n                  <input type=\"text\" name=\"actionPredicateJSONPath\" ng-model=\"action.predicate.jsonpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.predicate.type === \'JSON\'\">\n\n                  <label for=\"actionPredicate\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'FreeForm\' || action.predicate.type === \'Literal\'\">Predicate Value:</label>\n                  <input type=\"text\" name=\"actionPredicate\" ng-model=\"action.predicate.value\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.predicate.type === \'FreeForm\' || action.predicate.type === \'Literal\'\">\n                </div>\n\n                <div class=\"form-group\">\n                  <label for=\"actionTypeSelector\" style=\"width: 15%\" >Action Type: </label>\n                  <select name=\"actionTypeSelector\" ng-model=\"action.actionType\" ng-change=\"changedActionType(action)\" style=\"width: 30%\">\n                    <option value=\"\"></option>\n                    <option value=\"AddContent\">Add Content</option>\n                    <option value=\"AddCorrelationId\">Add Correlation Identifier</option>\n                    <option value=\"EvaluateURI\">Evaluate URI</option>\n                    <option value=\"SetDetail\">Set Detail</option>\n                    <option value=\"SetFault\">Set Fault Code</option>\n                    <option value=\"SetFaultDescription\">Set Fault Description</option>\n                    <option value=\"SetProperty\">Set Property</option>\n                  </select>\n\n                  <br>\n\n                  <label for=\"actionName\" style=\"width: 15%\" ng-show=\"action.actionType === \'AddContent\' || action.actionType === \'SetDetail\' || action.actionType === \'SetProperty\'\" >Name:</label>\n                  <input type=\"text\" name=\"actionName\" ng-model=\"action.name\" ng-change=\"setDirty()\" ng-show=\"action.actionType === \'AddContent\' || action.actionType === \'SetDetail\' || action.actionType === \'SetProperty\'\" style=\"width: 30%\" >\n\n                  <label style=\"width: 5%\" ng-show=\"action.actionType === \'AddContent\'\" ></label> <!-- TODO: Must be a better way -->\n\n                  <label for=\"actionType\" style=\"width: 15%\" ng-show=\"action.actionType === \'AddContent\'\" >Type:</label>\n                  <input type=\"text\" name=\"actionType\" ng-model=\"action.type\" ng-change=\"setDirty()\" ng-show=\"action.actionType === \'AddContent\'\" style=\"width: 30%\" >\n\n                  <label for=\"correlationScope\" ng-show=\"action.actionType === \'AddCorrelationId\'\" style=\"width: 15%\" >Correlation Scope: </label>\n                  <select name=\"correlationScope\" ng-model=\"action.scope\" ng-show=\"action.actionType === \'AddCorrelationId\'\" ng-change=\"setDirty()\" style=\"width: 30%\">\n                    <option value=\"Global\">Global</option>\n                    <option value=\"Interaction\">Interaction</option>\n                    <option value=\"Local\">Local</option>\n                  </select>\n\n                  <label for=\"actionTemplate\" ng-show=\"action.actionType === \'EvaluateURI\'\" style=\"width: 15%\" >Template URI:</label>\n                  <input type=\"text\" name=\"actionTemplate\" ng-model=\"action.template\" ng-change=\"setDirty()\" ng-show=\"action.actionType === \'EvaluateURI\'\" style=\"width: 30%\" >\n\n                </div>\n\n                <div class=\"form-group\" ng-if=\"action.actionType !== \'EvaluateURI\' && action.actionType !== undefined\" >\n\n                  <label for=\"actionTemplate\" style=\"width: 15%\" ng-show=\"action.actionType === \'EvaluateURI\'\" >Template URI:</label>\n                  <input type=\"text\" name=\"actionTemplate\" ng-model=\"action.template\" ng-change=\"setDirty()\" ng-show=\"action.actionType === \'EvaluateURI\'\" style=\"width: 30%\" >\n\n                  <label for=\"actionValueType\" style=\"width: 15%\" >Value Type: </label>\n                  <select name=\"actionValueType\" ng-model=\"action.expression.type\" ng-change=\"changedExpressionType(action.expression)\" style=\"width: 30%\">\n                    <option value=\"\"></option>\n                    <option value=\"Literal\">Literal</option>\n                    <option value=\"XML\">XML</option>\n                    <option value=\"JSON\">JSON</option>\n                    <option value=\"Text\">Text</option>\n                    <option value=\"FreeForm\">Free Form</option>\n                  </select>\n\n                  <br>\n\n                  <label for=\"actionValueSource\" style=\"width: 15%\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\">Value Source: </label>\n                  <select name=\"actionValueSource\" ng-model=\"action.expression.source\" ng-change=\"setDirty()\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\" style=\"width: 30%\">\n                    <option value=\"Content\">Content</option>\n                    <option value=\"Header\">Header</option>\n                  </select>\n\n                  <label style=\"width: 5%\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\"></label> <!-- TODO: Must be a better way -->\n\n                  <label for=\"actionValueKey\" style=\"width: 15%\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\">Value Key: </label>\n                  <input type=\"text\" name=\"actionValueKey\" ng-model=\"action.expression.key\" ng-change=\"setDirty()\" style=\"width: 30%\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\">\n\n                  <label for=\"actionValueXPath\" style=\"width: 15%\" ng-show=\"action.expression.type === \'XML\'\">Value XPath: <a href=\"http://www.w3schools.com/xsl/xpath_syntax.asp\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n                  <input type=\"text\" name=\"actionValueXPath\" ng-model=\"action.expression.xpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.expression.type === \'XML\'\">\n\n                  <label for=\"actionValueJSONPath\" style=\"width: 15%\" ng-show=\"action.expression.type === \'JSON\'\">Value JSONPath: <a href=\"http://goessner.net/articles/JsonPath/\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n                  <input type=\"text\" name=\"actionValueJSONPath\" ng-model=\"action.expression.jsonpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.expression.type === \'JSON\'\">\n\n                  <label for=\"actionValue\" style=\"width: 15%\" ng-show=\"action.expression.type === \'FreeForm\' || action.expression.type === \'Literal\'\">Value:</label>\n                  <input type=\"text\" name=\"actionValue\" ng-model=\"action.expression.value\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.expression.type === \'FreeForm\' || action.expression.type === \'Literal\'\">\n                </div>\n              </form>\n\n            </uib-accordion-group>\n          </uib-accordion>\n\n        </uib-accordion-group>\n      </uib-accordion>\n    </div>\n\n  </div>\n</div>\n");
$templateCache.put("plugins/btm/html/btxndisabled.html","<div class=\"col-md-12\" ng-controller=\"BTM.BTMDisabledController\">\n  <div class=\"text-center hk-urls-list hk-spinner-container\" ng-hide=\"businessTransactions\">\n    <div class=\"spinner spinner-lg\"></div>\n    <p class=\"hk-spinner-legend-below\">Loading...</p>\n  </div>\n\n  <div class=\"row\" ng-show=\"businessTransactions\" >\n\n    <hawkular-subtab class=\"hk-align-center\">\n      <div class=\"hk-nav-tabs-container\">\n        <ul class=\"nav nav-tabs nav-tabs-pf\">\n          <li><a href=\"active\" >Active</a></li>\n          <li><a href=\"candidates\">Candidates <i class=\"fa fa-flag\" ng-hide=\"candidateCount === 0\"></i></a></li>\n          <li class=\"active\"><a href=\"disabled\">Disabled</a></li>\n          <li><a href=\"ignored\">Ignored</a></li>\n        </ul>\n      </div>\n    </hawkular-subtab>\n\n    <section id=\"active\" class=\"hk-tab-content\">\n\n      <div class=\"col-md-9 hk-align-center\">\n        <ul class=\"list-group\" >\n          <br>\n          <div class=\"row\" >\n            Search: <input ng-model=\"query\">\n          </div>\n\n          <div class=\"hk-url-item\" ng-repeat=\"btxn in businessTransactions | filter:query\" >\n            <div class=\"panel panel-default hk-url-heading\" ng-show=\"btxn.summary.level === \'None\'\">\n              <a href=\"info/{{btxn.summary.name}}\">{{btxn.summary.name}}</a>\n              <span class=\"hk-settings pull-right\">\n                <a href=\"config/{{btxn.summary.name}}\" ><i class=\"fa fa-cog\"></i></a>\n                <a href=\"#\" ng-click=\"deleteBusinessTxn(btxn)\"><i class=\"fa fa-trash-o\"></i></a>\n              </span>\n            </div>\n          </div>\n       </ul>\n      </div>\n    </section>\n  </div>\n</div>\n\n");
$templateCache.put("plugins/btm/html/btxnignored.html","<div class=\"col-md-12\" ng-controller=\"BTM.BTMIgnoredController\">\n  <div class=\"text-center hk-urls-list hk-spinner-container\" ng-hide=\"businessTransactions\">\n    <div class=\"spinner spinner-lg\"></div>\n    <p class=\"hk-spinner-legend-below\">Loading...</p>\n  </div>\n\n  <div class=\"row\" ng-show=\"businessTransactions\" >\n\n    <hawkular-subtab class=\"hk-align-center\">\n      <div class=\"hk-nav-tabs-container\">\n        <ul class=\"nav nav-tabs nav-tabs-pf\">\n          <li><a href=\"active\" >Active</a></li>\n          <li><a href=\"candidates\">Candidates <i class=\"fa fa-flag\" ng-hide=\"candidateCount === 0\"></i></a></li>\n          <li><a href=\"disabled\">Disabled</a></li>\n          <li class=\"active\"><a href=\"ignored\">Ignored</a></li>\n        </ul>\n      </div>\n    </hawkular-subtab>\n\n    <section id=\"active\" class=\"hk-tab-content\">\n\n      <div class=\"col-md-9 hk-align-center\">\n        <ul class=\"list-group\" >\n          <br>\n          <div class=\"row\" >\n            Search: <input ng-model=\"query\">\n          </div>\n\n          <div class=\"hk-url-item\" ng-repeat=\"btxn in businessTransactions | filter:query\" >\n            <div class=\"panel panel-default hk-url-heading\" ng-show=\"btxn.summary.level === \'Ignore\'\">\n              <a href=\"info/{{btxn.summary.name}}\">{{btxn.summary.name}}</a>\n              <span class=\"hk-settings pull-right\">\n                <a href=\"config/{{btxn.summary.name}}\" ><i class=\"fa fa-cog\"></i></a>\n                <a href=\"#\" ng-click=\"deleteBusinessTxn(btxn)\"><i class=\"fa fa-trash-o\"></i></a>\n              </span>\n            </div>\n          </div>\n       </ul>\n      </div>\n    </section>\n  </div>\n</div>\n\n");
$templateCache.put("plugins/btm/html/btxninfo.html","<div class=\"row\">\n  <div class=\"col-md-12\" ng-controller=\"BTM.BTxnConfigController\">\n    <h1><span style=\"color:grey\">{{businessTransactionName}}</span></h1>\n\n    <span>\n      <form>\n        <div class=\"form-group\">\n          <label for=\"intervalField\" style=\"width: 10%\" class=\"\" >Aggregation Interval:</label>\n          <select name=\"intervalField\" ng-model=\"config.interval\" style=\"width: 10%\">\n            <option value=\"1000\">Seconds</option>\n            <option value=\"60000\">Minutes</option>\n            <option value=\"3600000\">Hours</option>\n          </select>\n\n          <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n          <label for=\"timeSpanField\" style=\"width: 5%\" >Time span:</label>\n          <select name=\"timeSpanField\" ng-model=\"criteria.startTime\" style=\"width: 10%\">\n            <option value=\"-60000\">1 Minute</option>\n            <option value=\"-600000\">10 Minutes</option>\n            <option value=\"-1800000\">30 Minutes</option>\n            <option value=\"-3600000\">1 Hour</option>\n            <option value=\"-14400000\">4 Hours</option>\n            <option value=\"-28800000\">8 Hours</option>\n            <option value=\"-43200000\">12 Hours</option>\n            <option value=\"-86400000\">Day</option>\n            <option value=\"-604800000\">Week</option>\n            <option value=\"-2419200000\">Month</option>\n            <option value=\"-15768000000\">6 Months</option>\n            <option value=\"-31536000000\">Year</option>\n            <option value=\"1\">All</option>\n          </select>\n\n          <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n          <label for=\"endTimeField\" style=\"width: 3%\" >Until:</label>\n          <select name=\"endTimeField\" ng-model=\"criteria.endTime\" style=\"width: 10%\">\n            <option value=\"0\">Now</option>\n          </select>\n        </div>\n      </form>\n    </span>\n\n    <div id=\"completiontimelinechart\"></div>\n\n  </div>\n</div>\n");}]); hawtioPluginLoader.addModule("hawkularbtm-templates");