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

/// <reference path='timelinePlugin.ts'/>
module Timeline {
  declare let dagreD3: any;

  export class TimelineDirective {

    public restrict = 'EA';
    public transclude = false;
    public replace = true;

    //private render = new dagreD3.render();
    public link: (scope, elm, attrs, ctrl) => any;

    constructor(public $compile, $sce, $location, $timeout, hkDurationFilter) {
      // necessary to ensure 'this' is this object <sigh>
      this.link = (scope, elm, attrs, ctrl) => {
        return this.doLink(scope, elm, attrs, ctrl, $compile, $sce, $location, $timeout, hkDurationFilter);
      };
    }

    private doLink(scope, elm, attrs, ctrl, $compile, $sce, $location, $timeout, hkDurationFilter): void {

      let curNodes = [];
      let revNodes = [];

      let parseNode = function(node, nodes) {
        node.children = _.filter(nodes, (n: any) => {
          return n.references.length > 0 && n.references[0].spanId === node.spanId;
        });
        _.forEach(node.children, (n) => {
          parseNode(n, nodes);
        });
      };

      let reverseRelationships = function(nodes) {
        let tmpRevNodes = [];
        _.forEach(nodes, (n) => {
          if (n.references.length === 0) {
            tmpRevNodes.push(n);
            parseNode(n, nodes);
          }
        });
        return tmpRevNodes;
      };
      revNodes = reverseRelationships(curNodes);

      let drawBars = undefined;

      let maxTime = _.max(scope[attrs['nodes']], (n) => { return n['endTimestamp']; })['endTimestamp'];

      // Configuration

      let barHeight = 24; // height of the timeline bars
      let barSpacing = 2; // spacing between bars

      let showLabels = true; // show labels inside bars (TODO: configure the field to show)
      let labelsOnHover = true; // hide labels and show only on scrubber hover

      let showYAxis = true; // show the y axis
      let yAxisMargin = 200; // show the y axis
      let yAxisIndent = 15; // the indent to the tree items on y axis
      let showLinks = true; // show the links on the tree items on y axis

      let showXAxis = true; // show the x axis
      let xTicks = 7; // number of ticks on the x axis

      let draggable = true; // ability to drag the chart
      let dragSpeed = 15; // factor to multiply mouse cursor change when dragging

      let scrubbable = true; // show the scrubber, a line following mouse cursor with timestamp

      let xExtraTime = 0; // add extra time besides last end timestamp

      let brushable = false; // in case it's overview
      let brushSize = parseInt(attrs['range'], 10) || (scope['tlViewRange'] ? scope['tlViewRange'][1] : maxTime);
      scope['tlViewRange'] = [0, brushSize];

      let transDuration = 0; // duration of transitions

      let _hideScrubber = false; // internal var to hide the scrubber when hovering logs

      let colors = [
        '#1f77b4', '#aec7e8',
        '#ff7f0e', '#ffbb78',
        '#2ca02c', '#98df8a',
        '#d62728', '#ff9896',
        '#9467bd', '#c5b0d5',
        '#8c564b', '#c49c94',
        '#e377c2', '#f7b6d2',
        '#7f7f7f', '#c7c7c7',
        '#bcbd22', '#dbdb8d',
        '#17becf', '#9edae5',
      ];

      let toggleBar = function(n) {
        let node: any = n;
        if (node.children) {
          node._children = node.children;
          delete node.children;
        } else {
          node.children = node._children;
          delete node._children;
        }
        drawBars();
      };

      let margin = {top: 20, right: 10, bottom: 20, left: 10 + (showYAxis ? yAxisMargin : 0)};

      let isOverview = attrs.hasOwnProperty('timelineOverview');
      if (isOverview) {
        barHeight = 3;
        barSpacing = 1;
        margin = {top: 20, right: 10, bottom: 20, left: 10};
        showXAxis = false;
        showYAxis = false;
        showLabels = false;
        draggable = false;
        scrubbable = false;
        brushable = true;
        // brushSize = scope[attrs['brushRange']];
      }

      // this is so that firefox and safari works properly with svg and <base href="/">
      let absoluteRef = function (id) {
        return 'url(' + $location.absUrl() + '#' + id + ')';
      };

      let showInfo = function(span) {
        $timeout(() => {
          scope.selectedSpan = span;
        }, 0);
      };

      let width = elm[0].clientWidth - margin.left - margin.right;
      let actualHeight = scope[attrs['nodes']].length * (barHeight + barSpacing);
      let height = actualHeight + margin.top + margin.bottom;

      let svg = d3.select('#timeline-container').append('svg')
                                                .attr('width', elm[0].clientWidth - margin.right)
                                                .attr('height', height + margin.top + margin.bottom);

      let xGridLines = svg.append('g').attr('class', 'x-grid-lines')
                                      .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');
      let labels = svg.append('g').attr('class', 'labels')
                                      .attr('transform', 'translate(' + margin.left + ',' + (margin.top + 5) + ')');
      let content = svg.append('g').attr('class', 'content')
                                   .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

      content.append('g').attr('class', 'bars');

      // Axis

      let yAxisScale = d3.scale.linear()
                               .domain([0, actualHeight])
                               .range([0, actualHeight]);

      if (showYAxis) {
        let yAxis = d3.svg.axis()
                          .scale(yAxisScale)
                          .orient('left')
                          .ticks(0);

        svg.append('g')
            .attr('class', 'y axis')
            .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')')
            .call(yAxis);
      }

      let xAxisScale = d3.scale.linear()
                         .domain([0, isOverview ? (maxTime + xExtraTime) : brushSize])
                         .range([0, width]);

      let xAxisTop = undefined, xAxisBottom = undefined, xAxisGroupT = undefined, xAxisGroupB = undefined;
      if (showXAxis) {

        xAxisTop = d3.svg.axis()
                          .scale(xAxisScale)
                          .orient('top')
                          .tickFormat((d) => { return d + (d > 0 ? 'ms' : ''); })
                          .ticks(xTicks);

        xAxisBottom = d3.svg.axis()
                                .scale(xAxisScale)
                                .orient('bottom')
                                .tickFormat((d) => { return d + (d > 0 ? 'ms' : ''); })
                                .ticks(xTicks);

        xAxisGroupT = svg.append('g')
                             .attr('class', 'x axis top')
                             .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')')
                             .call(xAxisTop);

        xAxisGroupB = svg.append('g')
                             .attr('transform', 'translate(' + margin.left + ',' + (margin.top + actualHeight) + ' )')
                             .attr('class', 'x axis bottom')
                             .call(xAxisBottom);

        xGridLines.selectAll('.x-grid-lines').data(xAxisScale.ticks(xTicks)).enter().append('line')
          .attr(
          {
              'class': 'horizontalGrid',
              'x1' : (d) => { return xAxisScale(d); },
              'x2' : (d) => { return xAxisScale(d); },
              'y1' : actualHeight,
              'y2' : 0,
              'fill' : 'none',
              'shape-rendering' : 'crispEdges',
              'stroke' : '#DADADA',
              'stroke-width' : '1px'
          });
      }

      scope.$watch(attrs['nodes'], (newNodes) => {
        curNodes = angular.copy(newNodes);
        maxTime = _.max(curNodes, (n) => { return n['endTimestamp']; })['endTimestamp'];
        actualHeight = curNodes.length * (barHeight + barSpacing);
        svg.attr('height', actualHeight + margin.top + margin.bottom);
        svg.select('#timeline-clipper rect').attr('height', actualHeight + margin.top + margin.bottom);
        svg.select('.x.axis.bottom')
          .attr('transform', 'translate(' + margin.left + ',' + (margin.top + actualHeight) + ' )');
        yAxisScale.domain([0, actualHeight])
                  .range([0, actualHeight]);
        let yAxis = d3.svg.axis()
                          .scale(yAxisScale)
                          .orient('left')
                          .ticks(0);
        svg.select('.y.axis').call(yAxis);
        if (isOverview) {
          barHeight = Math.min(height / curNodes.length - barSpacing, 3);
          xAxisScale.domain([0, maxTime]);
        } else {
          // if we want to resize bars to a predefined height instead...
          // barHeight = (height - margin.top - margin.bottom) / curNodes.length - barSpacing;
        }
        revNodes = reverseRelationships(curNodes);
        drawBars();
      }, true);

      // Bars

      let colorMap = {};

      let getServiceColor = function(serviceName) {
        if (!colorMap[serviceName]) {
          colorMap[serviceName] = colors[Object.keys(colorMap).length * 2];
        }
        return colorMap[serviceName];
      };

      drawBars = function() {
        let treeNodes = d3.layout.tree().nodes({label: 'root', children: revNodes}).slice(1);

        // select the existing bars
        let bars = content.select('.bars').selectAll('.bar').data(treeNodes, (n) => { return n.spanId; } );

        // add new duration bars
        let barsEnter = bars.enter().append('g').attr('class', 'bar')
                                                .attr('id', (d) => { return 'span-id-' + d.spanId; });
        // update existing bars
        bars.select('rect').transition().duration(transDuration)
                           .attr('x', (d) => { return xAxisScale(d.startTimestamp); })
                           .attr('y', (d, i) => { return i * (barHeight + barSpacing); })
                           .attr('height', barHeight)
                           .attr('width', (d) => { return xAxisScale(d.endTimestamp) - xAxisScale(d.startTimestamp); });

        // update existing bars text labels (on the bar itself)
        bars.select('text').attr('x', (d, i) => { return xAxisScale(d.startTimestamp) + 2; })
                           .attr('y', (d, i) => { return i * (barHeight + barSpacing); })
                           .style('font-size', (d) => { return Math.floor(barHeight * .7) + 'px'; });

        let barsEnterDurationGroup = barsEnter.append('g');
        barsEnterDurationGroup.append('rect').attr('x', (d) => { return xAxisScale(d.startTimestamp); })
                        .attr('y', (d, i) => { return i * (barHeight + barSpacing); })
                        .attr('height', barHeight)
                        .attr('width', (d) => { return xAxisScale(d.endTimestamp) - xAxisScale(d.startTimestamp); })
                        .style('fill', (d, i) => { return getServiceColor(_.find(d.tags, 'key', 'service')['value']); })
                        .on('click', (d) => { showInfo(d); })
                        .on('dblclick', (d) => {
                          scope.changeRange(Math.max(0, d.startTimestamp - 100),
                            Math.min(maxTime, d.endTimestamp + 100));
                        });

        if (showLabels) {
          barsEnterDurationGroup.append('text').attr('x', (d, i) => { return xAxisScale(d.startTimestamp) + 3; })
                                              .attr('y', (d, i) => { return i * (barHeight + barSpacing); })
                                              .attr('dy', '0.6em')
                                              .attr('class', 'timeline-hk-op-label')
                                              .classed('timeline-hk-op-label-hover', labelsOnHover)
                                              .text((d) => { return d.operationName; })
                                              .style('font-size', (d) => {
                                                return Math.floor(barHeight * .7) + 'px';
                                              })
                                              .on('click', (d) => { showInfo(d); })
                                              .on('dblclick', (d) => {
                                                scope.changeRange(Math.max(0, d.startTimestamp - 100),
                                                  Math.min(maxTime, d.endTimestamp + 100));
                                              });

        }

        bars.selectAll('.timeline-hk-op-label').attr('x', (d) => {
          let xPos = xAxisScale(d.startTimestamp);
          xPos = xPos < 0 ? 0 : xPos;
          return xPos + 3;
        });

        bars.selectAll('.timeline-hk-log').transition().duration(transDuration)
                                          .attr('cx', (d) => { return xAxisScale(d.timestamp); })
                                          .attr('cy', (d, i, x) => {
                                            return x * (barHeight + barSpacing) + (barHeight / 2);
                                          });

        barsEnterDurationGroup.append('g').each(function(span, spanIdx) {
          let tt = d3.tip()
                     .attr('class', 'd3-tip')
                     .offset([-10, 0])
                     .html((d) => {
                       let ttHtml = '<div class="pull-right"><i class="fa fa-clock-o"></i> ' + d.timestamp + 'ms</div>';
                       ttHtml += '<hr style="margin-bottom: 6px;"><div><ul>';
                       _.each(d.keyValue, (kv) => {
                         ttHtml += ('<li class="tt-prop"><strong>' + kv.key + '</strong> ' + kv.value + '</li>');
                       });
                       ttHtml += '</ul></div>';
                       return ttHtml;
                     });
          svg.call(tt);

          let logs = d3.select(this).selectAll('.timeline-hk-log').data(span.logs);

          logs.enter().append('circle')
                         .attr('cx', (d) => { return xAxisScale(d.timestamp); })
                         .attr('cy', (d) => { return spanIdx * (barHeight + barSpacing) + (barHeight / 2); })
                         .attr('r', 4)
                         .attr('idx', spanIdx)
                         .attr('class', (d) => { return 'timeline-hk-log ts-' + d.timestamp; })
                         .on('mouseover', (d) => {
                           _hideScrubber = true;
                           d3.select('circle.ts-' + d.timestamp).attr('r', 6);
                           tt.show(d);
                         })
                         .on('mouseout', (d) => {
                           _hideScrubber = false;
                           d3.select('circle.ts-' + d.timestamp).attr('r', 4);
                           tt.hide(d); });

          logs.exit().remove();
        });

        if (showXAxis) {
          svg.selectAll('.x.axis.top').call(xAxisTop);
          svg.selectAll('.x.axis.bottom').call(xAxisBottom);

          svg.selectAll('.horizontalGrid').remove();
          xGridLines.selectAll('.x-grid-lines').data(xAxisScale.ticks(xTicks)).enter().append('line')
          .attr(
          {
              'class': 'horizontalGrid',
              'x1' : (d) => { return xAxisScale(d); },
              'x2' : (d) => { return xAxisScale(d); },
              'y1' : actualHeight,
              'y2' : 0,
              'fill' : 'none',
              'shape-rendering' : 'crispEdges',
              'stroke' : '#DADADA',
              'stroke-width' : '1px'
          });
        }

        if (showYAxis) {
          // FIXME: For some reason just updating (when collapsing) is not working properly...
          labels.selectAll('.timeline-hk-y-label').remove();

          if (showLinks) {
            let links = d3.layout.tree().links(treeNodes);
            let linkItems = labels.selectAll('.link').data(links);

            linkItems.enter().insert('path', ':first-child').attr('class', 'link')
                                                            .style({
                                                              'stroke': '#DADADA',
                                                              'stroke-width': 1,
                                                              'fill': 'none'
                                                            });

            linkItems.exit().remove();

            linkItems.attr('d', (d, i) => {
              let sY = treeNodes.indexOf(d.source) * (barHeight + barSpacing);
              let tY = treeNodes.indexOf(d.target) * (barHeight + barSpacing);
              return 'M' + (-208 + d.source.depth * yAxisIndent) + ',' + yAxisScale(sY + 3) +
                'V' + yAxisScale(tY + 3) + 'H' + (-208 + d.target.depth * yAxisIndent);
            });
          }

          let yAxisLabels = labels.selectAll('.timeline-hk-y-label').data(treeNodes, (n) => { return n.spanId; } );
          let yAxisLabelsEnter = yAxisLabels.enter().append('g').attr('class', 'timeline-hk-y-label')
                                          .style('cursor', (d) => {
                                            return d.children || d._children ? 'pointer' : 'default';
                                          })
                                          .on('click', (d) => { toggleBar(d); });

          yAxisLabelsEnter.append('circle').attr('r', (d) => { return d.children || d._children ? 4 : 2; })
                                          .attr('cx', (d, i) => { return -margin.left + 2 + (yAxisIndent * d.depth); })
                                          .attr('cy', (d, i) => { return i * (barHeight + barSpacing) + 4; })
                                          .style('stroke-width', (d) => {
                                            return d.children || d._children ? 2 : 3;
                                          })
                                          .style('stroke', (d) => {
                                            return getServiceColor(_.find(d.tags, 'key', 'service')['value']);
                                          })
                                          .style('fill', (d) => {
                                            return d._children ?
                                              getServiceColor(_.find(d.tags, 'key', 'service')['value']) : 'white';
                                          });

          yAxisLabelsEnter.append('text').attr('dy', 5)
                                          .attr('class', 'timeline-hk-label')
                                          .attr('x', (d, i) => { return -margin.left + 10 + (yAxisIndent * d.depth); })
                                          .attr('y', (d, i) => { return i * (barHeight + barSpacing); })
                                          .attr('dy', '0.6em')
                                          .text((d) => { return _.find(d.tags, 'key', 'service')['value']; })
                                          .style('font-size', (d) => {
                                            return Math.floor(barHeight * .6) + 'px';
                                          });

          yAxisLabels.exit().remove();
        }

        bars.exit().remove();
      };

      let rangeChange = function(newRange, oldRange) {
        if (!newRange || !oldRange) {
          return;
        }
        brushSize = newRange[1] - newRange[0];
        xAxisScale.domain(newRange);
        drawBars();
      };

      scope.changeRange = function(newRangeX1, newRangeX2) {
        scope['tlViewRange'] = [newRangeX1 > 0 ? newRangeX1 : 0 , newRangeX2];
      };

      // Scrubber

      if (scrubbable) {
        let scrubber = content.append('g').attr('class', 'scrubber');
        scrubber.append('line')
          .attr('x1', -2)
          .attr('x2', -2)
          .attr('y1', 0)
          .attr('y2', actualHeight)
          .attr('class', 'timeline-hk-marker');
        scrubber.append('text').text((d) => { return d; });

        let scrub = function(coordinates) {
          let x = coordinates[0] > margin.left ? (coordinates[0] - margin.left) : 0;
          let y = coordinates[1] - margin.top;
          scrubber.attr('opacity', (_hideScrubber && x > 0) ? 0 : 1);
          let currentTimePos = xAxisScale.invert(x);
          let doScrub = currentTimePos > 0 && y <= actualHeight && y > 0;
          if (doScrub) {
            scrubber.attr('transform', 'translate(' + x + ', 0)');
            scrubber.select('text').text(parseInt(currentTimePos, 10) + 'ms')
                                   .attr('transform', 'translate(5,' + y + ')');
            let bars = svg.select('.content').selectAll('.bar');
            bars.each(function(bar) {
              d3.select(this).classed('hover', (x) => {
                return currentTimePos >= bar.startTimestamp && currentTimePos <= bar.endTimestamp;
              });
            });
          } else {
            scrubber.attr('transform', 'translate(0, 0)');
            scrubber.select('text').text('');
            svg.select('.content').selectAll('.bar').classed('hover', false);
          }
        };

        svg.on('mousemove', function() {
          let coordinates = d3.mouse(this);
          scrub(coordinates);
        });
      }

      if (draggable) {
        let drag = d3.behavior.drag();
        drag.on('dragstart', () => {
          svg.style('cursor', 'move');
        });
        drag.on('dragend', () => {
          svg.style('cursor', 'default');
        });
        drag.on('drag', () => {
          $timeout((event) => {
            if (event && event.dx) {
              let newX1 = Math.max(0, scope['tlViewRange'][0] - event.dx * dragSpeed);
              let newX2 = Math.min(newX1 + brushSize, maxTime + xExtraTime);
              newX1 = Math.min(newX1, newX2 - brushSize);
              scope.changeRange(newX1, newX2);
              return;
            }
          }, 0, true, d3.event);
          d3.event.sourceEvent.stopPropagation(); // silence other listeners
        });
        svg.call(drag);
      }

      if (brushable) {
        let brush = d3.svg.brush()
                          .x(xAxisScale)
                          .extent([0, brushSize]);

        scope.$watch('tlViewRange', (newRange, oldRange) => {
          brush.extent(newRange || [0, brushSize]);
          brush(d3.select('.timeline-hk-brush'));
        });

        brush.on('brush', function() {
          $timeout(function() {
            scope['tlViewRange'] = brush.extent();
          });
        });

        let g = content.append('g').attr('class', 'timeline-hk-brush');

        brush(g);

        g.selectAll('rect').attr('height', height);
      }

      if (!isOverview) {
        let mask = svg.append('defs')
                      .append('clipPath')
                      .attr('id', 'timeline-clipper');

        mask.append('rect')
            .attr('x', 0)
            .attr('y', 0)
            .attr('width', width)
            .attr('height', height)
            .attr('class', 'rect-mask')
            .style('fill', 'white')
            .style('opacity', 1);

        content.attr('clip-path', absoluteRef('timeline-clipper'));

        scope.$watch('tlViewRange', (newRange, oldRange) => {
          rangeChange(newRange, oldRange);
        });
      }
    }
  }
}
