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
   - **Custom CSS**: optional CSS applied to the rendered form
   - **Mappings**: map “source element id” → “output variable name”

## Usage (Jenkinsfile)

This plugin provides a `@Symbol("uiHtmlFormParameter")`, so it can be used from Pipeline in one of the following ways (depending on your Pipeline style and Jenkins versions).

### Declarative Pipeline

```groovy
pipeline {
  agent any
  parameters {
    uiHtmlFormParameter(
      name: 'UI',
      description: 'Custom HTML form',
      templateHtml: '<label for=\"html-parameters-branch\">Branch</label><input id=\"html-parameters-branch\" value=\"main\" />',
      customCss: '.myForm { margin: 0; }',
      mappings: [
        [sourceId: 'html-parameters-branch', outputName: 'BRANCH']
      ]
    )
  }
  stages {
    stage('Show') {
      steps {
        echo \"BRANCH=${env.BRANCH}\"
      }
    }
  }
}
```

### Scripted Pipeline

```groovy
properties([
  parameters([
    [$class: 'org.jenkinsci.plugins.uiparameter.HtmlFormParameterDefinition',
      name: 'UI',
      description: 'Custom HTML form',
      templateHtml: '<label for=\"html-parameters-branch\">Branch</label><input id=\"html-parameters-branch\" value=\"main\" />',
      mappings: [[sourceId: 'html-parameters-branch', outputName: 'BRANCH']]
    ]
  ])
])

node {
  echo \"BRANCH=${env.BRANCH}\"
}
```

## Security model

This plugin accepts HTML/CSS, therefore it must defend against XSS and related injection risks.

- **HTML sanitization**: input `templateHtml` is sanitized with jsoup `Safelist.relaxed()` plus a limited set of form-related tags and attributes.
- **No scripts / event handlers**: unsafe tags and inline event handlers are stripped during sanitization.
- **No inline styles in HTML**: inline `style` attributes are removed from `templateHtml`. Use `customCss` instead.
- **Jelly defaults**: UI templates use `escape-by-default='true'`.
- **CSS sanitization**: `customCss` is sanitized defensively to prevent breaking out of `<style>` blocks.

### ⚠️ Important warning about Custom CSS

`customCss` is applied on the Jenkins “Build with Parameters” page. Even if it is sanitized to prevent breaking out of the `<style>` element, **CSS can still interfere with the Jenkins UI** in subtle ways.

In particular, a user with **Job/Configure** permission can craft CSS that:

- hides or repositions UI elements
- makes dialogs/buttons look like something else
- causes administrators (or other users) to take unintended actions

Only allow trusted users to configure jobs using this parameter type. This plugin is **not** suitable for environments where job configuration is delegated to untrusted users.

### Required prefixes for `id` and `class`

To avoid interfering with Jenkins UI (where ids and classes are used for styling and attaching behaviors), this plugin requires:

- Every HTML element `id` in `templateHtml` must start with `html-parameters-`
- Every CSS class token in `templateHtml` must start with `html-parameters-`

Configuration is rejected if these rules are violated.

Even with sanitization, do not grant untrusted users the permission to configure jobs with arbitrary HTML/CSS.

## Compatibility

- **Jenkins baseline**: see `pom.xml` (`jenkins.version`)

## License

MIT. See [`LICENSE`](LICENSE).
