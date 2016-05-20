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
    ['xeditable','ui.bootstrap','angularUtils.directives.dirPagination','hawkularbtm-templates']);

  let tab = undefined;

  _module.config(['$locationProvider', '$routeProvider', 'HawtioNavBuilderProvider',
    ($locationProvider, $routeProvider: ng.route.IRouteProvider, builder: HawtioMainNav.BuilderFactory) => {
    tab = builder.create()
      .id(APM.pluginName)
      .title(() => 'Application Performance')
      .href(() => '/hawkular-ui/apm')
      .rank(30)
      .build();
    builder.configureRouting($routeProvider, tab);
    $locationProvider.html5Mode(true);
    $routeProvider.
      when('/hawkular-ui/apm', {
        templateUrl: 'plugins/apm/html/apm.html',
        controller: 'APM.APMController'
      }).
      when('/', { redirectTo: '/hawkular-ui/apm' });
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
