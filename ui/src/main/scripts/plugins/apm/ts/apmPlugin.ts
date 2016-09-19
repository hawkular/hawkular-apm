///
/// Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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

  let tab = undefined;

  _module.config(['$locationProvider', '$routeProvider', 'HawtioNavBuilderProvider',
    ($locationProvider, $routeProvider: ng.route.IRouteProvider, builder: HawtioMainNav.BuilderFactory) => {
    tab = builder.create()
      .id(APM.pluginName)
      .title(() => 'Application Performance')
      .href(() => '/hawkular-ui/apm')
      .subPath('Components', 'components', builder.join(APM.templatePath, 'apm.html'))
      .subPath('Distributed Tracing', 'tracing', builder.join(E2E.templatePath, 'e2e.html'))
      .subPath('Business Transactions', 'btm', builder.join(BTM.templatePath, 'btm.html'))
      .rank(30)
      .build();
    builder.configureRouting($routeProvider, tab);
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
      when('/hawkular-ui/apm/btm', {
        templateUrl: 'plugins/btm/html/btm.html',
        controller: 'BTM.BTMController'
      }).
      when('/hawkular-ui/apm/btm/candidates', {
        templateUrl: 'plugins/btm/html/btxncandidates.html',
        controller: 'BTM.BTMCandidatesController'
      }).
      when('/hawkular-ui/apm/btm/ignored', {
        templateUrl: 'plugins/btm/html/btxnignored.html',
        controller: 'BTM.BTMIgnoredController'
      }).
      when('/hawkular-ui/apm/btm/config/:businesstransaction', {
        templateUrl: 'plugins/btm/html/btxnconfig.html',
        controller: 'BTM.BTxnConfigController',
        resolve: {
          btxn: function($http, $route, $location, toastr) {
            return $http.get('/hawkular/apm/config/businesstxn/full/' +
              $route.current.params.businesstransaction).then(function(resp) {
              if (!resp.data) {
                $location.path('/hawkular-ui/apm/btm');
                toastr.info('You were redirected to this page because you requested an invalid Business Transaction.');
              }
              return resp.data;
            }, function(resp) {
              toastr.info('You were redirected to this page because you requested an invalid Business Transaction.');
              $location.path('/hawkular-ui/apm/btm');
            });
          }
        }
      }).
      when('/hawkular-ui/apm/btm/info/:businesstransaction', {
        templateUrl: 'plugins/btm/html/btxninfo.html',
        controller: 'BTM.BTxnInfoController',
        resolve: {
          btxn: function($http, $route, $location, toastr) {
            return $http.get('/hawkular/apm/config/businesstxn/full/' +
              $route.current.params.businesstransaction).then(function(resp) {
              if (!resp.data) {
                resp.data = {
                  level: 'All'
                };
              }
              return resp.data;
            }, function(resp) {
              toastr.info('You were redirected to this page because you requested an invalid Business Transaction.');
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
    HawtioNav.add(tab);
    log.debug('loaded');
  }]);

  hawtioPluginLoader.addModule(APM.pluginName);
}
