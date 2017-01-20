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

/// <reference path='instanceViewDiagramPlugin.ts'/>
module InstanceViewDiagram {
  declare let dagreD3: any;

  export class InstanceViewDiagramDirective {

    public restrict = 'EA';
    public transclude = false;
    public replace = true;

    //private render = new dagreD3.render();
    public link: (scope, elm, attrs, ctrl) => any;

    constructor(public $compile, $sce, hkDurationFilter) {
      // necessary to ensure 'this' is this object <sigh>
      this.link = (scope, elm, attrs, ctrl) => {
        return this.doLink(scope, elm, attrs, ctrl, $compile, $sce, hkDurationFilter);
      };
    }

    private doLink(scope, elm, attrs, ctrl, $compile, $sce, hkDurationFilter): void {

      // Severity strip width (must be pair)
      let stripeWidth = 12;

      // Set up zoom support
      let svg = d3.select('svg#idetails'),
        inner = svg.select('g'),
        zoom = d3.behavior.zoom().on('zoom', () => {
          inner.attr('transform', 'translate(' + d3.event.translate + ')' +
            'scale(' + d3.event.scale + ')');
        });
      svg.call(zoom);
      let render = new dagreD3.render();
      let g = new dagreD3.graphlib.Graph();

      render.shapes().producer = function(parent, bbox, node) {
        let w = bbox.width + 10,
            h = bbox.height + 10,
            points = [{ x: 0, y: 0 }];
            for (let i = 0; i < stripeWidth; i++) {
              points.push({'x': i, y: (i % 2 === 0 ? 0 : -h)});
              points.push({'x': i, y: (i % 2 !== 0 ? 0 : -h)});
            }
            points = points.concat([
              { x: w,      y: 0 },
              { x: w + 10, y: -h / 2 },
              { x: w,      y: -h },
              { x: 0,      y: -h },
              { x: 0,      y: 0 }
            ]);
            let shapeSvg = parent.insert('polygon', ':first-child')
              .attr('points', points.map(function(d) { return d.x + ',' + d.y; }).join(' '))
              .attr('transform', 'translate(' + (-w / 1.85) + ',' + (h * 4 / 8) + ')');

        node.intersect = function(point) {
          return dagreD3.intersect.polygon(node, points, point);
        };

        return shapeSvg;
      };

      render.shapes().consumer = function(parent, bbox, node) {
        let w = bbox.width + 20,
            h = bbox.height + 10,
            points = [{ x: 0, y: 0 }];
            for (let i = 0; i < stripeWidth + 1; i++) {
              points.push({'x': i, y: (i % 2 === 0 ? 0 : -h)});
              points.push({'x': i + 10, y: -h / 2});
              points.push({'x': i, y: (i % 2 !== 0 ? 0 : -h)});
            }
            points = points.concat([
              { x:  w, y: -h },
              { x:  w, y: 0 },
              { x: 10, y: 0 },
            ]);
            let shapeSvg = parent.insert('polygon', ':first-child')
              .attr('points', points.map(function(d) { return d.x + ',' + d.y; }).join(' '))
              .attr('transform', 'translate(' + (-w / 2.15) + ',' + (h * 4 / 8) + ')');

        node.intersect = function(point) {
          return dagreD3.intersect.polygon(node, points, point);
        };

        return shapeSvg;
      };

      render.shapes().component = function(parent, bbox, node) {
        let w = bbox.width + 20,
            h = bbox.height + 8,
            points = [];
            for (let i = stripeWidth; i >= 0; i--) {
              points.push({'x': i, y: (i % 2 === 0 ? 0 : -h)});
              points.push({'x': i - stripeWidth, y: -h / 2});
              points.push({'x': i, y: (i % 2 !== 0 ? 0 : -h)});
            }
            points = points.concat([
              { x: 10,     y: -h  },
              { x: w - 10, y: -h },
              { x: w,      y: -h / 2 },
              { x: w - 10, y: 0 },
              { x: 10,     y: 0 },
              { x: 0,      y: -h / 2 }
            ]);
            let shapeSvg = parent.insert('polygon', ':first-child')
              .attr('points', points.map(function(d) { return d.x + ',' + d.y; }).join(' '))
              .attr('transform', 'translate(' + (-w / 2.1) + ',' + (h * 4 / 8) + ')');

        node.intersect = function(point) {
          return dagreD3.intersect.polygon(node, points, point);
        };

        return shapeSvg;
      };

      let prevNodes = [];
      let currNodes = [];

      let skipNodes = [];

      function clear() {
        let svg = d3.select('svg#idetails > g');
        svg.selectAll('*').remove();
      }

      function drawNode(d) {
        d.customId = d.customId || 'node' + Math.random();
        let skip = false;
        if (_.indexOf(skipNodes, d.customId) > -1) {
          skipNodes.splice(_.indexOf(skipNodes, d.customId), 1);
          skip = true;
        }
        let theShape = 'rect';
        switch (d.type) {
          case 'Producer':
            theShape = 'producer';
            break;
          case 'Component':
            theShape = 'component';
            break;
          case 'Consumer':
            theShape = 'consumer';
            break;
        }
        if (d.endpointType === null) {
          // look-ahead for internal spawn..
          if(d.nodes && d.nodes.length && d.nodes[0].endpointType === null) {
            let nxtNode = d.nodes[0];
            let showSpawn = true; // TODO: make toggleable or remove ?
            if (!showSpawn) {
              while (!showSpawn && nxtNode.endpointType === null) {
                nxtNode.customId = d.customId;
                nxtNode = nxtNode.nodes[0];
              }
              nxtNode.customId = d.customId;
            } else {
              theShape = 'circle';
              nxtNode.customId = d.customId;
            }
            //
            skipNodes.push(d.nodes[0].customId);
          }
        }
        if (!skip) {
          let serviceNameProp: any = _.find(d.properties, {name: 'service'});
          let serviceName = serviceNameProp ? serviceNameProp.value : '';

          if (d.componentType && d.componentType.toLowerCase() === 'database') {
            serviceName += '<i class="fa fa-database database pull-left"></i>&nbsp;';
          } else if (d.componentType && d.componentType.toLowerCase() === 'ejb') {
            serviceName += '<i class="fa fa-coffee ejb pull-left"></i>&nbsp;';
          }

          let uri = (d.uri || '') + (d.operation ? ('[' + d.operation + ']') : '');

          let label = '<span class="name service-name">' + (serviceName || '&nbsp;') + '</span>';
          label += '<div class="name">' + uri + '</div>';

          let nodeTT = '<ul>';
          let hasFault = false;
          _.each(d.properties, (property) => {
            if (!!property.value) {
              nodeTT += ('<li class=\'tt-prop\'><strong>' + property.name + '</strong> ' + property.value + '</li>');
              if (property.name === 'fault' ||
                (property.name === 'error' && property.value.toLowerCase() !== 'false')) {
                hasFault = true;
              }
            }
          });
          nodeTT += '</ul>';
          let tooltipId = d.customId.replace(/\W+/g, '_');
          scope[tooltipId] = $sce.trustAsHtml(nodeTT);

          let html = '<div class="node-label"' + ((nodeTT.length > 9) ? (' tooltip-append-to-body="true" ' +
            'tooltip-class="graph-tooltip" tooltip-html="' + tooltipId + '"') : '') + '>';
          if (theShape === 'circle') {
            label = '<div><i class="fa fa-share-alt spawn"></i></div>';
          } else {
            label += '<span class="stats">';
            label += '  <span class="duration pull-left">';
            label += '    <i class="fa fa-clock-o"></i>';
            label += '    ' + hkDurationFilter(d.duration);
            label += '  </span>';
            label += '</span>';
            if (hasFault) {
              label += '<span class="pull-right fault-indicator">';
              label += '  <i class="fa fa-warning"></i>';
              label += '</span>';
            }
          }
          label += '</div>';
          html += label;

          /*
          html += '<span class="name">' + d.id + '</span>';
          html += '<span class="stats">';
          html += '  <span class="duration pull-left"><i class="fa fa-clock-o"></i>' + d.averageDuration + 'ms</span>';
          html += '  <span class="node-count pull-right">' + d.count + '<i class="fa fa-clone"></i></span>';
          html += '</span>';
          */
          g.setNode(d.customId , {
            shape: theShape,
            labelType: 'html',
            label: html,
            class: 'entity severity' + d.severity + ' ' + theShape,
            rx: 5,
            ry: 5,
            padding: 0
          });
        }
        if (!angular.equals({}, d.nodes)) {
          _.each(d.nodes, (nd: any) => {
            drawNode(nd);
            if (d.customId !== nd.customId) {
              let classes = '';
              if (d.type === 'Producer' && nd.type === 'Consumer') {
                classes += ' black-line';
              }
              if (nd.endpointType === null) {
                classes += ' dashed-line';
              }
              g.setEdge(d.customId, nd.customId, {
                //labelType: 'html',
                //label: edge.count ? linkHtml : '',
                class: classes,
                width: 40
              });
            }
          });
        }
      }

      function draw(isUpdate) {
        isUpdate = false;
        if (!isUpdate) {
          g = new dagreD3.graphlib.Graph();
          zoom.scale(1);
          zoom.translate([0, 0]);
          inner.attr('transform', '');
          g.setGraph({
            nodesep: 10,
            ranksep: 50,
            // Left-to-right layout
            rankdir: 'LR',
            marginx: 20,
            marginy: 20
          });
        }
        prevNodes = angular.copy(currNodes);
        currNodes = [];

        // force removing existing tooltips, otherwise they'll get sticky
        angular.element('.graph-tooltip').remove();

        _.each(scope[attrs.nodes], (d) => {
          drawNode(d);
        });
        // remove nodes which were present and aren't anymore...
        _.each(prevNodes, (nodeId) => {
          g.removeNode(nodeId);
        });

        // we store the current transform, remove it so d3 draws it properly, and then re-apply it
        let curTransform = inner.attr('transform');
        inner.attr('transform', '');

        let res = inner.call(render, g);
        $compile(res[0])(scope);

        inner.attr('transform', curTransform);

        if (attrs.nodeDrag) {
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
            edge.points.slice(1, edge.points.length - 1);
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
        }
      }

      scope.$watch('rootNode', (value) => {
        if (value && value.length) {
          draw(false);
        } else {
          clear();
        }
      });

      scope.$watch(attrs.nodes, (value) => {
        if (value && value.length) {
          draw(true);
        } else {
          clear();
        }
      });

    }
  }
}
