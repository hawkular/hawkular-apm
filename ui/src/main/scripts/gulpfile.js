/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var gulp = require('gulp'),
    wiredep = require('wiredep').stream,
    eventStream = require('event-stream'),
    gulpLoadPlugins = require('gulp-load-plugins'),
    map = require('vinyl-map'),
    fs = require('fs'),
    path = require('path'),
    size = require('gulp-size'),
    uri = require('URIjs'),
    s = require('underscore.string'),
    hawtio = require('hawtio-node-backend'),
    tslint = require('gulp-tslint'),
    tslintRules = require('./tslint.json');

var plugins = gulpLoadPlugins({});
var pkg = require('./package.json');

var config = {
  main: '.',
  ts: ['plugins/**/*.ts'],
  testTs: ['test-plugins/**/*.ts'],
  less: './less/**/*.less',
  templates: ['plugins/**/*.html'],
  testTemplates: ['test-plugins/**/*.html'],
  templateModule: pkg.name + '-templates',
  testTemplateModule: pkg.name + '-test-templates',
  dist: './dist/',
  js: pkg.name + '.js',
  testJs: pkg.name + '-test.js',
  css: pkg.name + '.css',
  tsProject: plugins.typescript.createProject({
    target: 'ES5',
    module: 'commonjs',
    declarationFiles: true,
    noExternalResolve: false,
    removeComments: true
  }),
  testTsProject: plugins.typescript.createProject({
    target: 'ES5',
    module: 'commonjs',
    declarationFiles: false,
    noExternalResolve: false
  }),
  tsLintOptions: {
    rulesDirectory: './tslint-rules/'
  }
};

var normalSizeOptions = {
    showFiles: true
}, gZippedSizeOptions  = {
    showFiles: true,
    gzip: true
};


gulp.task('bower', function() {
  return gulp.src('index.html')
    .pipe(wiredep({}))
    .pipe(gulp.dest('.'));
});

/** Adjust the reference path of any typescript-built plugin this project depends on */
gulp.task('path-adjust', function() {
  return gulp.src('libs/**/includes.d.ts')
    .pipe(map(function(buf, filename) {
      var textContent = buf.toString();
      var newTextContent = textContent.replace(/"\.\.\/libs/gm, '"../../../libs');
      // console.log("Filename: ", filename, " old: ", textContent, " new:", newTextContent);
      return newTextContent;
    }))
    .pipe(gulp.dest('libs'));
});

gulp.task('clean-defs', function() {
  return gulp.src('defs.d.ts', { read: false })
    .pipe(plugins.clean());
});

gulp.task('tsc', ['clean-defs'], function() {
  var cwd = process.cwd();
  var tsResult = gulp.src(config.ts)
    .pipe(plugins.sourcemaps.init())
    .pipe(plugins.typescript(config.tsProject))
    .on('error', plugins.notify.onError({
      message: '#{ error.message }',
      title: 'Typescript compilation error'
    }));

    return eventStream.merge(
      tsResult.js
        .pipe(plugins.concat('compiled.js'))
        .pipe(plugins.sourcemaps.write())
        .pipe(gulp.dest('.')),
      tsResult.dts
        .pipe(gulp.dest('d.ts')))
        .pipe(map(function(buf, filename) {
          if (!s.endsWith(filename, 'd.ts')) {
            return buf;
          }
          var relative = path.relative(cwd, filename);
          fs.appendFileSync('defs.d.ts', '/// <reference path="' + relative + '"/>\n');
          return buf;
        }));
});

gulp.task('tslint', function(){
  gulp.src(config.ts)
    .pipe(tslint(config.tsLintOptions))
    .pipe(tslint.report('verbose'));
});

gulp.task('tslint-watch', function(){
  gulp.src(config.ts)
    .pipe(tslint(config.tsLintOptions))
    .pipe(tslint.report('prose', {
      emitError: false
    }));
});

gulp.task('less', function () {
  return gulp.src(config.less)
    .pipe(plugins.less({
      paths: [ path.join(__dirname, 'less', 'includes') ]
    }))
    .pipe(plugins.concat(config.css))
    .pipe(gulp.dest('./dist'));
});

gulp.task('template', ['tsc'], function() {
  return gulp.src(config.templates)
    .pipe(plugins.angularTemplatecache({
      filename: 'templates.js',
      root: 'plugins/',
      standalone: true,
      module: config.templateModule,
      templateFooter: '}]); hawtioPluginLoader.addModule("' + config.templateModule + '");'
    }))
    .pipe(gulp.dest('.'));
});

gulp.task('concat', ['template'], function() {
  var gZipSize = size(gZippedSizeOptions);
  var license = tslintRules.rules['license-header'][1];
  return gulp.src(['compiled.js', 'templates.js', 'js/**/*.js'])
    .pipe(plugins.concat(config.js))
    .pipe(plugins.header(license))
    .pipe(size(normalSizeOptions))
    .pipe(gZipSize)
    .pipe(gulp.dest(config.dist));
});

gulp.task('clean', ['concat'], function() {
  return gulp.src(['templates.js', 'compiled.js'], { read: false })
    .pipe(plugins.clean());
});

gulp.task('watch', ['build'], function() {
  plugins.watch(['libs/**/*.js', 'libs/**/*.css', 'index.html', config.dist + '/' + config.js], function() {
    gulp.start('reload');
  });
  plugins.watch(['libs/**/*.d.ts', config.ts, config.templates], function() {
    gulp.start(['tslint-watch', 'tsc', 'template', 'concat', 'clean']);
  });
  plugins.watch(config.less, function(){
    gulp.start('less', 'reload');
  })
});


gulp.task('connect', ['watch'], function() {
  /*
   * Example of fetching a URL from the environment, in this case for kubernetes
  var kube = uri(process.env.KUBERNETES_MASTER || 'http://localhost:8080');
  console.log("Connecting to Kubernetes on: " + kube);
  */
  var hawkularbtm = uri('http://localhost:8180/hawkular/btm');

  hawtio.setConfig({
    port: 2772,
    staticProxies: [
    {
      proto: hawkularbtm.protocol(),
      hostname: hawkularbtm.hostname(),
      port: hawkularbtm.port(),
      path: '/hawkular/btm',
      targetPath: '/hawkular/btm'
    }
    /*
    // proxy to a service, in this case kubernetes
    {
      proto: kube.protocol(),
      port: kube.port(),
      hostname: kube.hostname(),
      path: '/services/kubernetes',
      targetPath: kube.path()
    },
    // proxy to a jolokia instance
    {
      proto: kube.protocol(),
      hostname: kube.hostname(),
      port: kube.port(),
      path: '/jolokia',
      targetPath: '/hawtio/jolokia'
    }
    */
    ],
    staticAssets: [{
      path: '/',
      dir: '.'

    }],
    fallback: 'index.html',
    liveReload: {
      enabled: true
    }
  });
  /*
   * Example middleware that returns a 404 for templates
   * as they're already embedded in the js
  hawtio.use('/', function(req, res, next) {
          var path = req.originalUrl;
          // avoid returning these files, they should get pulled from js
          if (s.startsWith(path, '/plugins/') && s.endsWith(path, 'html')) {
            console.log("returning 404 for: ", path);
            res.statusCode = 404;
            res.end();
          } else {
            console.log("allowing: ", path);
            next();
          }
        });
        */
  hawtio.listen(function(server) {
    var host = server.address().address;
    var port = server.address().port;
    console.log("started from gulp file at ", host, ":", port);
  });
});

gulp.task('reload', function() {
  gulp.src('.')
    .pipe(hawtio.reload());
});

gulp.task('build', ['bower', 'path-adjust', 'tslint', 'tsc', 'less', 'template', 'concat', 'clean']);

gulp.task('default', ['connect']);



