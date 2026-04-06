# HTML Parameters Plugin (`html-parameters`)

Custom Jenkins parameter that renders a **sanitized HTML form** and maps submitted values to build variables.

## What it does

- Adds a new build parameter type: **UI HTML Form Parameter**
- Lets you provide a small HTML template with form controls (inputs/selects/textarea/etc.)
- Lets you map extracted values to build variables via a mapping table

## Installation

- Recommended: install from **Jenkins Plugin Manager** once published on `plugins.jenkins.io`
- For development: build locally and upload the generated `.hpi` in **Manage Jenkins → Plugins → Advanced → Upload Plugin**

## Usage (Freestyle / Pipeline job configuration UI)

1. Open job configuration
2. **This project is parameterized**
3. **Add Parameter → UI HTML Form Parameter**
4. Configure:
  - **Name**: parameter name
  - **Template HTML**: sanitized HTML fragment with form elements
  - **Custom CSS**: optional CSS applied only to your parameter markup (see below)
  - **Mappings**: map “source element id” → “output variable name”

## Styling with Jenkins Design Library (current Jenkins / weekly)

On supported Jenkins versions, the [Jenkins Design Library](https://weekly.ci.jenkins.io/design-library/) documents UI building blocks used by Jenkins itself.

- **Classes**: In your HTML template you may use `jenkins-` classes where the Design Library marks them as safe for plugins (see [Stylesheets — Prefixes](https://weekly.ci.jenkins.io/design-library/stylesheets/)). Do **not** use `app-` classes; the library states those are internal and unstable.
- **Colors / themes**: Prefer Jenkins palette classes and **CSS variables** from [Colors](https://weekly.ci.jenkins.io/design-library/colors/) so text and accents follow the user’s theme (including dark theme) instead of hard-coded hex/RGB values.
- **Examples**: Component pages such as [Buttons](https://weekly.ci.jenkins.io/design-library/buttons/) and [Spacing](https://weekly.ci.jenkins.io/design-library/spacing/) show concrete class names (for example `jenkins-button`, `jenkins-button--primary`, `jenkins-!-margin-3`).
- **Native `<select>`**: Jenkins styles the control like other form fields only when you match core’s markup: wrap the element in `<div class="jenkins-select">` and put `class="jenkins-select__input"` on the `<select>` itself (see how `f:select` renders in [`select.jelly`](https://github.com/jenkinsci/jenkins/blob/master/core/src/main/resources/lib/form/select.jelly) and the [Select](https://weekly.ci.jenkins.io/design-library/select) component page). Putting `jenkins-select` on the `<select>` alone usually falls back to the browser’s default appearance.

The parameter markup is wrapped in a root element with class `html-parameters-root`. **Custom CSS** must only use selectors that include the substring `html-parameters-` on every comma-separated selector (so Jenkins chrome outside your form cannot be targeted). Only `@media` at-rules are allowed.

### Light and dark themes (CSS variables)

Jenkins (including bundled styles and user-installed themes such as **Dark Theme**) resolves colors and some layout tokens through **CSS custom properties** on the page. Those values already change when the user switches theme, so in **Custom CSS** you should prefer `var(--token)` over fixed `rgb()` / `#hex` literals. The Design Library [Colors](https://weekly.ci.jenkins.io/design-library/colors/) page recommends using Jenkins-provided colors because they *“will automatically adapt to the user’s theme”*; on a running Jenkins instance that page lists, for each semantic and palette row, the paired **CSS variable** name next to its modifier class.

**How theme switching applies to this parameter**

You do **not** add a separate light/dark toggle inside the HTML for Jenkins’ built-in or plugin-driven themes. When someone changes appearance (**Manage Jenkins → Appearance**, user theme preference, or a plugin such as **Dark Theme**), Jenkins refreshes the global CSS variables. The same `var(--text-color)`, `var(--background)`, and related tokens in your **Custom CSS** then resolve to the new palette the next time **Build with Parameters** is opened. That matches the Design Library guidance on [Colors](https://weekly.ci.jenkins.io/design-library/colors/).

**Typical pattern**

1. Pick variable names from [Colors](https://weekly.ci.jenkins.io/design-library/colors/) (semantic rows such as **Text**, **Secondary text**, **Accent**, etc.) so foreground, background, and borders track the active Jenkins theme automatically.
2. Use them only inside selectors that contain `html-parameters-` (same rule as for any **Custom CSS**).

**Custom CSS example (snippet)**

```css
.html-parameters-panel {
  color: var(--text-color);
  background-color: var(--background);
  border-radius: var(--form-input-border-radius);
}
.html-parameters-panel .html-parameters-hint {
  color: var(--text-color-secondary);
  font-size: var(--font-size-sm);
}
```

Optional markup for the hint line (plugin-prefixed class):

```html
<small class="html-parameters-hint">Uses secondary text color for the active theme.</small>
```

#### Full example: theme-aware form (Declarative Pipeline)

This is a complete job: **template** uses `jenkins-` classes, a **table** for aligned fields, and **CSS-only tabs** (radio + labels + sibling selectors — no scripts). **Custom CSS** uses Jenkins variables so the shell follows light or dark when the user (or theme plugin) switches appearance. After changing theme, open **Build with Parameters** again to see updated colors.

```groovy
pipeline {
  agent any
  parameters {
    uiHtmlFormParameter(
      name: 'UI',
      description: 'Theme-aware layout: tabs + table + grouped build options',
      templateHtml: '''<div class="html-parameters-theme-demo jenkins-!-margin-3">
  <h3 class="html-parameters-theme-demo__title">Deploy bundle</h3>
  <small class="html-parameters-hint">Tabs use hidden radios and labels only (no JavaScript). Values map to separate env vars.</small>
  <div class="html-parameters-tabs jenkins-!-margin-top-2">
    <input class="html-parameters-tabs__radio" type="radio" name="html-parameters-tabstrip" id="html-parameters-tab-general" checked="" />
    <label class="html-parameters-tabs__tab jenkins-button" for="html-parameters-tab-general">General</label>
    <input class="html-parameters-tabs__radio" type="radio" name="html-parameters-tabstrip" id="html-parameters-tab-build" />
    <label class="html-parameters-tabs__tab jenkins-button" for="html-parameters-tab-build">Build</label>
    <input class="html-parameters-tabs__radio" type="radio" name="html-parameters-tabstrip" id="html-parameters-tab-notes" />
    <label class="html-parameters-tabs__tab jenkins-button" for="html-parameters-tab-notes">Notes</label>
    <div class="html-parameters-tab-content html-parameters-tab-content--general">
      <table class="jenkins-table__table html-parameters-data-table">
        <caption class="html-parameters-table-caption">Core settings</caption>
        <thead>
          <tr>
            <th class="jenkins-table__cell" scope="col">Setting</th>
            <th class="jenkins-table__cell" scope="col">Value</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <th class="jenkins-table__cell" scope="row">Branch</th>
            <td class="jenkins-table__cell">
              <input id="html-parameters-branch" class="jenkins-input" type="text" value="main" placeholder="e.g. main" />
            </td>
          </tr>
          <tr>
            <th class="jenkins-table__cell" scope="row">Environment</th>
            <td class="jenkins-table__cell">
              <div class="jenkins-select">
                <select id="html-parameters-env" class="jenkins-select__input">
                  <option value="dev" selected>Development</option>
                  <option value="staging">Staging</option>
                  <option value="prod">Production</option>
                </select>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="html-parameters-tab-content html-parameters-tab-content--build">
      <h4 class="html-parameters-section-title">Resources & safety</h4>
      <table class="jenkins-table__table html-parameters-data-table">
        <tbody>
          <tr>
            <th class="jenkins-table__cell" scope="row">Heap (MB)</th>
            <td class="jenkins-table__cell">
              <input id="html-parameters-heap" class="jenkins-input" type="text" value="512" />
            </td>
          </tr>
          <tr>
            <th class="jenkins-table__cell" scope="row">Parallel jobs</th>
            <td class="jenkins-table__cell">
              <input id="html-parameters-parallel" class="jenkins-input" type="number" min="1" max="32" step="1" value="4" />
            </td>
          </tr>
          <tr>
            <th class="jenkins-table__cell" scope="row">Dry run</th>
            <td class="jenkins-table__cell">
              <input id="html-parameters-dryrun" type="checkbox" />
              <label class="jenkins-form-label jenkins-!-margin-top-0" for="html-parameters-dryrun">Skip publishing artifacts</label>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="html-parameters-tab-content html-parameters-tab-content--notes">
      <div class="jenkins-form-item">
        <label class="jenkins-form-label" for="html-parameters-notes">Change summary</label>
        <textarea id="html-parameters-notes" class="jenkins-input" rows="4" cols="48" placeholder="Optional notes for this run"></textarea>
      </div>
    </div>
  </div>
</div>''',
      customCss: '''.html-parameters-theme-demo {
  color: var(--text-color);
  background-color: var(--background);
  border-radius: var(--form-input-border-radius);
  padding: 0.75rem 1rem;
  border: 1px solid var(--text-color-secondary);
}
.html-parameters-theme-demo .html-parameters-theme-demo__title {
  font-weight: 600;
  margin: 0 0 0.35rem 0;
}
.html-parameters-theme-demo .html-parameters-hint {
  color: var(--text-color-secondary);
  font-size: var(--font-size-sm);
  display: block;
  line-height: 1.4;
}
.html-parameters-theme-demo .html-parameters-tabs__radio {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}
.html-parameters-theme-demo .html-parameters-tabs__tab {
  margin: 0 0.35rem 0.35rem 0;
  display: inline-block;
  opacity: 0.75;
}
.html-parameters-theme-demo #html-parameters-tab-general:checked ~ label[for="html-parameters-tab-general"],
.html-parameters-theme-demo #html-parameters-tab-build:checked ~ label[for="html-parameters-tab-build"],
.html-parameters-theme-demo #html-parameters-tab-notes:checked ~ label[for="html-parameters-tab-notes"] {
  font-weight: 600;
  opacity: 1;
}
.html-parameters-theme-demo .html-parameters-tab-content {
  display: none;
  margin-top: 0.5rem;
}
.html-parameters-theme-demo #html-parameters-tab-general:checked ~ .html-parameters-tab-content--general,
.html-parameters-theme-demo #html-parameters-tab-build:checked ~ .html-parameters-tab-content--build,
.html-parameters-theme-demo #html-parameters-tab-notes:checked ~ .html-parameters-tab-content--notes {
  display: block;
}
.html-parameters-theme-demo .html-parameters-table-caption {
  text-align: left;
  font-weight: 600;
  margin: 0 0 0.35rem 0;
}
.html-parameters-theme-demo .html-parameters-data-table {
  width: 100%;
  border-collapse: collapse;
}
.html-parameters-theme-demo .html-parameters-section-title {
  margin: 0 0 0.5rem 0;
  font-size: var(--font-size-base);
}''',
      mappings: [
        [sourceId: 'html-parameters-branch', outputName: 'UI_BRANCH'],
        [sourceId: 'html-parameters-env', outputName: 'UI_ENV'],
        [sourceId: 'html-parameters-heap', outputName: 'UI_HEAP_MB'],
        [sourceId: 'html-parameters-parallel', outputName: 'UI_PARALLEL'],
        [sourceId: 'html-parameters-dryrun', outputName: 'UI_DRY_RUN'],
        [sourceId: 'html-parameters-notes', outputName: 'UI_NOTES']
      ]
    )
  }
  stages {
    stage('Show') {
      steps {
        echo "UI_BRANCH=${env.UI_BRANCH} UI_ENV=${env.UI_ENV} UI_HEAP_MB=${env.UI_HEAP_MB} UI_PARALLEL=${env.UI_PARALLEL} UI_DRY_RUN=${env.UI_DRY_RUN} UI_NOTES=${env.UI_NOTES}"
      }
    }
  }
}
```

If a variable is missing on an older controller, fall back to a narrower rule set or to the modifier classes from the same **Colors** table (for example classes paired with **Secondary text**). Confirm names on your target Jenkins version using the Design Library **Colors** page.

### Declarative Pipeline (Design Library classes)

Jenkins styles such as `jenkins-button` apply only to elements that carry the matching class. Here the same **tab strip** pattern is reused, but the **Build** tab uses a **definition list** (`<dl>` / `<dt>` / `<dd>`) instead of a second table — a different way to group labels and controls. Add **form** classes from the Design Library (for example [Form inputs](https://weekly.ci.jenkins.io/design-library/inputs/)) or style under a selector that includes `html-parameters-` in **Custom CSS**.

```groovy
pipeline {
  agent any
  parameters {
    uiHtmlFormParameter(
      name: 'UI',
      description: 'Tabs + table + definition list (Design Library classes)',
      templateHtml: '''<div class="html-parameters-panel jenkins-!-margin-3">
  <h3 class="html-parameters-panel__heading">Rollout planner</h3>
  <div class="html-parameters-tabs">
    <input class="html-parameters-tabs__radio" type="radio" name="html-parameters-panel-tabs" id="html-parameters-tab-topo" checked="" />
    <label class="html-parameters-tabs__tab jenkins-button jenkins-button--primary" for="html-parameters-tab-topo">Topology</label>
    <input class="html-parameters-tabs__radio" type="radio" name="html-parameters-panel-tabs" id="html-parameters-tab-approval" />
    <label class="html-parameters-tabs__tab jenkins-button" for="html-parameters-tab-approval">Approval</label>
    <div class="html-parameters-tab-content html-parameters-tab-content--topo">
      <table class="jenkins-table__table html-parameters-rollout-table">
        <caption class="html-parameters-rollout-caption">Where and how many</caption>
        <colgroup>
          <col class="html-parameters-col-key" />
          <col />
        </colgroup>
        <tbody>
          <tr>
            <th class="jenkins-table__cell" scope="row">Region</th>
            <td class="jenkins-table__cell">
              <div class="jenkins-select">
                <select id="html-parameters-region" class="jenkins-select__input">
                  <option value="eu-west-1" selected>EU West (Ireland)</option>
                  <option value="us-east-1">US East (N. Virginia)</option>
                  <option value="ap-south-1">AP South (Mumbai)</option>
                </select>
              </div>
            </td>
          </tr>
          <tr>
            <th class="jenkins-table__cell" scope="row">Replicas</th>
            <td class="jenkins-table__cell">
              <input id="html-parameters-replicas" class="jenkins-input" type="number" min="1" max="20" step="1" value="3" />
            </td>
          </tr>
          <tr>
            <th class="jenkins-table__cell" scope="row">Canary %</th>
            <td class="jenkins-table__cell">
              <input id="html-parameters-canary" class="jenkins-input" type="text" value="10" placeholder="0–100" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="html-parameters-tab-content html-parameters-tab-content--approval">
      <p class="html-parameters-dl-intro jenkins-!-margin-bottom-1">Change metadata (optional).</p>
      <dl class="html-parameters-approval-dl">
        <dt class="html-parameters-approval-dt">Owner email</dt>
        <dd class="html-parameters-approval-dd">
          <input id="html-parameters-owner" class="jenkins-input" type="text" value="team@example.com" />
        </dd>
        <dt class="html-parameters-approval-dt">Ticket ID</dt>
        <dd class="html-parameters-approval-dd">
          <input id="html-parameters-ticket" class="jenkins-input" type="text" value="" placeholder="JIRA-1234" />
        </dd>
        <dt class="html-parameters-approval-dt">Comments</dt>
        <dd class="html-parameters-approval-dd">
          <textarea id="html-parameters-approval-notes" class="jenkins-input" rows="3" cols="40"></textarea>
        </dd>
      </dl>
    </div>
  </div>
  <button type="button" class="jenkins-button jenkins-!-margin-top-2" disabled>Reference control (not mapped)</button>
</div>''',
      customCss: '''.html-parameters-panel {
  border-radius: var(--form-input-border-radius, 4px);
  border: 1px solid var(--text-color-secondary, #ccc);
  padding: 0.75rem 1rem;
}
.html-parameters-panel .html-parameters-panel__heading {
  margin: 0 0 0.5rem 0;
  font-weight: 600;
}
.html-parameters-panel .html-parameters-tabs__radio {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}
.html-parameters-panel .html-parameters-tabs__tab {
  margin: 0 0.35rem 0.35rem 0;
  display: inline-block;
}
.html-parameters-panel #html-parameters-tab-topo:checked ~ .html-parameters-tab-content--topo,
.html-parameters-panel #html-parameters-tab-approval:checked ~ .html-parameters-tab-content--approval {
  display: block;
}
.html-parameters-panel .html-parameters-tab-content {
  display: none;
  margin-top: 0.35rem;
}
.html-parameters-panel .html-parameters-rollout-caption {
  text-align: left;
  font-weight: 600;
  margin: 0 0 0.35rem 0;
}
.html-parameters-panel .html-parameters-rollout-table {
  width: 100%;
  border-collapse: collapse;
}
.html-parameters-panel .html-parameters-approval-dl {
  margin: 0;
  display: grid;
  grid-template-columns: minmax(7rem, 28%) 1fr;
  gap: 0.35rem 0.75rem;
  align-items: center;
}
.html-parameters-panel .html-parameters-approval-dt {
  margin: 0;
  font-weight: 600;
}
.html-parameters-panel .html-parameters-approval-dd {
  margin: 0;
}
@media (max-width: 900px) {
  .html-parameters-panel .html-parameters-approval-dl {
    grid-template-columns: 1fr;
  }
  .html-parameters-panel .jenkins-button {
    width: 100%;
    box-sizing: border-box;
  }
}''',
      mappings: [
        [sourceId: 'html-parameters-region', outputName: 'UI_REGION'],
        [sourceId: 'html-parameters-replicas', outputName: 'UI_REPLICAS'],
        [sourceId: 'html-parameters-canary', outputName: 'UI_CANARY_PCT'],
        [sourceId: 'html-parameters-owner', outputName: 'UI_OWNER'],
        [sourceId: 'html-parameters-ticket', outputName: 'UI_TICKET'],
        [sourceId: 'html-parameters-approval-notes', outputName: 'UI_APPROVAL_NOTES']
      ]
    )
  }
  stages {
    stage('Show') {
      steps {
        echo "UI_REGION=${env.UI_REGION} UI_REPLICAS=${env.UI_REPLICAS} UI_CANARY_PCT=${env.UI_CANARY_PCT} UI_OWNER=${env.UI_OWNER} UI_TICKET=${env.UI_TICKET}"
      }
    }
  }
}
```

Use semantic colors from the [Colors](https://weekly.ci.jenkins.io/design-library/colors/) page (CSS variables and color modifier classes) when you need theme-aware styling.

## Usage (Jenkinsfile)

This plugin provides a `@Symbol("uiHtmlFormParameter")`, so it can be used from Pipeline in one of the following ways (depending on your Pipeline style and Jenkins versions).

### Declarative Pipeline (minimal)

Each mapping `**outputName**` is injected as a build environment variable (for example `env.UI_BRANCH`). The parameter’s `**name**` (`UI`) is what you see under `params.UI` as the structured value (a map keyed by each `outputName`).

Prefer a **distinct** `outputName` (for example `UI_BRANCH`, `DEPLOY_ENV`) rather than very generic names such as `**BRANCH`** or `**PATH**`, which can clash with other Jenkins features, SCM integrations, or folder-level configuration and contribute to confusing build results.

**Pipeline defaults**: Declarative and scripted pipelines rely on `ParameterDefinition.getDefaultParameterValue()` so parameters exist on the build before any UI submit. This plugin implements that by reading defaults from the sanitized template (for example `value="main"` on `<input>`, `checked` on checkboxes, selected/first `<option>` on `<select>`). When **Build with Parameters** posts an empty JSON object `{}` for the hidden field, missing mapping keys are still filled from those same template defaults. If the submitted JSON **includes** a key with an empty string, that explicit empty value is kept.

Use normal HTML quotes inside a Groovy **single-quoted** string; do **not** use backslashes before `"` (that would put `\` into the HTML and break `id` / `for` matching).

This shorter example keeps **two visual groups** (headings + tables) without tabs: deployment targets in one table, runtime toggles in another.

```groovy
pipeline {
  agent any
  parameters {
    uiHtmlFormParameter(
      name: 'UI',
      description: 'Two grouped tables (no tabs)',
      templateHtml: '''<div class="html-parameters-compact jenkins-!-margin-2">
  <h4 class="html-parameters-compact__title">Deployment</h4>
  <table class="jenkins-table__table html-parameters-compact-table">
    <tbody>
      <tr>
        <th class="jenkins-table__cell" scope="row">Branch</th>
        <td class="jenkins-table__cell"><input id="html-parameters-c-branch" class="jenkins-input" type="text" value="main" /></td>
      </tr>
      <tr>
        <th class="jenkins-table__cell" scope="row">Stack</th>
        <td class="jenkins-table__cell">
          <div class="jenkins-select">
            <select id="html-parameters-c-stack" class="jenkins-select__input">
              <option value="blue" selected>Blue</option>
              <option value="green">Green</option>
            </select>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
  <h4 class="html-parameters-compact__title jenkins-!-margin-top-2">Runtime</h4>
  <table class="jenkins-table__table html-parameters-compact-table">
    <tbody>
      <tr>
        <th class="jenkins-table__cell" scope="row">Debug logs</th>
        <td class="jenkins-table__cell">
          <input id="html-parameters-c-debug" type="checkbox" checked />
          <label class="jenkins-form-label jenkins-!-margin-top-0" for="html-parameters-c-debug">Verbose pipeline logs</label>
        </td>
      </tr>
      <tr>
        <th class="jenkins-table__cell" scope="row">Timeout (min)</th>
        <td class="jenkins-table__cell">
          <input id="html-parameters-c-timeout" class="jenkins-input" type="number" min="5" max="180" value="30" />
        </td>
      </tr>
    </tbody>
  </table>
</div>''',
      customCss: '''.html-parameters-compact .html-parameters-compact__title {
  margin: 0 0 0.35rem 0;
  font-weight: 600;
}
.html-parameters-compact .html-parameters-compact-table {
  width: 100%;
  max-width: 36rem;
  border-collapse: collapse;
}''',
      mappings: [
        [sourceId: 'html-parameters-c-branch', outputName: 'UI_BRANCH'],
        [sourceId: 'html-parameters-c-stack', outputName: 'UI_STACK'],
        [sourceId: 'html-parameters-c-debug', outputName: 'UI_DEBUG'],
        [sourceId: 'html-parameters-c-timeout', outputName: 'UI_TIMEOUT_MIN']
      ]
    )
  }
  stages {
    stage('Show') {
      steps {
        echo "UI_BRANCH=${env.UI_BRANCH} UI_STACK=${env.UI_STACK} UI_DEBUG=${env.UI_DEBUG} UI_TIMEOUT_MIN=${env.UI_TIMEOUT_MIN}"
      }
    }
  }
}
```

### Scripted Pipeline

The same **two-table** layout as above, registered with `properties` instead of the Declarative `parameters { }` block:

```groovy
properties([
  parameters([
    uiHtmlFormParameter(
      name: 'UI',
      description: 'Two grouped tables (no tabs)',
      templateHtml: '''<div class="html-parameters-compact jenkins-!-margin-2">
  <h4 class="html-parameters-compact__title">Deployment</h4>
  <table class="jenkins-table__table html-parameters-compact-table">
    <tbody>
      <tr>
        <th class="jenkins-table__cell" scope="row">Branch</th>
        <td class="jenkins-table__cell"><input id="html-parameters-c-branch" class="jenkins-input" type="text" value="main" /></td>
      </tr>
      <tr>
        <th class="jenkins-table__cell" scope="row">Stack</th>
        <td class="jenkins-table__cell">
          <div class="jenkins-select">
            <select id="html-parameters-c-stack" class="jenkins-select__input">
              <option value="blue" selected>Blue</option>
              <option value="green">Green</option>
            </select>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
  <h4 class="html-parameters-compact__title jenkins-!-margin-top-2">Runtime</h4>
  <table class="jenkins-table__table html-parameters-compact-table">
    <tbody>
      <tr>
        <th class="jenkins-table__cell" scope="row">Debug logs</th>
        <td class="jenkins-table__cell">
          <input id="html-parameters-c-debug" type="checkbox" checked />
          <label class="jenkins-form-label jenkins-!-margin-top-0" for="html-parameters-c-debug">Verbose pipeline logs</label>
        </td>
      </tr>
      <tr>
        <th class="jenkins-table__cell" scope="row">Timeout (min)</th>
        <td class="jenkins-table__cell">
          <input id="html-parameters-c-timeout" class="jenkins-input" type="number" min="5" max="180" value="30" />
        </td>
      </tr>
    </tbody>
  </table>
</div>''',
      customCss: '''.html-parameters-compact .html-parameters-compact__title {
  margin: 0 0 0.35rem 0;
  font-weight: 600;
}
.html-parameters-compact .html-parameters-compact-table {
  width: 100%;
  max-width: 36rem;
  border-collapse: collapse;
}''',
      mappings: [
        [sourceId: 'html-parameters-c-branch', outputName: 'UI_BRANCH'],
        [sourceId: 'html-parameters-c-stack', outputName: 'UI_STACK'],
        [sourceId: 'html-parameters-c-debug', outputName: 'UI_DEBUG'],
        [sourceId: 'html-parameters-c-timeout', outputName: 'UI_TIMEOUT_MIN']
      ]
    )
  ])
])

node {
  echo "UI_BRANCH=${env.UI_BRANCH} UI_STACK=${env.UI_STACK} UI_DEBUG=${env.UI_DEBUG} UI_TIMEOUT_MIN=${env.UI_TIMEOUT_MIN}"
}
```

### Declarative Pipeline: `NotSerializableException: HtmlFormMapping`

Declarative Pipeline keeps the parsed `parameters` block in the CPS program state, so nested objects must be Java-serializable. If you see `java.io.NotSerializableException: org.jenkinsci.plugins.uiparameter.HtmlFormMapping` in the console (often right after a stage, when the workflow saves), upgrade to a plugin build that marks `HtmlFormMapping` as `Serializable` (or use **Scripted Pipeline** with `properties([parameters([...])])`, which does not embed that model in the same way).

## Trying newer Jenkins UI surfaces

Jenkins core is evolving toward dialog-based **Build with parameters** and refreshed job/build pages on recent releases. After installing a **weekly** Jenkins build, sign in as your user and open **Manage Jenkins → Users → *your user*** (or the user preferences area your version exposes). Under **Experimental flags** / **Experiments**, enable options for the **new Job page** and **new Build page** UI (exact labels depend on the Jenkins version), then run through **Build with Parameters** for jobs that use this parameter to confirm layout and styling.

## Security model

This plugin accepts HTML/CSS, therefore it must defend against XSS and related injection risks.

- **HTML sanitization**: input `templateHtml` is sanitized with jsoup `Safelist.relaxed()` plus a limited set of form-related tags and attributes.
- **No scripts / event handlers**: unsafe tags and inline event handlers are stripped during sanitization.
- **No inline styles in HTML**: inline `style` attributes are removed from `templateHtml`. Use `customCss` instead.
- **Jelly defaults**: UI templates use `escape-by-default='true'`.
- **CSS sanitization**: `customCss` is stripped of characters that could break out of `<style>` blocks, then validated so selectors only match markup containing `html-parameters-` (your template or the plugin wrapper). Only `@media` at-rules are allowed.

### ⚠️ Important warning about Custom CSS

Even with selector scoping, **only trusted users** should receive **Job/Configure** permission for jobs that use this parameter. Misconfigured HTML/CSS can still confuse users or weaken clarity of the parameters UI.

### Required prefixes for `id` and `class` in `templateHtml`

- Every element `**id`** must start with `html-parameters-`.
- Every CSS `**class**` token must start with `html-parameters-` **or** `jenkins-` (see [Design Library — Stylesheets](https://weekly.ci.jenkins.io/design-library/stylesheets/)). Do not use `app-` classes.

Configuration is rejected if these rules are violated.

Even with sanitization, do not grant untrusted users the permission to configure jobs with arbitrary HTML/CSS.

## Compatibility

- **Jenkins baseline**: see `pom.xml` (`jenkins.version`)

## License

MIT. See `[LICENSE](LICENSE)`.