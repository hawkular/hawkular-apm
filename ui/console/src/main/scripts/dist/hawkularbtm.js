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
            $scope.chart = "None";
            $scope.txnCountValues = [];
            $scope.faultCountValues = [];
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
                    console.log("Failed to get business txn summaries: " + JSON.stringify(resp));
                });
                $http.get('/hawkular/btm/analytics/businesstxn/unbounduris').then(function (resp) {
                    $scope.candidateCount = Object.keys(resp.data).length;
                }, function (resp) {
                    console.log("Failed to get candidate count: " + JSON.stringify(resp));
                });
            };
            $scope.reload();
            $interval(function () {
                $scope.reload();
            }, 10000);
            $scope.getBusinessTxnDetails = function (btxn) {
                $http.get('/hawkular/btm/analytics/businesstxn/completion/count?businessTransaction=' + btxn.summary.name).then(function (resp) {
                    btxn.count = resp.data;
                    $scope.reloadTxnCountGraph();
                }, function (resp) {
                    console.log("Failed to get count: " + JSON.stringify(resp));
                });
                $http.get('/hawkular/btm/analytics/businesstxn/completion/percentiles?businessTransaction=' + btxn.summary.name).then(function (resp) {
                    if (resp.data.percentiles[95] > 0) {
                        btxn.percentile95 = Math.round(resp.data.percentiles[95] / 1000000) / 1000;
                    }
                    else {
                        btxn.percentile95 = 0;
                    }
                }, function (resp) {
                    console.log("Failed to get completion percentiles: " + JSON.stringify(resp));
                });
                $http.get('/hawkular/btm/analytics/businesstxn/completion/faultcount?businessTransaction=' + btxn.summary.name).then(function (resp) {
                    btxn.faultcount = resp.data;
                    $scope.reloadFaultCountGraph();
                }, function (resp) {
                    console.log("Failed to get fault count: " + JSON.stringify(resp));
                });
                $http.get('/hawkular/btm/analytics/alerts/count/' + btxn.summary.name).then(function (resp) {
                    btxn.alerts = resp.data;
                }, function (resp) {
                    console.log("Failed to get alerts count: " + JSON.stringify(resp));
                });
            };
            $scope.deleteBusinessTxn = function (btxn) {
                if (confirm('Are you sure you want to delete business transaction \"' + btxn.summary.name + '\"?')) {
                    $http.delete('/hawkular/btm/config/businesstxn/' + btxn.summary.name).then(function (resp) {
                        console.log('Deleted: ' + btxn.summary.name);
                        $scope.businessTransactions.remove(btxn);
                    }, function (resp) {
                        console.log("Failed to delete business txn '" + btxn.summary.name + "': " + JSON.stringify(resp));
                    });
                }
            };
            $scope.initGraph = function () {
                $scope.btxncountpiechart = c3.generate({
                    bindto: '#btxntxncountpiechart',
                    data: {
                        json: [],
                        type: 'pie',
                        onclick: function (d, i) {
                            $location.path('info/' + d.id);
                        }
                    }
                });
                $scope.btxnfaultcountpiechart = c3.generate({
                    bindto: '#btxnfaultcountpiechart',
                    data: {
                        json: [],
                        type: 'pie',
                        onclick: function (d, i) {
                            $location.path('info/' + d.id);
                        }
                    }
                });
            };
            $scope.reloadTxnCountGraph = function () {
                var removeTxnCountValues = angular.copy($scope.txnCountValues);
                var btxndata = [];
                for (var i = 0; i < $scope.businessTransactions.length; i++) {
                    var btxn = $scope.businessTransactions[i];
                    if (btxn.count !== undefined && btxn.count > 0) {
                        var record = [];
                        record.push(btxn.summary.name);
                        record.push(btxn.count);
                        btxndata.push(record);
                        if ($scope.txnCountValues.contains(btxn.summary.name)) {
                            removeTxnCountValues.remove(btxn.summary.name);
                        }
                        else {
                            $scope.txnCountValues.add(btxn.summary.name);
                        }
                    }
                }
                $scope.btxncountpiechart.load({
                    columns: btxndata
                });
                for (var j = 0; j < removeTxnCountValues.length; j++) {
                    $scope.btxncountpiechart.unload(removeTxnCountValues[j]);
                    $scope.txnCountValues.remove(removeTxnCountValues[j]);
                }
            };
            $scope.reloadFaultCountGraph = function () {
                var removeFaultCountValues = angular.copy($scope.faultCountValues);
                var btxnfaultdata = [];
                for (var i = 0; i < $scope.businessTransactions.length; i++) {
                    var btxn = $scope.businessTransactions[i];
                    if (btxn.faultcount !== undefined && btxn.faultcount > 0) {
                        var record = [];
                        record.push(btxn.summary.name);
                        record.push(btxn.faultcount);
                        btxnfaultdata.push(record);
                        if ($scope.faultCountValues.contains(btxn.summary.name)) {
                            removeFaultCountValues.remove(btxn.summary.name);
                        }
                        else {
                            $scope.faultCountValues.add(btxn.summary.name);
                        }
                    }
                }
                $scope.btxnfaultcountpiechart.load({
                    columns: btxnfaultdata
                });
                for (var j = 0; j < removeFaultCountValues.length; j++) {
                    $scope.btxnfaultcountpiechart.unload(removeFaultCountValues[j]);
                    $scope.faultCountValues.remove(removeFaultCountValues[j]);
                }
            };
            $scope.initGraph();
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
                console.log("Failed to get business txn summaries: " + JSON.stringify(resp));
            });
            $scope.reload = function () {
                $http.get('/hawkular/btm/analytics/businesstxn/unbounduris?compress=true').then(function (resp) {
                    $scope.unbounduris = resp.data;
                    $scope.candidateCount = Object.keys(resp.data).length;
                    var selected = $scope.selecteduris;
                    $scope.selecteduris = [];
                    for (var i = 0; i < $scope.unbounduris.length; i++) {
                        for (var j = 0; j < selected.length; j++) {
                            if ($scope.unbounduris[i].uri === selected[j].uri) {
                                $scope.selecteduris.add($scope.unbounduris[i]);
                            }
                        }
                    }
                }, function (resp) {
                    console.log("Failed to get unbound URIs: " + JSON.stringify(resp));
                });
            };
            $scope.reload();
            $interval(function () {
                $scope.reload();
            }, 10000);
            $scope.addBusinessTxn = function () {
                var defn = {
                    filter: {
                        inclusions: []
                    },
                    processors: []
                };
                for (var i = 0; i < $scope.selecteduris.length; i++) {
                    defn.filter.inclusions.add($scope.selecteduris[i].regex);
                    if ($scope.selecteduris[i].template !== undefined) {
                        defn.processors.add({
                            description: "Process inbound request",
                            nodeType: "Consumer",
                            direction: "In",
                            uriFilter: $scope.selecteduris[i].regex,
                            actions: [{
                                    actionType: "EvaluateURI",
                                    description: "Extract parameters from path",
                                    template: $scope.selecteduris[i].template
                                }]
                        });
                    }
                }
                $http.put('/hawkular/btm/config/businesstxn/' + $scope.newBTxnName, defn).then(function (resp) {
                    $location.path('config/' + $scope.newBTxnName);
                }, function (resp) {
                    console.log("Failed to add business txn '" + $scope.newBTxnName + "': " + JSON.stringify(resp));
                });
            };
            $scope.ignoreBusinessTxn = function () {
                var defn = {
                    level: 'Ignore',
                    filter: {
                        inclusions: []
                    },
                    processors: []
                };
                for (var i = 0; i < $scope.selecteduris.length; i++) {
                    defn.filter.inclusions.add($scope.selecteduris[i].regex);
                    if ($scope.selecteduris[i].template !== undefined) {
                        defn.processors.add({
                            description: "Process inbound request",
                            nodeType: "Consumer",
                            direction: "In",
                            uriFilter: $scope.selecteduris[i].regex,
                            actions: [{
                                    actionType: "EvaluateURI",
                                    description: "Extract parameters from path",
                                    template: $scope.selecteduris[i].template
                                }]
                        });
                    }
                }
                $http.put('/hawkular/btm/config/businesstxn/' + $scope.newBTxnName, defn).then(function (resp) {
                    $location.path('config/' + $scope.newBTxnName);
                }, function (resp) {
                    console.log("Failed to ignore business txn '" + $scope.newBTxnName + "': " + JSON.stringify(resp));
                });
            };
            $scope.updateBusinessTxn = function () {
                $http.get('/hawkular/btm/config/businesstxn/' + $scope.existingBTxnName).then(function (resp) {
                    var btxn = resp.data;
                    for (var i = 0; i < $scope.selecteduris.length; i++) {
                        if (btxn.filter.inclusions.indexOf($scope.selecteduris[i].regex) === -1) {
                            btxn.filter.inclusions.add($scope.selecteduris[i].regex);
                        }
                    }
                    $http.put('/hawkular/btm/config/businesstxn/' + $scope.existingBTxnName, btxn).then(function (resp) {
                        console.log("Saved updated business txn '" + $scope.existingBTxnName + "': " + JSON.stringify(resp));
                        $location.path('config/' + $scope.existingBTxnName);
                    }, function (resp) {
                        console.log("Failed to save business txn '" + $scope.existingBTxnName + "': " + JSON.stringify(resp));
                    });
                }, function (resp) {
                    console.log("Failed to get business txn '" + $scope.existingBTxnName + "': " + JSON.stringify(resp));
                });
            };
            $scope.selectionChanged = function (uriinfo) {
                if ($scope.selecteduris.contains(uriinfo)) {
                    $scope.selecteduris.remove(uriinfo);
                }
                else {
                    $scope.selecteduris.add(uriinfo);
                }
            };
            $scope.isSelected = function (uriinfo) {
                return $scope.selecteduris.contains(uriinfo);
            };
            $scope.getLevel = function (level) {
                if (level === 'All') {
                    return "Active";
                }
                return level;
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
            $scope.messages = [];
            $http.get('/hawkular/btm/config/businesstxn/' + $scope.businessTransactionName).then(function (resp) {
                $scope.businessTransaction = resp.data;
                $scope.original = angular.copy($scope.businessTransaction);
                $http.post('/hawkular/btm/config/businesstxn/validate', $scope.businessTransaction).then(function (resp) {
                    $scope.messages = resp.data;
                }, function (resp) {
                    console.log("Failed to validate business txn '" + $scope.businessTransactionName + "': " + JSON.stringify(resp));
                });
            }, function (resp) {
                console.log("Failed to get business txn '" + $scope.businessTransactionName + "': " + JSON.stringify(resp));
            });
            $http.get('/hawkular/btm/analytics/businesstxn/unbounduris?compress=true').then(function (resp) {
                $scope.unboundURIs = [];
                for (var i = 0; i < resp.data.length; i++) {
                    if (resp.data[i].regex !== undefined) {
                        $scope.unboundURIs.add(resp.data[i].regex);
                    }
                }
            }, function (resp) {
                console.log("Failed to get unbound URIs: " + JSON.stringify(resp));
            });
            $scope.reload = function () {
                $http.get('/hawkular/btm/analytics/businesstxn/bounduris/' + $scope.businessTransactionName).then(function (resp) {
                    $scope.boundURIs = [];
                    for (var i = 0; i < resp.data.length; i++) {
                        var regex = $scope.escapeRegExp(resp.data[i]);
                        $scope.boundURIs.add(regex);
                    }
                }, function (resp) {
                    console.log("Failed to get bound URIs for business txn '" + $scope.businessTransactionName + "': " + JSON.stringify(resp));
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
            $scope.addAction = function (processor, type) {
                $scope.setDirty();
                var newAction = {
                    actionType: type,
                    description: "Action " + (processor.actions.length + 1)
                };
                if (type === 'AddCorrelationId') {
                    newAction['scope'] = 'Global';
                }
                processor.actions.add(newAction);
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
                    $scope.messages = resp.data;
                    $scope.original = angular.copy($scope.businessTransaction);
                    $scope.dirty = false;
                }, function (resp) {
                    console.log("Failed to save business txn '" + $scope.businessTransactionName + "': " + JSON.stringify(resp));
                });
            };
            $http.get('/hawkular/btm/config/businesstxn/' + $scope.businessTransactionName).then(function (resp) {
                $scope.businessTransaction = resp.data;
                $scope.original = angular.copy($scope.businessTransaction);
            }, function (resp) {
                console.log("Failed to get business txn '" + $scope.businessTransactionName + "': " + JSON.stringify(resp));
            });
            $scope.escapeRegExp = function (str) {
                if (str === undefined) {
                    return;
                }
                return "^" + str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&") + "$";
            };
            $scope.closeMessage = function (index) {
                $scope.messages.splice(index, 1);
            };
            $scope.getMessageType = function (entry) {
                var type = 'danger';
                if (entry.severity === 'Warning') {
                    type = 'warning';
                }
                else if (entry.severity === 'Info') {
                    type = 'success';
                }
                return type;
            };
            $scope.getMessageText = function (entry) {
                var message = "";
                if (entry.processor !== undefined) {
                    message = "[" + entry.processor;
                    if (entry.action !== undefined) {
                        message = message + "/" + entry.action;
                    }
                    message = message + "] ";
                }
                message = message + entry.message;
                return message;
            };
            $scope.isError = function (processor, action, field) {
                for (var i = 0; i < $scope.messages.length; i++) {
                    if ($scope.messages[i].processor === processor.description
                        && $scope.messages[i].action === action.description
                        && $scope.messages[i].field === field) {
                        return true;
                    }
                }
                return false;
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
                    console.log("Failed to get business txn summaries: " + JSON.stringify(resp));
                });
                $http.get('/hawkular/btm/analytics/businesstxn/unbounduris').then(function (resp) {
                    $scope.candidateCount = Object.keys(resp.data).length;
                }, function (resp) {
                    console.log("Failed to get candidate count: " + JSON.stringify(resp));
                });
            };
            $scope.reload();
            $scope.deleteBusinessTxn = function (btxn) {
                if (confirm('Are you sure you want to delete business transaction \"' + btxn.summary.name + '\"?')) {
                    $http.delete('/hawkular/btm/config/businesstxn/' + btxn.summary.name).then(function (resp) {
                        console.log('Deleted: ' + btxn.summary.name);
                        $scope.businessTransactions.remove(btxn);
                    }, function (resp) {
                        console.log("Failed to delete business txn '" + btxn.summary.name + "': " + JSON.stringify(resp));
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
                    console.log("Failed to get business txn summaries: " + JSON.stringify(resp));
                });
                $http.get('/hawkular/btm/analytics/businesstxn/unbounduris').then(function (resp) {
                    $scope.candidateCount = Object.keys(resp.data).length;
                }, function (resp) {
                    console.log("Failed to get candidate count: " + JSON.stringify(resp));
                });
            };
            $scope.reload();
            $scope.deleteBusinessTxn = function (btxn) {
                if (confirm('Are you sure you want to delete business transaction \"' + btxn.summary.name + '\"?')) {
                    $http.delete('/hawkular/btm/config/businesstxn/' + btxn.summary.name).then(function (resp) {
                        console.log('Deleted: ' + btxn.summary.name);
                        $scope.businessTransactions.remove(btxn);
                    }, function (resp) {
                        console.log("Failed to delete business txn '" + btxn.summary.name + "': " + JSON.stringify(resp));
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
            $scope.properties = [];
            $scope.propertyValues = [];
            $scope.faultValues = [];
            $scope.criteria = {
                businessTransaction: $scope.businessTransactionName,
                properties: [],
                faults: [],
                startTime: -3600000,
                endTime: "0",
                lowerBound: 0
            };
            $scope.config = {
                interval: 60000,
                selectedProperty: undefined,
                lowerBoundDisplay: 0,
                prevLowerBoundDisplay: 0
            };
            $scope.reload = function () {
                $http.post('/hawkular/btm/analytics/businesstxn/completion/statistics?interval=' + $scope.config.interval, $scope.criteria).then(function (resp) {
                    $scope.statistics = resp.data;
                    $scope.updatedBounds();
                    $scope.redrawLineChart();
                }, function (resp) {
                    console.log("Failed to get statistics: " + JSON.stringify(resp));
                });
                $http.post('/hawkular/btm/analytics/businesstxn/completion/faults', $scope.criteria).then(function (resp) {
                    $scope.faults = resp.data;
                    var removeFaultValues = angular.copy($scope.faultValues);
                    var faultdata = [];
                    for (var i = 0; i < $scope.faults.length; i++) {
                        var fault = $scope.faults[i];
                        var record = [];
                        record.push(fault.value);
                        record.push(fault.count);
                        faultdata.push(record);
                        if ($scope.faultValues.contains(fault.value)) {
                            removeFaultValues.remove(fault.value);
                        }
                        else {
                            $scope.faultValues.add(fault.value);
                        }
                    }
                    $scope.ctfaultschart.load({
                        columns: faultdata
                    });
                    for (var j = 0; j < removeFaultValues.length; j++) {
                        $scope.ctfaultschart.unload(removeFaultValues[j]);
                        $scope.faultValues.remove(removeFaultValues[j]);
                    }
                }, function (resp) {
                    console.log("Failed to get statistics: " + JSON.stringify(resp));
                });
                $http.get('/hawkular/btm/analytics/businesstxn/properties/' + $scope.businessTransactionName).then(function (resp) {
                    $scope.properties = resp.data;
                }, function (resp) {
                    console.log("Failed to get property info: " + JSON.stringify(resp));
                });
                if ($scope.config.selectedProperty !== undefined) {
                    $scope.reloadProperty();
                }
            };
            $scope.redrawLineChart = function () {
                $scope.ctlinechart.load({
                    json: $scope.statistics,
                    keys: {
                        value: ['max', 'average', 'min', 'count', 'faultCount'],
                        x: 'timestamp'
                    }
                });
            };
            $scope.reloadProperty = function () {
                $http.post('/hawkular/btm/analytics/businesstxn/completion/property/' + $scope.config.selectedProperty, $scope.criteria).then(function (resp) {
                    $scope.propertyDetails = resp.data;
                    var removePropertyValues = angular.copy($scope.propertyValues);
                    var propertydata = [];
                    for (var i = 0; i < $scope.propertyDetails.length; i++) {
                        var prop = $scope.propertyDetails[i];
                        var record = [];
                        record.push(prop.value);
                        record.push(prop.count);
                        propertydata.push(record);
                        if ($scope.propertyValues.contains(prop.value)) {
                            removePropertyValues.remove(prop.value);
                        }
                        else {
                            $scope.propertyValues.add(prop.value);
                        }
                    }
                    $scope.propertychart.load({
                        columns: propertydata
                    });
                    for (var j = 0; j < removePropertyValues.length; j++) {
                        $scope.propertychart.unload(removePropertyValues[j]);
                        $scope.propertyValues.remove(removePropertyValues[j]);
                    }
                }, function (resp) {
                    console.log("Failed to get property details for '" + $scope.config.selectedProperty + "': " + JSON.stringify(resp));
                });
            };
            $scope.reload();
            $interval(function () {
                if ($scope.criteria.endTime === "0" || $scope.config.prevLowerBoundDisplay !== $scope.config.lowerBoundDisplay) {
                    $scope.reload();
                    $scope.config.prevLowerBoundDisplay = $scope.config.lowerBoundDisplay;
                }
            }, 10000);
            $scope.initGraph = function () {
                $scope.ctlinechart = c3.generate({
                    bindto: '#completiontimelinechart',
                    data: {
                        json: [],
                        axes: {
                            max: 'y',
                            average: 'y',
                            min: 'y',
                            count: 'y2',
                            faultCount: 'y2'
                        },
                        type: 'line',
                        types: {
                            count: 'bar',
                            faultCount: 'bar'
                        },
                        keys: {
                            value: ['max', 'average', 'min', 'count', 'faultCount'],
                            x: 'timestamp'
                        }
                    },
                    color: {
                        pattern: ['#ff0000', '#33cc33', '#e5e600', '#99ccff', '#ffb3b3']
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
                        y: {
                            label: 'Seconds',
                            padding: { bottom: 0 },
                            tick: {
                                format: function (y) { return y / 1000000000; }
                            }
                        },
                        y2: {
                            show: true
                        }
                    }
                });
                $scope.ctfaultschart = c3.generate({
                    bindto: '#completiontimefaultschart',
                    data: {
                        json: [],
                        type: 'pie',
                        onclick: function (d, i) {
                            var fault = {
                                value: d.id
                            };
                            $scope.criteria.faults.add(fault);
                            $scope.reload();
                        }
                    }
                });
            };
            $scope.initGraph();
            $scope.propertyClicked = function () {
                $scope.initPropertyGraph($scope.config.selectedProperty);
            };
            $scope.initPropertyGraph = function (name) {
                $scope.propertychart = c3.generate({
                    bindto: '#completiontimepropertychart',
                    data: {
                        columns: [],
                        type: 'pie',
                        onclick: function (d, i) {
                            var property = {
                                name: name,
                                value: d.id
                            };
                            $scope.criteria.properties.add(property);
                            $scope.reload();
                        }
                    }
                });
                $scope.reloadProperty();
            };
            $scope.removeProperty = function (property) {
                $scope.criteria.properties.remove(property);
                $scope.reload();
            };
            $scope.removeFault = function (fault) {
                $scope.criteria.faults.remove(fault);
                $scope.reload();
            };
            $scope.toggleExclusion = function (element) {
                element.excluded = !element.excluded;
                $scope.reload();
            };
            $scope.updatedBounds = function () {
                if ($scope.config.lowerBoundDisplay === 0) {
                    $scope.criteria.lowerBound = 0;
                }
                else {
                    var maxDuration = 0;
                    for (var i = 0; i < $scope.statistics.length; i++) {
                        if ($scope.statistics[i].max > maxDuration) {
                            maxDuration = $scope.statistics[i].max;
                        }
                    }
                    if (maxDuration > 0) {
                        $scope.criteria.lowerBound = ($scope.config.lowerBoundDisplay * maxDuration) / 100;
                    }
                }
            };
            $scope.selectAction = function () {
                $scope.reload();
            };
            $scope.currentDateTime = function () {
                return new Date();
            };
        }]);
})(BTM || (BTM = {}));

//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbImluY2x1ZGVzLnRzIiwiYnRtL3RzL2J0bUdsb2JhbHMudHMiLCJidG0vdHMvYnRtUGx1Z2luLnRzIiwiYnRtL3RzL2J0bS50cyIsImJ0bS90cy9idHhuY2FuZGlkYXRlcy50cyIsImJ0bS90cy9idHhuY29uZmlnLnRzIiwiYnRtL3RzL2J0eG5kaXNhYmxlZC50cyIsImJ0bS90cy9idHhuaWdub3JlZC50cyIsImJ0bS90cy9idHhuaW5mby50cyJdLCJuYW1lcyI6WyJCVE0iXSwibWFwcGluZ3MiOiJBQUFBLDJEQUEyRDtBQUMzRCw0REFBNEQ7QUFDNUQsR0FBRztBQUNILG1FQUFtRTtBQUNuRSxvRUFBb0U7QUFDcEUsMkNBQTJDO0FBQzNDLEdBQUc7QUFDSCxnREFBZ0Q7QUFDaEQsR0FBRztBQUNILHVFQUF1RTtBQUN2RSxxRUFBcUU7QUFDckUsNEVBQTRFO0FBQzVFLHVFQUF1RTtBQUN2RSxrQ0FBa0M7QUFFbEMsMERBQTBEOztBQ2YxRCwyREFBMkQ7QUFDM0QsNERBQTREO0FBQzVELEdBQUc7QUFDSCxtRUFBbUU7QUFDbkUsb0VBQW9FO0FBQ3BFLDJDQUEyQztBQUMzQyxHQUFHO0FBQ0gsZ0RBQWdEO0FBQ2hELEdBQUc7QUFDSCx1RUFBdUU7QUFDdkUscUVBQXFFO0FBQ3JFLDRFQUE0RTtBQUM1RSx1RUFBdUU7QUFDdkUsa0NBQWtDO0FBRWxDLHlDQUF5QztBQUN6QyxJQUFPLEdBQUcsQ0FPVDtBQVBELFdBQU8sR0FBRyxFQUFDLENBQUM7SUFFQ0EsY0FBVUEsR0FBR0EsaUJBQWlCQSxDQUFDQTtJQUUvQkEsT0FBR0EsR0FBbUJBLE1BQU1BLENBQUNBLEdBQUdBLENBQUNBLGNBQVVBLENBQUNBLENBQUNBO0lBRTdDQSxnQkFBWUEsR0FBR0Esa0JBQWtCQSxDQUFDQTtBQUMvQ0EsQ0FBQ0EsRUFQTSxHQUFHLEtBQUgsR0FBRyxRQU9UOztBQ3ZCRCwyREFBMkQ7QUFDM0QsNERBQTREO0FBQzVELEdBQUc7QUFDSCxtRUFBbUU7QUFDbkUsb0VBQW9FO0FBQ3BF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LENBQUNBO1lBRUZBLE1BQU1BLENBQUNBLFlBQVlBLEdBQUdBO2dCQUNwQixNQUFNLENBQUMsTUFBTSxFQUFFLENBQUM7WUFDbEIsQ0FBQyxDQUFDQTtZQUVGQSxNQUFNQSxDQUFDQSxlQUFlQSxHQUFHQTtnQkFDdkIsTUFBTSxDQUFDLElBQUksSUFBSSxFQUFFLENBQUM7WUFDcEIsQ0FBQyxDQUFDQTtRQUVKQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtBQUNOQSxDQUFDQSxFQWpSTSxHQUFHLEtBQUgsR0FBRyxRQWlSVCIsImZpbGUiOiJjb21waWxlZC5qcyIsInNvdXJjZXNDb250ZW50IjpbIi8vLyBDb3B5cmlnaHQgMjAxNC0yMDE1IFJlZCBIYXQsIEluYy4gYW5kL29yIGl0cyBhZmZpbGlhdGVzXG4vLy8gYW5kIG90aGVyIGNvbnRyaWJ1dG9ycyBhcyBpbmRpY2F0ZWQgYnkgdGhlIEBhdXRob3IgdGFncy5cbi8vL1xuLy8vIExpY2Vuc2VkIHVuZGVyIHRoZSBBcGFjaGUgTGljZW5zZSwgVmVyc2lvbiAyLjAgKHRoZSBcIkxpY2Vuc2VcIik7XG4vLy8geW91IG1heSBub3QgdXNlIHRoaXMgZmlsZSBleGNlcHQgaW4gY29tcGxpYW5jZSB3aXRoIHRoZSBMaWNlbnNlLlxuLy8vIFlvdSBtYXkgb2J0YWluIGEgY29weSBvZiB0aGUgTGljZW5zZSBhdFxuLy8vXG4vLy8gICBodHRwOi8vd3d3LmFwYWNoZS5vcmcvbGljZW5zZXMvTElDRU5TRS0yLjBcbi8vL1xuLy8vIFVubGVzcyByZXF1aXJlZCBieSBhcHBsaWNhYmxlIGxhdyBvciBhZ3JlZWQgdG8gaW4gd3JpdGluZywgc29mdHdhcmVcbi8vLyBkaXN0cmlidXRlZCB1bmRlciB0aGUgTGljZW5zZSBpcyBkaXN0cmlidXRlZCBvbiBhbiBcIkFTIElTXCIgQkFTSVMsXG4vLy8gV0lUSE9VVCBXQVJSQU5USUVTIE9SIENPTkRJVElPTlMgT0YgQU5ZIEtJTkQsIGVpdGhlciBleHByZXNzIG9yIGltcGxpZWQuXG4vLy8gU2VlIHRoZSBMaWNlbnNlIGZvciB0aGUgc3BlY2lmaWMgbGFuZ3VhZ2UgZ292ZXJuaW5nIHBlcm1pc3Npb25zIGFuZFxuLy8vIGxpbWl0YXRpb25zIHVuZGVyIHRoZSBMaWNlbnNlLlxuXG4vLy8gPHJlZmVyZW5jZSBwYXRoPVwiLi4vbGlicy9oYXd0aW8tdXRpbGl0aWVzL2RlZnMuZC50c1wiLz5cbiIsIi8vLyBDb3B5cmlnaHQgMjAxNC0yMDE1IFJlZCBIYXQsIEluYy4gYW5kL29yIGl0cyBhZmZpbGlhdGVzXG4vLy8gYW5kIG90aGVyIGNvbnRyaWJ1dG9ycyBhcyBpbmRpY2F0ZWQgYnkgdGhlIEBhdXRob3IgdGFncy5cbi8vL1xuLy8vIExpY2Vuc2VkIHVuZGVyIHRoZSBBcGFjaGUgTGljZW5zZSwgVmVyc2lvbiAyLjAgKHRoZSBcIkxpY2Vuc2VcIik7XG4vLy8geW91IG1heSBub3QgdXNlIHRoaXMgZmlsZSBleGNlcHQgaW4gY29tcGxpYW5jZSB3aXRoIHRoZSBMaWNlbnNlLlxuLy8vIFlvdSBtYXkgb2J0YWluIGEgY29weSBvZiB0aGUgTGljZW5zZSBhdFxuLy8vXG4vLy8gICBodHRwOi8vd3d3LmFwYWNoZS5vcmcvbGljZW5zZXMvTElDRU5TRS0yLjBcbi8vL1xuLy8vIFVubGVzcyByZXF1aXJlZCBieSBhcHBsaWNhYmxlIGxhdyBvciBhZ3JlZWQgdG8gaW4gd3JpdGluZywgc29mdHdhcmVcbi8vLyBkaXN0cmlidXRlZCB1bmRlciB0aGUgTGljZW5zZSBpcyBkaXN0cmlidXRlZCBvbiBhbiBcIkFTIElTXCIgQkFTSVMsXG4vLy8gV0lUSE9VVCBXQVJSQU5USUVTIE9SIENPTkRJVElPTlMgT0YgQU5ZIEtJTkQsIGVpdGhlciBleHByZXNzIG9yIGltcGxpZWQuXG4vLy8gU2VlIHRoZSBMaWNlbnNlIGZvciB0aGUgc3BlY2lmaWMgbGFuZ3VhZ2UgZ292ZXJuaW5nIHBlcm1pc3Npb25zIGFuZFxuLy8vIGxpbWl0YXRpb25zIHVuZGVyIHRoZSBMaWNlbnNlLlxuXG4vLy8gPHJlZmVyZW5jZSBwYXRoPVwiLi4vLi4vaW5jbHVkZXMudHNcIi8+XG5tb2R1bGUgQlRNIHtcblxuICBleHBvcnQgdmFyIHBsdWdpbk5hbWUgPSBcImhhd3Rpby1hc3NlbWJseVwiO1xuXG4gIGV4cG9ydCB2YXIgbG9nOiBMb2dnaW5nLkxvZ2dlciA9IExvZ2dlci5nZXQocGx1Z2luTmFtZSk7XG5cbiAgZXhwb3J0IHZhciB0ZW1wbGF0ZVBhdGggPSBcInBsdWdpbnMvYnRtL2h0bWxcIjtcbn1cbiIsIi8vLyBDb3B5cmlnaHQgMjAxNC0yMDE1IFJlZCBIYXQsIEluYy4gYW5kL29yIGl0cyBhZmZpbGlhdGVzXG4vLy8gYW5kIG90aGVyIGNvbnRyaWJ1dG9ycyBhcyBpbmRpY2F0ZWQgYnkgdGhlIEBhdXRob3IgdGFncy5cbi8vL1xuLy8vIExpY2Vuc2VkIHVuZGVyIHRoZSBBcGFjaGUgTGljZW5zZSwgVmVyc2lvbiAyLjAgKHRoZSBcIkxpY2Vuc2VcIik7XG4vLy8geW91IG1heSBub3QgdXNlIHRoaXMgZmlsZSBleGNlcHQgaW4gY29tcGxpYW5jZSB3aXRoIHRoZSBMaWNlbnNlLlxuLy8vIFlvdSBtYXkgb2J0YWluIGEgY29weSBvZiB0aGUgTGljZW5zZSBhdFxuLy8vXG4vLy8gICBodHRwOi8vd3d3LmFwYWNoZS5vcmcvbGljZW5zZXMvTElDRU5TRS0yLjBcbi8vL1xuLy8vIFVubGVzcyByZXF1aXJlZCBieSBhcHBsaWNhYmxlIGxhdyBvciBhZ3JlZWQgdG8gaW4gd3JpdGluZywgc29mdHdhcmVcbi8vLyBkaXN0cmlidXRlZCB1bmRlciB0aGUgTGljZW5zZSBpcyBkaXN0cmlidXRlZCBvbiBhbiBcIkFTIElTXCIgQkFTSVMsXG4vLy8gV0lUSE9VVCBXQVJSQU5USUVTIE9SIENPTkRJVElPTlMgT0YgQU5ZIEtJTkQsIGVpdGhlciBleHByZXNzIG9yIGltcGxpZWQuXG4vLy8gU2VlIHRoZSBMaWNlbnNlIGZvciB0aGUgc3BlY2lmaWMgbGFuZ3VhZ2UgZ292ZXJuaW5nIHBlcm1pc3Npb25zIGFuZFxuLy8vIGxpbWl0YXRpb25zIHVuZGVyIHRoZSBMaWNlbnNlLlxuXG4vLy8gPHJlZmVyZW5jZSBwYXRoPVwiLi4vLi4vaW5jbHVkZXMudHNcIi8+XG4vLy8gPHJlZmVyZW5jZSBwYXRoPVwiYnRtR2xvYmFscy50c1wiLz5cbm1vZHVsZSBCVE0ge1xuXG4gIGV4cG9ydCB2YXIgX21vZHVsZSA9IGFuZ3VsYXIubW9kdWxlKEJUTS5wbHVnaW5OYW1lLCBbXCJ4ZWRpdGFibGVcIixcInVpLmJvb3RzdHJhcFwiXSk7XG5cbiAgdmFyIHRhYiA9IHVuZGVmaW5lZDtcblxuICBfbW9kdWxlLmNvbmZpZyhbXCIkbG9jYXRpb25Qcm92aWRlclwiLCBcIiRyb3V0ZVByb3ZpZGVyXCIsIFwiSGF3dGlvTmF2QnVpbGRlclByb3ZpZGVyXCIsXG4gICAgKCRsb2NhdGlvblByb3ZpZGVyLCAkcm91dGVQcm92aWRlcjogbmcucm91dGUuSVJvdXRlUHJvdmlkZXIsIGJ1aWxkZXI6IEhhd3Rpb01haW5OYXYuQnVpbGRlckZhY3RvcnkpID0+IHtcbiAgICB0YWIgPSBidWlsZGVyLmNyZWF0ZSgpXG4gICAgICAuaWQoQlRNLnBsdWdpbk5hbWUpXG4gICAgICAudGl0bGUoKCkgPT4gXCJCdXNpbmVzcyBUcmFuc2FjdGlvbnNcIilcbiAgICAgIC5ocmVmKCgpID0+IFwiL1wiKVxuICAgICAgLmJ1aWxkKCk7XG4gICAgYnVpbGRlci5jb25maWd1cmVSb3V0aW5nKCRyb3V0ZVByb3ZpZGVyLCB0YWIpO1xuICAgICRsb2NhdGlvblByb3ZpZGVyLmh0bWw1TW9kZSh0cnVlKTtcbiAgICAkcm91dGVQcm92aWRlci5cbiAgICAgIHdoZW4oJy8nLCB7XG4gICAgICAgIHRlbXBsYXRlVXJsOiAncGx1Z2lucy9idG0vaHRtbC9idG0uaHRtbCcsXG4gICAgICAgIGNvbnRyb2xsZXI6ICdCVE0uQlRNQ29udHJvbGxlcidcbiAgICAgIH0pLlxuICAgICAgd2hlbignL2FjdGl2ZScsIHtcbiAgICAgICAgdGVtcGxhdGVVcmw6ICdwbHVnaW5zL2J0bS9odG1sL2J0bS5odG1sJyxcbiAgICAgICAgY29udHJvbGxlcjogJ0JUTS5CVE1Db250cm9sbGVyJ1xuICAgICAgfSkuXG4gICAgICB3aGVuKCcvY2FuZGlkYXRlcycsIHtcbiAgICAgICAgdGVtcGxhdGVVcmw6ICdwbHVnaW5zL2J0bS9odG1sL2J0eG5jYW5kaWRhdGVzLmh0bWwnLFxuICAgICAgICBjb250cm9sbGVyOiAnQlRNLkJUTUNhbmRpZGF0ZXNDb250cm9sbGVyJ1xuICAgICAgfSkuXG4gICAgICB3aGVuKCcvZGlzYWJsZWQnLCB7XG4gICAgICAgIHRlbXBsYXRlVXJsOiAncGx1Z2lucy9idG0vaHRtbC9idHhuZGlzYWJsZWQuaHRtbCcsXG4gICAgICAgIGNvbnRyb2xsZXI6ICdCVE0uQlRNRGlzYWJsZWRDb250cm9sbGVyJ1xuICAgICAgfSkuXG4gICAgICB3aGVuKCcvaWdub3JlZCcsIHtcbiAgICAgICAgdGVtcGxhdGVVcmw6ICdwbHVnaW5zL2J0bS9odG1sL2J0eG5pZ25vcmVkLmh0bWwnLFxuICAgICAgICBjb250cm9sbGVyOiAnQlRNLkJUTUlnbm9yZWRDb250cm9sbGVyJ1xuICAgICAgfSkuXG4gICAgICB3aGVuKCcvY29uZmlnLzpidXNpbmVzc3RyYW5zYWN0aW9uJywge1xuICAgICAgICB0ZW1wbGF0ZVVybDogJ3BsdWdpbnMvYnRtL2h0bWwvYnR4bmNvbmZpZy5odG1sJyxcbiAgICAgICAgY29udHJvbGxlcjogJ0JUTS5CVHhuQ29uZmlnQ29udHJvbGxlcidcbiAgICAgIH0pLlxuICAgICAgd2hlbignL2luZm8vOmJ1c2luZXNzdHJhbnNhY3Rpb24nLCB7XG4gICAgICAgIHRlbXBsYXRlVXJsOiAncGx1Z2lucy9idG0vaHRtbC9idHhuaW5mby5odG1sJyxcbiAgICAgICAgY29udHJvbGxlcjogJ0JUTS5CVHhuSW5mb0NvbnRyb2xsZXInXG4gICAgICB9KTtcbiAgfV0pO1xuXG4gIF9tb2R1bGUucnVuKGZ1bmN0aW9uKCRodHRwLCRsb2NhdGlvbikge1xuICAgIC8vIE9ubHkgc2V0IGF1dGhvcml6YXRpb24gaWYgdXNpbmcgZGV2ZWxvcG1lbnQgVVJMXG4gICAgaWYgKCRsb2NhdGlvbi5hYnNVcmwoKS5pbmRleE9mKCdodHRwOi8vbG9jYWxob3N0OjI3NzIvJykgPT09IDApIHtcbiAgICAgICRodHRwLmRlZmF1bHRzLmhlYWRlcnMuY29tbW9uLkF1dGhvcml6YXRpb24gPSAnQmFzaWMgYW1SdlpUcHdZWE56ZDI5eVpBPT0nO1xuICAgIH1cbiAgfSk7XG5cbiAgX21vZHVsZS5ydW4oZnVuY3Rpb24oZWRpdGFibGVPcHRpb25zKSB7XG4gICAgZWRpdGFibGVPcHRpb25zLnRoZW1lID0gJ2JzMyc7IC8vIGJvb3RzdHJhcDMgdGhlbWUuIENhbiBiZSBhbHNvICdiczInLCAnZGVmYXVsdCdcbiAgfSk7XG5cbiAgX21vZHVsZS5ydW4oW1wiSGF3dGlvTmF2XCIsIChIYXd0aW9OYXY6IEhhd3Rpb01haW5OYXYuUmVnaXN0cnkpID0+IHtcbiAgICBIYXd0aW9OYXYuYWRkKHRhYik7XG4gICAgbG9nLmRlYnVnKFwibG9hZGVkXCIpO1xuICB9XSk7XG5cbiAgaGF3dGlvUGx1Z2luTG9hZGVyLmFkZE1vZHVsZShCVE0ucGx1Z2luTmFtZSk7XG59XG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cImJ0bVBsdWdpbi50c1wiLz5cbm1vZHVsZSBCVE0ge1xuXG4gIGRlY2xhcmUgdmFyIGMzOiBhbnk7XG5cbiAgZXhwb3J0IHZhciBCVE1Db250cm9sbGVyID0gX21vZHVsZS5jb250cm9sbGVyKFwiQlRNLkJUTUNvbnRyb2xsZXJcIiwgW1wiJHNjb3BlXCIsIFwiJGh0dHBcIiwgJyRsb2NhdGlvbicsICckaW50ZXJ2YWwnLCAoJHNjb3BlLCAkaHR0cCwgJGxvY2F0aW9uLCAkaW50ZXJ2YWwpID0+IHtcblxuICAgICRzY29wZS5uZXdCVHhuTmFtZSA9ICcnO1xuICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IDA7XG4gICAgXG4gICAgJHNjb3BlLmNoYXJ0ID0gXCJOb25lXCI7XG4gICAgXG4gICAgJHNjb3BlLnR4bkNvdW50VmFsdWVzID0gW107XG4gICAgJHNjb3BlLmZhdWx0Q291bnRWYWx1ZXMgPSBbXTtcblxuICAgICRzY29wZS5yZWxvYWQgPSBmdW5jdGlvbigpIHtcbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG5zdW1tYXJ5JykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucyA9IFtdO1xuICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IHJlc3AuZGF0YS5sZW5ndGg7IGkrKykge1xuICAgICAgICAgIHZhciBidHhuID0ge1xuICAgICAgICAgICAgc3VtbWFyeTogcmVzcC5kYXRhW2ldLFxuICAgICAgICAgICAgY291bnQ6IHVuZGVmaW5lZCxcbiAgICAgICAgICAgIGZhdWx0Y291bnQ6IHVuZGVmaW5lZCxcbiAgICAgICAgICAgIHBlcmNlbnRpbGU5NTogdW5kZWZpbmVkLFxuICAgICAgICAgICAgYWxlcnRzOiB1bmRlZmluZWRcbiAgICAgICAgICB9O1xuICAgICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucy5hZGQoYnR4bik7XG5cbiAgICAgICAgICAkc2NvcGUuZ2V0QnVzaW5lc3NUeG5EZXRhaWxzKGJ0eG4pO1xuICAgICAgICB9XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGJ1c2luZXNzIHR4biBzdW1tYXJpZXM6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgIH0pO1xuXG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL3VuYm91bmR1cmlzJykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IE9iamVjdC5rZXlzKHJlc3AuZGF0YSkubGVuZ3RoO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBjYW5kaWRhdGUgY291bnQ6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgIH0pO1xuICAgIH07XG5cbiAgICAkc2NvcGUucmVsb2FkKCk7XG5cbiAgICAkaW50ZXJ2YWwoZnVuY3Rpb24oKSB7XG4gICAgICAkc2NvcGUucmVsb2FkKCk7XG4gICAgfSwxMDAwMCk7XG5cbiAgICAkc2NvcGUuZ2V0QnVzaW5lc3NUeG5EZXRhaWxzID0gZnVuY3Rpb24oYnR4bikge1xuICAgICAgJGh0dHAuZ2V0KCcvaGF3a3VsYXIvYnRtL2FuYWx5dGljcy9idXNpbmVzc3R4bi9jb21wbGV0aW9uL2NvdW50P2J1c2luZXNzVHJhbnNhY3Rpb249JytidHhuLnN1bW1hcnkubmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGJ0eG4uY291bnQgPSByZXNwLmRhdGE7XG4gICAgICAgIFxuICAgICAgICAkc2NvcGUucmVsb2FkVHhuQ291bnRHcmFwaCgpO1xuXG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGNvdW50OiBcIitKU09OLnN0cmluZ2lmeShyZXNwKSk7XG4gICAgICB9KTtcblxuICAgICAgJGh0dHAuZ2V0KCcvaGF3a3VsYXIvYnRtL2FuYWx5dGljcy9idXNpbmVzc3R4bi9jb21wbGV0aW9uL3BlcmNlbnRpbGVzP2J1c2luZXNzVHJhbnNhY3Rpb249JytidHhuLnN1bW1hcnkubmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGlmIChyZXNwLmRhdGEucGVyY2VudGlsZXNbOTVdID4gMCkge1xuICAgICAgICAgIGJ0eG4ucGVyY2VudGlsZTk1ID0gTWF0aC5yb3VuZCggcmVzcC5kYXRhLnBlcmNlbnRpbGVzWzk1XSAvIDEwMDAwMDAgKSAvIDEwMDA7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgYnR4bi5wZXJjZW50aWxlOTUgPSAwO1xuICAgICAgICB9XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGNvbXBsZXRpb24gcGVyY2VudGlsZXM6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgIH0pO1xuXG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL2NvbXBsZXRpb24vZmF1bHRjb3VudD9idXNpbmVzc1RyYW5zYWN0aW9uPScrYnR4bi5zdW1tYXJ5Lm5hbWUpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBidHhuLmZhdWx0Y291bnQgPSByZXNwLmRhdGE7XG4gICAgICAgIFxuICAgICAgICAkc2NvcGUucmVsb2FkRmF1bHRDb3VudEdyYXBoKCk7XG5cbiAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgZmF1bHQgY291bnQ6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgIH0pO1xuXG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2FsZXJ0cy9jb3VudC8nK2J0eG4uc3VtbWFyeS5uYW1lKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgYnR4bi5hbGVydHMgPSByZXNwLmRhdGE7XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGFsZXJ0cyBjb3VudDogXCIrSlNPTi5zdHJpbmdpZnkocmVzcCkpO1xuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS5kZWxldGVCdXNpbmVzc1R4biA9IGZ1bmN0aW9uKGJ0eG4pIHtcbiAgICAgIGlmIChjb25maXJtKCdBcmUgeW91IHN1cmUgeW91IHdhbnQgdG8gZGVsZXRlIGJ1c2luZXNzIHRyYW5zYWN0aW9uIFxcXCInK2J0eG4uc3VtbWFyeS5uYW1lKydcXFwiPycpKSB7XG4gICAgICAgICRodHRwLmRlbGV0ZSgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG4vJytidHhuLnN1bW1hcnkubmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICAgY29uc29sZS5sb2coJ0RlbGV0ZWQ6ICcrYnR4bi5zdW1tYXJ5Lm5hbWUpO1xuICAgICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucy5yZW1vdmUoYnR4bik7XG4gICAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGRlbGV0ZSBidXNpbmVzcyB0eG4gJ1wiK2J0eG4uc3VtbWFyeS5uYW1lK1wiJzogXCIrSlNPTi5zdHJpbmdpZnkocmVzcCkpO1xuICAgICAgICB9KTtcbiAgICAgIH1cbiAgICB9O1xuXG4gICAgJHNjb3BlLmluaXRHcmFwaCA9IGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLmJ0eG5jb3VudHBpZWNoYXJ0ID0gYzMuZ2VuZXJhdGUoe1xuICAgICAgICBiaW5kdG86ICcjYnR4bnR4bmNvdW50cGllY2hhcnQnLFxuICAgICAgICBkYXRhOiB7XG4gICAgICAgICAganNvbjogW1xuICAgICAgICAgIF0sXG4gICAgICAgICAgdHlwZTogJ3BpZScsXG4gICAgICAgICAgb25jbGljazogZnVuY3Rpb24gKGQsIGkpIHtcbiAgICAgICAgICAgICRsb2NhdGlvbi5wYXRoKCdpbmZvLycrZC5pZCk7XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KTtcblxuICAgICAgJHNjb3BlLmJ0eG5mYXVsdGNvdW50cGllY2hhcnQgPSBjMy5nZW5lcmF0ZSh7XG4gICAgICAgIGJpbmR0bzogJyNidHhuZmF1bHRjb3VudHBpZWNoYXJ0JyxcbiAgICAgICAgZGF0YToge1xuICAgICAgICAgIGpzb246IFtcbiAgICAgICAgICBdLFxuICAgICAgICAgIHR5cGU6ICdwaWUnLFxuICAgICAgICAgIG9uY2xpY2s6IGZ1bmN0aW9uIChkLCBpKSB7XG4gICAgICAgICAgICAkbG9jYXRpb24ucGF0aCgnaW5mby8nK2QuaWQpO1xuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgfSk7XG4gICAgfTtcbiAgICBcbiAgICAkc2NvcGUucmVsb2FkVHhuQ291bnRHcmFwaCA9IGZ1bmN0aW9uKCkge1xuICAgICAgdmFyIHJlbW92ZVR4bkNvdW50VmFsdWVzID0gYW5ndWxhci5jb3B5KCRzY29wZS50eG5Db3VudFZhbHVlcyk7XG5cbiAgICAgIHZhciBidHhuZGF0YSA9IFtdO1xuXG4gICAgICBmb3IgKHZhciBpID0gMDsgaSA8ICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucy5sZW5ndGg7IGkrKykge1xuICAgICAgICB2YXIgYnR4biA9ICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uc1tpXTtcbiAgICAgICAgaWYgKGJ0eG4uY291bnQgIT09IHVuZGVmaW5lZCAmJiBidHhuLmNvdW50ID4gMCkge1xuICAgICAgICAgIHZhciByZWNvcmQ9WyBdO1xuICAgICAgICAgIHJlY29yZC5wdXNoKGJ0eG4uc3VtbWFyeS5uYW1lKTtcbiAgICAgICAgICByZWNvcmQucHVzaChidHhuLmNvdW50KTtcbiAgICAgICAgICBidHhuZGF0YS5wdXNoKHJlY29yZCk7XG5cbiAgICAgICAgICBpZiAoJHNjb3BlLnR4bkNvdW50VmFsdWVzLmNvbnRhaW5zKGJ0eG4uc3VtbWFyeS5uYW1lKSkge1xuICAgICAgICAgICAgcmVtb3ZlVHhuQ291bnRWYWx1ZXMucmVtb3ZlKGJ0eG4uc3VtbWFyeS5uYW1lKTtcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgJHNjb3BlLnR4bkNvdW50VmFsdWVzLmFkZChidHhuLnN1bW1hcnkubmFtZSk7XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9XG5cbiAgICAgICRzY29wZS5idHhuY291bnRwaWVjaGFydC5sb2FkKHtcbiAgICAgICAgY29sdW1uczogYnR4bmRhdGFcbiAgICAgIH0pO1xuXG4gICAgICBmb3IgKHZhciBqPTA7IGogPCByZW1vdmVUeG5Db3VudFZhbHVlcy5sZW5ndGg7IGorKykge1xuICAgICAgICAkc2NvcGUuYnR4bmNvdW50cGllY2hhcnQudW5sb2FkKHJlbW92ZVR4bkNvdW50VmFsdWVzW2pdKTtcbiAgICAgICAgJHNjb3BlLnR4bkNvdW50VmFsdWVzLnJlbW92ZShyZW1vdmVUeG5Db3VudFZhbHVlc1tqXSk7XG4gICAgICB9XG4gICAgfTtcblxuICAgICRzY29wZS5yZWxvYWRGYXVsdENvdW50R3JhcGggPSBmdW5jdGlvbigpIHtcbiAgICAgIHZhciByZW1vdmVGYXVsdENvdW50VmFsdWVzID0gYW5ndWxhci5jb3B5KCRzY29wZS5mYXVsdENvdW50VmFsdWVzKTtcblxuICAgICAgdmFyIGJ0eG5mYXVsdGRhdGEgPSBbXTtcblxuICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgdmFyIGJ0eG4gPSAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnNbaV07XG4gICAgICAgIGlmIChidHhuLmZhdWx0Y291bnQgIT09IHVuZGVmaW5lZCAmJiBidHhuLmZhdWx0Y291bnQgPiAwKSB7XG4gICAgICAgICAgdmFyIHJlY29yZD1bIF07XG4gICAgICAgICAgcmVjb3JkLnB1c2goYnR4bi5zdW1tYXJ5Lm5hbWUpO1xuICAgICAgICAgIHJlY29yZC5wdXNoKGJ0eG4uZmF1bHRjb3VudCk7XG4gICAgICAgICAgYnR4bmZhdWx0ZGF0YS5wdXNoKHJlY29yZCk7XG5cbiAgICAgICAgICBpZiAoJHNjb3BlLmZhdWx0Q291bnRWYWx1ZXMuY29udGFpbnMoYnR4bi5zdW1tYXJ5Lm5hbWUpKSB7XG4gICAgICAgICAgICByZW1vdmVGYXVsdENvdW50VmFsdWVzLnJlbW92ZShidHhuLnN1bW1hcnkubmFtZSk7XG4gICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICRzY29wZS5mYXVsdENvdW50VmFsdWVzLmFkZChidHhuLnN1bW1hcnkubmFtZSk7XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9XG5cbiAgICAgICRzY29wZS5idHhuZmF1bHRjb3VudHBpZWNoYXJ0LmxvYWQoe1xuICAgICAgICBjb2x1bW5zOiBidHhuZmF1bHRkYXRhXG4gICAgICB9KTtcblxuICAgICAgZm9yICh2YXIgaj0wOyBqIDwgcmVtb3ZlRmF1bHRDb3VudFZhbHVlcy5sZW5ndGg7IGorKykge1xuICAgICAgICAkc2NvcGUuYnR4bmZhdWx0Y291bnRwaWVjaGFydC51bmxvYWQocmVtb3ZlRmF1bHRDb3VudFZhbHVlc1tqXSk7XG4gICAgICAgICRzY29wZS5mYXVsdENvdW50VmFsdWVzLnJlbW92ZShyZW1vdmVGYXVsdENvdW50VmFsdWVzW2pdKTtcbiAgICAgIH1cbiAgICB9O1xuICAgIFxuICAgICRzY29wZS5pbml0R3JhcGgoKTtcblxuICB9XSk7XG5cbn1cbiIsIi8vLyBDb3B5cmlnaHQgMjAxNC0yMDE1IFJlZCBIYXQsIEluYy4gYW5kL29yIGl0cyBhZmZpbGlhdGVzXG4vLy8gYW5kIG90aGVyIGNvbnRyaWJ1dG9ycyBhcyBpbmRpY2F0ZWQgYnkgdGhlIEBhdXRob3IgdGFncy5cbi8vL1xuLy8vIExpY2Vuc2VkIHVuZGVyIHRoZSBBcGFjaGUgTGljZW5zZSwgVmVyc2lvbiAyLjAgKHRoZSBcIkxpY2Vuc2VcIik7XG4vLy8geW91IG1heSBub3QgdXNlIHRoaXMgZmlsZSBleGNlcHQgaW4gY29tcGxpYW5jZSB3aXRoIHRoZSBMaWNlbnNlLlxuLy8vIFlvdSBtYXkgb2J0YWluIGEgY29weSBvZiB0aGUgTGljZW5zZSBhdFxuLy8vXG4vLy8gICBodHRwOi8vd3d3LmFwYWNoZS5vcmcvbGljZW5zZXMvTElDRU5TRS0yLjBcbi8vL1xuLy8vIFVubGVzcyByZXF1aXJlZCBieSBhcHBsaWNhYmxlIGxhdyBvciBhZ3JlZWQgdG8gaW4gd3JpdGluZywgc29mdHdhcmVcbi8vLyBkaXN0cmlidXRlZCB1bmRlciB0aGUgTGljZW5zZSBpcyBkaXN0cmlidXRlZCBvbiBhbiBcIkFTIElTXCIgQkFTSVMsXG4vLy8gV0lUSE9VVCBXQVJSQU5USUVTIE9SIENPTkRJVElPTlMgT0YgQU5ZIEtJTkQsIGVpdGhlciBleHByZXNzIG9yIGltcGxpZWQuXG4vLy8gU2VlIHRoZSBMaWNlbnNlIGZvciB0aGUgc3BlY2lmaWMgbGFuZ3VhZ2UgZ292ZXJuaW5nIHBlcm1pc3Npb25zIGFuZFxuLy8vIGxpbWl0YXRpb25zIHVuZGVyIHRoZSBMaWNlbnNlLlxuXG4vLy8gPHJlZmVyZW5jZSBwYXRoPVwiYnRtUGx1Z2luLnRzXCIvPlxubW9kdWxlIEJUTSB7XG5cbiAgZXhwb3J0IHZhciBCVE1DYW5kaWRhdGVzQ29udHJvbGxlciA9IF9tb2R1bGUuY29udHJvbGxlcihcIkJUTS5CVE1DYW5kaWRhdGVzQ29udHJvbGxlclwiLCBbXCIkc2NvcGVcIiwgXCIkaHR0cFwiLCAnJGxvY2F0aW9uJywgJyR1aWJNb2RhbCcsICckaW50ZXJ2YWwnLCAoJHNjb3BlLCAkaHR0cCwgJGxvY2F0aW9uLCAkdWliTW9kYWwsICRpbnRlcnZhbCkgPT4ge1xuXG4gICAgJHNjb3BlLm5ld0JUeG5OYW1lID0gJyc7XG4gICAgJHNjb3BlLmV4aXN0aW5nQlR4bk5hbWUgPSAnJztcbiAgICAkc2NvcGUuc2VsZWN0ZWR1cmlzID0gWyBdO1xuICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IDA7XG5cbiAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vY29uZmlnL2J1c2luZXNzdHhuc3VtbWFyeScpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25zID0gcmVzcC5kYXRhO1xuICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGJ1c2luZXNzIHR4biBzdW1tYXJpZXM6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICB9KTtcblxuICAgICRzY29wZS5yZWxvYWQgPSBmdW5jdGlvbigpIHtcbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYnVzaW5lc3N0eG4vdW5ib3VuZHVyaXM/Y29tcHJlc3M9dHJ1ZScpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAkc2NvcGUudW5ib3VuZHVyaXMgPSByZXNwLmRhdGE7XG4gICAgICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IE9iamVjdC5rZXlzKHJlc3AuZGF0YSkubGVuZ3RoO1xuICAgICAgICBcbiAgICAgICAgdmFyIHNlbGVjdGVkID0gJHNjb3BlLnNlbGVjdGVkdXJpcztcbiAgICAgICAgJHNjb3BlLnNlbGVjdGVkdXJpcyA9IFtdO1xuICAgICAgICBcbiAgICAgICAgZm9yICh2YXIgaT0wOyBpIDwgJHNjb3BlLnVuYm91bmR1cmlzLmxlbmd0aDsgaSsrKSB7XG4gICAgICAgICAgZm9yICh2YXIgaj0wOyBqIDwgc2VsZWN0ZWQubGVuZ3RoOyBqKyspIHtcbiAgICAgICAgICAgIGlmICgkc2NvcGUudW5ib3VuZHVyaXNbaV0udXJpID09PSBzZWxlY3RlZFtqXS51cmkpIHtcbiAgICAgICAgICAgICAgJHNjb3BlLnNlbGVjdGVkdXJpcy5hZGQoJHNjb3BlLnVuYm91bmR1cmlzW2ldKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgdW5ib3VuZCBVUklzOiBcIitKU09OLnN0cmluZ2lmeShyZXNwKSk7XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbG9hZCgpO1xuXG4gICAgJGludGVydmFsKGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLnJlbG9hZCgpO1xuICAgIH0sMTAwMDApO1xuXG4gICAgJHNjb3BlLmFkZEJ1c2luZXNzVHhuID0gZnVuY3Rpb24oKSB7XG4gICAgICB2YXIgZGVmbiA9IHtcbiAgICAgICAgZmlsdGVyOiB7XG4gICAgICAgICAgaW5jbHVzaW9uczogW11cbiAgICAgICAgfSxcbiAgICAgICAgcHJvY2Vzc29yczogW11cbiAgICAgIH07XG4gICAgICBmb3IgKHZhciBpID0gMDsgaSA8ICRzY29wZS5zZWxlY3RlZHVyaXMubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgZGVmbi5maWx0ZXIuaW5jbHVzaW9ucy5hZGQoJHNjb3BlLnNlbGVjdGVkdXJpc1tpXS5yZWdleCk7XG4gICAgICAgIGlmICgkc2NvcGUuc2VsZWN0ZWR1cmlzW2ldLnRlbXBsYXRlICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICBkZWZuLnByb2Nlc3NvcnMuYWRkKHtcbiAgICAgICAgICAgIGRlc2NyaXB0aW9uOiBcIlByb2Nlc3MgaW5ib3VuZCByZXF1ZXN0XCIsXG4gICAgICAgICAgICBub2RlVHlwZTogXCJDb25zdW1lclwiLFxuICAgICAgICAgICAgZGlyZWN0aW9uOiBcIkluXCIsXG4gICAgICAgICAgICB1cmlGaWx0ZXI6ICRzY29wZS5zZWxlY3RlZHVyaXNbaV0ucmVnZXgsXG4gICAgICAgICAgICBhY3Rpb25zOiBbe1xuICAgICAgICAgICAgICBhY3Rpb25UeXBlOiBcIkV2YWx1YXRlVVJJXCIsXG4gICAgICAgICAgICAgIGRlc2NyaXB0aW9uOiBcIkV4dHJhY3QgcGFyYW1ldGVycyBmcm9tIHBhdGhcIixcbiAgICAgICAgICAgICAgdGVtcGxhdGU6ICRzY29wZS5zZWxlY3RlZHVyaXNbaV0udGVtcGxhdGVcbiAgICAgICAgICAgIH1dXG4gICAgICAgICAgfSk7XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICAgICRodHRwLnB1dCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG4vJyskc2NvcGUubmV3QlR4bk5hbWUsIGRlZm4pLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAkbG9jYXRpb24ucGF0aCgnY29uZmlnLycrJHNjb3BlLm5ld0JUeG5OYW1lKTtcbiAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBhZGQgYnVzaW5lc3MgdHhuICdcIiskc2NvcGUubmV3QlR4bk5hbWUrXCInOiBcIitKU09OLnN0cmluZ2lmeShyZXNwKSk7XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLmlnbm9yZUJ1c2luZXNzVHhuID0gZnVuY3Rpb24oKSB7XG4gICAgICB2YXIgZGVmbiA9IHtcbiAgICAgICAgbGV2ZWw6ICdJZ25vcmUnLFxuICAgICAgICBmaWx0ZXI6IHtcbiAgICAgICAgICBpbmNsdXNpb25zOiBbXVxuICAgICAgICB9LFxuICAgICAgICBwcm9jZXNzb3JzOiBbXVxuICAgICAgfTtcbiAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgJHNjb3BlLnNlbGVjdGVkdXJpcy5sZW5ndGg7IGkrKykge1xuICAgICAgICBkZWZuLmZpbHRlci5pbmNsdXNpb25zLmFkZCgkc2NvcGUuc2VsZWN0ZWR1cmlzW2ldLnJlZ2V4KTtcbiAgICAgICAgLy8gRXZlbiB0aG91Z2ggaWdub3JlZCwgYWRkIFVSSSBldmFsdWF0aW9uIGluIGNhc2UgbGF0ZXIgb24gd2Ugd2FudCB0byBtYW5hZ2UgdGhlIGJ0eG5cbiAgICAgICAgaWYgKCRzY29wZS5zZWxlY3RlZHVyaXNbaV0udGVtcGxhdGUgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAgIGRlZm4ucHJvY2Vzc29ycy5hZGQoe1xuICAgICAgICAgICAgZGVzY3JpcHRpb246IFwiUHJvY2VzcyBpbmJvdW5kIHJlcXVlc3RcIixcbiAgICAgICAgICAgIG5vZGVUeXBlOiBcIkNvbnN1bWVyXCIsXG4gICAgICAgICAgICBkaXJlY3Rpb246IFwiSW5cIixcbiAgICAgICAgICAgIHVyaUZpbHRlcjogJHNjb3BlLnNlbGVjdGVkdXJpc1tpXS5yZWdleCxcbiAgICAgICAgICAgIGFjdGlvbnM6IFt7XG4gICAgICAgICAgICAgIGFjdGlvblR5cGU6IFwiRXZhbHVhdGVVUklcIixcbiAgICAgICAgICAgICAgZGVzY3JpcHRpb246IFwiRXh0cmFjdCBwYXJhbWV0ZXJzIGZyb20gcGF0aFwiLFxuICAgICAgICAgICAgICB0ZW1wbGF0ZTogJHNjb3BlLnNlbGVjdGVkdXJpc1tpXS50ZW1wbGF0ZVxuICAgICAgICAgICAgfV1cbiAgICAgICAgICB9KTtcbiAgICAgICAgfVxuICAgICAgfVxuICAgICAgJGh0dHAucHV0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5uZXdCVHhuTmFtZSwgZGVmbikudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRsb2NhdGlvbi5wYXRoKCdjb25maWcvJyskc2NvcGUubmV3QlR4bk5hbWUpO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGlnbm9yZSBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5uZXdCVHhuTmFtZStcIic6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgIH0pO1xuICAgIH07XG5cbiAgICAkc2NvcGUudXBkYXRlQnVzaW5lc3NUeG4gPSBmdW5jdGlvbigpIHtcbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG4vJyskc2NvcGUuZXhpc3RpbmdCVHhuTmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIHZhciBidHhuID0gcmVzcC5kYXRhO1xuICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8ICRzY29wZS5zZWxlY3RlZHVyaXMubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgICBpZiAoYnR4bi5maWx0ZXIuaW5jbHVzaW9ucy5pbmRleE9mKCRzY29wZS5zZWxlY3RlZHVyaXNbaV0ucmVnZXgpID09PSAtMSkge1xuICAgICAgICAgICAgYnR4bi5maWx0ZXIuaW5jbHVzaW9ucy5hZGQoJHNjb3BlLnNlbGVjdGVkdXJpc1tpXS5yZWdleCk7XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgICRodHRwLnB1dCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG4vJyskc2NvcGUuZXhpc3RpbmdCVHhuTmFtZSxidHhuKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgICBjb25zb2xlLmxvZyhcIlNhdmVkIHVwZGF0ZWQgYnVzaW5lc3MgdHhuICdcIiskc2NvcGUuZXhpc3RpbmdCVHhuTmFtZStcIic6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgICAgICAkbG9jYXRpb24ucGF0aCgnY29uZmlnLycrJHNjb3BlLmV4aXN0aW5nQlR4bk5hbWUpO1xuICAgICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBzYXZlIGJ1c2luZXNzIHR4biAnXCIrJHNjb3BlLmV4aXN0aW5nQlR4bk5hbWUrXCInOiBcIitKU09OLnN0cmluZ2lmeShyZXNwKSk7XG4gICAgICAgIH0pO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lK1wiJzogXCIrSlNPTi5zdHJpbmdpZnkocmVzcCkpO1xuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS5zZWxlY3Rpb25DaGFuZ2VkID0gZnVuY3Rpb24odXJpaW5mbykge1xuICAgICAgaWYgKCRzY29wZS5zZWxlY3RlZHVyaXMuY29udGFpbnModXJpaW5mbykpIHtcbiAgICAgICAgJHNjb3BlLnNlbGVjdGVkdXJpcy5yZW1vdmUodXJpaW5mbyk7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICAkc2NvcGUuc2VsZWN0ZWR1cmlzLmFkZCh1cmlpbmZvKTtcbiAgICAgIH1cbiAgICB9O1xuICAgIFxuICAgICRzY29wZS5pc1NlbGVjdGVkID0gZnVuY3Rpb24odXJpaW5mbykge1xuICAgICAgcmV0dXJuICRzY29wZS5zZWxlY3RlZHVyaXMuY29udGFpbnModXJpaW5mbyk7XG4gICAgfTtcbiAgICBcbiAgICAkc2NvcGUuZ2V0TGV2ZWwgPSBmdW5jdGlvbihsZXZlbCkge1xuICAgICAgaWYgKGxldmVsID09PSAnQWxsJykge1xuICAgICAgICByZXR1cm4gXCJBY3RpdmVcIjtcbiAgICAgIH1cbiAgICAgIHJldHVybiBsZXZlbDtcbiAgICB9O1xuXG4gIH1dKTtcblxufVxuXG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cImJ0bVBsdWdpbi50c1wiLz5cbm1vZHVsZSBCVE0ge1xuXG4gIGV4cG9ydCB2YXIgQlR4bkNvbmZpZ0NvbnRyb2xsZXIgPSBfbW9kdWxlLmNvbnRyb2xsZXIoXCJCVE0uQlR4bkNvbmZpZ0NvbnRyb2xsZXJcIiwgW1wiJHNjb3BlXCIsIFwiJHJvdXRlUGFyYW1zXCIsIFwiJGh0dHBcIiwgJyRsb2NhdGlvbicsICckaW50ZXJ2YWwnLCAoJHNjb3BlLCAkcm91dGVQYXJhbXMsICRodHRwLCAkbG9jYXRpb24sICRpbnRlcnZhbCkgPT4ge1xuXG4gICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lID0gJHJvdXRlUGFyYW1zLmJ1c2luZXNzdHJhbnNhY3Rpb247XG4gICAgJHNjb3BlLmRpcnR5ID0gZmFsc2U7XG5cbiAgICAkc2NvcGUubmV3SW5jbHVzaW9uRmlsdGVyID0gJyc7XG4gICAgJHNjb3BlLm5ld0V4Y2x1c2lvbkZpbHRlciA9ICcnO1xuXG4gICAgJHNjb3BlLm1lc3NhZ2VzID0gW107XG5cbiAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vY29uZmlnL2J1c2luZXNzdHhuLycrJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uID0gcmVzcC5kYXRhO1xuICAgICAgJHNjb3BlLm9yaWdpbmFsID0gYW5ndWxhci5jb3B5KCRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uKTtcblxuICAgICAgJGh0dHAucG9zdCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG4vdmFsaWRhdGUnLCRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgJHNjb3BlLm1lc3NhZ2VzID0gcmVzcC5kYXRhO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIHZhbGlkYXRlIGJ1c2luZXNzIHR4biAnXCIrJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lK1wiJzogXCIrSlNPTi5zdHJpbmdpZnkocmVzcCkpO1xuICAgICAgfSk7XG4gICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgYnVzaW5lc3MgdHhuICdcIiskc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbk5hbWUrXCInOiBcIitKU09OLnN0cmluZ2lmeShyZXNwKSk7XG4gICAgfSk7XG5cbiAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL3VuYm91bmR1cmlzP2NvbXByZXNzPXRydWUnKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICRzY29wZS51bmJvdW5kVVJJcyA9IFsgXTtcbiAgICAgIGZvciAodmFyIGk9MDsgaSA8IHJlc3AuZGF0YS5sZW5ndGg7IGkrKykge1xuICAgICAgICBpZiAocmVzcC5kYXRhW2ldLnJlZ2V4ICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAkc2NvcGUudW5ib3VuZFVSSXMuYWRkKHJlc3AuZGF0YVtpXS5yZWdleCk7XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCB1bmJvdW5kIFVSSXM6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICB9KTtcblxuICAgICRzY29wZS5yZWxvYWQgPSBmdW5jdGlvbigpIHtcbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYnVzaW5lc3N0eG4vYm91bmR1cmlzLycrJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgJHNjb3BlLmJvdW5kVVJJcyA9IFsgXTtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCByZXNwLmRhdGEubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgICB2YXIgcmVnZXggPSAkc2NvcGUuZXNjYXBlUmVnRXhwKHJlc3AuZGF0YVtpXSk7XG4gICAgICAgICAgJHNjb3BlLmJvdW5kVVJJcy5hZGQocmVnZXgpO1xuICAgICAgICB9XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGJvdW5kIFVSSXMgZm9yIGJ1c2luZXNzIHR4biAnXCIrJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lK1wiJzogXCIrSlNPTi5zdHJpbmdpZnkocmVzcCkpO1xuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS5yZWxvYWQoKTtcblxuICAgICRpbnRlcnZhbChmdW5jdGlvbigpIHtcbiAgICAgICRzY29wZS5yZWxvYWQoKTtcbiAgICB9LDEwMDAwKTtcblxuICAgICRzY29wZS5hZGRJbmNsdXNpb25GaWx0ZXIgPSBmdW5jdGlvbigpIHtcbiAgICAgIGNvbnNvbGUubG9nKCdBZGQgaW5jbHVzaW9uIGZpbHRlcjogJyskc2NvcGUubmV3SW5jbHVzaW9uRmlsdGVyKTtcbiAgICAgIGlmICgkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbi5maWx0ZXIgPT09IG51bGwpIHtcbiAgICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24uZmlsdGVyID0ge1xuICAgICAgICAgIGluY2x1c2lvbnM6IFtdLFxuICAgICAgICAgIGV4Y2x1c2lvbnM6IFtdXG4gICAgICAgIH07XG4gICAgICB9XG4gICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbi5maWx0ZXIuaW5jbHVzaW9ucy5hZGQoJHNjb3BlLm5ld0luY2x1c2lvbkZpbHRlcik7XG4gICAgICAkc2NvcGUuc2V0RGlydHkoKTtcbiAgICAgICRzY29wZS5uZXdJbmNsdXNpb25GaWx0ZXIgPSAnJztcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbW92ZUluY2x1c2lvbkZpbHRlciA9IGZ1bmN0aW9uKGluY2x1c2lvbikge1xuICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24uZmlsdGVyLmluY2x1c2lvbnMucmVtb3ZlKGluY2x1c2lvbik7XG4gICAgICAkc2NvcGUuc2V0RGlydHkoKTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLmFkZEV4Y2x1c2lvbkZpbHRlciA9IGZ1bmN0aW9uKCkge1xuICAgICAgY29uc29sZS5sb2coJ0FkZCBleGNsdXNpb24gZmlsdGVyOiAnKyRzY29wZS5uZXdFeGNsdXNpb25GaWx0ZXIpO1xuICAgICAgaWYgKCRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uLmZpbHRlciA9PT0gbnVsbCkge1xuICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbi5maWx0ZXIgPSB7XG4gICAgICAgICAgaW5jbHVzaW9uczogW10sXG4gICAgICAgICAgZXhjbHVzaW9uczogW11cbiAgICAgICAgfTtcbiAgICAgIH1cbiAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uLmZpbHRlci5leGNsdXNpb25zLmFkZCgkc2NvcGUubmV3RXhjbHVzaW9uRmlsdGVyKTtcbiAgICAgICRzY29wZS5zZXREaXJ0eSgpO1xuICAgICAgJHNjb3BlLm5ld0V4Y2x1c2lvbkZpbHRlciA9ICcnO1xuICAgIH07XG5cbiAgICAkc2NvcGUucmVtb3ZlRXhjbHVzaW9uRmlsdGVyID0gZnVuY3Rpb24oZXhjbHVzaW9uKSB7XG4gICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbi5maWx0ZXIuZXhjbHVzaW9ucy5yZW1vdmUoZXhjbHVzaW9uKTtcbiAgICAgICRzY29wZS5zZXREaXJ0eSgpO1xuICAgIH07XG5cbiAgICAkc2NvcGUuZ2V0RXhwcmVzc2lvblRleHQgPSBmdW5jdGlvbihleHByZXNzaW9uKSB7XG4gICAgICBpZiAoZXhwcmVzc2lvbiA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgIHJldHVybiBcIlwiO1xuICAgICAgfVxuICAgICAgaWYgKGV4cHJlc3Npb24udHlwZSA9PT0gXCJYTUxcIikge1xuICAgICAgICByZXR1cm4gZXhwcmVzc2lvbi5zb3VyY2UgKyBcIltcIiArIGV4cHJlc3Npb24ua2V5ICsgXCJdXCIgKyBcIiB4cGF0aD1cIiArIGV4cHJlc3Npb24ueHBhdGg7XG4gICAgICB9XG4gICAgICBpZiAoZXhwcmVzc2lvbi50eXBlID09PSBcIkpTT05cIikge1xuICAgICAgICByZXR1cm4gZXhwcmVzc2lvbi5zb3VyY2UgKyBcIltcIiArIGV4cHJlc3Npb24ua2V5ICsgXCJdXCIgKyBcIiBqc29ucGF0aD1cIiArIGV4cHJlc3Npb24uanNvbnBhdGg7XG4gICAgICB9XG4gICAgICBpZiAoZXhwcmVzc2lvbi50eXBlID09PSBcIlRleHRcIikge1xuICAgICAgICByZXR1cm4gZXhwcmVzc2lvbi5zb3VyY2UgKyBcIltcIiArIGV4cHJlc3Npb24ua2V5ICsgXCJdXCI7XG4gICAgICB9XG4gICAgICByZXR1cm4gXCJVbmtub3duIGV4cHJlc3Npb24gdHlwZVwiO1xuICAgIH07XG5cbiAgICAkc2NvcGUuY2hhbmdlZEV4cHJlc3Npb25UeXBlID0gZnVuY3Rpb24oZXhwcmVzc2lvbikge1xuICAgICAgJHNjb3BlLnNldERpcnR5KCk7XG4gICAgICBleHByZXNzaW9uLmtleSA9IHVuZGVmaW5lZDtcbiAgICAgIGV4cHJlc3Npb24uc291cmNlID0gdW5kZWZpbmVkO1xuICAgICAgZXhwcmVzc2lvbi54cGF0aCA9IHVuZGVmaW5lZDtcbiAgICAgIGV4cHJlc3Npb24uanNvbnBhdGggPSB1bmRlZmluZWQ7XG5cbiAgICAgIGlmIChleHByZXNzaW9uLnR5cGUgPT09ICdYTUwnIHx8IGV4cHJlc3Npb24udHlwZSA9PT0gJ0pTT04nIHx8IGV4cHJlc3Npb24udHlwZSA9PT0gJ1RleHQnKSB7XG4gICAgICAgIGV4cHJlc3Npb24ua2V5ID0gJzAnO1xuICAgICAgICBleHByZXNzaW9uLnNvdXJjZSA9ICdDb250ZW50JztcbiAgICAgIH1cbiAgICB9O1xuXG4gICAgJHNjb3BlLmNoYW5nZWRBY3Rpb25UeXBlID0gZnVuY3Rpb24oYWN0aW9uKSB7XG4gICAgICAkc2NvcGUuc2V0RGlydHkoKTtcbiAgICAgIGFjdGlvbi5uYW1lID0gdW5kZWZpbmVkO1xuICAgICAgYWN0aW9uLnR5cGUgPSB1bmRlZmluZWQ7XG4gICAgICBhY3Rpb24uc2NvcGUgPSB1bmRlZmluZWQ7XG4gICAgICBhY3Rpb24udGVtcGxhdGUgPSB1bmRlZmluZWQ7XG4gICAgICBhY3Rpb24ucHJlZGljYXRlID0gdW5kZWZpbmVkO1xuICAgICAgYWN0aW9uLmV4cHJlc3Npb24gPSB1bmRlZmluZWQ7XG4gICAgfTtcblxuICAgICRzY29wZS5hZGRQcm9jZXNzb3IgPSBmdW5jdGlvbigpIHtcbiAgICAgICRzY29wZS5zZXREaXJ0eSgpO1xuICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24ucHJvY2Vzc29ycy5hZGQoe1xuICAgICAgICBkZXNjcmlwdGlvbjogXCJQcm9jZXNzb3IgXCIgKyAoJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24ucHJvY2Vzc29ycy5sZW5ndGggKyAxKSxcbiAgICAgICAgbm9kZVR5cGU6IFwiQ29uc3VtZXJcIixcbiAgICAgICAgZGlyZWN0aW9uOiBcIkluXCIsXG4gICAgICAgIGFjdGlvbnM6IFtdXG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLmRlbGV0ZVByb2Nlc3NvciA9IGZ1bmN0aW9uKHByb2Nlc3Nvcikge1xuICAgICAgaWYgKGNvbmZpcm0oJ0FyZSB5b3Ugc3VyZSB5b3Ugd2FudCB0byBkZWxldGUgdGhlIHByb2Nlc3Nvcj8nKSkge1xuICAgICAgICAkc2NvcGUuc2V0RGlydHkoKTtcbiAgICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24ucHJvY2Vzc29ycy5yZW1vdmUocHJvY2Vzc29yKTtcbiAgICAgIH1cbiAgICB9O1xuXG4gICAgJHNjb3BlLmFkZEFjdGlvbiA9IGZ1bmN0aW9uKHByb2Nlc3NvciwgdHlwZSkge1xuICAgICAgJHNjb3BlLnNldERpcnR5KCk7XG4gICAgICBcbiAgICAgIHZhciBuZXdBY3Rpb24gPSB7XG4gICAgICAgIGFjdGlvblR5cGU6IHR5cGUsXG4gICAgICAgIGRlc2NyaXB0aW9uOiBcIkFjdGlvbiBcIiArIChwcm9jZXNzb3IuYWN0aW9ucy5sZW5ndGggKyAxKVxuICAgICAgfTtcblxuICAgICAgaWYgKHR5cGUgPT09ICdBZGRDb3JyZWxhdGlvbklkJykge1xuICAgICAgICBuZXdBY3Rpb25bJ3Njb3BlJ10gPSAnR2xvYmFsJztcbiAgICAgIH1cblxuICAgICAgcHJvY2Vzc29yLmFjdGlvbnMuYWRkKG5ld0FjdGlvbik7XG4gICAgfTtcblxuICAgICRzY29wZS5kZWxldGVBY3Rpb24gPSBmdW5jdGlvbihwcm9jZXNzb3IsYWN0aW9uKSB7XG4gICAgICBpZiAoY29uZmlybSgnQXJlIHlvdSBzdXJlIHlvdSB3YW50IHRvIGRlbGV0ZSB0aGUgYWN0aW9uPycpKSB7XG4gICAgICAgICRzY29wZS5zZXREaXJ0eSgpO1xuICAgICAgICBwcm9jZXNzb3IuYWN0aW9ucy5yZW1vdmUoYWN0aW9uKTtcbiAgICAgIH1cbiAgICB9O1xuXG4gICAgJHNjb3BlLnNldERpcnR5ID0gZnVuY3Rpb24oKSB7XG4gICAgICAkc2NvcGUuZGlydHkgPSB0cnVlO1xuICAgIH07XG5cbiAgICAkc2NvcGUucmVzZXQgPSBmdW5jdGlvbigpIHtcbiAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uID0gYW5ndWxhci5jb3B5KCRzY29wZS5vcmlnaW5hbCk7XG4gICAgICAkc2NvcGUuZGlydHkgPSBmYWxzZTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnNhdmUgPSBmdW5jdGlvbigpIHtcbiAgICAgICRodHRwLnB1dCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG4vJyskc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbk5hbWUsJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24pLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAkc2NvcGUubWVzc2FnZXMgPSByZXNwLmRhdGE7XG4gICAgICAgICRzY29wZS5vcmlnaW5hbCA9IGFuZ3VsYXIuY29weSgkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbik7XG4gICAgICAgICRzY29wZS5kaXJ0eSA9IGZhbHNlO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIHNhdmUgYnVzaW5lc3MgdHhuICdcIiskc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbk5hbWUrXCInOiBcIitKU09OLnN0cmluZ2lmeShyZXNwKSk7XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJGh0dHAuZ2V0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uTmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbiA9IHJlc3AuZGF0YTtcbiAgICAgICRzY29wZS5vcmlnaW5hbCA9IGFuZ3VsYXIuY29weSgkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbik7XG4gICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgYnVzaW5lc3MgdHhuICdcIiskc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbk5hbWUrXCInOiBcIitKU09OLnN0cmluZ2lmeShyZXNwKSk7XG4gICAgfSk7XG5cbiAgICAkc2NvcGUuZXNjYXBlUmVnRXhwID0gZnVuY3Rpb24oc3RyKSB7XG4gICAgICBpZiAoc3RyID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuICAgICAgcmV0dXJuIFwiXlwiICsgc3RyLnJlcGxhY2UoL1tcXC1cXFtcXF1cXC9cXHtcXH1cXChcXClcXCpcXCtcXD9cXC5cXFxcXFxeXFwkXFx8XS9nLCBcIlxcXFwkJlwiKSArIFwiJFwiO1xuICAgIH07XG5cbiAgICAkc2NvcGUuY2xvc2VNZXNzYWdlID0gZnVuY3Rpb24oaW5kZXgpIHtcbiAgICAgICRzY29wZS5tZXNzYWdlcy5zcGxpY2UoaW5kZXgsIDEpO1xuICAgIH07XG5cbiAgICAkc2NvcGUuZ2V0TWVzc2FnZVR5cGUgPSBmdW5jdGlvbihlbnRyeSkge1xuICAgICAgdmFyIHR5cGUgPSAnZGFuZ2VyJztcbiAgICAgIGlmIChlbnRyeS5zZXZlcml0eSA9PT0gJ1dhcm5pbmcnKSB7XG4gICAgICAgIHR5cGUgPSAnd2FybmluZyc7XG4gICAgICB9IGVsc2UgaWYgKGVudHJ5LnNldmVyaXR5ID09PSAnSW5mbycpIHtcbiAgICAgICAgdHlwZSA9ICdzdWNjZXNzJztcbiAgICAgIH1cbiAgICAgIHJldHVybiB0eXBlO1xuICAgIH07XG4gICAgXG4gICAgJHNjb3BlLmdldE1lc3NhZ2VUZXh0ID0gZnVuY3Rpb24oZW50cnkpIHtcbiAgICAgIHZhciBtZXNzYWdlID0gXCJcIjtcbiAgICAgIGlmIChlbnRyeS5wcm9jZXNzb3IgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICBtZXNzYWdlID0gXCJbXCIgKyBlbnRyeS5wcm9jZXNzb3I7XG4gICAgICAgIFxuICAgICAgICBpZiAoZW50cnkuYWN0aW9uICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICBtZXNzYWdlID0gbWVzc2FnZSArIFwiL1wiICsgZW50cnkuYWN0aW9uO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICBtZXNzYWdlID0gbWVzc2FnZSArIFwiXSBcIjtcbiAgICAgIH1cbiAgICAgIFxuICAgICAgbWVzc2FnZSA9IG1lc3NhZ2UgKyBlbnRyeS5tZXNzYWdlO1xuXG4gICAgICByZXR1cm4gbWVzc2FnZTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLmlzRXJyb3IgPSBmdW5jdGlvbihwcm9jZXNzb3IsYWN0aW9uLGZpZWxkKSB7XG4gICAgICBmb3IgKHZhciBpID0gMDsgaSA8ICRzY29wZS5tZXNzYWdlcy5sZW5ndGg7IGkrKykge1xuICAgICAgICBpZiAoJHNjb3BlLm1lc3NhZ2VzW2ldLnByb2Nlc3NvciA9PT0gcHJvY2Vzc29yLmRlc2NyaXB0aW9uXG4gICAgICAgICAgICAmJiAkc2NvcGUubWVzc2FnZXNbaV0uYWN0aW9uID09PSBhY3Rpb24uZGVzY3JpcHRpb25cbiAgICAgICAgICAgICYmICRzY29wZS5tZXNzYWdlc1tpXS5maWVsZCA9PT0gZmllbGQpIHtcbiAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgfVxuICAgICAgfVxuICAgICAgcmV0dXJuIGZhbHNlO1xuICAgIH07XG5cbiAgfV0pO1xuXG59XG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cImJ0bVBsdWdpbi50c1wiLz5cbm1vZHVsZSBCVE0ge1xuXG4gIGV4cG9ydCB2YXIgQlRNRGlzYWJsZWRDb250cm9sbGVyID0gX21vZHVsZS5jb250cm9sbGVyKFwiQlRNLkJUTURpc2FibGVkQ29udHJvbGxlclwiLCBbXCIkc2NvcGVcIiwgXCIkaHR0cFwiLCAnJGxvY2F0aW9uJywgJyRpbnRlcnZhbCcsICgkc2NvcGUsICRodHRwLCAkbG9jYXRpb24sICRpbnRlcnZhbCkgPT4ge1xuXG4gICAgJHNjb3BlLm5ld0JUeG5OYW1lID0gJyc7XG4gICAgJHNjb3BlLmNhbmRpZGF0ZUNvdW50ID0gMDtcblxuICAgICRzY29wZS5yZWxvYWQgPSBmdW5jdGlvbigpIHtcbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG5zdW1tYXJ5JykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucyA9IFtdO1xuICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IHJlc3AuZGF0YS5sZW5ndGg7IGkrKykge1xuICAgICAgICAgIHZhciBidHhuID0ge1xuICAgICAgICAgICAgc3VtbWFyeTogcmVzcC5kYXRhW2ldXG4gICAgICAgICAgfTtcbiAgICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMuYWRkKGJ0eG4pO1xuICAgICAgICB9XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGJ1c2luZXNzIHR4biBzdW1tYXJpZXM6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgIH0pO1xuXG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL3VuYm91bmR1cmlzJykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IE9iamVjdC5rZXlzKHJlc3AuZGF0YSkubGVuZ3RoO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBjYW5kaWRhdGUgY291bnQ6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgIH0pO1xuICAgIH07XG5cbiAgICAkc2NvcGUucmVsb2FkKCk7XG5cbiAgICAkc2NvcGUuZGVsZXRlQnVzaW5lc3NUeG4gPSBmdW5jdGlvbihidHhuKSB7XG4gICAgICBpZiAoY29uZmlybSgnQXJlIHlvdSBzdXJlIHlvdSB3YW50IHRvIGRlbGV0ZSBidXNpbmVzcyB0cmFuc2FjdGlvbiBcXFwiJytidHhuLnN1bW1hcnkubmFtZSsnXFxcIj8nKSkge1xuICAgICAgICAkaHR0cC5kZWxldGUoJy9oYXdrdWxhci9idG0vY29uZmlnL2J1c2luZXNzdHhuLycrYnR4bi5zdW1tYXJ5Lm5hbWUpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAgIGNvbnNvbGUubG9nKCdEZWxldGVkOiAnK2J0eG4uc3VtbWFyeS5uYW1lKTtcbiAgICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMucmVtb3ZlKGJ0eG4pO1xuICAgICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBkZWxldGUgYnVzaW5lc3MgdHhuICdcIitidHhuLnN1bW1hcnkubmFtZStcIic6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgICAgfSk7XG4gICAgICB9XG4gICAgfTtcblxuICB9XSk7XG5cbn1cbiIsIi8vLyBDb3B5cmlnaHQgMjAxNC0yMDE1IFJlZCBIYXQsIEluYy4gYW5kL29yIGl0cyBhZmZpbGlhdGVzXG4vLy8gYW5kIG90aGVyIGNvbnRyaWJ1dG9ycyBhcyBpbmRpY2F0ZWQgYnkgdGhlIEBhdXRob3IgdGFncy5cbi8vL1xuLy8vIExpY2Vuc2VkIHVuZGVyIHRoZSBBcGFjaGUgTGljZW5zZSwgVmVyc2lvbiAyLjAgKHRoZSBcIkxpY2Vuc2VcIik7XG4vLy8geW91IG1heSBub3QgdXNlIHRoaXMgZmlsZSBleGNlcHQgaW4gY29tcGxpYW5jZSB3aXRoIHRoZSBMaWNlbnNlLlxuLy8vIFlvdSBtYXkgb2J0YWluIGEgY29weSBvZiB0aGUgTGljZW5zZSBhdFxuLy8vXG4vLy8gICBodHRwOi8vd3d3LmFwYWNoZS5vcmcvbGljZW5zZXMvTElDRU5TRS0yLjBcbi8vL1xuLy8vIFVubGVzcyByZXF1aXJlZCBieSBhcHBsaWNhYmxlIGxhdyBvciBhZ3JlZWQgdG8gaW4gd3JpdGluZywgc29mdHdhcmVcbi8vLyBkaXN0cmlidXRlZCB1bmRlciB0aGUgTGljZW5zZSBpcyBkaXN0cmlidXRlZCBvbiBhbiBcIkFTIElTXCIgQkFTSVMsXG4vLy8gV0lUSE9VVCBXQVJSQU5USUVTIE9SIENPTkRJVElPTlMgT0YgQU5ZIEtJTkQsIGVpdGhlciBleHByZXNzIG9yIGltcGxpZWQuXG4vLy8gU2VlIHRoZSBMaWNlbnNlIGZvciB0aGUgc3BlY2lmaWMgbGFuZ3VhZ2UgZ292ZXJuaW5nIHBlcm1pc3Npb25zIGFuZFxuLy8vIGxpbWl0YXRpb25zIHVuZGVyIHRoZSBMaWNlbnNlLlxuXG4vLy8gPHJlZmVyZW5jZSBwYXRoPVwiYnRtUGx1Z2luLnRzXCIvPlxubW9kdWxlIEJUTSB7XG5cbiAgZXhwb3J0IHZhciBCVE1JZ25vcmVkQ29udHJvbGxlciA9IF9tb2R1bGUuY29udHJvbGxlcihcIkJUTS5CVE1JZ25vcmVkQ29udHJvbGxlclwiLCBbXCIkc2NvcGVcIiwgXCIkaHR0cFwiLCAnJGxvY2F0aW9uJywgJyRpbnRlcnZhbCcsICgkc2NvcGUsICRodHRwLCAkbG9jYXRpb24sICRpbnRlcnZhbCkgPT4ge1xuXG4gICAgJHNjb3BlLm5ld0JUeG5OYW1lID0gJyc7XG4gICAgJHNjb3BlLmNhbmRpZGF0ZUNvdW50ID0gMDtcblxuICAgICRzY29wZS5yZWxvYWQgPSBmdW5jdGlvbigpIHtcbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG5zdW1tYXJ5JykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucyA9IFtdO1xuICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IHJlc3AuZGF0YS5sZW5ndGg7IGkrKykge1xuICAgICAgICAgIHZhciBidHhuID0ge1xuICAgICAgICAgICAgc3VtbWFyeTogcmVzcC5kYXRhW2ldXG4gICAgICAgICAgfTtcbiAgICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMuYWRkKGJ0eG4pO1xuICAgICAgICB9XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGJ1c2luZXNzIHR4biBzdW1tYXJpZXM6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgIH0pO1xuXG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL3VuYm91bmR1cmlzJykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IE9iamVjdC5rZXlzKHJlc3AuZGF0YSkubGVuZ3RoO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBjYW5kaWRhdGUgY291bnQ6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgIH0pO1xuICAgIH07XG5cbiAgICAkc2NvcGUucmVsb2FkKCk7XG5cbiAgICAkc2NvcGUuZGVsZXRlQnVzaW5lc3NUeG4gPSBmdW5jdGlvbihidHhuKSB7XG4gICAgICBpZiAoY29uZmlybSgnQXJlIHlvdSBzdXJlIHlvdSB3YW50IHRvIGRlbGV0ZSBidXNpbmVzcyB0cmFuc2FjdGlvbiBcXFwiJytidHhuLnN1bW1hcnkubmFtZSsnXFxcIj8nKSkge1xuICAgICAgICAkaHR0cC5kZWxldGUoJy9oYXdrdWxhci9idG0vY29uZmlnL2J1c2luZXNzdHhuLycrYnR4bi5zdW1tYXJ5Lm5hbWUpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAgIGNvbnNvbGUubG9nKCdEZWxldGVkOiAnK2J0eG4uc3VtbWFyeS5uYW1lKTtcbiAgICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMucmVtb3ZlKGJ0eG4pO1xuICAgICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBkZWxldGUgYnVzaW5lc3MgdHhuICdcIitidHhuLnN1bW1hcnkubmFtZStcIic6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgICAgfSk7XG4gICAgICB9XG4gICAgfTtcblxuICB9XSk7XG5cbn1cbiIsIi8vLyBDb3B5cmlnaHQgMjAxNC0yMDE1IFJlZCBIYXQsIEluYy4gYW5kL29yIGl0cyBhZmZpbGlhdGVzXG4vLy8gYW5kIG90aGVyIGNvbnRyaWJ1dG9ycyBhcyBpbmRpY2F0ZWQgYnkgdGhlIEBhdXRob3IgdGFncy5cbi8vL1xuLy8vIExpY2Vuc2VkIHVuZGVyIHRoZSBBcGFjaGUgTGljZW5zZSwgVmVyc2lvbiAyLjAgKHRoZSBcIkxpY2Vuc2VcIik7XG4vLy8geW91IG1heSBub3QgdXNlIHRoaXMgZmlsZSBleGNlcHQgaW4gY29tcGxpYW5jZSB3aXRoIHRoZSBMaWNlbnNlLlxuLy8vIFlvdSBtYXkgb2J0YWluIGEgY29weSBvZiB0aGUgTGljZW5zZSBhdFxuLy8vXG4vLy8gICBodHRwOi8vd3d3LmFwYWNoZS5vcmcvbGljZW5zZXMvTElDRU5TRS0yLjBcbi8vL1xuLy8vIFVubGVzcyByZXF1aXJlZCBieSBhcHBsaWNhYmxlIGxhdyBvciBhZ3JlZWQgdG8gaW4gd3JpdGluZywgc29mdHdhcmVcbi8vLyBkaXN0cmlidXRlZCB1bmRlciB0aGUgTGljZW5zZSBpcyBkaXN0cmlidXRlZCBvbiBhbiBcIkFTIElTXCIgQkFTSVMsXG4vLy8gV0lUSE9VVCBXQVJSQU5USUVTIE9SIENPTkRJVElPTlMgT0YgQU5ZIEtJTkQsIGVpdGhlciBleHByZXNzIG9yIGltcGxpZWQuXG4vLy8gU2VlIHRoZSBMaWNlbnNlIGZvciB0aGUgc3BlY2lmaWMgbGFuZ3VhZ2UgZ292ZXJuaW5nIHBlcm1pc3Npb25zIGFuZFxuLy8vIGxpbWl0YXRpb25zIHVuZGVyIHRoZSBMaWNlbnNlLlxuXG4vLy8gPHJlZmVyZW5jZSBwYXRoPVwiYnRtUGx1Z2luLnRzXCIvPlxubW9kdWxlIEJUTSB7XG5cbiAgZGVjbGFyZSB2YXIgYzM6IGFueTtcblxuICBleHBvcnQgdmFyIEJUeG5JbmZvQ29udHJvbGxlciA9IF9tb2R1bGUuY29udHJvbGxlcihcIkJUTS5CVHhuSW5mb0NvbnRyb2xsZXJcIiwgW1wiJHNjb3BlXCIsIFwiJHJvdXRlUGFyYW1zXCIsIFwiJGh0dHBcIiwgJyRsb2NhdGlvbicsICckaW50ZXJ2YWwnLCAoJHNjb3BlLCAkcm91dGVQYXJhbXMsICRodHRwLCAkbG9jYXRpb24sICRpbnRlcnZhbCkgPT4ge1xuXG4gICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lID0gJHJvdXRlUGFyYW1zLmJ1c2luZXNzdHJhbnNhY3Rpb247XG5cbiAgICAkc2NvcGUucHJvcGVydGllcyA9IFtdO1xuICAgIFxuICAgICRzY29wZS5wcm9wZXJ0eVZhbHVlcyA9IFtdO1xuICAgICRzY29wZS5mYXVsdFZhbHVlcyA9IFtdO1xuXG4gICAgJHNjb3BlLmNyaXRlcmlhID0ge1xuICAgICAgYnVzaW5lc3NUcmFuc2FjdGlvbjogJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lLFxuICAgICAgcHJvcGVydGllczogW10sXG4gICAgICBmYXVsdHM6IFtdLFxuICAgICAgc3RhcnRUaW1lOiAtMzYwMDAwMCxcbiAgICAgIGVuZFRpbWU6IFwiMFwiLFxuICAgICAgbG93ZXJCb3VuZDogMFxuICAgIH07XG5cbiAgICAkc2NvcGUuY29uZmlnID0ge1xuICAgICAgaW50ZXJ2YWw6IDYwMDAwLFxuICAgICAgc2VsZWN0ZWRQcm9wZXJ0eTogdW5kZWZpbmVkLFxuICAgICAgbG93ZXJCb3VuZERpc3BsYXk6IDAsXG4gICAgICBwcmV2TG93ZXJCb3VuZERpc3BsYXk6IDBcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbG9hZCA9IGZ1bmN0aW9uKCkge1xuICAgICAgJGh0dHAucG9zdCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYnVzaW5lc3N0eG4vY29tcGxldGlvbi9zdGF0aXN0aWNzP2ludGVydmFsPScrJHNjb3BlLmNvbmZpZy5pbnRlcnZhbCwgJHNjb3BlLmNyaXRlcmlhKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgJHNjb3BlLnN0YXRpc3RpY3MgPSByZXNwLmRhdGE7XG4gICAgICAgICRzY29wZS51cGRhdGVkQm91bmRzKCk7XG4gICAgICAgICRzY29wZS5yZWRyYXdMaW5lQ2hhcnQoKTtcbiAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgc3RhdGlzdGljczogXCIrSlNPTi5zdHJpbmdpZnkocmVzcCkpO1xuICAgICAgfSk7XG5cbiAgICAgICRodHRwLnBvc3QoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL2NvbXBsZXRpb24vZmF1bHRzJywgJHNjb3BlLmNyaXRlcmlhKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgJHNjb3BlLmZhdWx0cyA9IHJlc3AuZGF0YTtcbiAgICAgICAgXG4gICAgICAgIHZhciByZW1vdmVGYXVsdFZhbHVlcyA9IGFuZ3VsYXIuY29weSgkc2NvcGUuZmF1bHRWYWx1ZXMpO1xuXG4gICAgICAgIHZhciBmYXVsdGRhdGEgPSBbXTtcbiAgICAgICAgXG4gICAgICAgIGZvciAodmFyIGk9MDsgaSA8ICRzY29wZS5mYXVsdHMubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgICB2YXIgZmF1bHQgPSAkc2NvcGUuZmF1bHRzW2ldO1xuICAgICAgICAgIHZhciByZWNvcmQ9WyBdO1xuICAgICAgICAgIHJlY29yZC5wdXNoKGZhdWx0LnZhbHVlKTtcbiAgICAgICAgICByZWNvcmQucHVzaChmYXVsdC5jb3VudCk7XG4gICAgICAgICAgZmF1bHRkYXRhLnB1c2gocmVjb3JkKTtcblxuICAgICAgICAgIGlmICgkc2NvcGUuZmF1bHRWYWx1ZXMuY29udGFpbnMoZmF1bHQudmFsdWUpKSB7XG4gICAgICAgICAgICByZW1vdmVGYXVsdFZhbHVlcy5yZW1vdmUoZmF1bHQudmFsdWUpO1xuICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAkc2NvcGUuZmF1bHRWYWx1ZXMuYWRkKGZhdWx0LnZhbHVlKTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgICRzY29wZS5jdGZhdWx0c2NoYXJ0LmxvYWQoe1xuICAgICAgICAgIGNvbHVtbnM6IGZhdWx0ZGF0YVxuICAgICAgICB9KTtcblxuICAgICAgICBmb3IgKHZhciBqPTA7IGogPCByZW1vdmVGYXVsdFZhbHVlcy5sZW5ndGg7IGorKykge1xuICAgICAgICAgICRzY29wZS5jdGZhdWx0c2NoYXJ0LnVubG9hZChyZW1vdmVGYXVsdFZhbHVlc1tqXSk7XG4gICAgICAgICAgJHNjb3BlLmZhdWx0VmFsdWVzLnJlbW92ZShyZW1vdmVGYXVsdFZhbHVlc1tqXSk7XG4gICAgICAgIH1cblxuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBzdGF0aXN0aWNzOiBcIitKU09OLnN0cmluZ2lmeShyZXNwKSk7XG4gICAgICB9KTtcblxuICAgICAgJGh0dHAuZ2V0KCcvaGF3a3VsYXIvYnRtL2FuYWx5dGljcy9idXNpbmVzc3R4bi9wcm9wZXJ0aWVzLycrJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgJHNjb3BlLnByb3BlcnRpZXMgPSByZXNwLmRhdGE7XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IHByb3BlcnR5IGluZm86IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgIH0pO1xuICAgIFxuICAgICAgaWYgKCRzY29wZS5jb25maWcuc2VsZWN0ZWRQcm9wZXJ0eSAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICRzY29wZS5yZWxvYWRQcm9wZXJ0eSgpO1xuICAgICAgfVxuICAgIH07XG5cbiAgICAkc2NvcGUucmVkcmF3TGluZUNoYXJ0ID0gZnVuY3Rpb24oKSB7XG4gICAgICAkc2NvcGUuY3RsaW5lY2hhcnQubG9hZCh7XG4gICAgICAgIGpzb246ICRzY29wZS5zdGF0aXN0aWNzLFxuICAgICAgICBrZXlzOiB7XG4gICAgICAgICAgdmFsdWU6IFsnbWF4JywnYXZlcmFnZScsJ21pbicsJ2NvdW50JywnZmF1bHRDb3VudCddLFxuICAgICAgICAgIHg6ICd0aW1lc3RhbXAnXG4gICAgICAgIH1cbiAgICAgIH0pO1xuICAgIH07XG5cbiAgICAkc2NvcGUucmVsb2FkUHJvcGVydHkgPSBmdW5jdGlvbigpIHtcbiAgICAgICRodHRwLnBvc3QoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL2NvbXBsZXRpb24vcHJvcGVydHkvJyskc2NvcGUuY29uZmlnLnNlbGVjdGVkUHJvcGVydHksICRzY29wZS5jcml0ZXJpYSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5wcm9wZXJ0eURldGFpbHMgPSByZXNwLmRhdGE7XG4gICAgICAgIFxuICAgICAgICB2YXIgcmVtb3ZlUHJvcGVydHlWYWx1ZXMgPSBhbmd1bGFyLmNvcHkoJHNjb3BlLnByb3BlcnR5VmFsdWVzKTtcblxuICAgICAgICB2YXIgcHJvcGVydHlkYXRhID0gW107XG4gICAgICAgIFxuICAgICAgICBmb3IgKHZhciBpPTA7IGkgPCAkc2NvcGUucHJvcGVydHlEZXRhaWxzLmxlbmd0aDsgaSsrKSB7XG4gICAgICAgICAgdmFyIHByb3AgPSAkc2NvcGUucHJvcGVydHlEZXRhaWxzW2ldO1xuICAgICAgICAgIHZhciByZWNvcmQ9WyBdO1xuICAgICAgICAgIHJlY29yZC5wdXNoKHByb3AudmFsdWUpO1xuICAgICAgICAgIHJlY29yZC5wdXNoKHByb3AuY291bnQpO1xuICAgICAgICAgIHByb3BlcnR5ZGF0YS5wdXNoKHJlY29yZCk7XG5cbiAgICAgICAgICBpZiAoJHNjb3BlLnByb3BlcnR5VmFsdWVzLmNvbnRhaW5zKHByb3AudmFsdWUpKSB7XG4gICAgICAgICAgICByZW1vdmVQcm9wZXJ0eVZhbHVlcy5yZW1vdmUocHJvcC52YWx1ZSk7XG4gICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICRzY29wZS5wcm9wZXJ0eVZhbHVlcy5hZGQocHJvcC52YWx1ZSk7XG4gICAgICAgICAgfVxuICAgICAgICB9XG5cbiAgICAgICAgJHNjb3BlLnByb3BlcnR5Y2hhcnQubG9hZCh7XG4gICAgICAgICAgY29sdW1uczogcHJvcGVydHlkYXRhXG4gICAgICAgIH0pO1xuXG4gICAgICAgIGZvciAodmFyIGo9MDsgaiA8IHJlbW92ZVByb3BlcnR5VmFsdWVzLmxlbmd0aDsgaisrKSB7XG4gICAgICAgICAgJHNjb3BlLnByb3BlcnR5Y2hhcnQudW5sb2FkKHJlbW92ZVByb3BlcnR5VmFsdWVzW2pdKTtcbiAgICAgICAgICAkc2NvcGUucHJvcGVydHlWYWx1ZXMucmVtb3ZlKHJlbW92ZVByb3BlcnR5VmFsdWVzW2pdKTtcbiAgICAgICAgfVxuXG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IHByb3BlcnR5IGRldGFpbHMgZm9yICdcIiskc2NvcGUuY29uZmlnLnNlbGVjdGVkUHJvcGVydHkrXCInOiBcIitKU09OLnN0cmluZ2lmeShyZXNwKSk7XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbG9hZCgpO1xuXG4gICAgJGludGVydmFsKGZ1bmN0aW9uKCkge1xuICAgICAgaWYgKCRzY29wZS5jcml0ZXJpYS5lbmRUaW1lID09PSBcIjBcIiB8fCAkc2NvcGUuY29uZmlnLnByZXZMb3dlckJvdW5kRGlzcGxheSAhPT0gJHNjb3BlLmNvbmZpZy5sb3dlckJvdW5kRGlzcGxheSkge1xuICAgICAgICAkc2NvcGUucmVsb2FkKCk7XG4gICAgICAgIFxuICAgICAgICAkc2NvcGUuY29uZmlnLnByZXZMb3dlckJvdW5kRGlzcGxheSA9ICRzY29wZS5jb25maWcubG93ZXJCb3VuZERpc3BsYXk7XG4gICAgICB9XG4gICAgfSwxMDAwMCk7XG5cbiAgICAkc2NvcGUuaW5pdEdyYXBoID0gZnVuY3Rpb24oKSB7XG4gICAgICAkc2NvcGUuY3RsaW5lY2hhcnQgPSBjMy5nZW5lcmF0ZSh7XG4gICAgICAgIGJpbmR0bzogJyNjb21wbGV0aW9udGltZWxpbmVjaGFydCcsXG4gICAgICAgIGRhdGE6IHtcbiAgICAgICAgICBqc29uOiBbXG4gICAgICAgICAgXSxcbiAgICAgICAgICBheGVzOiB7XG4gICAgICAgICAgICBtYXg6ICd5JyxcbiAgICAgICAgICAgIGF2ZXJhZ2U6ICd5JyxcbiAgICAgICAgICAgIG1pbjogJ3knLFxuICAgICAgICAgICAgY291bnQ6ICd5MicsXG4gICAgICAgICAgICBmYXVsdENvdW50OiAneTInXG4gICAgICAgICAgfSxcbiAgICAgICAgICB0eXBlOiAnbGluZScsXG4gICAgICAgICAgdHlwZXM6IHtcbiAgICAgICAgICAgIGNvdW50OiAnYmFyJyxcbiAgICAgICAgICAgIGZhdWx0Q291bnQ6ICdiYXInXG4gICAgICAgICAgfSxcbiAgICAgICAgICBrZXlzOiB7XG4gICAgICAgICAgICB2YWx1ZTogWydtYXgnLCdhdmVyYWdlJywnbWluJywnY291bnQnLCdmYXVsdENvdW50J10sXG4gICAgICAgICAgICB4OiAndGltZXN0YW1wJ1xuICAgICAgICAgIH1cbiAgICAgICAgfSxcbiAgICAgICAgY29sb3I6IHtcbiAgICAgICAgICBwYXR0ZXJuOiBbJyNmZjAwMDAnLCAnIzMzY2MzMycsICcjZTVlNjAwJywgJyM5OWNjZmYnLCAnI2ZmYjNiMyddXG4gICAgICAgIH0sXG4gICAgICAgIGF4aXM6IHtcbiAgICAgICAgICB4OiB7XG4gICAgICAgICAgICB0eXBlOiAndGltZXNlcmllcycsXG4gICAgICAgICAgICB0aWNrOiB7XG4gICAgICAgICAgICAgIGN1bGxpbmc6IHtcbiAgICAgICAgICAgICAgICBtYXg6IDYgLy8gdGhlIG51bWJlciBvZiB0aWNrIHRleHRzIHdpbGwgYmUgYWRqdXN0ZWQgdG8gbGVzcyB0aGFuIHRoaXMgdmFsdWVcbiAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgZm9ybWF0OiAnJVktJW0tJWQgJUg6JU06JVMnXG4gICAgICAgICAgICB9XG4gICAgICAgICAgfSxcbiAgICAgICAgICB5OiB7XG4gICAgICAgICAgICBsYWJlbDogJ1NlY29uZHMnLFxuICAgICAgICAgICAgcGFkZGluZzoge2JvdHRvbTogMH0sXG4gICAgICAgICAgICB0aWNrOiB7XG4gICAgICAgICAgICAgIGZvcm1hdDogZnVuY3Rpb24gKHkpIHsgcmV0dXJuIHkgLyAxMDAwMDAwMDAwOyB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgfSxcbiAgICAgICAgICB5Mjoge1xuICAgICAgICAgICAgc2hvdzogdHJ1ZVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgfSk7XG5cbiAgICAgICRzY29wZS5jdGZhdWx0c2NoYXJ0ID0gYzMuZ2VuZXJhdGUoe1xuICAgICAgICBiaW5kdG86ICcjY29tcGxldGlvbnRpbWVmYXVsdHNjaGFydCcsXG4gICAgICAgIGRhdGE6IHtcbiAgICAgICAgICBqc29uOiBbXG4gICAgICAgICAgXSxcbiAgICAgICAgICB0eXBlOiAncGllJyxcbiAgICAgICAgICBvbmNsaWNrOiBmdW5jdGlvbiAoZCwgaSkge1xuICAgICAgICAgICAgdmFyIGZhdWx0ID0ge1xuICAgICAgICAgICAgICB2YWx1ZTogZC5pZFxuICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICRzY29wZS5jcml0ZXJpYS5mYXVsdHMuYWRkKGZhdWx0KTtcbiAgICAgICAgICAgICRzY29wZS5yZWxvYWQoKTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0pO1xuXG4gICAgfTtcbiAgICBcbiAgICAkc2NvcGUuaW5pdEdyYXBoKCk7XG5cbiAgICAkc2NvcGUucHJvcGVydHlDbGlja2VkID0gZnVuY3Rpb24oKSB7XG4gICAgICAkc2NvcGUuaW5pdFByb3BlcnR5R3JhcGgoJHNjb3BlLmNvbmZpZy5zZWxlY3RlZFByb3BlcnR5KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLmluaXRQcm9wZXJ0eUdyYXBoID0gZnVuY3Rpb24obmFtZSkge1xuICAgICAgJHNjb3BlLnByb3BlcnR5Y2hhcnQgPSBjMy5nZW5lcmF0ZSh7XG4gICAgICAgIGJpbmR0bzogJyNjb21wbGV0aW9udGltZXByb3BlcnR5Y2hhcnQnLFxuICAgICAgICBkYXRhOiB7XG4gICAgICAgICAgY29sdW1uczogW1xuICAgICAgICAgIF0sXG4gICAgICAgICAgdHlwZTogJ3BpZScsXG4gICAgICAgICAgb25jbGljazogZnVuY3Rpb24gKGQsIGkpIHtcbiAgICAgICAgICAgIHZhciBwcm9wZXJ0eSA9IHtcbiAgICAgICAgICAgICAgbmFtZTogbmFtZSxcbiAgICAgICAgICAgICAgdmFsdWU6IGQuaWRcbiAgICAgICAgICAgIH07XG4gICAgICAgICAgICAkc2NvcGUuY3JpdGVyaWEucHJvcGVydGllcy5hZGQocHJvcGVydHkpO1xuICAgICAgICAgICAgJHNjb3BlLnJlbG9hZCgpO1xuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgfSk7XG5cbiAgICAgICRzY29wZS5yZWxvYWRQcm9wZXJ0eSgpO1xuICAgIH07XG5cbiAgICAkc2NvcGUucmVtb3ZlUHJvcGVydHkgPSBmdW5jdGlvbihwcm9wZXJ0eSkge1xuICAgICAgJHNjb3BlLmNyaXRlcmlhLnByb3BlcnRpZXMucmVtb3ZlKHByb3BlcnR5KTtcbiAgICAgICRzY29wZS5yZWxvYWQoKTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbW92ZUZhdWx0ID0gZnVuY3Rpb24oZmF1bHQpIHtcbiAgICAgICRzY29wZS5jcml0ZXJpYS5mYXVsdHMucmVtb3ZlKGZhdWx0KTtcbiAgICAgICRzY29wZS5yZWxvYWQoKTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnRvZ2dsZUV4Y2x1c2lvbiA9IGZ1bmN0aW9uKGVsZW1lbnQpIHtcbiAgICAgIGVsZW1lbnQuZXhjbHVkZWQgPSAhZWxlbWVudC5leGNsdWRlZDtcbiAgICAgICRzY29wZS5yZWxvYWQoKTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnVwZGF0ZWRCb3VuZHMgPSBmdW5jdGlvbigpIHtcbiAgICAgIGlmICgkc2NvcGUuY29uZmlnLmxvd2VyQm91bmREaXNwbGF5ID09PSAwKSB7XG4gICAgICAgICRzY29wZS5jcml0ZXJpYS5sb3dlckJvdW5kID0gMDtcbiAgICAgIH0gZWxzZSB7XG4gICAgICAgIHZhciBtYXhEdXJhdGlvbiA9IDA7XG4gICAgICAgIGZvciAodmFyIGk9MDsgaSA8ICRzY29wZS5zdGF0aXN0aWNzLmxlbmd0aDsgaSsrKSB7XG4gICAgICAgICAgaWYgKCRzY29wZS5zdGF0aXN0aWNzW2ldLm1heCA+IG1heER1cmF0aW9uKSB7XG4gICAgICAgICAgICBtYXhEdXJhdGlvbiA9ICRzY29wZS5zdGF0aXN0aWNzW2ldLm1heDtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgaWYgKG1heER1cmF0aW9uID4gMCkge1xuICAgICAgICAgICRzY29wZS5jcml0ZXJpYS5sb3dlckJvdW5kID0gKCAkc2NvcGUuY29uZmlnLmxvd2VyQm91bmREaXNwbGF5ICogbWF4RHVyYXRpb24gKSAvIDEwMDtcbiAgICAgICAgfVxuICAgICAgfVxuICAgIH07XG5cbiAgICAkc2NvcGUuc2VsZWN0QWN0aW9uID0gZnVuY3Rpb24oKSB7XG4gICAgICAkc2NvcGUucmVsb2FkKCk7XG4gICAgfTtcblxuICAgICRzY29wZS5jdXJyZW50RGF0ZVRpbWUgPSBmdW5jdGlvbigpIHtcbiAgICAgIHJldHVybiBuZXcgRGF0ZSgpO1xuICAgIH07XG5cbiAgfV0pO1xufVxuIl0sInNvdXJjZVJvb3QiOiIvc291cmNlLyJ9

angular.module("hawkularbtm-templates", []).run(["$templateCache", function($templateCache) {$templateCache.put("plugins/btm/html/btm.html","<div class=\"col-md-12\" ng-controller=\"BTM.BTMController\">\n  <div class=\"text-center hk-urls-list hk-spinner-container\" ng-hide=\"businessTransactions\">\n    <div class=\"spinner spinner-lg\"></div>\n    <p class=\"hk-spinner-legend-below\">Loading...</p>\n  </div>\n\n  <div class=\"row\" ng-show=\"businessTransactions\" >\n    <hawkular-subtab class=\"hk-align-center\">\n      <div class=\"hk-nav-tabs-container\">\n        <ul class=\"nav nav-tabs nav-tabs-pf\">\n          <li class=\"active\"><a href=\"active\" >Active</a></li>\n          <li><a href=\"candidates\">Candidates <i class=\"fa fa-flag\" ng-hide=\"candidateCount === 0\"></i></a></li>\n          <li><a href=\"disabled\">Disabled</a></li>\n          <li><a href=\"ignored\">Ignored</a></li>\n        </ul>\n      </div>\n    </hawkular-subtab>\n\n    <section id=\"active\" class=\"hk-tab-content\">\n\n      <div class=\"col-md-12\">\n        <ul class=\"list-group\" >\n          <br>\n          <div class=\"row\" >\n            Search: <input ng-model=\"query\">\n\n            <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n            <label for=\"chartType\" style=\"width: 3%\" >Chart:</label>\n            <select name=\"chartType\" ng-model=\"chart\" style=\"width: 10%\">\n              <option value=\"TxnCount\">Transaction Count</option>\n              <option value=\"FaultCount\">Fault Count</option>\n              <option value=\"None\">None</option>\n            </select>\n\n            <a href=\"/hawkular-ui/btm-analytics\" class=\"btn pull-right\" target=\"_blank\"><i class=\"fa fa-line-chart\"/></a>\n          </div>\n\n          <div class=\"hk-url-item col-md-6\" ng-hide=\"chart === \'None\'\" >\n            <div id=\"btxntxncountpiechart\" ng-show=\"chart === \'TxnCount\'\"></div>\n            <div id=\"btxnfaultcountpiechart\" ng-show=\"chart === \'FaultCount\'\"></div>\n          </div>\n\n          <div class=\"hk-url-item col-md-6\" ng-repeat=\"btxn in businessTransactions | filter:query\" >\n            <div class=\"panel panel-default hk-url-heading\" ng-show=\"btxn.summary.level === \'All\'\">\n              <a href=\"info/{{btxn.summary.name}}\">{{btxn.summary.name}}</a>\n              <span class=\"hk-settings pull-right\">\n                <a href=\"config/{{btxn.summary.name}}\" ><i class=\"fa fa-cog\"></i></a>\n                <a href=\"#\" ng-click=\"deleteBusinessTxn(btxn)\"><i class=\"fa fa-trash-o\"></i></a>\n              </span>\n            </div>\n\n            <div class=\"panel panel-default hk-summary\" ng-show=\"btxn.summary.level === \'All\'\">\n              <div class=\"row\">\n                <div class=\"col-sm-3 hk-summary-item\">\n                  <a href=\"info/{{btxn.summary.name}}\">\n                    <span class=\"hk-data\" ng-show=\"btxn.count !== undefined\">{{btxn.count}}</i></span>\n                    <span class=\"hk-data spinner\" ng-hide=\"btxn.count !== undefined\" popover=\"Your data is being collected. You should see something in a few seconds.\" popover-trigger=\"mouseenter\" popover-placement=\"bottom\"></span>\n                    <span class=\"hk-item\">Transactions (per hour)</span>\n                  </a>\n                </div>\n                <div class=\"col-sm-3 hk-summary-item\">\n                  <a href=\"info/{{btxn.summary.name}}\">\n                    <span class=\"hk-data\" ng-show=\"btxn.percentile95 !== undefined\">{{btxn.percentile95}}</i></span>\n                    <span class=\"hk-data spinner\" ng-hide=\"btxn.percentile95 !== undefined\" popover=\"Your data is being collected. You should see something in a few seconds.\" popover-trigger=\"mouseenter\" popover-placement=\"bottom\"></span>\n                    <span class=\"hk-item\">Completion (secs 95%)</span>\n                  </a>\n                </div>\n                <div class=\"col-sm-3 hk-summary-item\">\n                  <a href=\"info/{{btxn.summary.name}}\">\n                    <span class=\"hk-data\" ng-show=\"btxn.faultcount !== undefined\">{{btxn.faultcount}}</i></span>\n                    <span class=\"hk-data spinner\" ng-hide=\"btxn.faultcount !== undefined\" popover=\"Your data is being collected. You should see something in a few seconds.\" popover-trigger=\"mouseenter\" popover-placement=\"bottom\"></span>\n                    <span class=\"hk-item\">Faults</span>\n                  </a>\n                </div>\n                <div class=\"col-sm-3 hk-summary-item\">\n                  <a href=\"info/{{btxn.summary.name}}\">\n                    <span class=\"hk-data\" ng-show=\"btxn.alerts !== undefined\">{{btxn.alerts}} <i class=\"fa fa-flag\" ng-show=\"btxn.alerts > 0\"></i></span>\n                    <span class=\"hk-data spinner\" ng-hide=\"btxn.alerts !== undefined\" popover=\"Your data is being collected. You should see something in a few seconds.\" popover-trigger=\"mouseenter\" popover-placement=\"bottom\"></span>\n                    <span class=\"hk-item\">Alerts</span>\n                  </a>\n                </div>\n              </div>\n\n            </div>\n\n          </div>\n       </ul>\n      </div>\n    </section>\n  </div>\n</div>\n\n");
$templateCache.put("plugins/btm/html/btxncandidates.html","<div class=\"col-md-12\" ng-controller=\"BTM.BTMCandidatesController\">\n\n  <div class=\"text-center hk-urls-list hk-spinner-container\" ng-hide=\"unbounduris\">\n    <div class=\"spinner spinner-lg\"></div>\n    <p class=\"hk-spinner-legend-below\">Loading...</p>\n  </div>\n\n  <div class=\"row\" ng-show=\"unbounduris\" >\n    <hawkular-subtab class=\"hk-align-center\">\n      <div class=\"hk-nav-tabs-container\">\n        <ul class=\"nav nav-tabs nav-tabs-pf\">\n          <li><a href=\"active\" >Active</a></li>\n          <li class=\"active\"><a href=\"candidates\">Candidates <i class=\"fa fa-flag\" ng-hide=\"candidateCount === 0\"></i></a></li>\n          <li><a href=\"disabled\">Disabled</a></li>\n          <li><a href=\"ignored\">Ignored</a></li>\n        </ul>\n      </div>\n    </hawkular-subtab>\n\n    <section id=\"active\" class=\"hk-tab-content\">\n\n      <br>\n\n      <form class=\"form-horizontal hk-add-url\" name=\"addBTxnForm\" role=\"form\" novalidate >\n        <div class=\"form-group input\">\n          <div class=\"col-lg-6 col-sm-8 col-xs-12 hk-align-center\">\n            <div class=\"input-group input-group-lg\">\n              <input type=\"text\" class=\"form-control\" name=\"newBTxnNameField\"\n                   ng-model=\"newBTxnName\" ng-model-options=\"{ updateOn: \'default blur\'}\"\n                   placeholder=\"Business transaction name\">\n              <span class=\"input-group-btn\">\n                <input class=\"btn btn-primary\" type=\"submit\" ng-disabled=\"!newBTxnName\" ng-click=\"addBusinessTxn()\" value=\"Manage\" />\n                <input class=\"btn btn-danger\" type=\"button\" ng-disabled=\"!newBTxnName\" ng-click=\"ignoreBusinessTxn()\" value=\"Ignore\" />\n              </span>\n\n              <span class=\"input-group-btn\">\n              </span>\n\n              <select id=\"repeatSelect\" class=\"form-control\" ng-model=\"existingBTxnName\" >\n                <option value=\"\">Select existing ....</i></option>\n                <option ng-repeat=\"btxn in businessTransactions\" value=\"{{btxn.name}}\">{{btxn.name}} ({{getLevel(btxn.level)}})</option>\n              </select>\n              <span class=\"input-group-btn\">\n                <input class=\"btn btn-primary\" type=\"submit\" ng-disabled=\"!existingBTxnName || selecteduris.length == 0\" ng-click=\"updateBusinessTxn()\" value=\"Update\" />\n              </span>\n            </div>\n          </div>\n        </div>\n      </form>\n\n      <div class=\"col-md-9 hk-align-center\">\n        <ul class=\"list-group\" >\n          <br>\n          <div class=\"row\" >\n            Search: <input ng-model=\"query\">\n          </div>\n          <br>\n\n          <div class=\"panel panel-default hk-url-heading\">\n            <div ng-repeat=\"uriinfo in unbounduris | filter:query\" >\n              <label>\n                <input type=\"checkbox\" name=\"selectedURIs[]\"\n                  value=\"{{uriinfo.uri}}\"\n                  ng-checked=\"isSelected(uriinfo)\"\n                  ng-click=\"selectionChanged(uriinfo)\"\n                  ng-disabled=\"!newBTxnName && !existingBTxnName\">\n                  <span ng-hide=\"!newBTxnName && !existingBTxnName\" style=\"color:black\">{{uriinfo.uri}} [ {{uriinfo.endpointType}}]</span>\n                  <span ng-show=\"!newBTxnName && !existingBTxnName\" style=\"color:grey\"><i>{{uriinfo.uri}} [ {{uriinfo.endpointType}}]</i></span>\n              </label>\n            </div>\n          </div>\n       </ul>\n      </div>\n    </section>\n  </div>\n</div>\n\n");
$templateCache.put("plugins/btm/html/btxnconfig.html","<div class=\"row\">\n  <div class=\"col-md-12\" ng-controller=\"BTM.BTxnConfigController\">\n    <h1><span style=\"color:grey\">{{businessTransactionName}}</span></h1>\n\n    <button type=\"button\" class=\"btn btn-success btn-sm\" ng-click=\"save()\" ng-disabled=\"!dirty\">Save</button>\n    <button type=\"button\" class=\"btn btn-danger btn-sm\" ng-click=\"reset()\" ng-disabled=\"!dirty\">Discard</button>\n\n    <br>\n    <br>\n\n    <uib-alert ng-repeat=\"message in messages\" type=\"{{getMessageType(message)}}\" close=\"closeMessage($index)\"><strong>{{getMessageText(message)}}</strong></uib-alert>\n\n    <a href=\"#\" editable-textarea=\"businessTransaction.description\" e-rows=\"14\" e-cols=\"120\" rows=\"7\" onaftersave=\"setDirty()\" >\n        <pre><i>{{ businessTransaction.description || \'No description\' }}</i></pre>\n    </a>\n\n    <div class=\"col-md-12\" >\n      <h2>Filters</h2>\n    </div>\n\n    <div class=\"col-md-6\" >\n\n      <h4>Inclusion</h4>\n\n      <!-- TODO: Use angular-ui/bootstrap typeahead to autofill possible inclusion URIs -->\n\n      <ul class=\"list-group\">\n        <li class=\"list-group-item\" ng-repeat=\"inclusion in businessTransaction.filter.inclusions\" >{{inclusion}}<span class=\"glyphicon glyphicon-remove pull-right\" aria-hidden=\"true\" ng-click=\"removeInclusionFilter(inclusion)\"></span></li>\n        <li class=\"list-group-item\" >\n          <form class=\"form-horizontal hk-add-url\" name=\"addInclusionForm\" role=\"form\" autocomplete=\"off\" ng-submit=\"addInclusionFilter()\">\n            <div class=\"input-group input-group-lg\">\n              <input type=\"text\" class=\"form-control\" name=\"newInclusionFilterField\"\n                   ng-model=\"newInclusionFilter\" ng-model-options=\"{ updateOn: \'default blur\'}\"\n                   placeholder=\"Enter an inclusion filter (regular expression)\"\n                   uib-typeahead=\"uri for uri in unboundURIs | filter:$viewValue | limitTo:12\" required>\n              <span class=\"input-group-btn\">\n                <button class=\"btn btn-primary\" type=\"submit\" ng-disabled=\"!newInclusionFilter\" >\n                  <div ng-show=\"addProgress\" class=\"spinner spinner-sm\"></div>\n                  <span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\"\"></span>\n                </button>\n              </span>\n            </div>\n          </form>\n        </li>\n      </ul>\n    </div>\n\n    <div class=\"col-md-6\" >\n      <h4>Exclusion (applied after inclusions)</h4>\n\n      <ul class=\"list-group\">\n        <li class=\"list-group-item\" ng-repeat=\"exclusion in businessTransaction.filter.exclusions\" >{{exclusion}}<span class=\"glyphicon glyphicon-remove pull-right\" aria-hidden=\"true\" ng-click=\"removeExclusionFilter(exclusion)\"></span></li>\n        <li class=\"list-group-item\" >\n          <form class=\"form-horizontal hk-add-url\" name=\"addExclusionForm\" role=\"form\" autocomplete=\"off\" novalidate ng-submit=\"addExclusionFilter()\">\n            <div class=\"input-group input-group-lg\">\n              <input type=\"text\" class=\"form-control\" name=\"newExclusionFilterField\"\n                   ng-model=\"newExclusionFilter\" ng-model-options=\"{ updateOn: \'default blur\'}\"\n                   placeholder=\"Enter an exclusion filter (regular expression)\"\n                   uib-typeahead=\"uri for uri in unboundURIs | filter:$viewValue | limitTo:12\" required>\n              <span class=\"input-group-btn\">\n                <button class=\"btn btn-primary\" type=\"submit\" ng-disabled=\"!newExclusionFilter\" >\n                  <div ng-show=\"addProgress\" class=\"spinner spinner-sm\"></div>\n                  <span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\"\"></span>\n                </button>\n              </span>\n            </div>\n          </form>\n        </li>\n      </ul>\n    </div>\n\n    <!-- TODO: Styles -->\n\n    <div class=\"col-md-12\" >\n      <form>\n        <div class=\"form-group\">\n          <label for=\"level\" style=\"width: 10%\" >Reporting Level:</label>\n          <select name=\"nodeType\" ng-model=\"businessTransaction.level\" ng-change=\"setDirty()\" style=\"width: 10%\">\n            <option value=\"All\">All</option>\n            <option value=\"None\">None</option>\n            <option value=\"Ignore\">Ignore</option>\n          </select>\n        </div>\n      </form>\n    </div>\n\n    <div class=\"col-md-12\" >\n      <h2>Processors <a class=\"btn btn-primary\" ng-click=\"addProcessor()\"><span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\"\"></span></a></h2>\n    </div>\n\n    <div class=\"col-md-12\" >\n\n      <uib-accordion>\n        <uib-accordion-group ng-repeat=\"processor in businessTransaction.processors\" is-open=\"false\" is-disabled=\"false\">\n          <uib-accordion-heading>{{processor.description}} <a class=\"btn btn-link hk-delete pull-right\" href=\"#\" uibTooltip=\"Delete\" tooltip-trigger tooltip-placement=\"top\" ng-click=\"deleteProcessor(processor)\"><i class=\"fa fa-trash-o\"></i></a></uib-accordion-heading>\n\n          <form>\n            <div class=\"form-group\">\n              <label for=\"description\" style=\"width: 15%\" >Description:</label>\n              <input type=\"text\" name=\"description\" ng-model=\"processor.description\" ng-change=\"setDirty()\" style=\"width: 80%\" >\n            </div>\n\n            <div class=\"form-group\">\n              <label for=\"nodeType\" style=\"width: 15%\" > Node type: </label>\n              <select name=\"nodeType\" ng-model=\"processor.nodeType\" ng-change=\"setDirty()\" style=\"width: 30%\">\n                <option value=\"Consumer\">Consumer</option>\n                <option value=\"Producer\">Producer</option>\n                <option value=\"Component\">Component</option>\n              </select>\n\n              <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n              <label for=\"direction\" style=\"width: 15%\" >Direction: </label>\n              <select name=\"direction\" ng-model=\"processor.direction\" ng-change=\"setDirty()\" style=\"width: 30%\">\n                <option value=\"In\">In</option>\n                <option value=\"Out\">Out</option>\n              </select>\n\n              <label for=\"uriFilter\" style=\"width: 15%\" >URI filter:</label>\n              <input type=\"text\" name=\"uriFilter\"\n                   ng-model=\"processor.uriFilter\" ng-model-options=\"{ updateOn: \'default blur\'}\"\n                   placeholder=\"Enter URI filter (regular expression)\"\n                   uib-typeahead=\"uri for uri in boundURIs | filter:$viewValue | limitTo:12\"\n                   ng-change=\"setDirty()\" style=\"width: 80%\" >\n\n              <label for=\"operation\" style=\"width: 15%\" >Operation:</label>\n              <input type=\"text\" name=\"operation\" ng-model=\"processor.operation\" ng-change=\"setDirty()\" style=\"width: 30%\" >\n\n              <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n              <label for=\"faultFilter\" style=\"width: 15%\" >Fault filter:</label>\n              <input type=\"text\" name=\"faultFilter\" ng-model=\"processor.faultFilter\" ng-change=\"setDirty()\" style=\"width: 30%\" >\n            </div>\n\n            <div class=\"form-group\">\n              <label for=\"predicateType\" style=\"width: 15%\" >Predicate Type: </label>\n              <select name=\"predicateType\" ng-model=\"processor.predicate.type\" ng-change=\"changedExpressionType(processor.predicate)\" style=\"width: 30%\">\n                <option value=\"\"></option>\n                <option value=\"Literal\">Literal</option>\n                <option value=\"XML\">XML</option>\n                <option value=\"JSON\">JSON</option>\n                <option value=\"Text\">Text</option>\n                <option value=\"FreeForm\">Free Form</option>\n              </select>\n\n              <br>\n\n              <label for=\"predicateSource\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\">Source: </label>\n              <select name=\"predicateSource\" ng-model=\"processor.predicate.source\" ng-change=\"setDirty()\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\" style=\"width: 30%\">\n                <option value=\"Content\">Content</option>\n                <option value=\"Header\">Header</option>\n              </select>\n\n              <label style=\"width: 5%\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\"></label> <!-- TODO: Must be a better way -->\n\n              <label for=\"predicateKey\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\">Key: </label>\n              <input type=\"text\" name=\"predicateKey\" ng-model=\"processor.predicate.key\" ng-change=\"setDirty()\" style=\"width: 30%\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\">\n\n              <label for=\"predicateXPath\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'XML\'\">XPath: <a href=\"http://www.w3schools.com/xsl/xpath_syntax.asp\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n              <input type=\"text\" name=\"predicateXPath\" ng-model=\"processor.predicate.xpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"processor.predicate.type === \'XML\'\">\n\n              <label for=\"predicateJSONPath\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'JSON\'\">JSONPath: <a href=\"http://goessner.net/articles/JsonPath/\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n              <input type=\"text\" name=\"predicateJSONPath\" ng-model=\"processor.predicate.jsonpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"processor.predicate.type === \'JSON\'\">\n\n              <label for=\"predicateValue\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'FreeForm\' || action.expression.type === \'Literal\'\">Value:</label>\n              <input type=\"text\" name=\"predicateValue\" ng-model=\"processor.predicate.value\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"processor.predicate.type === \'FreeForm\' || action.expression.type === \'Literal\'\">\n            </div>\n          </form>\n\n          <h4>Actions <span uib-dropdown>\n            <a href id=\"simple-dropdown\" uib-dropdown-toggle class=\"btn btn-primary\">\n              <span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\"\"></span>\n            </a>\n            <ul class=\"uib-dropdown-menu\" aria-labelledby=\"simple-dropdown\">\n              <li><a href ng-click=\"addAction(processor, \'AddContent\')\">Add Content</a></li>\n              <li><a href ng-click=\"addAction(processor, \'AddCorrelationId\')\">Add Correlation Identifier</a></li>\n              <li><a href ng-click=\"addAction(processor, \'EvaluateURI\')\">Evaluate URI</a></li>\n              <li><a href ng-click=\"addAction(processor, \'SetDetail\')\">Set Detail</a></li>\n              <li><a href ng-click=\"addAction(processor, \'SetFault\')\">Set Fault Code</a></li>\n              <li><a href ng-click=\"addAction(processor, \'SetFaultDescription\')\">Set Fault Description</a></li>\n              <li><a href ng-click=\"addAction(processor, \'SetProperty\')\">Set Property</a></li>\n            </ul>\n            </span>\n          </h4>\n\n          <uib-accordion>\n            <uib-accordion-group ng-repeat=\"action in processor.actions\" is-open=\"false\" is-disabled=\"false\">\n              <uib-accordion-heading>[ {{action.actionType}} {{action.name}} ]: {{action.description}} <a class=\"btn btn-link hk-delete pull-right\" href=\"#\" uibTooltip=\"Delete\" tooltip-trigger tooltip-placement=\"top\" ng-click=\"deleteAction(processor,action)\"><i class=\"fa fa-trash-o\"></i></a></uib-accordion-heading>\n\n              <form>\n                <div class=\"form-group\">\n                  <label for=\"description\" style=\"width: 15%\" >Description:</label>\n                  <input type=\"text\" name=\"description\" ng-model=\"action.description\" ng-change=\"setDirty()\" style=\"width: 80%\" >\n                </div>\n\n                <div class=\"form-group\">\n                  <label for=\"actionPredicateType\" style=\"width: 15%\" >Predicate Type: </label>\n                  <select name=\"actionPredicateType\" ng-model=\"action.predicate.type\" ng-change=\"changedExpressionType(action.predicate)\" style=\"width: 30%\">\n                    <option value=\"\"></option>\n                    <option value=\"Literal\">Literal</option>\n                    <option value=\"XML\">XML</option>\n                    <option value=\"JSON\">JSON</option>\n                    <option value=\"Text\">Text</option>\n                    <option value=\"FreeForm\">Free Form</option>\n                  </select>\n\n                  <br>\n\n                  <label for=\"actionPredicateSource\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'XML\' || action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\">Predicate Source: </label>\n                  <select name=\"actionPredicateSource\" ng-model=\"action.predicate.source\" ng-change=\"setDirty()\" ng-show=\"action.predicate.type === \'XML\' ||action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\" style=\"width: 30%\">\n                    <option value=\"Content\">Content</option>\n                    <option value=\"Header\">Header</option>\n                  </select>\n\n                  <label style=\"width: 5%\" ng-show=\"action.predicate.type === \'XML\' || action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\"></label> <!-- TODO: Must be a better way -->\n\n                  <label for=\"actionPredicateKey\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'XML\' || action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\">Predicate Key: </label>\n                  <input type=\"text\" name=\"actionPredicateKey\" ng-model=\"action.predicate.key\" ng-change=\"setDirty()\" style=\"width: 30%\" ng-show=\"action.predicate.type === \'XML\' || action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\">\n\n                  <label for=\"actionPredicateXPath\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'XML\'\">Predicate XPath: <a href=\"http://www.w3schools.com/xsl/xpath_syntax.asp\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n                  <input type=\"text\" name=\"actionPredicateXPath\" ng-model=\"action.predicate.xpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.predicate.type === \'XML\'\">\n\n                  <label for=\"actionPredicateJSONPath\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'JSON\'\">Predicate JSONPath: <a href=\"http://goessner.net/articles/JsonPath/\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n                  <input type=\"text\" name=\"actionPredicateJSONPath\" ng-model=\"action.predicate.jsonpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.predicate.type === \'JSON\'\">\n\n                  <label for=\"actionPredicate\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'FreeForm\' || action.predicate.type === \'Literal\'\">Predicate Value:</label>\n                  <input type=\"text\" name=\"actionPredicate\" ng-model=\"action.predicate.value\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.predicate.type === \'FreeForm\' || action.predicate.type === \'Literal\'\">\n                </div>\n\n                <div class=\"form-group\">\n\n                  <!-- HWKBTM-273 Using \'ng-class\' attribute to try to highlight the error field, but at the point\n                       where the form is displayed the errors aren\'t available, and their retrieval does not\n                       cause a change in state that refreshes the field. -->\n\n                  <label for=\"actionName\" ng-class=\"{error:isError(processor,action,\'name\')}\" style=\"width: 15%\" ng-show=\"action.actionType === \'AddContent\' || action.actionType === \'SetDetail\' || action.actionType === \'SetProperty\'\" >Name:</label>\n                  <input type=\"text\" name=\"actionName\" ng-model=\"action.name\" ng-change=\"setDirty()\" ng-show=\"action.actionType === \'AddContent\' || action.actionType === \'SetDetail\' || action.actionType === \'SetProperty\'\" style=\"width: 30%\" >\n\n                  <label style=\"width: 5%\" ng-show=\"action.actionType === \'AddContent\'\" ></label> <!-- TODO: Must be a better way -->\n\n                  <label for=\"actionType\" style=\"width: 15%\" ng-show=\"action.actionType === \'AddContent\'\" >Type:</label>\n                  <input type=\"text\" name=\"actionType\" ng-model=\"action.type\" ng-change=\"setDirty()\" ng-show=\"action.actionType === \'AddContent\'\" style=\"width: 30%\" >\n\n                  <label for=\"correlationScope\" ng-show=\"action.actionType === \'AddCorrelationId\'\" style=\"width: 15%\" >Correlation Scope: </label>\n                  <select name=\"correlationScope\" ng-model=\"action.scope\" ng-show=\"action.actionType === \'AddCorrelationId\'\" ng-change=\"setDirty()\" style=\"width: 30%\">\n                    <option value=\"Global\">Global</option>\n                    <option value=\"Interaction\">Interaction</option>\n                    <option value=\"Local\">Local</option>\n                  </select>\n\n                  <label for=\"actionTemplate\" ng-class=\"{error:isError(processor,action,\'template\')}\" ng-show=\"action.actionType === \'EvaluateURI\'\" style=\"width: 15%\" >Template URI:</label>\n                  <input type=\"text\" name=\"actionTemplate\" ng-model=\"action.template\" ng-change=\"setDirty()\" ng-show=\"action.actionType === \'EvaluateURI\'\" style=\"width: 30%\" >\n\n                </div>\n\n                <div class=\"form-group\" ng-if=\"action.actionType !== \'EvaluateURI\' && action.actionType !== undefined\" >\n\n                  <label for=\"actionValueType\" ng-class=\"{error:isError(processor,action,\'expression\')}\" style=\"width: 15%\" >Value Type: </label>\n                  <select name=\"actionValueType\" ng-model=\"action.expression.type\" ng-change=\"changedExpressionType(action.expression)\" style=\"width: 30%\">\n                    <option value=\"\"></option>\n                    <option value=\"Literal\">Literal</option>\n                    <option value=\"XML\">XML</option>\n                    <option value=\"JSON\">JSON</option>\n                    <option value=\"Text\">Text</option>\n                    <option value=\"FreeForm\">Free Form</option>\n                  </select>\n\n                  <br>\n\n                  <label for=\"actionValueSource\" style=\"width: 15%\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\">Value Source: </label>\n                  <select name=\"actionValueSource\" ng-model=\"action.expression.source\" ng-change=\"setDirty()\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\" style=\"width: 30%\">\n                    <option value=\"Content\">Content</option>\n                    <option value=\"Header\">Header</option>\n                  </select>\n\n                  <label style=\"width: 5%\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\"></label> <!-- TODO: Must be a better way -->\n\n                  <label for=\"actionValueKey\" style=\"width: 15%\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\">Value Key: </label>\n                  <input type=\"text\" name=\"actionValueKey\" ng-model=\"action.expression.key\" ng-change=\"setDirty()\" style=\"width: 30%\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\">\n\n                  <label for=\"actionValueXPath\" style=\"width: 15%\" ng-show=\"action.expression.type === \'XML\'\">Value XPath: <a href=\"http://www.w3schools.com/xsl/xpath_syntax.asp\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n                  <input type=\"text\" name=\"actionValueXPath\" ng-model=\"action.expression.xpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.expression.type === \'XML\'\">\n\n                  <label for=\"actionValueJSONPath\" style=\"width: 15%\" ng-show=\"action.expression.type === \'JSON\'\">Value JSONPath: <a href=\"http://goessner.net/articles/JsonPath/\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n                  <input type=\"text\" name=\"actionValueJSONPath\" ng-model=\"action.expression.jsonpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.expression.type === \'JSON\'\">\n\n                  <label for=\"actionValue\" style=\"width: 15%\" ng-show=\"action.expression.type === \'FreeForm\' || action.expression.type === \'Literal\'\">Value:</label>\n                  <input type=\"text\" name=\"actionValue\" ng-model=\"action.expression.value\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.expression.type === \'FreeForm\' || action.expression.type === \'Literal\'\">\n                </div>\n              </form>\n\n            </uib-accordion-group>\n          </uib-accordion>\n\n          <!-- Provide padding as otherwise the action dropdown, when no actions, gets hidden\n               (Must be a better way, but this works for now) -->\n          <div>\n            <br>\n            <br>\n            <br>\n            <br>\n            <br>\n            <br>\n            <br>\n            <br>\n            <br>\n          </div>\n\n        </uib-accordion-group>\n      </uib-accordion>\n    </div>\n\n  </div>\n</div>\n");
$templateCache.put("plugins/btm/html/btxndisabled.html","<div class=\"col-md-12\" ng-controller=\"BTM.BTMDisabledController\">\n  <div class=\"text-center hk-urls-list hk-spinner-container\" ng-hide=\"businessTransactions\">\n    <div class=\"spinner spinner-lg\"></div>\n    <p class=\"hk-spinner-legend-below\">Loading...</p>\n  </div>\n\n  <div class=\"row\" ng-show=\"businessTransactions\" >\n\n    <hawkular-subtab class=\"hk-align-center\">\n      <div class=\"hk-nav-tabs-container\">\n        <ul class=\"nav nav-tabs nav-tabs-pf\">\n          <li><a href=\"active\" >Active</a></li>\n          <li><a href=\"candidates\">Candidates <i class=\"fa fa-flag\" ng-hide=\"candidateCount === 0\"></i></a></li>\n          <li class=\"active\"><a href=\"disabled\">Disabled</a></li>\n          <li><a href=\"ignored\">Ignored</a></li>\n        </ul>\n      </div>\n    </hawkular-subtab>\n\n    <section id=\"active\" class=\"hk-tab-content\">\n\n      <div class=\"col-md-9 hk-align-center\">\n        <ul class=\"list-group\" >\n          <br>\n          <div class=\"row\" >\n            Search: <input ng-model=\"query\">\n          </div>\n\n          <div class=\"hk-url-item\" ng-repeat=\"btxn in businessTransactions | filter:query\" >\n            <div class=\"panel panel-default hk-url-heading\" ng-show=\"btxn.summary.level === \'None\'\">\n              <a href=\"info/{{btxn.summary.name}}\">{{btxn.summary.name}}</a>\n              <span class=\"hk-settings pull-right\">\n                <a href=\"config/{{btxn.summary.name}}\" ><i class=\"fa fa-cog\"></i></a>\n                <a href=\"#\" ng-click=\"deleteBusinessTxn(btxn)\"><i class=\"fa fa-trash-o\"></i></a>\n              </span>\n            </div>\n          </div>\n       </ul>\n      </div>\n    </section>\n  </div>\n</div>\n\n");
$templateCache.put("plugins/btm/html/btxnignored.html","<div class=\"col-md-12\" ng-controller=\"BTM.BTMIgnoredController\">\n  <div class=\"text-center hk-urls-list hk-spinner-container\" ng-hide=\"businessTransactions\">\n    <div class=\"spinner spinner-lg\"></div>\n    <p class=\"hk-spinner-legend-below\">Loading...</p>\n  </div>\n\n  <div class=\"row\" ng-show=\"businessTransactions\" >\n\n    <hawkular-subtab class=\"hk-align-center\">\n      <div class=\"hk-nav-tabs-container\">\n        <ul class=\"nav nav-tabs nav-tabs-pf\">\n          <li><a href=\"active\" >Active</a></li>\n          <li><a href=\"candidates\">Candidates <i class=\"fa fa-flag\" ng-hide=\"candidateCount === 0\"></i></a></li>\n          <li><a href=\"disabled\">Disabled</a></li>\n          <li class=\"active\"><a href=\"ignored\">Ignored</a></li>\n        </ul>\n      </div>\n    </hawkular-subtab>\n\n    <section id=\"active\" class=\"hk-tab-content\">\n\n      <div class=\"col-md-9 hk-align-center\">\n        <ul class=\"list-group\" >\n          <br>\n          <div class=\"row\" >\n            Search: <input ng-model=\"query\">\n          </div>\n\n          <div class=\"hk-url-item\" ng-repeat=\"btxn in businessTransactions | filter:query\" >\n            <div class=\"panel panel-default hk-url-heading\" ng-show=\"btxn.summary.level === \'Ignore\'\">\n              <a href=\"info/{{btxn.summary.name}}\">{{btxn.summary.name}}</a>\n              <span class=\"hk-settings pull-right\">\n                <a href=\"config/{{btxn.summary.name}}\" ><i class=\"fa fa-cog\"></i></a>\n                <a href=\"#\" ng-click=\"deleteBusinessTxn(btxn)\"><i class=\"fa fa-trash-o\"></i></a>\n              </span>\n            </div>\n          </div>\n       </ul>\n      </div>\n    </section>\n  </div>\n</div>\n\n");
$templateCache.put("plugins/btm/html/btxninfo.html","<div class=\"row\">\n  <div class=\"col-md-12\" ng-controller=\"BTM.BTxnConfigController\">\n    <h1><span style=\"color:grey\">{{businessTransactionName}}</span></h1>\n\n    <div class=\"form-group\" >\n      <span ng-repeat=\"fault in criteria.faults\">\n        <span ng-show=\"!fault.excluded\">\n          <a class=\"btn btn-success\" ng-click=\"toggleExclusion(fault)\">\n            <i>fault</i>: {{fault.value}}\n            <a class=\"btn btn-default\" ng-click=\"removeFault(fault)\">\n              <span class=\"glyphicon glyphicon-remove\" aria-hidden=\"true\"></span>\n            </a>\n          </a>\n        </span>\n        <span ng-show=\"fault.excluded\">\n          <a class=\"btn btn-danger\" ng-click=\"toggleExclusion(fault)\">\n            <i>fault</i>: {{fault.value}}\n            <a class=\"btn btn-default\" ng-click=\"removeFault(fault)\">\n              <span class=\"glyphicon glyphicon-remove\" aria-hidden=\"true\"></span>\n            </a>\n          </a>\n        </span>\n        <label style=\"width: 1%\" ></label> <!-- TODO: Must be a better way -->\n      </span>\n\n      <span ng-repeat=\"property in criteria.properties\">\n        <span ng-show=\"!property.excluded\">\n          <a class=\"btn btn-success\" ng-click=\"toggleExclusion(property)\">\n            <i>{{property.name}}</i>: {{property.value}}\n            <a class=\"btn btn-default\" ng-click=\"removeProperty(property)\">\n              <span class=\"glyphicon glyphicon-remove\" aria-hidden=\"true\"></span>\n            </a>\n          </a>\n        </span>\n        <span ng-show=\"property.excluded\">\n          <a class=\"btn btn-danger\" ng-click=\"toggleExclusion(property)\">\n            <i>{{property.name}}</i>: {{property.value}}\n            <a class=\"btn btn-default\" ng-click=\"removeProperty(property)\">\n              <span class=\"glyphicon glyphicon-remove\" aria-hidden=\"true\"></span>\n            </a>\n          </a>\n        </span>\n        <label style=\"width: 1%\" ></label> <!-- TODO: Must be a better way -->\n      </span>\n    </div>\n\n    <span>\n      <form>\n        <div class=\"form-group\">\n          <label for=\"intervalField\" style=\"width: 10%\" class=\"\" >Aggregation Interval:</label>\n          <select name=\"intervalField\" ng-model=\"config.interval\" ng-change=\"selectAction()\" style=\"width: 10%\">\n            <option value=\"1000\">1 Second</option>\n            <option value=\"10000\">10 Second</option>\n            <option value=\"30000\">30 Second</option>\n            <option value=\"60000\">1 Minute</option>\n            <option value=\"600000\">10 Minutes</option>\n            <option value=\"3600000\">1 Hour</option>\n            <option value=\"86400000\">1 Day</option>\n            <option value=\"604800000\">7 Day</option>\n          </select>\n\n          <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n          <label for=\"timeSpanField\" style=\"width: 5%\" >Time Span:</label>\n          <select name=\"timeSpanField\" ng-model=\"criteria.startTime\" ng-change=\"selectAction()\" style=\"width: 10%\">\n            <option value=\"-60000\">1 Minute</option>\n            <option value=\"-600000\">10 Minutes</option>\n            <option value=\"-1800000\">30 Minutes</option>\n            <option value=\"-3600000\">1 Hour</option>\n            <option value=\"-14400000\">4 Hours</option>\n            <option value=\"-28800000\">8 Hours</option>\n            <option value=\"-43200000\">12 Hours</option>\n            <option value=\"-86400000\">Day</option>\n            <option value=\"-604800000\">Week</option>\n            <option value=\"-2419200000\">Month</option>\n            <option value=\"-15768000000\">6 Months</option>\n            <option value=\"-31536000000\">Year</option>\n            <option value=\"1\">All</option>\n          </select>\n\n          <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n          <label for=\"endTimeField\" style=\"width: 3%\" >Until:</label>\n          <select name=\"endTimeField\" ng-model=\"criteria.endTime\" ng-change=\"selectAction()\" style=\"width: 10%\">\n            <option value=\"0\">Now</option>\n            <option value=\"{{currentDateTime().getTime()}}\">{{currentDateTime() | date:\'dd MMM yyyy HH:mm:ss\'}}</option>\n          </select>\n\n          <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n          <label for=\"lowerBoundField\" style=\"width: 8%\" >Lower Bound(%):</label>\n          <input type=\"number\" ng-model=\"config.lowerBoundDisplay\"\n                name=\"lowerBoundField\" ng-change=\"updatedBounds()\"\n                min=\"0\" max=\"100\">\n        </div>\n      </form>\n    </span>\n\n    <div id=\"completiontimelinechart\"></div>\n\n    <div class=\"col-md-6\">\n        <h2><span style=\"color:grey\">Faults</span></h2>\n\n        <div id=\"completiontimefaultschart\"></div>\n    </div>\n\n    <div class=\"col-md-6\">\n        <h2><span style=\"color:grey\">Property</span>\n          <select name=\"propertyField\" ng-model=\"config.selectedProperty\" ng-change=\"propertyClicked()\">\n            <option ng-repeat=\"property in properties\">{{property.name}}</option>\n          </select>\n        </h2>\n\n        <div id=\"completiontimepropertychart\"></div>\n    </div>\n\n  </div>\n</div>\n");}]); hawtioPluginLoader.addModule("hawkularbtm-templates");