/*
 * Binds UI HTML form parameter fields to the hidden JSON value on form submit / input.
 */
(function () {
  'use strict';

  function readValueById(id) {
    if (!id) {
      return '';
    }
    const el = document.getElementById(id);
    if (!el) {
      return '';
    }

    const tag = (el.tagName || '').toLowerCase();
    if (tag === 'input') {
      const t = (el.type || '').toLowerCase();
      if (t === 'checkbox') {
        return el.checked ? 'true' : 'false';
      }
      if (t === 'radio') {
        return el.checked ? (el.value || 'on') : '';
      }
      return el.value || '';
    }
    if (tag === 'select' || tag === 'textarea') {
      return el.value || '';
    }
    return (el.textContent || '').trim();
  }

  function fillHiddenInputs(root) {
    const jsonInput = root.querySelector('input.uiparameter-json[name="value"]');
    if (!jsonInput) {
      return;
    }

    const values = {};
    const mappings = root.querySelectorAll('[data-uiparam-output][data-uiparam-source]');
    for (const node of mappings) {
      const outputName = node.getAttribute('data-uiparam-output');
      const sourceId = node.getAttribute('data-uiparam-source');
      if (!outputName) {
        continue;
      }
      values[outputName] = readValueById(sourceId);
    }
    jsonInput.value = JSON.stringify(values);
  }

  function init() {
    if (window.__uiparameterBindingInit) {
      return;
    }
    window.__uiparameterBindingInit = true;

    const refreshAll = () => {
      const currentRoots = document.querySelectorAll('.uiparameter-root');
      for (const r of currentRoots) {
        fillHiddenInputs(r);
      }
    };

    refreshAll();

    /* Delays: parameter UI may mount after DOMContentLoaded (dialog / React). */
    setTimeout(refreshAll, 0);
    setTimeout(refreshAll, 300);
    setTimeout(refreshAll, 1000);

    /*
     * Do not require a wrapping <form>. Newer Jenkins UIs may render "Build with parameters"
     * outside a traditional form; previously we skipped binding and left value="{}", so env
     * injection saw empty mappings.
     */
    document.addEventListener(
      'submit',
      function () {
        refreshAll();
      },
      true
    );

    document.addEventListener(
      'input',
      function (e) {
        if (e.target && e.target.closest && e.target.closest('.uiparameter-root')) {
          refreshAll();
        }
      },
      true
    );

    document.addEventListener(
      'change',
      function (e) {
        if (e.target && e.target.closest && e.target.closest('.uiparameter-root')) {
          refreshAll();
        }
      },
      true
    );

    /*
     * Capture clicks so we refresh before actions that might not fire submit (e.g. some
     * programmatic flows) or when the user hits Build in a dialog.
     */
    document.addEventListener(
      'click',
      function () {
        refreshAll();
      },
      true
    );
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
