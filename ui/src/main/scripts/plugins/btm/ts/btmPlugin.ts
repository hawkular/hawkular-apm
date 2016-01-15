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
module BTM {

  export var _module = angular.module(BTM.pluginName, ["xeditable","ui.bootstrap"]);

  var tab = undefined;

  _module.config(["$locationProvider", "$routeProvider", "HawtioNavBuilderProvider",
    ($locationProvider, $routeProvider: ng.route.IRouteProvider, builder: HawtioMainNav.BuilderFactory) => {
    tab = builder.create()
      .id(BTM.pluginName)
      .title(() => "Business Transactions")
      .href(() => "/active")
      .build();
    builder.configureRouting($routeProvider, tab);
    $locationProvider.html5Mode(true);
    $routeProvider.
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

  _module.run(function($http,$location) {
    // Only set authorization if using development URL
    if ($location.absUrl().indexOf('http://localhost:2772/') === 0) {
      $http.defaults.headers.common.Authorization = 'Basic amRvZTpwYXNzd29yZA==';
    }
  });

  _module.run(function(editableOptions) {
    editableOptions.theme = 'bs3'; // bootstrap3 theme. Can be also 'bs2', 'default'
  });

  _module.run(["HawtioNav", (HawtioNav: HawtioMainNav.Registry) => {
    HawtioNav.add(tab);
    log.debug("loaded");
  }]);

  hawtioPluginLoader.addModule(BTM.pluginName);
}
