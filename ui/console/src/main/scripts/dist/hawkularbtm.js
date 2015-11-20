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
                $http.get('/hawkular/btm/analytics/businesstxn/completion/count?businessTransaction=' + btxn.summary.name).then(function (resp) {
                    btxn.count = resp.data;
                }, function (resp) {
                    console.log("Failed to get count: " + resp);
                });
                $http.get('/hawkular/btm/analytics/businesstxn/completion/percentiles?businessTransaction=' + btxn.summary.name).then(function (resp) {
                    if (resp.data.percentiles[95] > 0) {
                        btxn.percentile95 = Math.round(resp.data.percentiles[95] / 1000000) / 1000;
                    }
                    else {
                        btxn.percentile95 = 0;
                    }
                }, function (resp) {
                    console.log("Failed to get completion percentiles: " + resp);
                });
                $http.get('/hawkular/btm/analytics/businesstxn/completion/faultcount?businessTransaction=' + btxn.summary.name).then(function (resp) {
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
            $http.get('/hawkular/btm/analytics/businesstxn/unbounduris').then(function (resp) {
                $scope.unboundURIs = [];
                for (var i = 0; i < resp.data.length; i++) {
                    var regex = $scope.escapeRegExp(resp.data[i].uri);
                    if (regex !== undefined) {
                        $scope.unboundURIs.add(regex);
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
                processor.actions.add({
                    actionType: type,
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
                console.log("Failed to get business txn '" + $scope.businessTransactionName + "': " + resp);
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
            $scope.properties = [];
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
                    console.log("Failed to get statistics: " + resp);
                });
                $http.post('/hawkular/btm/analytics/businesstxn/completion/faults', $scope.criteria).then(function (resp) {
                    $scope.faults = resp.data;
                    var faultdata = [];
                    for (var i = 0; i < $scope.faults.length; i++) {
                        var fault = $scope.faults[i];
                        var record = [];
                        record.push(fault.value);
                        record.push(fault.count);
                        faultdata.push(record);
                    }
                    $scope.ctfaultschart.unload();
                    $scope.ctfaultschart.load({
                        columns: faultdata
                    });
                }, function (resp) {
                    console.log("Failed to get statistics: " + resp);
                });
                $http.get('/hawkular/btm/analytics/businesstxn/properties/' + $scope.businessTransactionName).then(function (resp) {
                    $scope.properties = resp.data;
                }, function (resp) {
                    console.log("Failed to get property info: " + resp);
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
                    var propertydata = [];
                    for (var i = 0; i < $scope.propertyDetails.length; i++) {
                        var prop = $scope.propertyDetails[i];
                        var record = [];
                        record.push(prop.value);
                        record.push(prop.count);
                        propertydata.push(record);
                    }
                    $scope.propertychart.unload();
                    $scope.propertychart.load({
                        columns: propertydata
                    });
                }, function (resp) {
                    console.log("Failed to get property details for '" + $scope.config.selectedProperty + "': " + resp);
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

//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbImluY2x1ZGVzLnRzIiwiYnRtL3RzL2J0bUdsb2JhbHMudHMiLCJidG0vdHMvYnRtUGx1Z2luLnRzIiwiYnRtL3RzL2J0bS50cyIsImJ0bS90cy9idHhuY2FuZGlkYXRlcy50cyIsImJ0bS90cy9idHhuY29uZmlnLnRzIiwiYnRtL3RzL2J0eG5kaXNhYmxlZC50cyIsImJ0bS90cy9idHhuaWdub3JlZC50cyIsImJ0bS90cy9idHhuaW5mby50cyJdLCJuYW1lcyI6WyJCVE0iXSwibWFwcGluZ3MiOiJBQUFBLDJEQUEyRDtBQUMzRCw0REFBNEQ7QUFDNUQsR0FBRztBQUNILG1FQUFtRTtBQUNuRSxvRUFBb0U7QUFDcEUsMkNBQTJDO0FBQzNDLEdBQUc7QUFDSCxnREFBZ0Q7QUFDaEQsR0FBRztBQUNILHVFQUF1RTtBQUN2RSxxRUFBcUU7QUFDckUsNEVBQTRFO0FBQzVFLHVFQUF1RTtBQUN2RSxrQ0FBa0M7QUFFbEMsMERBQTBEOztBQ2YxRCwyREFBMkQ7QUFDM0QsNERBQTREO0FBQzVELEdBQUc7QUFDSCxtRUFBbUU7QUFDbkUsb0VBQW9FO0FBQ3BFLDJDQUEyQztBQUMzQyxHQUFHO0FBQ0gsZ0RBQWdEO0FBQ2hELEdBQUc7QUFDSCx1RUFBdUU7QUFDdkUscUVBQXFFO0FBQ3JFLDRFQUE0RTtBQUM1RSx1RUFBdUU7QUFDdkUsa0NBQWtDO0FBRWxDLHlDQUF5QztBQUN6QyxJQUFPLEdBQUcsQ0FPVDtBQVBELFdBQU8sR0FBRyxFQUFDLENBQUM7SUFFQ0EsY0FBVUEsR0FBR0EsaUJBQWlCQSxDQUFDQTtJQUUvQkEsT0FBR0EsR0FBbUJBLE1BQU1BLENBQUNBLEdBQUdBLENBQUNBLGNBQVVBLENBQUNBLENBQUNBO0lBRTdDQSxnQkFBWUEsR0FBR0Esa0JBQWtCQSxDQUFDQTtBQUMvQ0EsQ0FBQ0EsRUFQTSxHQUFHLEtBQUgsR0FBRyxRQU9UOztBQ3ZCRCwyREFBMkQ7QUFDM0QsNERBQTREO0FBQzVELEdBQUc7QUFDSCxtRUFBbUU7QUFDbkUsb0VBQW9FO0FBQ3BF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QUcsQ0FBQyxDQUFDLEdBQUcsQ0FBQyxDQUFDLEdBQUMsQ0FBQyxFQUFFLENBQUMsR0FBRyxNQUFNLENBQUMsVUFBVSxDQUFDLE1BQU0sRUFBRSxDQUFDLEVBQUUsRUFBRSxDQUFDO3dCQUNoRCxFQUFFLENBQUMsQ0FBQyxNQUFNLENBQUMsVUFBVSxDQUFDLENBQUMsQ0FBQyxDQUFDLEdBQUcsR0FBRyxXQUFXLENBQUMsQ0FBQyxDQUFDOzRCQUMzQyxXQUFXLEdBQUcsTUFBTSxDQUFDLFVBQVUsQ0FBQyxDQUFDLENBQUMsQ0FBQyxHQUFHLENBQUM7d0JBQ3pDLENBQUM7b0JBQ0gsQ0FBQztvQkFDRCxFQUFFLENBQUMsQ0FBQyxXQUFXLEdBQUcsQ0FBQyxDQUFDLENBQUMsQ0FBQzt3QkFDcEIsTUFBTSxDQUFDLFFBQVEsQ0FBQyxVQUFVLEdBQUcsQ0FBRSxNQUFNLENBQUMsTUFBTSxDQUFDLGlCQUFpQixHQUFHLFdBQVcsQ0FBRSxHQUFHLEdBQUcsQ0FBQztvQkFDdkYsQ0FBQztnQkFDSCxDQUFDO1lBQ0gsQ0FBQyxDQUFDQTtZQUVGQSxNQUFNQSxDQUFDQSxZQUFZQSxHQUFHQTtnQkFDcEIsTUFBTSxDQUFDLE1BQU0sRUFBRSxDQUFDO1lBQ2xCLENBQUMsQ0FBQ0E7WUFFRkEsTUFBTUEsQ0FBQ0EsZUFBZUEsR0FBR0E7Z0JBQ3ZCLE1BQU0sQ0FBQyxJQUFJLElBQUksRUFBRSxDQUFDO1lBQ3BCLENBQUMsQ0FBQ0E7UUFFSkEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7QUFDTkEsQ0FBQ0EsRUF4UE0sR0FBRyxLQUFILEdBQUcsUUF3UFQiLCJmaWxlIjoiY29tcGlsZWQuanMiLCJzb3VyY2VzQ29udGVudCI6WyIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cIi4uL2xpYnMvaGF3dGlvLXV0aWxpdGllcy9kZWZzLmQudHNcIi8+XG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cIi4uLy4uL2luY2x1ZGVzLnRzXCIvPlxubW9kdWxlIEJUTSB7XG5cbiAgZXhwb3J0IHZhciBwbHVnaW5OYW1lID0gXCJoYXd0aW8tYXNzZW1ibHlcIjtcblxuICBleHBvcnQgdmFyIGxvZzogTG9nZ2luZy5Mb2dnZXIgPSBMb2dnZXIuZ2V0KHBsdWdpbk5hbWUpO1xuXG4gIGV4cG9ydCB2YXIgdGVtcGxhdGVQYXRoID0gXCJwbHVnaW5zL2J0bS9odG1sXCI7XG59XG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cIi4uLy4uL2luY2x1ZGVzLnRzXCIvPlxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cImJ0bUdsb2JhbHMudHNcIi8+XG5tb2R1bGUgQlRNIHtcblxuICBleHBvcnQgdmFyIF9tb2R1bGUgPSBhbmd1bGFyLm1vZHVsZShCVE0ucGx1Z2luTmFtZSwgW1wieGVkaXRhYmxlXCIsXCJ1aS5ib290c3RyYXBcIl0pO1xuXG4gIHZhciB0YWIgPSB1bmRlZmluZWQ7XG5cbiAgX21vZHVsZS5jb25maWcoW1wiJGxvY2F0aW9uUHJvdmlkZXJcIiwgXCIkcm91dGVQcm92aWRlclwiLCBcIkhhd3Rpb05hdkJ1aWxkZXJQcm92aWRlclwiLFxuICAgICgkbG9jYXRpb25Qcm92aWRlciwgJHJvdXRlUHJvdmlkZXI6IG5nLnJvdXRlLklSb3V0ZVByb3ZpZGVyLCBidWlsZGVyOiBIYXd0aW9NYWluTmF2LkJ1aWxkZXJGYWN0b3J5KSA9PiB7XG4gICAgdGFiID0gYnVpbGRlci5jcmVhdGUoKVxuICAgICAgLmlkKEJUTS5wbHVnaW5OYW1lKVxuICAgICAgLnRpdGxlKCgpID0+IFwiQnVzaW5lc3MgVHJhbnNhY3Rpb25zXCIpXG4gICAgICAuaHJlZigoKSA9PiBcIi9cIilcbiAgICAgIC5idWlsZCgpO1xuICAgIGJ1aWxkZXIuY29uZmlndXJlUm91dGluZygkcm91dGVQcm92aWRlciwgdGFiKTtcbiAgICAkbG9jYXRpb25Qcm92aWRlci5odG1sNU1vZGUodHJ1ZSk7XG4gICAgJHJvdXRlUHJvdmlkZXIuXG4gICAgICB3aGVuKCcvJywge1xuICAgICAgICB0ZW1wbGF0ZVVybDogJ3BsdWdpbnMvYnRtL2h0bWwvYnRtLmh0bWwnLFxuICAgICAgICBjb250cm9sbGVyOiAnQlRNLkJUTUNvbnRyb2xsZXInXG4gICAgICB9KS5cbiAgICAgIHdoZW4oJy9hY3RpdmUnLCB7XG4gICAgICAgIHRlbXBsYXRlVXJsOiAncGx1Z2lucy9idG0vaHRtbC9idG0uaHRtbCcsXG4gICAgICAgIGNvbnRyb2xsZXI6ICdCVE0uQlRNQ29udHJvbGxlcidcbiAgICAgIH0pLlxuICAgICAgd2hlbignL2NhbmRpZGF0ZXMnLCB7XG4gICAgICAgIHRlbXBsYXRlVXJsOiAncGx1Z2lucy9idG0vaHRtbC9idHhuY2FuZGlkYXRlcy5odG1sJyxcbiAgICAgICAgY29udHJvbGxlcjogJ0JUTS5CVE1DYW5kaWRhdGVzQ29udHJvbGxlcidcbiAgICAgIH0pLlxuICAgICAgd2hlbignL2Rpc2FibGVkJywge1xuICAgICAgICB0ZW1wbGF0ZVVybDogJ3BsdWdpbnMvYnRtL2h0bWwvYnR4bmRpc2FibGVkLmh0bWwnLFxuICAgICAgICBjb250cm9sbGVyOiAnQlRNLkJUTURpc2FibGVkQ29udHJvbGxlcidcbiAgICAgIH0pLlxuICAgICAgd2hlbignL2lnbm9yZWQnLCB7XG4gICAgICAgIHRlbXBsYXRlVXJsOiAncGx1Z2lucy9idG0vaHRtbC9idHhuaWdub3JlZC5odG1sJyxcbiAgICAgICAgY29udHJvbGxlcjogJ0JUTS5CVE1JZ25vcmVkQ29udHJvbGxlcidcbiAgICAgIH0pLlxuICAgICAgd2hlbignL2NvbmZpZy86YnVzaW5lc3N0cmFuc2FjdGlvbicsIHtcbiAgICAgICAgdGVtcGxhdGVVcmw6ICdwbHVnaW5zL2J0bS9odG1sL2J0eG5jb25maWcuaHRtbCcsXG4gICAgICAgIGNvbnRyb2xsZXI6ICdCVE0uQlR4bkNvbmZpZ0NvbnRyb2xsZXInXG4gICAgICB9KS5cbiAgICAgIHdoZW4oJy9pbmZvLzpidXNpbmVzc3RyYW5zYWN0aW9uJywge1xuICAgICAgICB0ZW1wbGF0ZVVybDogJ3BsdWdpbnMvYnRtL2h0bWwvYnR4bmluZm8uaHRtbCcsXG4gICAgICAgIGNvbnRyb2xsZXI6ICdCVE0uQlR4bkluZm9Db250cm9sbGVyJ1xuICAgICAgfSk7XG4gIH1dKTtcblxuICBfbW9kdWxlLnJ1bihmdW5jdGlvbigkaHR0cCwkbG9jYXRpb24pIHtcbiAgICAvLyBPbmx5IHNldCBhdXRob3JpemF0aW9uIGlmIHVzaW5nIGRldmVsb3BtZW50IFVSTFxuICAgIGlmICgkbG9jYXRpb24uYWJzVXJsKCkuaW5kZXhPZignaHR0cDovL2xvY2FsaG9zdDoyNzcyLycpID09PSAwKSB7XG4gICAgICAkaHR0cC5kZWZhdWx0cy5oZWFkZXJzLmNvbW1vbi5BdXRob3JpemF0aW9uID0gJ0Jhc2ljIGFtUnZaVHB3WVhOemQyOXlaQT09JztcbiAgICB9XG4gIH0pO1xuXG4gIF9tb2R1bGUucnVuKGZ1bmN0aW9uKGVkaXRhYmxlT3B0aW9ucykge1xuICAgIGVkaXRhYmxlT3B0aW9ucy50aGVtZSA9ICdiczMnOyAvLyBib290c3RyYXAzIHRoZW1lLiBDYW4gYmUgYWxzbyAnYnMyJywgJ2RlZmF1bHQnXG4gIH0pO1xuXG4gIF9tb2R1bGUucnVuKFtcIkhhd3Rpb05hdlwiLCAoSGF3dGlvTmF2OiBIYXd0aW9NYWluTmF2LlJlZ2lzdHJ5KSA9PiB7XG4gICAgSGF3dGlvTmF2LmFkZCh0YWIpO1xuICAgIGxvZy5kZWJ1ZyhcImxvYWRlZFwiKTtcbiAgfV0pO1xuXG4gIGhhd3Rpb1BsdWdpbkxvYWRlci5hZGRNb2R1bGUoQlRNLnBsdWdpbk5hbWUpO1xufVxuIiwiLy8vIENvcHlyaWdodCAyMDE0LTIwMTUgUmVkIEhhdCwgSW5jLiBhbmQvb3IgaXRzIGFmZmlsaWF0ZXNcbi8vLyBhbmQgb3RoZXIgY29udHJpYnV0b3JzIGFzIGluZGljYXRlZCBieSB0aGUgQGF1dGhvciB0YWdzLlxuLy8vXG4vLy8gTGljZW5zZWQgdW5kZXIgdGhlIEFwYWNoZSBMaWNlbnNlLCBWZXJzaW9uIDIuMCAodGhlIFwiTGljZW5zZVwiKTtcbi8vLyB5b3UgbWF5IG5vdCB1c2UgdGhpcyBmaWxlIGV4Y2VwdCBpbiBjb21wbGlhbmNlIHdpdGggdGhlIExpY2Vuc2UuXG4vLy8gWW91IG1heSBvYnRhaW4gYSBjb3B5IG9mIHRoZSBMaWNlbnNlIGF0XG4vLy9cbi8vLyAgIGh0dHA6Ly93d3cuYXBhY2hlLm9yZy9saWNlbnNlcy9MSUNFTlNFLTIuMFxuLy8vXG4vLy8gVW5sZXNzIHJlcXVpcmVkIGJ5IGFwcGxpY2FibGUgbGF3IG9yIGFncmVlZCB0byBpbiB3cml0aW5nLCBzb2Z0d2FyZVxuLy8vIGRpc3RyaWJ1dGVkIHVuZGVyIHRoZSBMaWNlbnNlIGlzIGRpc3RyaWJ1dGVkIG9uIGFuIFwiQVMgSVNcIiBCQVNJUyxcbi8vLyBXSVRIT1VUIFdBUlJBTlRJRVMgT1IgQ09ORElUSU9OUyBPRiBBTlkgS0lORCwgZWl0aGVyIGV4cHJlc3Mgb3IgaW1wbGllZC5cbi8vLyBTZWUgdGhlIExpY2Vuc2UgZm9yIHRoZSBzcGVjaWZpYyBsYW5ndWFnZSBnb3Zlcm5pbmcgcGVybWlzc2lvbnMgYW5kXG4vLy8gbGltaXRhdGlvbnMgdW5kZXIgdGhlIExpY2Vuc2UuXG5cbi8vLyA8cmVmZXJlbmNlIHBhdGg9XCJidG1QbHVnaW4udHNcIi8+XG5tb2R1bGUgQlRNIHtcblxuICBleHBvcnQgdmFyIEJUTUNvbnRyb2xsZXIgPSBfbW9kdWxlLmNvbnRyb2xsZXIoXCJCVE0uQlRNQ29udHJvbGxlclwiLCBbXCIkc2NvcGVcIiwgXCIkaHR0cFwiLCAnJGxvY2F0aW9uJywgJyRpbnRlcnZhbCcsICgkc2NvcGUsICRodHRwLCAkbG9jYXRpb24sICRpbnRlcnZhbCkgPT4ge1xuXG4gICAgJHNjb3BlLm5ld0JUeG5OYW1lID0gJyc7XG4gICAgJHNjb3BlLmNhbmRpZGF0ZUNvdW50ID0gMDtcblxuICAgICRzY29wZS5yZWxvYWQgPSBmdW5jdGlvbigpIHtcbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG5zdW1tYXJ5JykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucyA9IFtdO1xuICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IHJlc3AuZGF0YS5sZW5ndGg7IGkrKykge1xuICAgICAgICAgIHZhciBidHhuID0ge1xuICAgICAgICAgICAgc3VtbWFyeTogcmVzcC5kYXRhW2ldLFxuICAgICAgICAgICAgY291bnQ6IHVuZGVmaW5lZCxcbiAgICAgICAgICAgIGZhdWx0Y291bnQ6IHVuZGVmaW5lZCxcbiAgICAgICAgICAgIHBlcmNlbnRpbGU5NTogdW5kZWZpbmVkLFxuICAgICAgICAgICAgYWxlcnRzOiB1bmRlZmluZWRcbiAgICAgICAgICB9O1xuICAgICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucy5hZGQoYnR4bik7XG5cbiAgICAgICAgICAkc2NvcGUuZ2V0QnVzaW5lc3NUeG5EZXRhaWxzKGJ0eG4pO1xuICAgICAgICB9XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGJ1c2luZXNzIHR4biBzdW1tYXJpZXM6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG5cbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYnVzaW5lc3N0eG4vdW5ib3VuZHVyaXMnKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgJHNjb3BlLmNhbmRpZGF0ZUNvdW50ID0gT2JqZWN0LmtleXMocmVzcC5kYXRhKS5sZW5ndGg7XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGNhbmRpZGF0ZSBjb3VudDogXCIrcmVzcCk7XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbG9hZCgpO1xuXG4gICAgJGludGVydmFsKGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLnJlbG9hZCgpO1xuICAgIH0sMTAwMDApO1xuXG4gICAgJHNjb3BlLmdldEJ1c2luZXNzVHhuRGV0YWlscyA9IGZ1bmN0aW9uKGJ0eG4pIHtcbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYnVzaW5lc3N0eG4vY29tcGxldGlvbi9jb3VudD9idXNpbmVzc1RyYW5zYWN0aW9uPScrYnR4bi5zdW1tYXJ5Lm5hbWUpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBidHhuLmNvdW50ID0gcmVzcC5kYXRhO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBjb3VudDogXCIrcmVzcCk7XG4gICAgICB9KTtcblxuICAgICAgJGh0dHAuZ2V0KCcvaGF3a3VsYXIvYnRtL2FuYWx5dGljcy9idXNpbmVzc3R4bi9jb21wbGV0aW9uL3BlcmNlbnRpbGVzP2J1c2luZXNzVHJhbnNhY3Rpb249JytidHhuLnN1bW1hcnkubmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGlmIChyZXNwLmRhdGEucGVyY2VudGlsZXNbOTVdID4gMCkge1xuICAgICAgICAgIGJ0eG4ucGVyY2VudGlsZTk1ID0gTWF0aC5yb3VuZCggcmVzcC5kYXRhLnBlcmNlbnRpbGVzWzk1XSAvIDEwMDAwMDAgKSAvIDEwMDA7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgYnR4bi5wZXJjZW50aWxlOTUgPSAwO1xuICAgICAgICB9XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IGNvbXBsZXRpb24gcGVyY2VudGlsZXM6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG5cbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYnVzaW5lc3N0eG4vY29tcGxldGlvbi9mYXVsdGNvdW50P2J1c2luZXNzVHJhbnNhY3Rpb249JytidHhuLnN1bW1hcnkubmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGJ0eG4uZmF1bHRjb3VudCA9IHJlc3AuZGF0YTtcbiAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgZmF1bHQgY291bnQ6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG5cbiAgICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9hbmFseXRpY3MvYWxlcnRzL2NvdW50LycrYnR4bi5zdW1tYXJ5Lm5hbWUpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBidHhuLmFsZXJ0cyA9IHJlc3AuZGF0YTtcbiAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgYWxlcnRzIGNvdW50OiBcIityZXNwKTtcbiAgICAgIH0pO1xuICAgIH07XG5cbiAgICAkc2NvcGUuZGVsZXRlQnVzaW5lc3NUeG4gPSBmdW5jdGlvbihidHhuKSB7XG4gICAgICBpZiAoY29uZmlybSgnQXJlIHlvdSBzdXJlIHlvdSB3YW50IHRvIGRlbGV0ZSBidXNpbmVzcyB0cmFuc2FjdGlvbiBcXFwiJytidHhuLnN1bW1hcnkubmFtZSsnXFxcIj8nKSkge1xuICAgICAgICAkaHR0cC5kZWxldGUoJy9oYXdrdWxhci9idG0vY29uZmlnL2J1c2luZXNzdHhuLycrYnR4bi5zdW1tYXJ5Lm5hbWUpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAgIGNvbnNvbGUubG9nKCdEZWxldGVkOiAnK2J0eG4uc3VtbWFyeS5uYW1lKTtcbiAgICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMucmVtb3ZlKGJ0eG4pO1xuICAgICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBkZWxldGUgYnVzaW5lc3MgdHhuICdcIitidHhuLnN1bW1hcnkubmFtZStcIic6IFwiK3Jlc3ApO1xuICAgICAgICB9KTtcbiAgICAgIH1cbiAgICB9O1xuXG4gIH1dKTtcblxufVxuIiwiLy8vIENvcHlyaWdodCAyMDE0LTIwMTUgUmVkIEhhdCwgSW5jLiBhbmQvb3IgaXRzIGFmZmlsaWF0ZXNcbi8vLyBhbmQgb3RoZXIgY29udHJpYnV0b3JzIGFzIGluZGljYXRlZCBieSB0aGUgQGF1dGhvciB0YWdzLlxuLy8vXG4vLy8gTGljZW5zZWQgdW5kZXIgdGhlIEFwYWNoZSBMaWNlbnNlLCBWZXJzaW9uIDIuMCAodGhlIFwiTGljZW5zZVwiKTtcbi8vLyB5b3UgbWF5IG5vdCB1c2UgdGhpcyBmaWxlIGV4Y2VwdCBpbiBjb21wbGlhbmNlIHdpdGggdGhlIExpY2Vuc2UuXG4vLy8gWW91IG1heSBvYnRhaW4gYSBjb3B5IG9mIHRoZSBMaWNlbnNlIGF0XG4vLy9cbi8vLyAgIGh0dHA6Ly93d3cuYXBhY2hlLm9yZy9saWNlbnNlcy9MSUNFTlNFLTIuMFxuLy8vXG4vLy8gVW5sZXNzIHJlcXVpcmVkIGJ5IGFwcGxpY2FibGUgbGF3IG9yIGFncmVlZCB0byBpbiB3cml0aW5nLCBzb2Z0d2FyZVxuLy8vIGRpc3RyaWJ1dGVkIHVuZGVyIHRoZSBMaWNlbnNlIGlzIGRpc3RyaWJ1dGVkIG9uIGFuIFwiQVMgSVNcIiBCQVNJUyxcbi8vLyBXSVRIT1VUIFdBUlJBTlRJRVMgT1IgQ09ORElUSU9OUyBPRiBBTlkgS0lORCwgZWl0aGVyIGV4cHJlc3Mgb3IgaW1wbGllZC5cbi8vLyBTZWUgdGhlIExpY2Vuc2UgZm9yIHRoZSBzcGVjaWZpYyBsYW5ndWFnZSBnb3Zlcm5pbmcgcGVybWlzc2lvbnMgYW5kXG4vLy8gbGltaXRhdGlvbnMgdW5kZXIgdGhlIExpY2Vuc2UuXG5cbi8vLyA8cmVmZXJlbmNlIHBhdGg9XCJidG1QbHVnaW4udHNcIi8+XG5tb2R1bGUgQlRNIHtcblxuICBleHBvcnQgdmFyIEJUTUNhbmRpZGF0ZXNDb250cm9sbGVyID0gX21vZHVsZS5jb250cm9sbGVyKFwiQlRNLkJUTUNhbmRpZGF0ZXNDb250cm9sbGVyXCIsIFtcIiRzY29wZVwiLCBcIiRodHRwXCIsICckbG9jYXRpb24nLCAnJHVpYk1vZGFsJywgJyRpbnRlcnZhbCcsICgkc2NvcGUsICRodHRwLCAkbG9jYXRpb24sICR1aWJNb2RhbCwgJGludGVydmFsKSA9PiB7XG5cbiAgICAkc2NvcGUubmV3QlR4bk5hbWUgPSAnJztcbiAgICAkc2NvcGUuZXhpc3RpbmdCVHhuTmFtZSA9ICcnO1xuICAgICRzY29wZS5zZWxlY3RlZHVyaXMgPSBbIF07XG4gICAgJHNjb3BlLmNhbmRpZGF0ZUNvdW50ID0gMDtcblxuICAgICRodHRwLmdldCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG5zdW1tYXJ5JykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMgPSByZXNwLmRhdGE7XG4gICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgYnVzaW5lc3MgdHhuIHN1bW1hcmllczogXCIrcmVzcCk7XG4gICAgfSk7XG5cbiAgICAkc2NvcGUucmVsb2FkID0gZnVuY3Rpb24oKSB7XG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL3VuYm91bmR1cmlzJykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS51bmJvdW5kdXJpcyA9IHJlc3AuZGF0YTtcbiAgICAgICAgJHNjb3BlLmNhbmRpZGF0ZUNvdW50ID0gT2JqZWN0LmtleXMocmVzcC5kYXRhKS5sZW5ndGg7XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IHVuYm91bmQgVVJJczogXCIrcmVzcCk7XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbG9hZCgpO1xuXG4gICAgJGludGVydmFsKGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLnJlbG9hZCgpO1xuICAgIH0sMTAwMDApO1xuXG4gICAgJHNjb3BlLmFkZEJ1c2luZXNzVHhuID0gZnVuY3Rpb24oKSB7XG4gICAgICB2YXIgZGVmbiA9IHtcbiAgICAgICAgZmlsdGVyOiB7XG4gICAgICAgICAgaW5jbHVzaW9uczogJHNjb3BlLnNlbGVjdGVkdXJpc1xuICAgICAgICB9XG4gICAgICB9O1xuICAgICAgJGh0dHAucHV0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5uZXdCVHhuTmFtZSwgZGVmbikudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRsb2NhdGlvbi5wYXRoKCdjb25maWcvJyskc2NvcGUubmV3QlR4bk5hbWUpO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGFkZCBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5uZXdCVHhuTmFtZStcIic6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS5pZ25vcmVCdXNpbmVzc1R4biA9IGZ1bmN0aW9uKCkge1xuICAgICAgdmFyIGRlZm4gPSB7XG4gICAgICAgIGxldmVsOiAnSWdub3JlJyxcbiAgICAgICAgZmlsdGVyOiB7XG4gICAgICAgICAgaW5jbHVzaW9uczogJHNjb3BlLnNlbGVjdGVkdXJpc1xuICAgICAgICB9XG4gICAgICB9O1xuICAgICAgJGh0dHAucHV0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5uZXdCVHhuTmFtZSwgZGVmbikudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRsb2NhdGlvbi5wYXRoKCdjb25maWcvJyskc2NvcGUubmV3QlR4bk5hbWUpO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGlnbm9yZSBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5uZXdCVHhuTmFtZStcIic6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS51cGRhdGVCdXNpbmVzc1R4biA9IGZ1bmN0aW9uKCkge1xuICAgICAgJGh0dHAuZ2V0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgdmFyIGJ0eG4gPSByZXNwLmRhdGE7XG4gICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgJHNjb3BlLnNlbGVjdGVkdXJpcy5sZW5ndGg7IGkrKykge1xuICAgICAgICAgIGlmIChidHhuLmZpbHRlci5pbmNsdXNpb25zLmluZGV4T2YoJHNjb3BlLnNlbGVjdGVkdXJpc1tpXSkgPT09IC0xKSB7XG4gICAgICAgICAgICBidHhuLmZpbHRlci5pbmNsdXNpb25zLmFkZCgkc2NvcGUuc2VsZWN0ZWR1cmlzW2ldKTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgJGh0dHAucHV0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lLGJ0eG4pLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAgIGNvbnNvbGUubG9nKFwiU2F2ZWQgdXBkYXRlZCBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lK1wiJzogXCIrcmVzcCk7XG4gICAgICAgICAgJGxvY2F0aW9uLnBhdGgoJ2NvbmZpZy8nKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lKTtcbiAgICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gc2F2ZSBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lK1wiJzogXCIrcmVzcCk7XG4gICAgICAgIH0pO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5leGlzdGluZ0JUeG5OYW1lK1wiJzogXCIrcmVzcCk7XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnNlbGVjdGlvbkNoYW5nZWQgPSBmdW5jdGlvbih1cmkpIHtcbiAgICAgIHZhciByZWdleCA9ICRzY29wZS5lc2NhcGVSZWdFeHAodXJpKTtcbiAgICAgIGlmICgkc2NvcGUuc2VsZWN0ZWR1cmlzLmNvbnRhaW5zKHJlZ2V4KSkge1xuICAgICAgICAkc2NvcGUuc2VsZWN0ZWR1cmlzLnJlbW92ZShyZWdleCk7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICAkc2NvcGUuc2VsZWN0ZWR1cmlzLmFkZChyZWdleCk7XG4gICAgICB9XG4gICAgfTtcbiAgICBcbiAgICAkc2NvcGUuaXNTZWxlY3RlZCA9IGZ1bmN0aW9uKHVyaSkge1xuICAgICAgdmFyIHJlZ2V4ID0gJHNjb3BlLmVzY2FwZVJlZ0V4cCh1cmkpO1xuICAgICAgcmV0dXJuICRzY29wZS5zZWxlY3RlZHVyaXMuY29udGFpbnMocmVnZXgpO1xuICAgIH07XG4gICAgXG4gICAgJHNjb3BlLmdldExldmVsID0gZnVuY3Rpb24obGV2ZWwpIHtcbiAgICAgIGlmIChsZXZlbCA9PT0gJ0FsbCcpIHtcbiAgICAgICAgcmV0dXJuIFwiQWN0aXZlXCI7XG4gICAgICB9XG4gICAgICByZXR1cm4gbGV2ZWw7XG4gICAgfTtcblxuICAgICRzY29wZS5lc2NhcGVSZWdFeHAgPSBmdW5jdGlvbihzdHIpIHtcbiAgICAgIHJldHVybiBcIl5cIiArIHN0ci5yZXBsYWNlKC9bXFwtXFxbXFxdXFwvXFx7XFx9XFwoXFwpXFwqXFwrXFw/XFwuXFxcXFxcXlxcJFxcfF0vZywgXCJcXFxcJCZcIikgKyBcIiRcIjtcbiAgICB9O1xuXG4gIH1dKTtcblxufVxuXG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cImJ0bVBsdWdpbi50c1wiLz5cbm1vZHVsZSBCVE0ge1xuXG4gIGV4cG9ydCB2YXIgQlR4bkNvbmZpZ0NvbnRyb2xsZXIgPSBfbW9kdWxlLmNvbnRyb2xsZXIoXCJCVE0uQlR4bkNvbmZpZ0NvbnRyb2xsZXJcIiwgW1wiJHNjb3BlXCIsIFwiJHJvdXRlUGFyYW1zXCIsIFwiJGh0dHBcIiwgJyRsb2NhdGlvbicsICckaW50ZXJ2YWwnLCAoJHNjb3BlLCAkcm91dGVQYXJhbXMsICRodHRwLCAkbG9jYXRpb24sICRpbnRlcnZhbCkgPT4ge1xuXG4gICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lID0gJHJvdXRlUGFyYW1zLmJ1c2luZXNzdHJhbnNhY3Rpb247XG4gICAgJHNjb3BlLmRpcnR5ID0gZmFsc2U7XG5cbiAgICAkc2NvcGUubmV3SW5jbHVzaW9uRmlsdGVyID0gJyc7XG4gICAgJHNjb3BlLm5ld0V4Y2x1c2lvbkZpbHRlciA9ICcnO1xuXG4gICAgJHNjb3BlLm1lc3NhZ2VzID0gW107XG5cbiAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vY29uZmlnL2J1c2luZXNzdHhuLycrJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uID0gcmVzcC5kYXRhO1xuICAgICAgJHNjb3BlLm9yaWdpbmFsID0gYW5ndWxhci5jb3B5KCRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uKTtcblxuICAgICAgJGh0dHAucG9zdCgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG4vdmFsaWRhdGUnLCRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgJHNjb3BlLm1lc3NhZ2VzID0gcmVzcC5kYXRhO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIHZhbGlkYXRlIGJ1c2luZXNzIHR4biAnXCIrJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lK1wiJzogXCIrSlNPTi5zdHJpbmdpZnkocmVzcCkpO1xuICAgICAgfSk7XG4gICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgYnVzaW5lc3MgdHhuICdcIiskc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbk5hbWUrXCInOiBcIitKU09OLnN0cmluZ2lmeShyZXNwKSk7XG4gICAgfSk7XG5cbiAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL3VuYm91bmR1cmlzJykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAkc2NvcGUudW5ib3VuZFVSSXMgPSBbIF07XG4gICAgICBmb3IgKHZhciBpPTA7IGkgPCByZXNwLmRhdGEubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgdmFyIHJlZ2V4ID0gJHNjb3BlLmVzY2FwZVJlZ0V4cChyZXNwLmRhdGFbaV0udXJpKTtcbiAgICAgICAgaWYgKHJlZ2V4ICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgICAkc2NvcGUudW5ib3VuZFVSSXMuYWRkKHJlZ2V4KTtcbiAgICAgICAgfVxuICAgICAgfVxuICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gZ2V0IHVuYm91bmQgVVJJczogXCIrcmVzcCk7XG4gICAgfSk7XG5cbiAgICAkc2NvcGUucmVsb2FkID0gZnVuY3Rpb24oKSB7XG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL2JvdW5kdXJpcy8nKyRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uTmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5ib3VuZFVSSXMgPSBbIF07XG4gICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgcmVzcC5kYXRhLmxlbmd0aDsgaSsrKSB7XG4gICAgICAgICAgdmFyIHJlZ2V4ID0gJHNjb3BlLmVzY2FwZVJlZ0V4cChyZXNwLmRhdGFbaV0pO1xuICAgICAgICAgICRzY29wZS5ib3VuZFVSSXMuYWRkKHJlZ2V4KTtcbiAgICAgICAgfVxuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBib3VuZCBVUklzIGZvciBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uTmFtZStcIic6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgIH0pO1xuICAgIH07XG5cbiAgICAkc2NvcGUucmVsb2FkKCk7XG5cbiAgICAkaW50ZXJ2YWwoZnVuY3Rpb24oKSB7XG4gICAgICAkc2NvcGUucmVsb2FkKCk7XG4gICAgfSwxMDAwMCk7XG5cbiAgICAkc2NvcGUuYWRkSW5jbHVzaW9uRmlsdGVyID0gZnVuY3Rpb24oKSB7XG4gICAgICBjb25zb2xlLmxvZygnQWRkIGluY2x1c2lvbiBmaWx0ZXI6ICcrJHNjb3BlLm5ld0luY2x1c2lvbkZpbHRlcik7XG4gICAgICBpZiAoJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24uZmlsdGVyID09PSBudWxsKSB7XG4gICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uLmZpbHRlciA9IHtcbiAgICAgICAgICBpbmNsdXNpb25zOiBbXSxcbiAgICAgICAgICBleGNsdXNpb25zOiBbXVxuICAgICAgICB9O1xuICAgICAgfVxuICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24uZmlsdGVyLmluY2x1c2lvbnMuYWRkKCRzY29wZS5uZXdJbmNsdXNpb25GaWx0ZXIpO1xuICAgICAgJHNjb3BlLnNldERpcnR5KCk7XG4gICAgICAkc2NvcGUubmV3SW5jbHVzaW9uRmlsdGVyID0gJyc7XG4gICAgfTtcblxuICAgICRzY29wZS5yZW1vdmVJbmNsdXNpb25GaWx0ZXIgPSBmdW5jdGlvbihpbmNsdXNpb24pIHtcbiAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uLmZpbHRlci5pbmNsdXNpb25zLnJlbW92ZShpbmNsdXNpb24pO1xuICAgICAgJHNjb3BlLnNldERpcnR5KCk7XG4gICAgfTtcblxuICAgICRzY29wZS5hZGRFeGNsdXNpb25GaWx0ZXIgPSBmdW5jdGlvbigpIHtcbiAgICAgIGNvbnNvbGUubG9nKCdBZGQgZXhjbHVzaW9uIGZpbHRlcjogJyskc2NvcGUubmV3RXhjbHVzaW9uRmlsdGVyKTtcbiAgICAgIGlmICgkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbi5maWx0ZXIgPT09IG51bGwpIHtcbiAgICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24uZmlsdGVyID0ge1xuICAgICAgICAgIGluY2x1c2lvbnM6IFtdLFxuICAgICAgICAgIGV4Y2x1c2lvbnM6IFtdXG4gICAgICAgIH07XG4gICAgICB9XG4gICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbi5maWx0ZXIuZXhjbHVzaW9ucy5hZGQoJHNjb3BlLm5ld0V4Y2x1c2lvbkZpbHRlcik7XG4gICAgICAkc2NvcGUuc2V0RGlydHkoKTtcbiAgICAgICRzY29wZS5uZXdFeGNsdXNpb25GaWx0ZXIgPSAnJztcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbW92ZUV4Y2x1c2lvbkZpbHRlciA9IGZ1bmN0aW9uKGV4Y2x1c2lvbikge1xuICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24uZmlsdGVyLmV4Y2x1c2lvbnMucmVtb3ZlKGV4Y2x1c2lvbik7XG4gICAgICAkc2NvcGUuc2V0RGlydHkoKTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLmdldEV4cHJlc3Npb25UZXh0ID0gZnVuY3Rpb24oZXhwcmVzc2lvbikge1xuICAgICAgaWYgKGV4cHJlc3Npb24gPT09IHVuZGVmaW5lZCkge1xuICAgICAgICByZXR1cm4gXCJcIjtcbiAgICAgIH1cbiAgICAgIGlmIChleHByZXNzaW9uLnR5cGUgPT09IFwiWE1MXCIpIHtcbiAgICAgICAgcmV0dXJuIGV4cHJlc3Npb24uc291cmNlICsgXCJbXCIgKyBleHByZXNzaW9uLmtleSArIFwiXVwiICsgXCIgeHBhdGg9XCIgKyBleHByZXNzaW9uLnhwYXRoO1xuICAgICAgfVxuICAgICAgaWYgKGV4cHJlc3Npb24udHlwZSA9PT0gXCJKU09OXCIpIHtcbiAgICAgICAgcmV0dXJuIGV4cHJlc3Npb24uc291cmNlICsgXCJbXCIgKyBleHByZXNzaW9uLmtleSArIFwiXVwiICsgXCIganNvbnBhdGg9XCIgKyBleHByZXNzaW9uLmpzb25wYXRoO1xuICAgICAgfVxuICAgICAgaWYgKGV4cHJlc3Npb24udHlwZSA9PT0gXCJUZXh0XCIpIHtcbiAgICAgICAgcmV0dXJuIGV4cHJlc3Npb24uc291cmNlICsgXCJbXCIgKyBleHByZXNzaW9uLmtleSArIFwiXVwiO1xuICAgICAgfVxuICAgICAgcmV0dXJuIFwiVW5rbm93biBleHByZXNzaW9uIHR5cGVcIjtcbiAgICB9O1xuXG4gICAgJHNjb3BlLmNoYW5nZWRFeHByZXNzaW9uVHlwZSA9IGZ1bmN0aW9uKGV4cHJlc3Npb24pIHtcbiAgICAgICRzY29wZS5zZXREaXJ0eSgpO1xuICAgICAgZXhwcmVzc2lvbi5rZXkgPSB1bmRlZmluZWQ7XG4gICAgICBleHByZXNzaW9uLnNvdXJjZSA9IHVuZGVmaW5lZDtcbiAgICAgIGV4cHJlc3Npb24ueHBhdGggPSB1bmRlZmluZWQ7XG4gICAgICBleHByZXNzaW9uLmpzb25wYXRoID0gdW5kZWZpbmVkO1xuXG4gICAgICBpZiAoZXhwcmVzc2lvbi50eXBlID09PSAnWE1MJyB8fCBleHByZXNzaW9uLnR5cGUgPT09ICdKU09OJyB8fCBleHByZXNzaW9uLnR5cGUgPT09ICdUZXh0Jykge1xuICAgICAgICBleHByZXNzaW9uLmtleSA9ICcwJztcbiAgICAgICAgZXhwcmVzc2lvbi5zb3VyY2UgPSAnQ29udGVudCc7XG4gICAgICB9XG4gICAgfTtcblxuICAgICRzY29wZS5jaGFuZ2VkQWN0aW9uVHlwZSA9IGZ1bmN0aW9uKGFjdGlvbikge1xuICAgICAgJHNjb3BlLnNldERpcnR5KCk7XG4gICAgICBhY3Rpb24ubmFtZSA9IHVuZGVmaW5lZDtcbiAgICAgIGFjdGlvbi50eXBlID0gdW5kZWZpbmVkO1xuICAgICAgYWN0aW9uLnNjb3BlID0gdW5kZWZpbmVkO1xuICAgICAgYWN0aW9uLnRlbXBsYXRlID0gdW5kZWZpbmVkO1xuICAgICAgYWN0aW9uLnByZWRpY2F0ZSA9IHVuZGVmaW5lZDtcbiAgICAgIGFjdGlvbi5leHByZXNzaW9uID0gdW5kZWZpbmVkO1xuICAgIH07XG5cbiAgICAkc2NvcGUuYWRkUHJvY2Vzc29yID0gZnVuY3Rpb24oKSB7XG4gICAgICAkc2NvcGUuc2V0RGlydHkoKTtcbiAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uLnByb2Nlc3NvcnMuYWRkKHtcbiAgICAgICAgZGVzY3JpcHRpb246IFwiUHJvY2Vzc29yIFwiICsgKCRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uLnByb2Nlc3NvcnMubGVuZ3RoICsgMSksXG4gICAgICAgIG5vZGVUeXBlOiBcIkNvbnN1bWVyXCIsXG4gICAgICAgIGRpcmVjdGlvbjogXCJJblwiLFxuICAgICAgICBhY3Rpb25zOiBbXVxuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS5kZWxldGVQcm9jZXNzb3IgPSBmdW5jdGlvbihwcm9jZXNzb3IpIHtcbiAgICAgIGlmIChjb25maXJtKCdBcmUgeW91IHN1cmUgeW91IHdhbnQgdG8gZGVsZXRlIHRoZSBwcm9jZXNzb3I/JykpIHtcbiAgICAgICAgJHNjb3BlLnNldERpcnR5KCk7XG4gICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uLnByb2Nlc3NvcnMucmVtb3ZlKHByb2Nlc3Nvcik7XG4gICAgICB9XG4gICAgfTtcblxuICAgICRzY29wZS5hZGRBY3Rpb24gPSBmdW5jdGlvbihwcm9jZXNzb3IsIHR5cGUpIHtcbiAgICAgICRzY29wZS5zZXREaXJ0eSgpO1xuICAgICAgcHJvY2Vzc29yLmFjdGlvbnMuYWRkKHtcbiAgICAgICAgYWN0aW9uVHlwZTogdHlwZSxcbiAgICAgICAgZGVzY3JpcHRpb246IFwiQWN0aW9uIFwiICsgKHByb2Nlc3Nvci5hY3Rpb25zLmxlbmd0aCArIDEpXG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLmRlbGV0ZUFjdGlvbiA9IGZ1bmN0aW9uKHByb2Nlc3NvcixhY3Rpb24pIHtcbiAgICAgIGlmIChjb25maXJtKCdBcmUgeW91IHN1cmUgeW91IHdhbnQgdG8gZGVsZXRlIHRoZSBhY3Rpb24/JykpIHtcbiAgICAgICAgJHNjb3BlLnNldERpcnR5KCk7XG4gICAgICAgIHByb2Nlc3Nvci5hY3Rpb25zLnJlbW92ZShhY3Rpb24pO1xuICAgICAgfVxuICAgIH07XG5cbiAgICAkc2NvcGUuc2V0RGlydHkgPSBmdW5jdGlvbigpIHtcbiAgICAgICRzY29wZS5kaXJ0eSA9IHRydWU7XG4gICAgfTtcblxuICAgICRzY29wZS5yZXNldCA9IGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb24gPSBhbmd1bGFyLmNvcHkoJHNjb3BlLm9yaWdpbmFsKTtcbiAgICAgICRzY29wZS5kaXJ0eSA9IGZhbHNlO1xuICAgIH07XG5cbiAgICAkc2NvcGUuc2F2ZSA9IGZ1bmN0aW9uKCkge1xuICAgICAgJGh0dHAucHV0KCcvaGF3a3VsYXIvYnRtL2NvbmZpZy9idXNpbmVzc3R4bi8nKyRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uTmFtZSwkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbikudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5tZXNzYWdlcyA9IHJlc3AuZGF0YTtcbiAgICAgICAgJHNjb3BlLm9yaWdpbmFsID0gYW5ndWxhci5jb3B5KCRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uKTtcbiAgICAgICAgJHNjb3BlLmRpcnR5ID0gZmFsc2U7XG4gICAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICAgY29uc29sZS5sb2coXCJGYWlsZWQgdG8gc2F2ZSBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uTmFtZStcIic6IFwiK0pTT04uc3RyaW5naWZ5KHJlc3ApKTtcbiAgICAgIH0pO1xuICAgIH07XG5cbiAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vY29uZmlnL2J1c2luZXNzdHhuLycrJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25OYW1lKS50aGVuKGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uID0gcmVzcC5kYXRhO1xuICAgICAgJHNjb3BlLm9yaWdpbmFsID0gYW5ndWxhci5jb3B5KCRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uKTtcbiAgICB9LGZ1bmN0aW9uKHJlc3ApIHtcbiAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBidXNpbmVzcyB0eG4gJ1wiKyRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uTmFtZStcIic6IFwiK3Jlc3ApO1xuICAgIH0pO1xuXG4gICAgJHNjb3BlLmVzY2FwZVJlZ0V4cCA9IGZ1bmN0aW9uKHN0cikge1xuICAgICAgaWYgKHN0ciA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cbiAgICAgIHJldHVybiBcIl5cIiArIHN0ci5yZXBsYWNlKC9bXFwtXFxbXFxdXFwvXFx7XFx9XFwoXFwpXFwqXFwrXFw/XFwuXFxcXFxcXlxcJFxcfF0vZywgXCJcXFxcJCZcIikgKyBcIiRcIjtcbiAgICB9O1xuXG4gICAgJHNjb3BlLmNsb3NlTWVzc2FnZSA9IGZ1bmN0aW9uKGluZGV4KSB7XG4gICAgICAkc2NvcGUubWVzc2FnZXMuc3BsaWNlKGluZGV4LCAxKTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLmdldE1lc3NhZ2VUeXBlID0gZnVuY3Rpb24oZW50cnkpIHtcbiAgICAgIHZhciB0eXBlID0gJ2Rhbmdlcic7XG4gICAgICBpZiAoZW50cnkuc2V2ZXJpdHkgPT09ICdXYXJuaW5nJykge1xuICAgICAgICB0eXBlID0gJ3dhcm5pbmcnO1xuICAgICAgfSBlbHNlIGlmIChlbnRyeS5zZXZlcml0eSA9PT0gJ0luZm8nKSB7XG4gICAgICAgIHR5cGUgPSAnc3VjY2Vzcyc7XG4gICAgICB9XG4gICAgICByZXR1cm4gdHlwZTtcbiAgICB9O1xuICAgIFxuICAgICRzY29wZS5nZXRNZXNzYWdlVGV4dCA9IGZ1bmN0aW9uKGVudHJ5KSB7XG4gICAgICB2YXIgbWVzc2FnZSA9IFwiXCI7XG4gICAgICBpZiAoZW50cnkucHJvY2Vzc29yICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgbWVzc2FnZSA9IFwiW1wiICsgZW50cnkucHJvY2Vzc29yO1xuICAgICAgICBcbiAgICAgICAgaWYgKGVudHJ5LmFjdGlvbiAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgICAgbWVzc2FnZSA9IG1lc3NhZ2UgKyBcIi9cIiArIGVudHJ5LmFjdGlvbjtcbiAgICAgICAgfVxuICAgICAgICBcbiAgICAgICAgbWVzc2FnZSA9IG1lc3NhZ2UgKyBcIl0gXCI7XG4gICAgICB9XG4gICAgICBcbiAgICAgIG1lc3NhZ2UgPSBtZXNzYWdlICsgZW50cnkubWVzc2FnZTtcblxuICAgICAgcmV0dXJuIG1lc3NhZ2U7XG4gICAgfTtcblxuICAgICRzY29wZS5pc0Vycm9yID0gZnVuY3Rpb24ocHJvY2Vzc29yLGFjdGlvbixmaWVsZCkge1xuICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCAkc2NvcGUubWVzc2FnZXMubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgaWYgKCRzY29wZS5tZXNzYWdlc1tpXS5wcm9jZXNzb3IgPT09IHByb2Nlc3Nvci5kZXNjcmlwdGlvblxuICAgICAgICAgICAgJiYgJHNjb3BlLm1lc3NhZ2VzW2ldLmFjdGlvbiA9PT0gYWN0aW9uLmRlc2NyaXB0aW9uXG4gICAgICAgICAgICAmJiAkc2NvcGUubWVzc2FnZXNbaV0uZmllbGQgPT09IGZpZWxkKSB7XG4gICAgICAgICAgcmV0dXJuIHRydWU7XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICAgIHJldHVybiBmYWxzZTtcbiAgICB9O1xuXG4gIH1dKTtcblxufVxuIiwiLy8vIENvcHlyaWdodCAyMDE0LTIwMTUgUmVkIEhhdCwgSW5jLiBhbmQvb3IgaXRzIGFmZmlsaWF0ZXNcbi8vLyBhbmQgb3RoZXIgY29udHJpYnV0b3JzIGFzIGluZGljYXRlZCBieSB0aGUgQGF1dGhvciB0YWdzLlxuLy8vXG4vLy8gTGljZW5zZWQgdW5kZXIgdGhlIEFwYWNoZSBMaWNlbnNlLCBWZXJzaW9uIDIuMCAodGhlIFwiTGljZW5zZVwiKTtcbi8vLyB5b3UgbWF5IG5vdCB1c2UgdGhpcyBmaWxlIGV4Y2VwdCBpbiBjb21wbGlhbmNlIHdpdGggdGhlIExpY2Vuc2UuXG4vLy8gWW91IG1heSBvYnRhaW4gYSBjb3B5IG9mIHRoZSBMaWNlbnNlIGF0XG4vLy9cbi8vLyAgIGh0dHA6Ly93d3cuYXBhY2hlLm9yZy9saWNlbnNlcy9MSUNFTlNFLTIuMFxuLy8vXG4vLy8gVW5sZXNzIHJlcXVpcmVkIGJ5IGFwcGxpY2FibGUgbGF3IG9yIGFncmVlZCB0byBpbiB3cml0aW5nLCBzb2Z0d2FyZVxuLy8vIGRpc3RyaWJ1dGVkIHVuZGVyIHRoZSBMaWNlbnNlIGlzIGRpc3RyaWJ1dGVkIG9uIGFuIFwiQVMgSVNcIiBCQVNJUyxcbi8vLyBXSVRIT1VUIFdBUlJBTlRJRVMgT1IgQ09ORElUSU9OUyBPRiBBTlkgS0lORCwgZWl0aGVyIGV4cHJlc3Mgb3IgaW1wbGllZC5cbi8vLyBTZWUgdGhlIExpY2Vuc2UgZm9yIHRoZSBzcGVjaWZpYyBsYW5ndWFnZSBnb3Zlcm5pbmcgcGVybWlzc2lvbnMgYW5kXG4vLy8gbGltaXRhdGlvbnMgdW5kZXIgdGhlIExpY2Vuc2UuXG5cbi8vLyA8cmVmZXJlbmNlIHBhdGg9XCJidG1QbHVnaW4udHNcIi8+XG5tb2R1bGUgQlRNIHtcblxuICBleHBvcnQgdmFyIEJUTURpc2FibGVkQ29udHJvbGxlciA9IF9tb2R1bGUuY29udHJvbGxlcihcIkJUTS5CVE1EaXNhYmxlZENvbnRyb2xsZXJcIiwgW1wiJHNjb3BlXCIsIFwiJGh0dHBcIiwgJyRsb2NhdGlvbicsICckaW50ZXJ2YWwnLCAoJHNjb3BlLCAkaHR0cCwgJGxvY2F0aW9uLCAkaW50ZXJ2YWwpID0+IHtcblxuICAgICRzY29wZS5uZXdCVHhuTmFtZSA9ICcnO1xuICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IDA7XG5cbiAgICAkc2NvcGUucmVsb2FkID0gZnVuY3Rpb24oKSB7XG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vY29uZmlnL2J1c2luZXNzdHhuc3VtbWFyeScpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMgPSBbXTtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCByZXNwLmRhdGEubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgICB2YXIgYnR4biA9IHtcbiAgICAgICAgICAgIHN1bW1hcnk6IHJlc3AuZGF0YVtpXVxuICAgICAgICAgIH07XG4gICAgICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25zLmFkZChidHhuKTtcbiAgICAgICAgfVxuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBidXNpbmVzcyB0eG4gc3VtbWFyaWVzOiBcIityZXNwKTtcbiAgICAgIH0pO1xuXG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL3VuYm91bmR1cmlzJykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IE9iamVjdC5rZXlzKHJlc3AuZGF0YSkubGVuZ3RoO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBjYW5kaWRhdGUgY291bnQ6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS5yZWxvYWQoKTtcblxuICAgICRzY29wZS5kZWxldGVCdXNpbmVzc1R4biA9IGZ1bmN0aW9uKGJ0eG4pIHtcbiAgICAgIGlmIChjb25maXJtKCdBcmUgeW91IHN1cmUgeW91IHdhbnQgdG8gZGVsZXRlIGJ1c2luZXNzIHRyYW5zYWN0aW9uIFxcXCInK2J0eG4uc3VtbWFyeS5uYW1lKydcXFwiPycpKSB7XG4gICAgICAgICRodHRwLmRlbGV0ZSgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG4vJytidHhuLnN1bW1hcnkubmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICAgY29uc29sZS5sb2coJ0RlbGV0ZWQ6ICcrYnR4bi5zdW1tYXJ5Lm5hbWUpO1xuICAgICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucy5yZW1vdmUoYnR4bik7XG4gICAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGRlbGV0ZSBidXNpbmVzcyB0eG4gJ1wiK2J0eG4uc3VtbWFyeS5uYW1lK1wiJzogXCIrcmVzcCk7XG4gICAgICAgIH0pO1xuICAgICAgfVxuICAgIH07XG5cbiAgfV0pO1xuXG59XG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cImJ0bVBsdWdpbi50c1wiLz5cbm1vZHVsZSBCVE0ge1xuXG4gIGV4cG9ydCB2YXIgQlRNSWdub3JlZENvbnRyb2xsZXIgPSBfbW9kdWxlLmNvbnRyb2xsZXIoXCJCVE0uQlRNSWdub3JlZENvbnRyb2xsZXJcIiwgW1wiJHNjb3BlXCIsIFwiJGh0dHBcIiwgJyRsb2NhdGlvbicsICckaW50ZXJ2YWwnLCAoJHNjb3BlLCAkaHR0cCwgJGxvY2F0aW9uLCAkaW50ZXJ2YWwpID0+IHtcblxuICAgICRzY29wZS5uZXdCVHhuTmFtZSA9ICcnO1xuICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IDA7XG5cbiAgICAkc2NvcGUucmVsb2FkID0gZnVuY3Rpb24oKSB7XG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vY29uZmlnL2J1c2luZXNzdHhuc3VtbWFyeScpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbnMgPSBbXTtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCByZXNwLmRhdGEubGVuZ3RoOyBpKyspIHtcbiAgICAgICAgICB2YXIgYnR4biA9IHtcbiAgICAgICAgICAgIHN1bW1hcnk6IHJlc3AuZGF0YVtpXVxuICAgICAgICAgIH07XG4gICAgICAgICAgJHNjb3BlLmJ1c2luZXNzVHJhbnNhY3Rpb25zLmFkZChidHhuKTtcbiAgICAgICAgfVxuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBidXNpbmVzcyB0eG4gc3VtbWFyaWVzOiBcIityZXNwKTtcbiAgICAgIH0pO1xuXG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL3VuYm91bmR1cmlzJykudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5jYW5kaWRhdGVDb3VudCA9IE9iamVjdC5rZXlzKHJlc3AuZGF0YSkubGVuZ3RoO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBjYW5kaWRhdGUgY291bnQ6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS5yZWxvYWQoKTtcblxuICAgICRzY29wZS5kZWxldGVCdXNpbmVzc1R4biA9IGZ1bmN0aW9uKGJ0eG4pIHtcbiAgICAgIGlmIChjb25maXJtKCdBcmUgeW91IHN1cmUgeW91IHdhbnQgdG8gZGVsZXRlIGJ1c2luZXNzIHRyYW5zYWN0aW9uIFxcXCInK2J0eG4uc3VtbWFyeS5uYW1lKydcXFwiPycpKSB7XG4gICAgICAgICRodHRwLmRlbGV0ZSgnL2hhd2t1bGFyL2J0bS9jb25maWcvYnVzaW5lc3N0eG4vJytidHhuLnN1bW1hcnkubmFtZSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICAgY29uc29sZS5sb2coJ0RlbGV0ZWQ6ICcrYnR4bi5zdW1tYXJ5Lm5hbWUpO1xuICAgICAgICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9ucy5yZW1vdmUoYnR4bik7XG4gICAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGRlbGV0ZSBidXNpbmVzcyB0eG4gJ1wiK2J0eG4uc3VtbWFyeS5uYW1lK1wiJzogXCIrcmVzcCk7XG4gICAgICAgIH0pO1xuICAgICAgfVxuICAgIH07XG5cbiAgfV0pO1xuXG59XG4iLCIvLy8gQ29weXJpZ2h0IDIwMTQtMjAxNSBSZWQgSGF0LCBJbmMuIGFuZC9vciBpdHMgYWZmaWxpYXRlc1xuLy8vIGFuZCBvdGhlciBjb250cmlidXRvcnMgYXMgaW5kaWNhdGVkIGJ5IHRoZSBAYXV0aG9yIHRhZ3MuXG4vLy9cbi8vLyBMaWNlbnNlZCB1bmRlciB0aGUgQXBhY2hlIExpY2Vuc2UsIFZlcnNpb24gMi4wICh0aGUgXCJMaWNlbnNlXCIpO1xuLy8vIHlvdSBtYXkgbm90IHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgdGhlIExpY2Vuc2UgYXRcbi8vL1xuLy8vICAgaHR0cDovL3d3dy5hcGFjaGUub3JnL2xpY2Vuc2VzL0xJQ0VOU0UtMi4wXG4vLy9cbi8vLyBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4vLy8gZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gXCJBUyBJU1wiIEJBU0lTLFxuLy8vIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLlxuLy8vIFNlZSB0aGUgTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmRcbi8vLyBsaW1pdGF0aW9ucyB1bmRlciB0aGUgTGljZW5zZS5cblxuLy8vIDxyZWZlcmVuY2UgcGF0aD1cImJ0bVBsdWdpbi50c1wiLz5cbm1vZHVsZSBCVE0ge1xuXG4gIGRlY2xhcmUgdmFyIGMzOiBhbnk7XG5cbiAgZXhwb3J0IHZhciBCVHhuSW5mb0NvbnRyb2xsZXIgPSBfbW9kdWxlLmNvbnRyb2xsZXIoXCJCVE0uQlR4bkluZm9Db250cm9sbGVyXCIsIFtcIiRzY29wZVwiLCBcIiRyb3V0ZVBhcmFtc1wiLCBcIiRodHRwXCIsICckbG9jYXRpb24nLCAnJGludGVydmFsJywgKCRzY29wZSwgJHJvdXRlUGFyYW1zLCAkaHR0cCwgJGxvY2F0aW9uLCAkaW50ZXJ2YWwpID0+IHtcblxuICAgICRzY29wZS5idXNpbmVzc1RyYW5zYWN0aW9uTmFtZSA9ICRyb3V0ZVBhcmFtcy5idXNpbmVzc3RyYW5zYWN0aW9uO1xuXG4gICAgJHNjb3BlLnByb3BlcnRpZXMgPSBbXTtcbiAgICBcbiAgICAkc2NvcGUuY3JpdGVyaWEgPSB7XG4gICAgICBidXNpbmVzc1RyYW5zYWN0aW9uOiAkc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbk5hbWUsXG4gICAgICBwcm9wZXJ0aWVzOiBbXSxcbiAgICAgIGZhdWx0czogW10sXG4gICAgICBzdGFydFRpbWU6IC0zNjAwMDAwLFxuICAgICAgZW5kVGltZTogXCIwXCIsXG4gICAgICBsb3dlckJvdW5kOiAwXG4gICAgfTtcblxuICAgICRzY29wZS5jb25maWcgPSB7XG4gICAgICBpbnRlcnZhbDogNjAwMDAsXG4gICAgICBzZWxlY3RlZFByb3BlcnR5OiB1bmRlZmluZWQsXG4gICAgICBsb3dlckJvdW5kRGlzcGxheTogMCxcbiAgICAgIHByZXZMb3dlckJvdW5kRGlzcGxheTogMFxuICAgIH07XG5cbiAgICAkc2NvcGUucmVsb2FkID0gZnVuY3Rpb24oKSB7XG4gICAgICAkaHR0cC5wb3N0KCcvaGF3a3VsYXIvYnRtL2FuYWx5dGljcy9idXNpbmVzc3R4bi9jb21wbGV0aW9uL3N0YXRpc3RpY3M/aW50ZXJ2YWw9Jyskc2NvcGUuY29uZmlnLmludGVydmFsLCAkc2NvcGUuY3JpdGVyaWEpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAkc2NvcGUuc3RhdGlzdGljcyA9IHJlc3AuZGF0YTtcbiAgICAgICAgJHNjb3BlLnVwZGF0ZWRCb3VuZHMoKTtcbiAgICAgICAgJHNjb3BlLnJlZHJhd0xpbmVDaGFydCgpO1xuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBzdGF0aXN0aWNzOiBcIityZXNwKTtcbiAgICAgIH0pO1xuXG4gICAgICAkaHR0cC5wb3N0KCcvaGF3a3VsYXIvYnRtL2FuYWx5dGljcy9idXNpbmVzc3R4bi9jb21wbGV0aW9uL2ZhdWx0cycsICRzY29wZS5jcml0ZXJpYSkudGhlbihmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgICRzY29wZS5mYXVsdHMgPSByZXNwLmRhdGE7XG4gICAgICAgIFxuICAgICAgICB2YXIgZmF1bHRkYXRhID0gW107XG4gICAgICAgIFxuICAgICAgICBmb3IgKHZhciBpPTA7IGkgPCAkc2NvcGUuZmF1bHRzLmxlbmd0aDsgaSsrKSB7XG4gICAgICAgICAgdmFyIGZhdWx0ID0gJHNjb3BlLmZhdWx0c1tpXTtcbiAgICAgICAgICB2YXIgcmVjb3JkPVsgXTtcbiAgICAgICAgICByZWNvcmQucHVzaChmYXVsdC52YWx1ZSk7XG4gICAgICAgICAgcmVjb3JkLnB1c2goZmF1bHQuY291bnQpO1xuICAgICAgICAgIGZhdWx0ZGF0YS5wdXNoKHJlY29yZCk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgICRzY29wZS5jdGZhdWx0c2NoYXJ0LnVubG9hZCgpO1xuXG4gICAgICAgICRzY29wZS5jdGZhdWx0c2NoYXJ0LmxvYWQoe1xuICAgICAgICAgIGNvbHVtbnM6IGZhdWx0ZGF0YVxuICAgICAgICB9KTtcblxuICAgICAgfSxmdW5jdGlvbihyZXNwKSB7XG4gICAgICAgIGNvbnNvbGUubG9nKFwiRmFpbGVkIHRvIGdldCBzdGF0aXN0aWNzOiBcIityZXNwKTtcbiAgICAgIH0pO1xuXG4gICAgICAkaHR0cC5nZXQoJy9oYXdrdWxhci9idG0vYW5hbHl0aWNzL2J1c2luZXNzdHhuL3Byb3BlcnRpZXMvJyskc2NvcGUuYnVzaW5lc3NUcmFuc2FjdGlvbk5hbWUpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAkc2NvcGUucHJvcGVydGllcyA9IHJlc3AuZGF0YTtcbiAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgcHJvcGVydHkgaW5mbzogXCIrcmVzcCk7XG4gICAgICB9KTtcbiAgICBcbiAgICAgIGlmICgkc2NvcGUuY29uZmlnLnNlbGVjdGVkUHJvcGVydHkgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICAkc2NvcGUucmVsb2FkUHJvcGVydHkoKTtcbiAgICAgIH1cbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlZHJhd0xpbmVDaGFydCA9IGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLmN0bGluZWNoYXJ0LmxvYWQoe1xuICAgICAgICBqc29uOiAkc2NvcGUuc3RhdGlzdGljcyxcbiAgICAgICAga2V5czoge1xuICAgICAgICAgIHZhbHVlOiBbJ21heCcsJ2F2ZXJhZ2UnLCdtaW4nLCdjb3VudCcsJ2ZhdWx0Q291bnQnXSxcbiAgICAgICAgICB4OiAndGltZXN0YW1wJ1xuICAgICAgICB9XG4gICAgICB9KTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbG9hZFByb3BlcnR5ID0gZnVuY3Rpb24oKSB7XG4gICAgICAkaHR0cC5wb3N0KCcvaGF3a3VsYXIvYnRtL2FuYWx5dGljcy9idXNpbmVzc3R4bi9jb21wbGV0aW9uL3Byb3BlcnR5LycrJHNjb3BlLmNvbmZpZy5zZWxlY3RlZFByb3BlcnR5LCAkc2NvcGUuY3JpdGVyaWEpLnRoZW4oZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICAkc2NvcGUucHJvcGVydHlEZXRhaWxzID0gcmVzcC5kYXRhO1xuICAgICAgICBcbiAgICAgICAgdmFyIHByb3BlcnR5ZGF0YSA9IFtdO1xuICAgICAgICBcbiAgICAgICAgZm9yICh2YXIgaT0wOyBpIDwgJHNjb3BlLnByb3BlcnR5RGV0YWlscy5sZW5ndGg7IGkrKykge1xuICAgICAgICAgIHZhciBwcm9wID0gJHNjb3BlLnByb3BlcnR5RGV0YWlsc1tpXTtcbiAgICAgICAgICB2YXIgcmVjb3JkPVsgXTtcbiAgICAgICAgICByZWNvcmQucHVzaChwcm9wLnZhbHVlKTtcbiAgICAgICAgICByZWNvcmQucHVzaChwcm9wLmNvdW50KTtcbiAgICAgICAgICBwcm9wZXJ0eWRhdGEucHVzaChyZWNvcmQpO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAkc2NvcGUucHJvcGVydHljaGFydC51bmxvYWQoKTtcblxuICAgICAgICAkc2NvcGUucHJvcGVydHljaGFydC5sb2FkKHtcbiAgICAgICAgICBjb2x1bW5zOiBwcm9wZXJ0eWRhdGFcbiAgICAgICAgfSk7XG5cbiAgICAgIH0sZnVuY3Rpb24ocmVzcCkge1xuICAgICAgICBjb25zb2xlLmxvZyhcIkZhaWxlZCB0byBnZXQgcHJvcGVydHkgZGV0YWlscyBmb3IgJ1wiKyRzY29wZS5jb25maWcuc2VsZWN0ZWRQcm9wZXJ0eStcIic6IFwiK3Jlc3ApO1xuICAgICAgfSk7XG4gICAgfTtcblxuICAgICRzY29wZS5yZWxvYWQoKTtcblxuICAgICRpbnRlcnZhbChmdW5jdGlvbigpIHtcbiAgICAgIGlmICgkc2NvcGUuY3JpdGVyaWEuZW5kVGltZSA9PT0gXCIwXCIgfHwgJHNjb3BlLmNvbmZpZy5wcmV2TG93ZXJCb3VuZERpc3BsYXkgIT09ICRzY29wZS5jb25maWcubG93ZXJCb3VuZERpc3BsYXkpIHtcbiAgICAgICAgJHNjb3BlLnJlbG9hZCgpO1xuICAgICAgICBcbiAgICAgICAgJHNjb3BlLmNvbmZpZy5wcmV2TG93ZXJCb3VuZERpc3BsYXkgPSAkc2NvcGUuY29uZmlnLmxvd2VyQm91bmREaXNwbGF5O1xuICAgICAgfVxuICAgIH0sMTAwMDApO1xuXG4gICAgJHNjb3BlLmluaXRHcmFwaCA9IGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLmN0bGluZWNoYXJ0ID0gYzMuZ2VuZXJhdGUoe1xuICAgICAgICBiaW5kdG86ICcjY29tcGxldGlvbnRpbWVsaW5lY2hhcnQnLFxuICAgICAgICBkYXRhOiB7XG4gICAgICAgICAganNvbjogW1xuICAgICAgICAgIF0sXG4gICAgICAgICAgYXhlczoge1xuICAgICAgICAgICAgbWF4OiAneScsXG4gICAgICAgICAgICBhdmVyYWdlOiAneScsXG4gICAgICAgICAgICBtaW46ICd5JyxcbiAgICAgICAgICAgIGNvdW50OiAneTInLFxuICAgICAgICAgICAgZmF1bHRDb3VudDogJ3kyJ1xuICAgICAgICAgIH0sXG4gICAgICAgICAgdHlwZTogJ2xpbmUnLFxuICAgICAgICAgIHR5cGVzOiB7XG4gICAgICAgICAgICBjb3VudDogJ2JhcicsXG4gICAgICAgICAgICBmYXVsdENvdW50OiAnYmFyJ1xuICAgICAgICAgIH0sXG4gICAgICAgICAga2V5czoge1xuICAgICAgICAgICAgdmFsdWU6IFsnbWF4JywnYXZlcmFnZScsJ21pbicsJ2NvdW50JywnZmF1bHRDb3VudCddLFxuICAgICAgICAgICAgeDogJ3RpbWVzdGFtcCdcbiAgICAgICAgICB9XG4gICAgICAgIH0sXG4gICAgICAgIGNvbG9yOiB7XG4gICAgICAgICAgcGF0dGVybjogWycjZmYwMDAwJywgJyMzM2NjMzMnLCAnI2U1ZTYwMCcsICcjOTljY2ZmJywgJyNmZmIzYjMnXVxuICAgICAgICB9LFxuICAgICAgICBheGlzOiB7XG4gICAgICAgICAgeDoge1xuICAgICAgICAgICAgdHlwZTogJ3RpbWVzZXJpZXMnLFxuICAgICAgICAgICAgdGljazoge1xuICAgICAgICAgICAgICBjdWxsaW5nOiB7XG4gICAgICAgICAgICAgICAgbWF4OiA2IC8vIHRoZSBudW1iZXIgb2YgdGljayB0ZXh0cyB3aWxsIGJlIGFkanVzdGVkIHRvIGxlc3MgdGhhbiB0aGlzIHZhbHVlXG4gICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgIGZvcm1hdDogJyVZLSVtLSVkICVIOiVNOiVTJ1xuICAgICAgICAgICAgfVxuICAgICAgICAgIH0sXG4gICAgICAgICAgeToge1xuICAgICAgICAgICAgbGFiZWw6ICdTZWNvbmRzJyxcbiAgICAgICAgICAgIHBhZGRpbmc6IHtib3R0b206IDB9LFxuICAgICAgICAgICAgdGljazoge1xuICAgICAgICAgICAgICBmb3JtYXQ6IGZ1bmN0aW9uICh5KSB7IHJldHVybiB5IC8gMTAwMDAwMDAwMDsgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgIH0sXG4gICAgICAgICAgeTI6IHtcbiAgICAgICAgICAgIHNob3c6IHRydWVcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0pO1xuXG4gICAgICAkc2NvcGUuY3RmYXVsdHNjaGFydCA9IGMzLmdlbmVyYXRlKHtcbiAgICAgICAgYmluZHRvOiAnI2NvbXBsZXRpb250aW1lZmF1bHRzY2hhcnQnLFxuICAgICAgICBkYXRhOiB7XG4gICAgICAgICAganNvbjogW1xuICAgICAgICAgIF0sXG4gICAgICAgICAgdHlwZTogJ3BpZScsXG4gICAgICAgICAgb25jbGljazogZnVuY3Rpb24gKGQsIGkpIHtcbiAgICAgICAgICAgIHZhciBmYXVsdCA9IHtcbiAgICAgICAgICAgICAgdmFsdWU6IGQuaWRcbiAgICAgICAgICAgIH07XG4gICAgICAgICAgICAkc2NvcGUuY3JpdGVyaWEuZmF1bHRzLmFkZChmYXVsdCk7XG4gICAgICAgICAgICAkc2NvcGUucmVsb2FkKCk7XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KTtcblxuICAgIH07XG4gICAgXG4gICAgJHNjb3BlLmluaXRHcmFwaCgpO1xuXG4gICAgJHNjb3BlLnByb3BlcnR5Q2xpY2tlZCA9IGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLmluaXRQcm9wZXJ0eUdyYXBoKCRzY29wZS5jb25maWcuc2VsZWN0ZWRQcm9wZXJ0eSk7XG4gICAgfTtcblxuICAgICRzY29wZS5pbml0UHJvcGVydHlHcmFwaCA9IGZ1bmN0aW9uKG5hbWUpIHtcbiAgICAgICRzY29wZS5wcm9wZXJ0eWNoYXJ0ID0gYzMuZ2VuZXJhdGUoe1xuICAgICAgICBiaW5kdG86ICcjY29tcGxldGlvbnRpbWVwcm9wZXJ0eWNoYXJ0JyxcbiAgICAgICAgZGF0YToge1xuICAgICAgICAgIGNvbHVtbnM6IFtcbiAgICAgICAgICBdLFxuICAgICAgICAgIHR5cGU6ICdwaWUnLFxuICAgICAgICAgIG9uY2xpY2s6IGZ1bmN0aW9uIChkLCBpKSB7XG4gICAgICAgICAgICB2YXIgcHJvcGVydHkgPSB7XG4gICAgICAgICAgICAgIG5hbWU6IG5hbWUsXG4gICAgICAgICAgICAgIHZhbHVlOiBkLmlkXG4gICAgICAgICAgICB9O1xuICAgICAgICAgICAgJHNjb3BlLmNyaXRlcmlhLnByb3BlcnRpZXMuYWRkKHByb3BlcnR5KTtcbiAgICAgICAgICAgICRzY29wZS5yZWxvYWQoKTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0pO1xuXG4gICAgICAkc2NvcGUucmVsb2FkUHJvcGVydHkoKTtcbiAgICB9O1xuXG4gICAgJHNjb3BlLnJlbW92ZVByb3BlcnR5ID0gZnVuY3Rpb24ocHJvcGVydHkpIHtcbiAgICAgICRzY29wZS5jcml0ZXJpYS5wcm9wZXJ0aWVzLnJlbW92ZShwcm9wZXJ0eSk7XG4gICAgICAkc2NvcGUucmVsb2FkKCk7XG4gICAgfTtcblxuICAgICRzY29wZS5yZW1vdmVGYXVsdCA9IGZ1bmN0aW9uKGZhdWx0KSB7XG4gICAgICAkc2NvcGUuY3JpdGVyaWEuZmF1bHRzLnJlbW92ZShmYXVsdCk7XG4gICAgICAkc2NvcGUucmVsb2FkKCk7XG4gICAgfTtcblxuICAgICRzY29wZS50b2dnbGVFeGNsdXNpb24gPSBmdW5jdGlvbihlbGVtZW50KSB7XG4gICAgICBlbGVtZW50LmV4Y2x1ZGVkID0gIWVsZW1lbnQuZXhjbHVkZWQ7XG4gICAgICAkc2NvcGUucmVsb2FkKCk7XG4gICAgfTtcblxuICAgICRzY29wZS51cGRhdGVkQm91bmRzID0gZnVuY3Rpb24oKSB7XG4gICAgICBpZiAoJHNjb3BlLmNvbmZpZy5sb3dlckJvdW5kRGlzcGxheSA9PT0gMCkge1xuICAgICAgICAkc2NvcGUuY3JpdGVyaWEubG93ZXJCb3VuZCA9IDA7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICB2YXIgbWF4RHVyYXRpb24gPSAwO1xuICAgICAgICBmb3IgKHZhciBpPTA7IGkgPCAkc2NvcGUuc3RhdGlzdGljcy5sZW5ndGg7IGkrKykge1xuICAgICAgICAgIGlmICgkc2NvcGUuc3RhdGlzdGljc1tpXS5tYXggPiBtYXhEdXJhdGlvbikge1xuICAgICAgICAgICAgbWF4RHVyYXRpb24gPSAkc2NvcGUuc3RhdGlzdGljc1tpXS5tYXg7XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGlmIChtYXhEdXJhdGlvbiA+IDApIHtcbiAgICAgICAgICAkc2NvcGUuY3JpdGVyaWEubG93ZXJCb3VuZCA9ICggJHNjb3BlLmNvbmZpZy5sb3dlckJvdW5kRGlzcGxheSAqIG1heER1cmF0aW9uICkgLyAxMDA7XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9O1xuXG4gICAgJHNjb3BlLnNlbGVjdEFjdGlvbiA9IGZ1bmN0aW9uKCkge1xuICAgICAgJHNjb3BlLnJlbG9hZCgpO1xuICAgIH07XG5cbiAgICAkc2NvcGUuY3VycmVudERhdGVUaW1lID0gZnVuY3Rpb24oKSB7XG4gICAgICByZXR1cm4gbmV3IERhdGUoKTtcbiAgICB9O1xuXG4gIH1dKTtcbn1cbiJdLCJzb3VyY2VSb290IjoiL3NvdXJjZS8ifQ==

angular.module("hawkularbtm-templates", []).run(["$templateCache", function($templateCache) {$templateCache.put("plugins/btm/html/btm.html","<div class=\"col-md-12\" ng-controller=\"BTM.BTMController\">\n  <div class=\"text-center hk-urls-list hk-spinner-container\" ng-hide=\"businessTransactions\">\n    <div class=\"spinner spinner-lg\"></div>\n    <p class=\"hk-spinner-legend-below\">Loading...</p>\n  </div>\n\n  <div class=\"row\" ng-show=\"businessTransactions\" >\n    <hawkular-subtab class=\"hk-align-center\">\n      <div class=\"hk-nav-tabs-container\">\n        <ul class=\"nav nav-tabs nav-tabs-pf\">\n          <li class=\"active\"><a href=\"active\" >Active</a></li>\n          <li><a href=\"candidates\">Candidates <i class=\"fa fa-flag\" ng-hide=\"candidateCount === 0\"></i></a></li>\n          <li><a href=\"disabled\">Disabled</a></li>\n          <li><a href=\"ignored\">Ignored</a></li>\n        </ul>\n      </div>\n    </hawkular-subtab>\n\n    <section id=\"active\" class=\"hk-tab-content\">\n\n      <div class=\"col-md-9 hk-align-center\">\n        <ul class=\"list-group\" >\n          <br>\n          <div class=\"row\" >\n            Search: <input ng-model=\"query\">\n            <a href=\"/hawkular-ui/btm-analytics\" class=\"btn btn-info pull-right\" target=\"_blank\">View Analytics</a>\n          </div>\n\n          <div class=\"hk-url-item\" ng-repeat=\"btxn in businessTransactions | filter:query\" >\n            <div class=\"panel panel-default hk-url-heading\" ng-show=\"btxn.summary.level === \'All\'\">\n              <a href=\"info/{{btxn.summary.name}}\">{{btxn.summary.name}}</a>\n              <span class=\"hk-settings pull-right\">\n                <a href=\"config/{{btxn.summary.name}}\" ><i class=\"fa fa-cog\"></i></a>\n                <a href=\"#\" ng-click=\"deleteBusinessTxn(btxn)\"><i class=\"fa fa-trash-o\"></i></a>\n              </span>\n            </div>\n\n            <div class=\"panel panel-default hk-summary\" ng-show=\"btxn.summary.level === \'All\'\">\n              <div class=\"row\">\n                <div class=\"col-sm-3 hk-summary-item\">\n                  <a href=\"info/{{btxn.summary.name}}\">\n                    <span class=\"hk-data\" ng-show=\"btxn.count !== undefined\">{{btxn.count}}</i></span>\n                    <span class=\"hk-data spinner\" ng-hide=\"btxn.count !== undefined\" popover=\"Your data is being collected. You should see something in a few seconds.\" popover-trigger=\"mouseenter\" popover-placement=\"bottom\"></span>\n                    <span class=\"hk-item\">Transactions (per hour)</span>\n                  </a>\n                </div>\n                <div class=\"col-sm-3 hk-summary-item\">\n                  <a href=\"info/{{btxn.summary.name}}\">\n                    <span class=\"hk-data\" ng-show=\"btxn.percentile95 !== undefined\">{{btxn.percentile95}} sec</i></span>\n                    <span class=\"hk-data spinner\" ng-hide=\"btxn.percentile95 !== undefined\" popover=\"Your data is being collected. You should see something in a few seconds.\" popover-trigger=\"mouseenter\" popover-placement=\"bottom\"></span>\n                    <span class=\"hk-item\">Completion (95%)</span>\n                  </a>\n                </div>\n                <div class=\"col-sm-3 hk-summary-item\">\n                  <a href=\"info/{{btxn.summary.name}}\">\n                    <span class=\"hk-data\" ng-show=\"btxn.faultcount !== undefined\">{{btxn.faultcount}}</i></span>\n                    <span class=\"hk-data spinner\" ng-hide=\"btxn.faultcount !== undefined\" popover=\"Your data is being collected. You should see something in a few seconds.\" popover-trigger=\"mouseenter\" popover-placement=\"bottom\"></span>\n                    <span class=\"hk-item\">Faults</span>\n                  </a>\n                </div>\n                <div class=\"col-sm-3 hk-summary-item\">\n                  <a href=\"info/{{btxn.summary.name}}\">\n                    <span class=\"hk-data\" ng-show=\"btxn.alerts !== undefined\">{{btxn.alerts}} <i class=\"fa fa-flag\" ng-show=\"btxn.alerts > 0\"></i></span>\n                    <span class=\"hk-data spinner\" ng-hide=\"btxn.alerts !== undefined\" popover=\"Your data is being collected. You should see something in a few seconds.\" popover-trigger=\"mouseenter\" popover-placement=\"bottom\"></span>\n                    <span class=\"hk-item\">Alerts</span>\n                  </a>\n                </div>\n              </div>\n\n            </div>\n\n          </div>\n       </ul>\n      </div>\n    </section>\n  </div>\n</div>\n\n");
$templateCache.put("plugins/btm/html/btxncandidates.html","<div class=\"col-md-12\" ng-controller=\"BTM.BTMCandidatesController\">\n\n  <div class=\"text-center hk-urls-list hk-spinner-container\" ng-hide=\"unbounduris\">\n    <div class=\"spinner spinner-lg\"></div>\n    <p class=\"hk-spinner-legend-below\">Loading...</p>\n  </div>\n\n  <div class=\"row\" ng-show=\"unbounduris\" >\n    <hawkular-subtab class=\"hk-align-center\">\n      <div class=\"hk-nav-tabs-container\">\n        <ul class=\"nav nav-tabs nav-tabs-pf\">\n          <li><a href=\"active\" >Active</a></li>\n          <li class=\"active\"><a href=\"candidates\">Candidates <i class=\"fa fa-flag\" ng-hide=\"candidateCount === 0\"></i></a></li>\n          <li><a href=\"disabled\">Disabled</a></li>\n          <li><a href=\"ignored\">Ignored</a></li>\n        </ul>\n      </div>\n    </hawkular-subtab>\n\n    <section id=\"active\" class=\"hk-tab-content\">\n\n      <br>\n\n      <form class=\"form-horizontal hk-add-url\" name=\"addBTxnForm\" role=\"form\" novalidate >\n        <div class=\"form-group input\">\n          <div class=\"col-lg-6 col-sm-8 col-xs-12 hk-align-center\">\n            <div class=\"input-group input-group-lg\">\n              <input type=\"text\" class=\"form-control\" name=\"newBTxnNameField\"\n                   ng-model=\"newBTxnName\" ng-model-options=\"{ updateOn: \'default blur\'}\"\n                   placeholder=\"Business transaction name\">\n              <span class=\"input-group-btn\">\n                <input class=\"btn btn-primary\" type=\"submit\" ng-disabled=\"!newBTxnName\" ng-click=\"addBusinessTxn()\" value=\"Manage\" />\n                <input class=\"btn btn-danger\" type=\"button\" ng-disabled=\"!newBTxnName\" ng-click=\"ignoreBusinessTxn()\" value=\"Ignore\" />\n              </span>\n\n              <span class=\"input-group-btn\">\n              </span>\n\n              <select id=\"repeatSelect\" class=\"form-control\" ng-model=\"existingBTxnName\" >\n                <option value=\"\">Select existing ....</i></option>\n                <option ng-repeat=\"btxn in businessTransactions\" value=\"{{btxn.name}}\">{{btxn.name}} ({{getLevel(btxn.level)}})</option>\n              </select>\n              <span class=\"input-group-btn\">\n                <input class=\"btn btn-primary\" type=\"submit\" ng-disabled=\"!existingBTxnName || selecteduris.length == 0\" ng-click=\"updateBusinessTxn()\" value=\"Update\" />\n              </span>\n            </div>\n          </div>\n        </div>\n      </form>\n\n      <div class=\"col-md-9 hk-align-center\">\n        <ul class=\"list-group\" >\n          <br>\n          <div class=\"row\" >\n            Search: <input ng-model=\"query\">\n          </div>\n          <br>\n\n          <div class=\"panel panel-default hk-url-heading\">\n            <div ng-repeat=\"uriinfo in unbounduris | filter:query\" >\n              <label>\n                <input type=\"checkbox\" name=\"selectedURIs[]\"\n                  value=\"{{uriinfo.uri}}\"\n                  ng-checked=\"isSelected(uriinfo.uri)\"\n                  ng-click=\"selectionChanged(uriinfo.uri)\"\n                  ng-disabled=\"!newBTxnName && !existingBTxnName\">\n                  <span ng-hide=\"!newBTxnName && !existingBTxnName\" style=\"color:black\">{{uriinfo.uri}} [ {{uriinfo.endpointType}}]</span>\n                  <span ng-show=\"!newBTxnName && !existingBTxnName\" style=\"color:grey\"><i>{{uriinfo.uri}} [ {{uriinfo.endpointType}}]</i></span>\n              </label>\n            </div>\n          </div>\n       </ul>\n      </div>\n    </section>\n  </div>\n</div>\n\n");
$templateCache.put("plugins/btm/html/btxnconfig.html","<div class=\"row\">\n  <div class=\"col-md-12\" ng-controller=\"BTM.BTxnConfigController\">\n    <h1><span style=\"color:grey\">{{businessTransactionName}}</span></h1>\n\n    <button type=\"button\" class=\"btn btn-success btn-sm\" ng-click=\"save()\" ng-disabled=\"!dirty\">Save</button>\n    <button type=\"button\" class=\"btn btn-danger btn-sm\" ng-click=\"reset()\" ng-disabled=\"!dirty\">Discard</button>\n\n    <br>\n    <br>\n\n    <uib-alert ng-repeat=\"message in messages\" type=\"{{getMessageType(message)}}\" close=\"closeMessage($index)\"><strong>{{getMessageText(message)}}</strong></uib-alert>\n\n    <a href=\"#\" editable-textarea=\"businessTransaction.description\" e-rows=\"14\" e-cols=\"120\" rows=\"7\" onaftersave=\"setDirty()\" >\n        <pre><i>{{ businessTransaction.description || \'No description\' }}</i></pre>\n    </a>\n\n    <div class=\"col-md-12\" >\n      <h2>Filters</h2>\n    </div>\n\n    <div class=\"col-md-6\" >\n\n      <h4>Inclusion</h4>\n\n      <!-- TODO: Use angular-ui/bootstrap typeahead to autofill possible inclusion URIs -->\n\n      <ul class=\"list-group\">\n        <li class=\"list-group-item\" ng-repeat=\"inclusion in businessTransaction.filter.inclusions\" >{{inclusion}}<span class=\"glyphicon glyphicon-remove pull-right\" aria-hidden=\"true\" ng-click=\"removeInclusionFilter(inclusion)\"></span></li>\n        <li class=\"list-group-item\" >\n          <form class=\"form-horizontal hk-add-url\" name=\"addInclusionForm\" role=\"form\" autocomplete=\"off\" ng-submit=\"addInclusionFilter()\">\n            <div class=\"input-group input-group-lg\">\n              <input type=\"text\" class=\"form-control\" name=\"newInclusionFilterField\"\n                   ng-model=\"newInclusionFilter\" ng-model-options=\"{ updateOn: \'default blur\'}\"\n                   placeholder=\"Enter an inclusion filter (regular expression)\"\n                   uib-typeahead=\"uri for uri in unboundURIs | filter:$viewValue | limitTo:12\" required>\n              <span class=\"input-group-btn\">\n                <button class=\"btn btn-primary\" type=\"submit\" ng-disabled=\"!newInclusionFilter\" >\n                  <div ng-show=\"addProgress\" class=\"spinner spinner-sm\"></div>\n                  <span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\"\"></span>\n                </button>\n              </span>\n            </div>\n          </form>\n        </li>\n      </ul>\n    </div>\n\n    <div class=\"col-md-6\" >\n      <h4>Exclusion (applied after inclusions)</h4>\n\n      <ul class=\"list-group\">\n        <li class=\"list-group-item\" ng-repeat=\"exclusion in businessTransaction.filter.exclusions\" >{{exclusion}}<span class=\"glyphicon glyphicon-remove pull-right\" aria-hidden=\"true\" ng-click=\"removeExclusionFilter(exclusion)\"></span></li>\n        <li class=\"list-group-item\" >\n          <form class=\"form-horizontal hk-add-url\" name=\"addExclusionForm\" role=\"form\" autocomplete=\"off\" novalidate ng-submit=\"addExclusionFilter()\">\n            <div class=\"input-group input-group-lg\">\n              <input type=\"text\" class=\"form-control\" name=\"newExclusionFilterField\"\n                   ng-model=\"newExclusionFilter\" ng-model-options=\"{ updateOn: \'default blur\'}\"\n                   placeholder=\"Enter an exclusion filter (regular expression)\"\n                   uib-typeahead=\"uri for uri in unboundURIs | filter:$viewValue | limitTo:12\" required>\n              <span class=\"input-group-btn\">\n                <button class=\"btn btn-primary\" type=\"submit\" ng-disabled=\"!newExclusionFilter\" >\n                  <div ng-show=\"addProgress\" class=\"spinner spinner-sm\"></div>\n                  <span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\"\"></span>\n                </button>\n              </span>\n            </div>\n          </form>\n        </li>\n      </ul>\n    </div>\n\n    <!-- TODO: Styles -->\n\n    <div class=\"col-md-12\" >\n      <form>\n        <div class=\"form-group\">\n          <label for=\"level\" style=\"width: 10%\" >Reporting Level:</label>\n          <select name=\"nodeType\" ng-model=\"businessTransaction.level\" ng-change=\"setDirty()\" style=\"width: 10%\">\n            <option value=\"All\">All</option>\n            <option value=\"None\">None</option>\n            <option value=\"Ignore\">Ignore</option>\n          </select>\n        </div>\n      </form>\n    </div>\n\n    <div class=\"col-md-12\" >\n      <h2>Processors <a class=\"btn btn-primary\" ng-click=\"addProcessor()\"><span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\"\"></span></a></h2>\n    </div>\n\n    <div class=\"col-md-12\" >\n\n      <uib-accordion>\n        <uib-accordion-group ng-repeat=\"processor in businessTransaction.processors\" is-open=\"false\" is-disabled=\"false\">\n          <uib-accordion-heading>{{processor.description}} <a class=\"btn btn-link hk-delete pull-right\" href=\"#\" uibTooltip=\"Delete\" tooltip-trigger tooltip-placement=\"top\" ng-click=\"deleteProcessor(processor)\"><i class=\"fa fa-trash-o\"></i></a></uib-accordion-heading>\n\n          <form>\n            <div class=\"form-group\">\n              <label for=\"description\" style=\"width: 15%\" >Description:</label>\n              <input type=\"text\" name=\"description\" ng-model=\"processor.description\" ng-change=\"setDirty()\" style=\"width: 80%\" >\n            </div>\n\n            <div class=\"form-group\">\n              <label for=\"nodeType\" style=\"width: 15%\" > Node type: </label>\n              <select name=\"nodeType\" ng-model=\"processor.nodeType\" ng-change=\"setDirty()\" style=\"width: 30%\">\n                <option value=\"Consumer\">Consumer</option>\n                <option value=\"Producer\">Producer</option>\n                <option value=\"Component\">Component</option>\n              </select>\n\n              <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n              <label for=\"direction\" style=\"width: 15%\" >Direction: </label>\n              <select name=\"direction\" ng-model=\"processor.direction\" ng-change=\"setDirty()\" style=\"width: 30%\">\n                <option value=\"In\">In</option>\n                <option value=\"Out\">Out</option>\n              </select>\n\n              <label for=\"uriFilter\" style=\"width: 15%\" >URI filter:</label>\n              <input type=\"text\" name=\"uriFilter\"\n                   ng-model=\"processor.uriFilter\" ng-model-options=\"{ updateOn: \'default blur\'}\"\n                   placeholder=\"Enter URI filter (regular expression)\"\n                   uib-typeahead=\"uri for uri in boundURIs | filter:$viewValue | limitTo:12\"\n                   ng-change=\"setDirty()\" style=\"width: 80%\" >\n\n              <label for=\"operation\" style=\"width: 15%\" >Operation:</label>\n              <input type=\"text\" name=\"operation\" ng-model=\"processor.operation\" ng-change=\"setDirty()\" style=\"width: 30%\" >\n\n              <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n              <label for=\"faultFilter\" style=\"width: 15%\" >Fault filter:</label>\n              <input type=\"text\" name=\"faultFilter\" ng-model=\"processor.faultFilter\" ng-change=\"setDirty()\" style=\"width: 30%\" >\n            </div>\n\n            <div class=\"form-group\">\n              <label for=\"predicateType\" style=\"width: 15%\" >Predicate Type: </label>\n              <select name=\"predicateType\" ng-model=\"processor.predicate.type\" ng-change=\"changedExpressionType(processor.predicate)\" style=\"width: 30%\">\n                <option value=\"\"></option>\n                <option value=\"Literal\">Literal</option>\n                <option value=\"XML\">XML</option>\n                <option value=\"JSON\">JSON</option>\n                <option value=\"Text\">Text</option>\n                <option value=\"FreeForm\">Free Form</option>\n              </select>\n\n              <br>\n\n              <label for=\"predicateSource\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\">Source: </label>\n              <select name=\"predicateSource\" ng-model=\"processor.predicate.source\" ng-change=\"setDirty()\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\" style=\"width: 30%\">\n                <option value=\"Content\">Content</option>\n                <option value=\"Header\">Header</option>\n              </select>\n\n              <label style=\"width: 5%\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\"></label> <!-- TODO: Must be a better way -->\n\n              <label for=\"predicateKey\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\">Key: </label>\n              <input type=\"text\" name=\"predicateKey\" ng-model=\"processor.predicate.key\" ng-change=\"setDirty()\" style=\"width: 30%\" ng-show=\"processor.predicate.type === \'XML\' || processor.predicate.type === \'JSON\' || processor.predicate.type === \'Text\'\">\n\n              <label for=\"predicateXPath\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'XML\'\">XPath: <a href=\"http://www.w3schools.com/xsl/xpath_syntax.asp\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n              <input type=\"text\" name=\"predicateXPath\" ng-model=\"processor.predicate.xpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"processor.predicate.type === \'XML\'\">\n\n              <label for=\"predicateJSONPath\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'JSON\'\">JSONPath: <a href=\"http://goessner.net/articles/JsonPath/\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n              <input type=\"text\" name=\"predicateJSONPath\" ng-model=\"processor.predicate.jsonpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"processor.predicate.type === \'JSON\'\">\n\n              <label for=\"predicateValue\" style=\"width: 15%\" ng-show=\"processor.predicate.type === \'FreeForm\' || action.expression.type === \'Literal\'\">Value:</label>\n              <input type=\"text\" name=\"predicateValue\" ng-model=\"processor.predicate.value\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"processor.predicate.type === \'FreeForm\' || action.expression.type === \'Literal\'\">\n            </div>\n          </form>\n\n          <h4>Actions <span uib-dropdown>\n            <a href id=\"simple-dropdown\" uib-dropdown-toggle class=\"btn btn-primary\">\n              <span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\"\"></span>\n            </a>\n            <ul class=\"uib-dropdown-menu\" aria-labelledby=\"simple-dropdown\">\n              <li><a href ng-click=\"addAction(processor, \'AddContent\')\">Add Content</a></li>\n              <li><a href ng-click=\"addAction(processor, \'AddCorrelationId\')\">Add Correlation Identifier</a></li>\n              <li><a href ng-click=\"addAction(processor, \'EvaluateURI\')\">Evaluate URI</a></li>\n              <li><a href ng-click=\"addAction(processor, \'SetDetail\')\">Set Detail</a></li>\n              <li><a href ng-click=\"addAction(processor, \'SetFault\')\">Set Fault Code</a></li>\n              <li><a href ng-click=\"addAction(processor, \'SetFaultDescription\')\">Set Fault Description</a></li>\n              <li><a href ng-click=\"addAction(processor, \'SetProperty\')\">Set Property</a></li>\n            </ul>\n            </span>\n          </h4>\n\n          <uib-accordion>\n            <uib-accordion-group ng-repeat=\"action in processor.actions\" is-open=\"false\" is-disabled=\"false\">\n              <uib-accordion-heading>[ {{action.actionType}} {{action.name}} ]: {{action.description}} <a class=\"btn btn-link hk-delete pull-right\" href=\"#\" uibTooltip=\"Delete\" tooltip-trigger tooltip-placement=\"top\" ng-click=\"deleteAction(processor,action)\"><i class=\"fa fa-trash-o\"></i></a></uib-accordion-heading>\n\n              <form>\n                <div class=\"form-group\">\n                  <label for=\"description\" style=\"width: 15%\" >Description:</label>\n                  <input type=\"text\" name=\"description\" ng-model=\"action.description\" ng-change=\"setDirty()\" style=\"width: 80%\" >\n                </div>\n\n                <div class=\"form-group\">\n                  <label for=\"actionPredicateType\" style=\"width: 15%\" >Predicate Type: </label>\n                  <select name=\"actionPredicateType\" ng-model=\"action.predicate.type\" ng-change=\"changedExpressionType(action.predicate)\" style=\"width: 30%\">\n                    <option value=\"\"></option>\n                    <option value=\"Literal\">Literal</option>\n                    <option value=\"XML\">XML</option>\n                    <option value=\"JSON\">JSON</option>\n                    <option value=\"Text\">Text</option>\n                    <option value=\"FreeForm\">Free Form</option>\n                  </select>\n\n                  <br>\n\n                  <label for=\"actionPredicateSource\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'XML\' || action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\">Predicate Source: </label>\n                  <select name=\"actionPredicateSource\" ng-model=\"action.predicate.source\" ng-change=\"setDirty()\" ng-show=\"action.predicate.type === \'XML\' ||action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\" style=\"width: 30%\">\n                    <option value=\"Content\">Content</option>\n                    <option value=\"Header\">Header</option>\n                  </select>\n\n                  <label style=\"width: 5%\" ng-show=\"action.predicate.type === \'XML\' || action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\"></label> <!-- TODO: Must be a better way -->\n\n                  <label for=\"actionPredicateKey\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'XML\' || action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\">Predicate Key: </label>\n                  <input type=\"text\" name=\"actionPredicateKey\" ng-model=\"action.predicate.key\" ng-change=\"setDirty()\" style=\"width: 30%\" ng-show=\"action.predicate.type === \'XML\' || action.predicate.type === \'JSON\' || action.predicate.type === \'Text\'\">\n\n                  <label for=\"actionPredicateXPath\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'XML\'\">Predicate XPath: <a href=\"http://www.w3schools.com/xsl/xpath_syntax.asp\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n                  <input type=\"text\" name=\"actionPredicateXPath\" ng-model=\"action.predicate.xpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.predicate.type === \'XML\'\">\n\n                  <label for=\"actionPredicateJSONPath\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'JSON\'\">Predicate JSONPath: <a href=\"http://goessner.net/articles/JsonPath/\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n                  <input type=\"text\" name=\"actionPredicateJSONPath\" ng-model=\"action.predicate.jsonpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.predicate.type === \'JSON\'\">\n\n                  <label for=\"actionPredicate\" style=\"width: 15%\" ng-show=\"action.predicate.type === \'FreeForm\' || action.predicate.type === \'Literal\'\">Predicate Value:</label>\n                  <input type=\"text\" name=\"actionPredicate\" ng-model=\"action.predicate.value\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.predicate.type === \'FreeForm\' || action.predicate.type === \'Literal\'\">\n                </div>\n\n                <div class=\"form-group\">\n\n                  <!-- HWKBTM-273 Using \'ng-class\' attribute to try to highlight the error field, but at the point\n                       where the form is displayed the errors aren\'t available, and their retrieval does not\n                       cause a change in state that refreshes the field. -->\n\n                  <label for=\"actionName\" style=\"width: 15%\" ng-show=\"action.actionType === \'AddContent\' || action.actionType === \'SetDetail\' || action.actionType === \'SetProperty\'\" >Name:</label>\n                  <input type=\"text\" ng-class=\"{error:isError(processor,action,\'name\')}\" name=\"actionName\" ng-model=\"action.name\" ng-change=\"setDirty()\" ng-show=\"action.actionType === \'AddContent\' || action.actionType === \'SetDetail\' || action.actionType === \'SetProperty\'\" style=\"width: 30%\" >\n\n                  <label style=\"width: 5%\" ng-show=\"action.actionType === \'AddContent\'\" ></label> <!-- TODO: Must be a better way -->\n\n                  <label for=\"actionType\" style=\"width: 15%\" ng-show=\"action.actionType === \'AddContent\'\" >Type:</label>\n                  <input type=\"text\" name=\"actionType\" ng-model=\"action.type\" ng-change=\"setDirty()\" ng-show=\"action.actionType === \'AddContent\'\" style=\"width: 30%\" >\n\n                  <label for=\"correlationScope\" ng-show=\"action.actionType === \'AddCorrelationId\'\" style=\"width: 15%\" >Correlation Scope: </label>\n                  <select name=\"correlationScope\" ng-model=\"action.scope\" ng-show=\"action.actionType === \'AddCorrelationId\'\" ng-change=\"setDirty()\" style=\"width: 30%\">\n                    <option value=\"Global\">Global</option>\n                    <option value=\"Interaction\">Interaction</option>\n                    <option value=\"Local\">Local</option>\n                  </select>\n\n                  <label for=\"actionTemplate\" ng-show=\"action.actionType === \'EvaluateURI\'\" style=\"width: 15%\" >Template URI:</label>\n                  <input type=\"text\" name=\"actionTemplate\" ng-model=\"action.template\" ng-change=\"setDirty()\" ng-show=\"action.actionType === \'EvaluateURI\'\" style=\"width: 30%\" >\n\n                </div>\n\n                <div class=\"form-group\" ng-if=\"action.actionType !== \'EvaluateURI\' && action.actionType !== undefined\" >\n\n                  <label for=\"actionTemplate\" style=\"width: 15%\" ng-show=\"action.actionType === \'EvaluateURI\'\" >Template URI:</label>\n                  <input type=\"text\" name=\"actionTemplate\" ng-model=\"action.template\" ng-change=\"setDirty()\" ng-show=\"action.actionType === \'EvaluateURI\'\" style=\"width: 30%\" >\n\n                  <label for=\"actionValueType\" style=\"width: 15%\" >Value Type: </label>\n                  <select name=\"actionValueType\" ng-model=\"action.expression.type\" ng-change=\"changedExpressionType(action.expression)\" style=\"width: 30%\">\n                    <option value=\"\"></option>\n                    <option value=\"Literal\">Literal</option>\n                    <option value=\"XML\">XML</option>\n                    <option value=\"JSON\">JSON</option>\n                    <option value=\"Text\">Text</option>\n                    <option value=\"FreeForm\">Free Form</option>\n                  </select>\n\n                  <br>\n\n                  <label for=\"actionValueSource\" style=\"width: 15%\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\">Value Source: </label>\n                  <select name=\"actionValueSource\" ng-model=\"action.expression.source\" ng-change=\"setDirty()\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\" style=\"width: 30%\">\n                    <option value=\"Content\">Content</option>\n                    <option value=\"Header\">Header</option>\n                  </select>\n\n                  <label style=\"width: 5%\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\"></label> <!-- TODO: Must be a better way -->\n\n                  <label for=\"actionValueKey\" style=\"width: 15%\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\">Value Key: </label>\n                  <input type=\"text\" name=\"actionValueKey\" ng-model=\"action.expression.key\" ng-change=\"setDirty()\" style=\"width: 30%\" ng-show=\"action.expression.type === \'XML\' || action.expression.type === \'JSON\' || action.expression.type === \'Text\'\">\n\n                  <label for=\"actionValueXPath\" style=\"width: 15%\" ng-show=\"action.expression.type === \'XML\'\">Value XPath: <a href=\"http://www.w3schools.com/xsl/xpath_syntax.asp\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n                  <input type=\"text\" name=\"actionValueXPath\" ng-model=\"action.expression.xpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.expression.type === \'XML\'\">\n\n                  <label for=\"actionValueJSONPath\" style=\"width: 15%\" ng-show=\"action.expression.type === \'JSON\'\">Value JSONPath: <a href=\"http://goessner.net/articles/JsonPath/\"  target=\"_blank\"><i class=\"fa fa-info-circle\"></i></a></label>\n                  <input type=\"text\" name=\"actionValueJSONPath\" ng-model=\"action.expression.jsonpath\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.expression.type === \'JSON\'\">\n\n                  <label for=\"actionValue\" style=\"width: 15%\" ng-show=\"action.expression.type === \'FreeForm\' || action.expression.type === \'Literal\'\">Value:</label>\n                  <input type=\"text\" name=\"actionValue\" ng-model=\"action.expression.value\" ng-change=\"setDirty()\" style=\"width: 80%\" ng-show=\"action.expression.type === \'FreeForm\' || action.expression.type === \'Literal\'\">\n                </div>\n              </form>\n\n            </uib-accordion-group>\n          </uib-accordion>\n\n          <!-- Provide padding as otherwise the action dropdown, when no actions, gets hidden\n               (Must be a better way, but this works for now) -->\n          <div>\n            <br>\n            <br>\n            <br>\n            <br>\n            <br>\n            <br>\n            <br>\n            <br>\n            <br>\n          </div>\n\n        </uib-accordion-group>\n      </uib-accordion>\n    </div>\n\n  </div>\n</div>\n");
$templateCache.put("plugins/btm/html/btxndisabled.html","<div class=\"col-md-12\" ng-controller=\"BTM.BTMDisabledController\">\n  <div class=\"text-center hk-urls-list hk-spinner-container\" ng-hide=\"businessTransactions\">\n    <div class=\"spinner spinner-lg\"></div>\n    <p class=\"hk-spinner-legend-below\">Loading...</p>\n  </div>\n\n  <div class=\"row\" ng-show=\"businessTransactions\" >\n\n    <hawkular-subtab class=\"hk-align-center\">\n      <div class=\"hk-nav-tabs-container\">\n        <ul class=\"nav nav-tabs nav-tabs-pf\">\n          <li><a href=\"active\" >Active</a></li>\n          <li><a href=\"candidates\">Candidates <i class=\"fa fa-flag\" ng-hide=\"candidateCount === 0\"></i></a></li>\n          <li class=\"active\"><a href=\"disabled\">Disabled</a></li>\n          <li><a href=\"ignored\">Ignored</a></li>\n        </ul>\n      </div>\n    </hawkular-subtab>\n\n    <section id=\"active\" class=\"hk-tab-content\">\n\n      <div class=\"col-md-9 hk-align-center\">\n        <ul class=\"list-group\" >\n          <br>\n          <div class=\"row\" >\n            Search: <input ng-model=\"query\">\n          </div>\n\n          <div class=\"hk-url-item\" ng-repeat=\"btxn in businessTransactions | filter:query\" >\n            <div class=\"panel panel-default hk-url-heading\" ng-show=\"btxn.summary.level === \'None\'\">\n              <a href=\"info/{{btxn.summary.name}}\">{{btxn.summary.name}}</a>\n              <span class=\"hk-settings pull-right\">\n                <a href=\"config/{{btxn.summary.name}}\" ><i class=\"fa fa-cog\"></i></a>\n                <a href=\"#\" ng-click=\"deleteBusinessTxn(btxn)\"><i class=\"fa fa-trash-o\"></i></a>\n              </span>\n            </div>\n          </div>\n       </ul>\n      </div>\n    </section>\n  </div>\n</div>\n\n");
$templateCache.put("plugins/btm/html/btxnignored.html","<div class=\"col-md-12\" ng-controller=\"BTM.BTMIgnoredController\">\n  <div class=\"text-center hk-urls-list hk-spinner-container\" ng-hide=\"businessTransactions\">\n    <div class=\"spinner spinner-lg\"></div>\n    <p class=\"hk-spinner-legend-below\">Loading...</p>\n  </div>\n\n  <div class=\"row\" ng-show=\"businessTransactions\" >\n\n    <hawkular-subtab class=\"hk-align-center\">\n      <div class=\"hk-nav-tabs-container\">\n        <ul class=\"nav nav-tabs nav-tabs-pf\">\n          <li><a href=\"active\" >Active</a></li>\n          <li><a href=\"candidates\">Candidates <i class=\"fa fa-flag\" ng-hide=\"candidateCount === 0\"></i></a></li>\n          <li><a href=\"disabled\">Disabled</a></li>\n          <li class=\"active\"><a href=\"ignored\">Ignored</a></li>\n        </ul>\n      </div>\n    </hawkular-subtab>\n\n    <section id=\"active\" class=\"hk-tab-content\">\n\n      <div class=\"col-md-9 hk-align-center\">\n        <ul class=\"list-group\" >\n          <br>\n          <div class=\"row\" >\n            Search: <input ng-model=\"query\">\n          </div>\n\n          <div class=\"hk-url-item\" ng-repeat=\"btxn in businessTransactions | filter:query\" >\n            <div class=\"panel panel-default hk-url-heading\" ng-show=\"btxn.summary.level === \'Ignore\'\">\n              <a href=\"info/{{btxn.summary.name}}\">{{btxn.summary.name}}</a>\n              <span class=\"hk-settings pull-right\">\n                <a href=\"config/{{btxn.summary.name}}\" ><i class=\"fa fa-cog\"></i></a>\n                <a href=\"#\" ng-click=\"deleteBusinessTxn(btxn)\"><i class=\"fa fa-trash-o\"></i></a>\n              </span>\n            </div>\n          </div>\n       </ul>\n      </div>\n    </section>\n  </div>\n</div>\n\n");
$templateCache.put("plugins/btm/html/btxninfo.html","<div class=\"row\">\n  <div class=\"col-md-12\" ng-controller=\"BTM.BTxnConfigController\">\n    <h1><span style=\"color:grey\">{{businessTransactionName}}</span></h1>\n\n    <div class=\"form-group\" >\n      <span ng-repeat=\"fault in criteria.faults\">\n        <span ng-show=\"!fault.excluded\">\n          <a class=\"btn btn-success\" ng-click=\"toggleExclusion(fault)\">\n            <i>fault</i>: {{fault.value}}\n            <a class=\"btn btn-default\" ng-click=\"removeFault(fault)\">\n              <span class=\"glyphicon glyphicon-remove\" aria-hidden=\"true\"></span>\n            </a>\n          </a>\n        </span>\n        <span ng-show=\"fault.excluded\">\n          <a class=\"btn btn-danger\" ng-click=\"toggleExclusion(fault)\">\n            <i>fault</i>: {{fault.value}}\n            <a class=\"btn btn-default\" ng-click=\"removeFault(fault)\">\n              <span class=\"glyphicon glyphicon-remove\" aria-hidden=\"true\"></span>\n            </a>\n          </a>\n        </span>\n        <label style=\"width: 1%\" ></label> <!-- TODO: Must be a better way -->\n      </span>\n\n      <span ng-repeat=\"property in criteria.properties\">\n        <span ng-show=\"!property.excluded\">\n          <a class=\"btn btn-success\" ng-click=\"toggleExclusion(property)\">\n            <i>{{property.name}}</i>: {{property.value}}\n            <a class=\"btn btn-default\" ng-click=\"removeProperty(property)\">\n              <span class=\"glyphicon glyphicon-remove\" aria-hidden=\"true\"></span>\n            </a>\n          </a>\n        </span>\n        <span ng-show=\"property.excluded\">\n          <a class=\"btn btn-danger\" ng-click=\"toggleExclusion(property)\">\n            <i>{{property.name}}</i>: {{property.value}}\n            <a class=\"btn btn-default\" ng-click=\"removeProperty(property)\">\n              <span class=\"glyphicon glyphicon-remove\" aria-hidden=\"true\"></span>\n            </a>\n          </a>\n        </span>\n        <label style=\"width: 1%\" ></label> <!-- TODO: Must be a better way -->\n      </span>\n    </div>\n\n    <span>\n      <form>\n        <div class=\"form-group\">\n          <label for=\"intervalField\" style=\"width: 10%\" class=\"\" >Aggregation Interval:</label>\n          <select name=\"intervalField\" ng-model=\"config.interval\" ng-change=\"selectAction()\" style=\"width: 10%\">\n            <option value=\"1000\">1 Second</option>\n            <option value=\"10000\">10 Second</option>\n            <option value=\"30000\">30 Second</option>\n            <option value=\"60000\">1 Minute</option>\n            <option value=\"600000\">10 Minutes</option>\n            <option value=\"3600000\">1 Hour</option>\n            <option value=\"86400000\">1 Day</option>\n            <option value=\"604800000\">7 Day</option>\n          </select>\n\n          <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n          <label for=\"timeSpanField\" style=\"width: 5%\" >Time Span:</label>\n          <select name=\"timeSpanField\" ng-model=\"criteria.startTime\" ng-change=\"selectAction()\" style=\"width: 10%\">\n            <option value=\"-60000\">1 Minute</option>\n            <option value=\"-600000\">10 Minutes</option>\n            <option value=\"-1800000\">30 Minutes</option>\n            <option value=\"-3600000\">1 Hour</option>\n            <option value=\"-14400000\">4 Hours</option>\n            <option value=\"-28800000\">8 Hours</option>\n            <option value=\"-43200000\">12 Hours</option>\n            <option value=\"-86400000\">Day</option>\n            <option value=\"-604800000\">Week</option>\n            <option value=\"-2419200000\">Month</option>\n            <option value=\"-15768000000\">6 Months</option>\n            <option value=\"-31536000000\">Year</option>\n            <option value=\"1\">All</option>\n          </select>\n\n          <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n          <label for=\"endTimeField\" style=\"width: 3%\" >Until:</label>\n          <select name=\"endTimeField\" ng-model=\"criteria.endTime\" ng-change=\"selectAction()\" style=\"width: 10%\">\n            <option value=\"0\">Now</option>\n            <option value=\"{{currentDateTime().getTime()}}\">{{currentDateTime() | date:\'dd MMM yyyy HH:mm:ss\'}}</option>\n          </select>\n\n          <label style=\"width: 5%\" ></label> <!-- TODO: Must be a better way -->\n\n          <label for=\"lowerBoundField\" style=\"width: 8%\" >Lower Bound(%):</label>\n          <input type=\"number\" ng-model=\"config.lowerBoundDisplay\"\n                name=\"lowerBoundField\" ng-change=\"updatedBounds()\"\n                min=\"0\" max=\"100\">\n        </div>\n      </form>\n    </span>\n\n    <div id=\"completiontimelinechart\"></div>\n\n    <div class=\"col-md-6\">\n        <h2><span style=\"color:grey\">Faults</span></h2>\n\n        <div id=\"completiontimefaultschart\"></div>\n    </div>\n\n    <div class=\"col-md-6\">\n        <h2><span style=\"color:grey\">Property</span>\n          <select name=\"propertyField\" ng-model=\"config.selectedProperty\" ng-change=\"propertyClicked()\">\n            <option ng-repeat=\"property in properties\">{{property.name}}</option>\n          </select>\n        </h2>\n\n        <div id=\"completiontimepropertychart\"></div>\n    </div>\n\n  </div>\n</div>\n");}]); hawtioPluginLoader.addModule("hawkularbtm-templates");