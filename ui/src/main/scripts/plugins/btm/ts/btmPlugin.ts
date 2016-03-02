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
/// <reference path="btmGlobals.ts"/>
module BTM {

  export let _module = angular.module(BTM.pluginName, ['xeditable', 'ui.bootstrap', 'hawkularbtm-templates', 'toastr']);

  let tab = undefined;

  _module.config(['$locationProvider', '$routeProvider', 'HawtioNavBuilderProvider',
    ($locationProvider, $routeProvider: ng.route.IRouteProvider, builder: HawtioMainNav.BuilderFactory) => {
    tab = builder.create()
      .id(BTM.pluginName)
      .title(() => 'Business Transactions')
      .href(() => '/hawkular-ui/btm')
      .build();
    builder.configureRouting($routeProvider, tab);
    $locationProvider.html5Mode(true);
    $routeProvider.
      when('/hawkular-ui/btm', {
        templateUrl: 'plugins/btm/html/btm.html',
        controller: 'BTM.BTMController'
      }).
      when('/hawkular-ui/btm/candidates', {
        templateUrl: 'plugins/btm/html/btxncandidates.html',
        controller: 'BTM.BTMCandidatesController'
      }).
      when('/hawkular-ui/btm/ignored', {
        templateUrl: 'plugins/btm/html/btxnignored.html',
        controller: 'BTM.BTMIgnoredController'
      }).
      when('/hawkular-ui/btm/config/:businesstransaction', {
        templateUrl: 'plugins/btm/html/btxnconfig.html',
        controller: 'BTM.BTxnConfigController',
        resolve: {
          btxn: function($http, $route, $location, toastr) {
            return $http.get('/hawkular/btm/config/businesstxn/full/' +
              $route.current.params.businesstransaction).then(function(resp) {
              if (!resp.data) {
                $location.path('/hawkular-ui/btm');
                toastr.info('You were redirected to this page because you requested an invalid Business Transaction.');
              }
              return resp.data;
            }, function(resp) {
              toastr.info('You were redirected to this page because you requested an invalid Business Transaction.');
              $location.path('/hawkular-ui/btm');
            });
          }
        }
      }).
      when('/hawkular-ui/btm/info/:businesstransaction', {
        templateUrl: 'plugins/btm/html/btxninfo.html',
        controller: 'BTM.BTxnInfoController',
        resolve: {
          btxn: function($http, $route, $location, toastr) {
            return $http.get('/hawkular/btm/config/businesstxn/full/' +
              $route.current.params.businesstransaction).then(function(resp) {
              if (!resp.data) {
                $location.path('/hawkular-ui/btm');
                toastr.info('You were redirected to this page because you requested an invalid Business Transaction.');
              }
              return resp.data;
            }, function(resp) {
              toastr.info('You were redirected to this page because you requested an invalid Business Transaction.');
              $location.path('/hawkular-ui/btm');
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

  hawtioPluginLoader.registerPreBootstrapTask((next) => {
    window['KeycloakConfig'] = '/keycloak.json';
    next();
  }, true);

  hawtioPluginLoader.addModule(BTM.pluginName);
}
