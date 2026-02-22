(function () {
  if (window.__hdimSharedNavLoaded) {
    return;
  }
  window.__hdimSharedNavLoaded = true;

  function executeEmbeddedScripts(root) {
    var scripts = root.querySelectorAll('script');
    scripts.forEach(function (oldScript) {
      var newScript = document.createElement('script');
      if (oldScript.src) {
        newScript.src = oldScript.src;
      }
      Array.prototype.forEach.call(oldScript.attributes, function (attr) {
        if (attr.name !== 'src') {
          newScript.setAttribute(attr.name, attr.value);
        }
      });
      newScript.textContent = oldScript.textContent;
      oldScript.parentNode.replaceChild(newScript, oldScript);
    });
  }

  function injectSharedNav(html) {
    var mount = document.getElementById('hdim-shared-nav-root');
    if (!mount) {
      return;
    }
    var wrapper = document.createElement('div');
    wrapper.innerHTML = html;
    while (wrapper.firstChild) {
      mount.appendChild(wrapper.firstChild);
    }
    executeEmbeddedScripts(mount);
  }

  fetch('/shared-nav.html', { credentials: 'same-origin' })
    .then(function (response) {
      if (!response.ok) {
        throw new Error('Failed to load shared-nav.html: ' + response.status);
      }
      return response.text();
    })
    .then(injectSharedNav)
    .catch(function (err) {
      console.error('[hdim-nav] loader error', err);
    });
})();
