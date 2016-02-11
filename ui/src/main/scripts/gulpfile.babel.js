/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
import yargs from 'yargs';
import concat from 'gulp-concat';
import del from 'del';
import eventStream from 'event-stream';
import fs from 'fs';
import gulp from 'gulp';
import gulpLoadPlugins from 'gulp-load-plugins';
import less from 'gulp-less';
import map from 'vinyl-map';
import path from 'path';
import s from 'underscore.string';
import size from 'gulp-size';
import sourcemaps from 'gulp-sourcemaps';
import typescript from 'gulp-typescript';
import tslint from 'gulp-tslint';
import wiredeps from 'wiredep';
import xml2js from 'xml2js';
import protractor from 'gulp-protractor';

const argv = yargs.argv;
const webdriver = protractor.webdriver_update;
const wiredep = wiredeps.stream;

let jsString;
let inProgress = false;

// CONSTANTS

const POM_MAIN_PATH = '../../../pom.xml';
const DIST_TARGET_PATH = '../../../dist/target/';
const WF_CONSOLE_PATH = 'modules/org/hawkular/nest/main/deployments/hawkular-btm-ui.war/dist/';

const plugins = gulpLoadPlugins({
  rename: {
    'gulp-if-else': 'gulpif'
  }
});

const pkg = require('./package.json');
const normalSizeOptions = {
  showFiles: true
}, gZippedSizeOptions = {
  showFiles: true,
  gzip: true
};

const testConfig = {
  ts: ['test/**/.*ts'],
  specJs: pkg.name + '.spec.js',
  e2eRoot: 'test/e2e',
  e2dist: '.protractor',
  configFile: 'protractor.conf.js',
  srcPrefix: '../../src/main/',
  e2eCopySource: 'test/**/*',
  e2eCopyTarget: './test'
};

const config = {
  main: '.',
  ts: ['plugins/**/*.ts'],
  templates: ['plugins/**/*.html'],
  templateModule: pkg.name + '-templates',
  dist: './dist/',
  js: pkg.name + '.js',
  testJs: pkg.name + '.spec.js',
  tsProject: typescript.createProject({
    target: 'ES5',
    module: 'commonjs',
    declarationFiles: true,
    noExternalResolve: false
  }),
  srcPrefix: '../../src/main/scripts/',
  serverPath: DIST_TARGET_PATH + 'hawkular-btm-ui-1.0.0-SNAPSHOT/' + WF_CONSOLE_PATH
};

gulp.task('set-server-path', (done) => {
  const parser = new xml2js.Parser();
parser.addListener('end', (result) => {
  config.serverPath = DIST_TARGET_PATH + 'hawkular-btm-dist-' + result.project.version + '/' + WF_CONSOLE_PATH;
done();
});

fs.readFile(POM_MAIN_PATH, 'utf8', (err, xmlString) => {
  if (err) throw err;
parser.parseString(xmlString);
});
});

gulp.task('wiredep', () => {
  const cacheBuster = Date.now();

gulp.src('*.html')
  .pipe(wiredep({
    fileTypes: {
      html: {
        replace: {
          js: '<script src="{{filePath}}?v=' + cacheBuster + '"></script>',
          css: '<link rel="stylesheet" href="{{filePath}}?v=' + cacheBuster + '" />'
        }
      }
    }
  }))
  .pipe(gulp.dest('.'));
});


/** Adjust the reference path of any typescript-built plugin this project depends on
 * the hawt.io projects need this */
gulp.task('path-adjust', () => {
  gulp.src('libs/**/includes.d.ts')
  .pipe(map((buf, filename) => {
      const textContent = buf.toString();
const newTextContent = textContent.replace(/"\.\.\/libs/gm, '"../../../libs');
// console.log("Filename: ", filename, " old: ", textContent, " new:", newTextContent);
return newTextContent;
}))
.pipe(gulp.dest('libs'));
gulp.src('libs/**/hawkRest.d.ts')
  .pipe(map((buf, filename) => {
      const textContent = buf.toString();
const newTextContent = textContent.replace(/\.\.\/lib\//gm, '../../../libs/');
return newTextContent;
}))
.pipe(gulp.dest('libs'));
});

gulp.task('clean-defs', () => {
  del(['defs.d.ts/**']);
});

gulp.task('git-sha', (cb) => {
  const versionFile = 'version.js';

if (!jsString) {
  plugins.git.exec({args: "log -n 1 --pretty='%h'"}, (err, stdout) => {
    if (err) throw err;
  const gitSha = stdout.slice(0, -1);
  jsString = 'var HawkularVersion = \"' + gitSha + '\";';
  fs.writeFileSync(versionFile, jsString);
  cb();
});
} else {
  fs.writeFileSync(versionFile, jsString);
  cb();
}
});

const gulpTsc = function (done, filesDest, filesGlob) {
  const cwd = process.cwd();
  const tsResult = gulp.src(filesGlob)
    .pipe(sourcemaps.init())
    .pipe(typescript(config.tsProject))
    .on('error', plugins.notify.onError({
      message: '<%= error.message %>',
      title: 'Typescript compilation error'
    }));

  return eventStream.merge(
    tsResult.js
      .pipe(concat('compiled.js'))
      .pipe(sourcemaps.write())
      .pipe(gulp.dest(filesDest)),
    tsResult.dts
      .pipe(gulp.dest('d.ts')))
    .pipe(map((buf, filename) => {
        if (!s.endsWith(filename, 'd.ts')) {
    return buf;
  }
  const relative = path.relative(cwd, filename);
  fs.appendFileSync('defs.d.ts', '/// <reference path="' + relative + '"/>\n');
  return buf;
})).on('end', () => {
    done && done();
});
};

gulp.task('tsc', ['clean-defs'], (done) => {
  gulpTsc(done, '.', config.ts);
});

gulp.task('tsc-live', ['copy-sources', 'clean-defs'], (done) => {
  gulpTsc(done, '.', config.ts);
});

gulp.task('tslint', () => {
  gulp.src(config.ts)
  .pipe(tslint())
  .pipe(tslint.report('verbose'));
});

gulp.task('tslint-watch', ['copy-sources'], () => {
  gulp.src(config.ts)
  .pipe(tslint())
  .pipe(tslint.report('prose', {
    emitError: false
  }));
});

const gulpTemplate = function () {
  return gulp.src(config.templates)
    .pipe(plugins.angularTemplatecache({
      filename: 'templates.js',
      root: '',
      base: function (file) {
        const filename = /[^/]*$/.exec(file.relative).input;
        return 'plugins/' + filename;
      },
      standalone: true,
      module: config.templateModule,
      templateFooter: '}]); hawtioPluginLoader.addModule("' + config.templateModule + '");'
    }))
    .pipe(gulp.dest('.'));
};

gulp.task('template', ['tsc'], () => {
  return gulpTemplate();
});

gulp.task('template-live', ['tsc-live', 'copy-sources'], () => {
  return gulpTemplate();
});

const gulpLess = function (done) {
  gulp.src(['plugins/**/*.less'])
    .pipe(less())
    .pipe(concat('hawkularbtm.css'))
    .pipe(gulp.dest(config.dist))
    .on('end', () => {
    done && done();
});
};

gulp.task('less', () => {
  gulpLess();
});

gulp.task('less-live', ['copy-sources'], (done) => {
  gulpLess(done);
});

const gulpConcat = function (filesRoot, filesDist, config) {
  const gZipSize = size(gZippedSizeOptions);

  return gulp.src([filesRoot + '/compiled.js', filesRoot + '/templates.js', filesRoot + '/version.js'])
    .pipe(sourcemaps.init({loadMaps: true}))
    .pipe(plugins.ngAnnotate())
    .pipe(concat(config))
    .pipe(sourcemaps.write())
    .pipe(plugins.gulpif(argv.production, plugins.uglify))
    .pipe(gulp.dest(filesDist))
    .pipe(size(normalSizeOptions))
    .pipe(gZipSize);
};

gulp.task('protractor-clean', () => {
  return del([testConfig.e2dist]);
});

gulp.task('protractor-ts-spec', ['copy-tests'], (done) => {
  var filesRoot = testConfig.e2eRoot;
  var filesGlob = [
  `${filesRoot}/**/*.ts`
  ];

  gulpTsc(done, filesRoot, filesGlob);
});

gulp.task('protractor-update', (done) => {
  webdriver({}, done);
});

gulp.task('protractor-concat', ['protractor-ts-spec', 'protractor-update'], () => {
  return gulpConcat(testConfig.e2eRoot, testConfig.e2dist + '/' + testConfig.e2eRoot, config.testJs);
});

gulp.task('protractor-run', ['protractor-concat'], () => {
  return gulp.src(testConfig.e2dist + '/' + testConfig.e2eRoot + '/**/*.spec.js')
    .pipe(plugins.protractor.protractor({
      configFile: testConfig.e2eRoot + '/' + testConfig.configFile
    }))
    .on('error', e => { throw e });
});

gulp.task('concat', ['template', 'tsc', 'git-sha'], () => {
  return gulpConcat('.', config.dist, config.js);
});

gulp.task('concat-live', ['template-live', 'tsc-live', 'git-sha'], () => {
  return gulpConcat('.', config.dist, config.js);
});

gulp.task('clean', ['concat'], () => {
  del(['templates.js', 'compiled.js']);
});

gulp.task('watch-server', ['build-live', 'copy-kettle-js', 'copy-kettle-css'], () => {
  plugins.watch([config.srcPrefix + 'plugins/**/*.ts', config.srcPrefix + '/plugins/**/*.html'], () => {
  gulp.start('copy-kettle-js');
});

plugins.watch([config.srcPrefix + '/plugins/**/*.less'], () => {
  gulp.start('copy-kettle-css');
});
});

gulp.task('clean-sources', (done) => {
  if (!inProgress) {
  inProgress = true;
  del(['./plugins/**/*.ts', './plugins/**/*.less', './plugins/**/*.html']).then(done());
}
else {
  done();
}
});

gulp.task('copy-tests', (done) => {
  gulp.src([testConfig.srcPrefix + testConfig.e2eCopySource])
      .pipe(gulp.dest(testConfig.e2eCopyTarget)).on('end', () => {
        done();
      });
});

gulp.task('copy-sources', ['clean-sources'], (done) => {
  const src = [config.srcPrefix + 'plugins/**/*'];

gulp.src(src)
  .pipe(gulp.dest('./plugins')).on('end', () => {
  done();
});
});

gulp.task('copy-kettle-js', ['build-live', 'set-server-path'], () => {
  inProgress = false;
gulp.src(['dist/hawkularbtm.js'])
  .pipe(gulp.dest(config.serverPath));
});

gulp.task('copy-vendor-fonts', (done) => {
  const src = [config.srcPrefix + 'plugins/**/vendor/fonts/*.*'];

gulp.src(src)
  .pipe(gulp.dest(config.dist)).on('end', () => {
  done();
});
});

gulp.task('copy-vendor-js', (done) => {
  const src = [config.srcPrefix + 'plugins/**/vendor/**/*.js'];

gulp.src(src)
  .pipe(gulp.dest(config.dist)).on('end', () => {
  done();
});
});

gulp.task('copy-vendor-css', (done) => {
  const src = [config.srcPrefix + 'plugins/**/vendor/**/*.css'];

gulp.src(src)
  .pipe(gulp.dest(config.dist)).on('end', () => {
  done();
});
});

gulp.task('copy-kettle-css', ['less-live', 'set-server-path'], () => {
  inProgress = false;
gulp.src(['dist/hawkularbtm.css'])
  .pipe(gulp.dest(config.serverPath));
});

gulp.task('build', ['wiredep', 'path-adjust', 'tslint', 'tsc', 'less', 'template', 'concat', 'copy-vendor-js', 'copy-vendor-css', 'copy-vendor-fonts', 'clean']);
gulp.task('build-live', ['copy-sources', 'wiredep', 'path-adjust', 'tslint-watch', 'tsc-live', 'less-live', 'template-live', 'concat-live', 'copy-vendor-js', 'copy-vendor-css', 'copy-vendor-fonts']);
gulp.task('test-e2e', ['copy-tests', 'protractor-ts-spec', 'protractor-update', 'protractor-concat', 'protractor-run']);
