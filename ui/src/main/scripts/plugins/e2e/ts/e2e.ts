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

/// <reference path="e2ePlugin.ts"/>
module E2E {

  declare let dagreD3: any;

  export let E2EController = _module.controller('E2E.E2EController', ['$scope', '$routeParams', '$http', '$location',
    '$interval', '$timeout', ($scope, $routeParams, $http, $location, $interval, $timeout) => {

    // some sample data while we don't have an actual endpoint
    $scope.sampleData =
    [
    {
      'averageDuration': 342,
      'count': 1,
      'id': '/items[POST]',
      'maximumDuration': 342,
      'minimumDuration': 342,
      'outbound': {}
    },
    {
      'averageDuration': 375,
      'count': 2,
      'id': '/search[GET]',
      'maximumDuration': 734,
      'minimumDuration': 17,
      'outbound': {}
    },
    {
      'averageDuration': 2296,
      'count': 2,
      'id': '/book[GET]',
      'maximumDuration': 3527,
      'minimumDuration': 1065,
      'outbound': {
        '/book/14293[GET]': {
          'averageLatency': 130,
          'count': 2,
          'maximumLatency': 256,
          'minimumLatency': 4
        }
      }
    },
    {
      'averageDuration': 275,
      'count': 2,
      'id': '/book/14293[GET]',
      'maximumDuration': 548,
      'minimumDuration': 2,
      'outbound': {}
    },
    {
      'averageDuration': 2979,
      'count': 2,
      'id': '/items[GET]',
      'maximumDuration': 4203,
      'minimumDuration': 1755,
      'outbound': {
        '/book[GET]': {
          'averageLatency': 121,
          'count': 1,
          'maximumLatency': 121,
          'minimumLatency': 121
        }
      }
    }
    ];

    $scope.drawGraph = function() {

      // Set up zoom support
      let svg = d3.select('svg'),
      inner = svg.select('g'),
      zoom = d3.behavior.zoom().on('zoom', function() {
        inner.attr('transform', 'translate(' + d3.event.translate + ')' + 'scale(' + d3.event.scale + ')');
      });
      svg.call(zoom);
      let render = new dagreD3.render();
      // Left-to-right layout
      let g = new dagreD3.graphlib.Graph();
      g.setGraph({
        nodesep: 10,
        ranksep: 50,
        rankdir: 'LR',
        marginx: 20,
        marginy: 20
      });

      function draw(isUpdate) {
        _.each($scope.sampleData, (d) => {
          let className = d.averageDuration < 500 ? 'success' : 'danger';
          let html = '<div>';
          html += '<span class="status"></span>';
          html += '<span class="node-count pull-right">' + d.count + '</span>';
          html += '<span class="name">' + d.id + '</span>';
          html += '<span class="stats"><span class="duration">' + d.averageDuration + '</span></span>';
          html += '</div>';
          g.setNode(d.id, {
            labelType: 'html',
            label: html,
            rx: 5,
            ry: 5,
            padding: 0,
            class: className
          });
          if (!angular.equals({}, d.outbound)) {
            let nd: any = Object.keys(d.outbound);
            g.setEdge(d.id, Object.keys(d.outbound)[0], {
              label: d.outbound[nd].count,
              class: '',
              width: 40
            });
          }
        });

        inner.call(render, g);
      }

      draw(false);
    };
  }

]);

}
