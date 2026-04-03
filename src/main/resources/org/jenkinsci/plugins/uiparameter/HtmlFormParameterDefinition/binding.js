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
    const roots = document.querySelectorAll('.uiparameter-root');
    if (!roots || roots.length === 0) {
      return;
    }

    for (const root of roots) {
      const jsonInput = root.querySelector('input.uiparameter-json[name="value"]');
      if (!jsonInput) {
        continue;
      }

      const form = jsonInput.closest('form') || document.querySelector('form');
      if (!form) {
        continue;
      }

      fillHiddenInputs(root);

      const refreshAll = () => {
        for (const r of roots) {
          fillHiddenInputs(r);
        }
      };

      form.addEventListener('submit', refreshAll, true);
      form.addEventListener('input', refreshAll, true);
      form.addEventListener('change', refreshAll, true);
      break;
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
