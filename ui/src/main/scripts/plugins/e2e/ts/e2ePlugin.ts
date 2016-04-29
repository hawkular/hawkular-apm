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
/// <reference path="e2eGlobals.ts"/>
module E2E {

  export var _module = angular.module(E2E.pluginName, ['ui.bootstrap', 'patternfly.select']);

  let tab = undefined;

  _module.config(['$locationProvider', '$routeProvider', 'HawtioNavBuilderProvider',
    ($locationProvider, $routeProvider: ng.route.IRouteProvider, builder: HawtioMainNav.BuilderFactory) => {
    tab = builder.create()
      .id(E2E.pluginName)
      .title(() => 'End-to-End View')
      .href(() => '/hawkular-ui/e2e')
      .build();
    //builder.configureRouting($routeProvider, tab);
    //$locationProvider.html5Mode(true);
    $routeProvider.
      when('/hawkular-ui/e2e', {
        templateUrl: 'plugins/e2e/html/e2e.html',
        controller: 'E2E.E2EController'
      });
  }]);

  _module.run(['HawtioNav', (HawtioNav: HawtioMainNav.Registry) => {
    HawtioNav.add(tab);
    log.debug('loaded');
  }]);

  hawtioPluginLoader.addModule(E2E.pluginName);
}
