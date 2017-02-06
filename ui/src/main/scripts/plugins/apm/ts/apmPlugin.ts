///
/// Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///    http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

/// <reference path="../../includes.ts"/>
/// <reference path="apmGlobals.ts"/>
module APM {

  export var _module = angular.module(APM.pluginName,
    ['xeditable','ui.bootstrap','angularUtils.directives.dirPagination','hawkularapm-templates']);

  let apmTab = undefined;
  let trcTab = undefined;
  let btmTab = undefined;
  let svcTab = undefined;

  _module.config(['$locationProvider', '$routeProvider', 'HawtioNavBuilderProvider',
    ($locationProvider, $routeProvider: ng.route.IRouteProvider, builder: HawtioMainNav.BuilderFactory) => {
    apmTab = builder.create()
      .id(APM.pluginName)
      .title(() => 'Components')
      .href(() => '/hawkular-ui/apm/components')
      .rank(30)
      .build();
    trcTab = builder.create()
      .id('tracing')
      .title(() => 'Distributed Tracing')
      .href(() => '/hawkular-ui/apm/tracing')
      .rank(20)
      .build();
    svcTab = builder.create()
      .id('services')
      .title(() => 'Services')
      .href(() => '/hawkular-ui/apm/services')
      .rank(15)
      .build();
    btmTab = builder.create()
      .id('btm')
      .title(() => 'Transactions')
      .href(() => '/hawkular-ui/apm/btm')
      .rank(10)
      .build();

    builder.configureRouting($routeProvider, apmTab);
    builder.configureRouting($routeProvider, trcTab);
    builder.configureRouting($routeProvider, svcTab);
    builder.configureRouting($routeProvider, btmTab);
    $locationProvider.html5Mode(true);
    $routeProvider.
      when('/hawkular-ui/apm/components', {
        templateUrl: 'plugins/apm/html/apm.html',
        controller: 'APM.APMController'
      }).
      when('/hawkular-ui/apm/tracing', {
        templateUrl: 'plugins/e2e/html/e2e.html',
        controller: 'E2E.E2EController'
      }).
      when('/hawkular-ui/apm/services', {
        templateUrl: 'plugins/services/html/services.html',
        controller: 'Services.ServicesController'
      }).
      when('/hawkular-ui/apm/btm', {
        templateUrl: 'plugins/btm/html/btm.html',
        controller: 'BTM.TxnController'
      }).
      when('/hawkular-ui/apm/btm/candidates', {
        templateUrl: 'plugins/btm/html/btxncandidates.html',
        controller: 'BTM.TxnCandidatesController'
      }).
      when('/hawkular-ui/apm/btm/ignored', {
        templateUrl: 'plugins/btm/html/btxnignored.html',
        controller: 'BTM.TxnIgnoredController'
      }).
      when('/hawkular-ui/apm/btm/config/:transaction', {
        templateUrl: 'plugins/btm/html/btxnconfig.html',
        controller: 'BTM.TxnConfigController',
        resolve: {
          txn: function($http, $route, $location, toastr) {
            return $http.get('/hawkular/apm/config/transaction/full/' +
              $route.current.params.transaction).then(function(resp) {
              if (!resp.data) {
                $location.path('/hawkular-ui/apm/btm');
                toastr.info('You were redirected to this page because you requested an invalid transaction.');
              }
              return resp.data;
            }, function(resp) {
              toastr.info('You were redirected to this page because you requested an invalid transaction.');
              $location.path('/hawkular-ui/apm/btm');
            });
          }
        }
      }).
      when('/hawkular-ui/apm/btm/info/:transaction', {
        templateUrl: 'plugins/btm/html/btxninfo.html',
        controller: 'BTM.TxnInfoController',
        resolve: {
          txn: function($http, $route, $location, toastr) {
            return $http.get('/hawkular/apm/config/transaction/full/' +
              $route.current.params.transaction).then(function(resp) {
              if (!resp.data) {
                resp.data = {
                  level: 'All'
                };
              }
              return resp.data;
            }, function(resp) {
              toastr.info('You were redirected to this page because you requested an invalid transaction.');
              $location.path('/hawkular-ui/apm/btm');
            });
          }
        }
      });
  }]);

  _module.run(function(editableOptions) {
    editableOptions.theme = 'bs3'; // bootstrap3 theme. Can be also 'bs2', 'default'
  });

  _module.run(['HawtioNav', (HawtioNav: HawtioMainNav.Registry) => {
    HawtioNav.add(apmTab);
    HawtioNav.add(trcTab);
    HawtioNav.add(svcTab);
    HawtioNav.add(btmTab);
    log.debug('loaded');
  }]);

  hawtioPluginLoader.addModule(APM.pluginName);
}
