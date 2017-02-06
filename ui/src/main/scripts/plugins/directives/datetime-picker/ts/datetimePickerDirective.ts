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

/// <reference path='datetimePickerPlugin.ts'/>
module DatetimePicker {
  declare let moment: any;

  export class DatetimePickerDirective {

    public require = '?ngModel';
    public restrict = 'EA';
    public transclude = false;
    public replace = true;

    public scope = {
      options: '=',
      onChange: '&',
      onClick: '&',
      minDate: '@',
      maxDate: '@'
    };

    public link: (scope, elm, attrs, ctrl) => any;

    constructor(public $timeout) {
      // necessary to ensure 'this' is this object <sigh>
      this.link = (scope, elm, attrs, ctrl) => {
        return this.doLink(scope, elm, attrs, ctrl, $timeout);
      };
    }

    private doLink(scope, elm, attrs, ctrl, $timeout): void {

      scope.$watch(scope.minDate, (newValue, oldValue) => {
        if (newValue) {
          elm.data('DateTimePicker').minDate(newValue);
        }
      });

      scope.$watch(scope.maxDate, (newValue, oldValue) => {
        if (newValue) {
          elm.data('DateTimePicker').maxDate(newValue);
        }
      });

      elm.on('dp.change', (e) => {
        $timeout(() => {
          let dtp = elm.data('DateTimePicker');
          ctrl.$setViewValue(dtp.date());
          scope.onChange();
        });
      });

      elm.on('click', (e) => {
        scope.onClick(e);
      });

      ctrl.$render = () => {
        if (!!ctrl) {
          if (ctrl.$viewValue === undefined) {
            ctrl.$viewValue = null;
          } else if (!(ctrl.$viewValue instanceof moment)) {
            ctrl.$viewValue = moment(ctrl.$viewValue);
          }
          elm.data('DateTimePicker').date(ctrl.$viewValue);
        }
      };

      elm.datetimepicker(scope.options);
    }

  }
}
