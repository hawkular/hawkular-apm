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

/// <reference path="../../../includes.ts"/>
/// <reference path="filterSidebarGlobals.ts"/>
/// <reference path="filterSidebarDirective.ts"/>
module FilterSidebar {
  _module.directive('filterSidebar', ['$compile', '$rootScope', '$http', ($compile, $rootScope, $http) => {
    return new FilterSidebar.FilterSidebarDirective($compile, $rootScope, $http);
  }]);

  // from http://stackoverflow.com/a/23882699
  _module.filter('groupBy', ($parse) => {
    return _.memoize((items, field) => {
      let getter = $parse(field);
      return _.groupBy(items, (item) => {
        return getter(item);
      });
    },
    (items) => { // Specific hasher function for filter properties objects
      return _.reduce(items, (hash, obj: any) => {
        return hash + (obj.name + obj.value + obj.operator);
      }, '');
    });
  });

  hawtioPluginLoader.addModule(pluginName);
}
