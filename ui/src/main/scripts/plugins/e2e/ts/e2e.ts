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

    // get top level nodes
    $scope.findTopLevels = function() {
      $scope.topLevel = [];
      $scope.outbounds = [];
      $scope.reverseInbounds = [];
      _.each($scope.sampleData, (node) => {
        $scope.topLevel.push(node.id);
        let outbounds = Object.keys(node.outbound);
        $scope.outbounds = _.union($scope.outbounds, outbounds);
      });
      $scope.topLevel = _.xor($scope.topLevel, $scope.outbounds);
    };

    let doFilter = function(nodeId, clear) {
      if (clear) {
        $scope.allNodes = angular.copy($scope.sampleData);
        $scope.filteredNodes = [];
      }
      let filtered = _.remove($scope.allNodes, (node: any) => {
        return node.id === nodeId;
      });
      $scope.filteredNodes.push(filtered[0]);
      _.each(filtered, (node: any) => {
        _.each(Object.keys(node.outbound), (outbound) => {
          doFilter(outbound, false);
        });
      });
    };

    $scope.filterByTopLevel = function(nodeId, clear) {
      doFilter(nodeId, true);
      $scope.drawGraph();
    };

    $scope.drawGraph = function() {
      // Set up zoom support
      let svg = d3.select('svg'),
        inner = svg.select('g'),
        zoom = d3.behavior.zoom().on('zoom', () => {
          inner.attr('transform', 'translate(' + d3.event.translate + ')' +
            'scale(' + d3.event.scale + ')');
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
        _.each(/*$scope.sampleData*/$scope.filteredNodes, (d) => {
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

      // code for supporting node drag (adapted from http://jsfiddle.net/egfx43hs/11/)

      let safeId = function(id) {
        return id.replace(/[\[|&;$%@"<>()+,/\]]/g, '_');
      };

      //give IDs to each of the nodes so that they can be accessed
      svg.selectAll('g.node rect').attr('id', (d) => {
        return 'node' + safeId(d);
      });
      svg.selectAll('g.edgePath path').attr('id', (e) => {
        return safeId(e.v) + '-' + safeId(e.w);
      });
      svg.selectAll('g.edgeLabel g').attr('id', (e) => {
        return 'label_' + safeId(e.v) + '-' + safeId(e.w);
      });

      g.nodes().forEach((v) => {
        let node = g.node(v);
        node.customId = 'node' + safeId(v);
      });
      g.edges().forEach((e) => {
        let edge = g.edge(e.v, e.w);
        edge.customId = safeId(e.v) + '-' + safeId(e.w);
      });

      let nodeDrag = d3.behavior.drag().on('dragstart', dragstart).on('drag', dragmove);

      let edgeDrag = d3.behavior.drag()
      .on('dragstart', dragstart)
      .on('drag', (d) => {
        translateEdge(g.edge(d.v, d.w), d3.event.dx, d3.event.dy);
        $('#' + g.edge(d.v, d.w).customId).attr('d', calcPoints(d));
      });

      nodeDrag.call(svg.selectAll('g.node'));
      edgeDrag.call(svg.selectAll('g.edgePath'));

      function dragstart(d) {
        d3.event.sourceEvent.stopPropagation();
      }

      function dragmove(d) {
        let node = d3.select(this),
            selectedNode = g.node(d);
        let prevX = selectedNode.x,
            prevY = selectedNode.y;

        selectedNode.x += d3.event.dx;
        selectedNode.y += d3.event.dy;
        node.attr('transform', 'translate(' + selectedNode.x + ',' + selectedNode.y + ')');

        let dx = selectedNode.x - prevX,
            dy = selectedNode.y - prevY;

        g.edges().forEach((e) => {
          if (e.v === d || e.w === d) {
            let edge = g.edge(e.v, e.w);
            translateEdge(g.edge(e.v, e.w), dx, dy);
            $('#' + edge.customId).attr('d', calcPoints(e));
            let label = $('#label_' + edge.customId);
            let xforms = label.attr('transform');
            let parts = /translate\(\s*([^\s,)]+)[ ,]([^\s,)]+)/.exec(xforms);
            let X = parseInt(parts[1], 10) + dx, Y = parseInt(parts[2], 10) + dy;
            label.attr('transform', 'translate(' + X + ',' + Y + ')');
          }
        });
      }

      function translateEdge(e, dx, dy) {
        e.points.forEach((p) => {
          p.x = p.x + dx;
          p.y = p.y + dy;
        });
      }

      // taken from dagre-d3 source code (not the exact same)
      function calcPoints(e) {
        let edge = g.edge(e.v, e.w),
            tail = g.node(e.v),
            head = g.node(e.w);
        let points = edge.points.slice(1, edge.points.length - 1);
        /*let afterslice = */edge.points.slice(1, edge.points.length - 1);
        points.unshift(intersectRect(tail, points[0]));
        points.push(intersectRect(head, points[points.length - 1]));
        return d3.svg.line()
          .x((d) => {
            return d.x;
          })
          .y((d) => {
            return d.y;
          })
          .interpolate('linear')
          (points);
        }

      // taken from dagre-d3 source code (not the exact same)
      function intersectRect(node, point) {
        let x = node.x;
        let y = node.y;
        let dx = point.x - x;
        let dy = point.y - y;
        let w = parseInt($('#' + node.customId).attr('width'), 10) / 2;
        let h = parseInt($('#' + node.customId).attr('height'), 10) / 2;
        let sx = 0,
            sy = 0;
        if (Math.abs(dy) * w > Math.abs(dx) * h) {
          // Intersection is top or bottom of rect.
          if (dy < 0) {
            h = -h;
          }
          sx = dy === 0 ? 0 : h * dx / dy;
          sy = h;
        } else {
          // Intersection is left or right of rect.
          if (dx < 0) {
            w = -w;
          }
          sx = w;
          sy = dx === 0 ? 0 : w * dy / dx;
        }
        return {
          x: x + sx,
          y: y + sy
        };
      }
    };

    $scope.findTopLevels();

  }]);

}
